package com.example.Escolar.Service;

import com.example.Escolar.Exception.ResourceNotFoundException;
import com.example.Escolar.Model.Acceso;
import com.example.Escolar.Model.Grado;
import com.example.Escolar.Model.Nivel;
import com.example.Escolar.Repository.AccesoRepository;
import com.example.Escolar.Repository.GradoRepository;
import com.example.Escolar.Repository.NivelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeSet;

/**
 * Orden de niveles y grados. Lo asigna el sistema y solo se mueve con subir o
 * bajar, así nunca hay repetidos ni huecos. Un mismo grado puede tener varias
 * filas (una por turno); todas comparten su orden y se mueven juntas.
 */
@Service
@RequiredArgsConstructor
public class OrdenAcademicoService {

    /** Grado dentro de la secuencia del colegio: nivel y orden dentro del nivel. */
    public record Posicion(Integer idNivel, Integer orden) {
    }

    public static final Comparator<Nivel> POR_ORDEN_NIVEL =
            Comparator.comparing(Nivel::getOrden, Comparator.nullsLast(Comparator.naturalOrder()))
                    .thenComparing(Nivel::getIdNivel);

    public static final Comparator<Grado> POR_ORDEN_GRADO =
            Comparator.comparing((Grado g) -> g.getNivel(), POR_ORDEN_NIVEL)
                    .thenComparing(Grado::getOrden, Comparator.nullsLast(Comparator.naturalOrder()))
                    .thenComparing(Grado::getIdGrado);

    private final NivelRepository nivelRepository;
    private final GradoRepository gradoRepository;
    private final AccesoRepository accesoRepository;

    /** Datos previos al campo orden: se ordenan por id y luego se corrigen con las flechas. */
    @Transactional
    public void inicializar() {
        List<Nivel> niveles = nivelRepository.findByAccesoNot(eliminado()).stream()
                .sorted(Comparator.comparing(Nivel::getIdNivel)).toList();
        int maxNivel = niveles.stream().map(Nivel::getOrden).filter(Objects::nonNull).max(Integer::compare).orElse(0);
        for (Nivel nivel : niveles) {
            if (nivel.getOrden() == null) {
                nivel.setOrden(++maxNivel);
                nivelRepository.save(nivel);
            }
        }
        for (Nivel nivel : niveles) {
            List<Grado> grados = gradoRepository.findByNivelAndAccesoNot(nivel, eliminado()).stream()
                    .sorted(Comparator.comparing(Grado::getIdGrado)).toList();
            for (Grado grado : grados) {
                if (grado.getOrden() == null) {
                    grado.setOrden(ordenParaNuevo(grado.getNombre(), nivel, grado.getIdGrado()));
                    gradoRepository.save(grado);
                }
            }
        }
    }

    /** Un grado con el mismo nombre y nivel comparte orden; si no, va al final del nivel. */
    public int ordenParaNuevo(String nombre, Nivel nivel, Integer idExcluir) {
        List<Grado> grados = gradoRepository.findByNivelAndAccesoNot(nivel, eliminado()).stream()
                .filter(g -> !g.getIdGrado().equals(idExcluir))
                .filter(g -> g.getOrden() != null)
                .toList();
        return grados.stream()
                .filter(g -> g.getNombre().trim().equalsIgnoreCase(nombre.trim()))
                .map(Grado::getOrden)
                .findFirst()
                .orElseGet(() -> grados.stream().map(Grado::getOrden).max(Integer::compare).orElse(0) + 1);
    }

    /** Quita los huecos que deja un grado eliminado o movido de nivel. */
    @Transactional
    public void compactar(Nivel nivel) {
        List<Grado> grados = gradoRepository.findByNivelAndAccesoNot(nivel, eliminado());
        List<Integer> ordenes = new TreeSet<>(grados.stream().map(Grado::getOrden).filter(Objects::nonNull).toList())
                .stream().toList();
        for (Grado grado : grados) {
            if (grado.getOrden() == null) {
                continue;
            }
            int nuevo = ordenes.indexOf(grado.getOrden()) + 1;
            if (nuevo != grado.getOrden()) {
                grado.setOrden(nuevo);
                gradoRepository.save(grado);
            }
        }
    }

    @Transactional
    public void moverGrado(Integer idGrado, boolean subir) {
        Grado grado = gradoRepository.findByIdGradoAndAccesoNot(idGrado, eliminado())
                .orElseThrow(() -> new ResourceNotFoundException("Grado no encontrado con id " + idGrado));
        int actual = grado.getOrden();
        int destino = subir ? actual - 1 : actual + 1;
        List<Grado> delNivel = gradoRepository.findByNivelAndAccesoNot(grado.getNivel(), eliminado());
        List<Grado> vecinos = delNivel.stream().filter(g -> Objects.equals(g.getOrden(), destino)).toList();
        if (vecinos.isEmpty()) {
            throw new IllegalArgumentException(subir
                    ? "El grado ya es el primero de su nivel"
                    : "El grado ya es el último de su nivel");
        }
        delNivel.stream().filter(g -> Objects.equals(g.getOrden(), actual)).forEach(g -> g.setOrden(destino));
        vecinos.forEach(g -> g.setOrden(actual));
        gradoRepository.saveAll(delNivel);
    }

    @Transactional
    public void moverNivel(Integer idNivel, boolean subir) {
        Nivel nivel = nivelRepository.findByIdNivelAndAccesoNot(idNivel, eliminado())
                .orElseThrow(() -> new ResourceNotFoundException("Nivel no encontrado con id " + idNivel));
        int actual = nivel.getOrden();
        int destino = subir ? actual - 1 : actual + 1;
        Nivel vecino = nivelRepository.findByAccesoNot(eliminado()).stream()
                .filter(n -> Objects.equals(n.getOrden(), destino))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(subir
                        ? "El nivel ya es el primero"
                        : "El nivel ya es el último"));
        nivel.setOrden(destino);
        vecino.setOrden(actual);
        nivelRepository.saveAll(List.of(nivel, vecino));
    }

    public Posicion posicion(Grado grado) {
        return new Posicion(grado.getNivel().getIdNivel(), grado.getOrden());
    }

    /** Grado siguiente: el de orden inmediato en el nivel o, si es el último, el primero del nivel siguiente. */
    public Optional<Posicion> siguiente(Grado grado) {
        Acceso eliminado = eliminado();
        Nivel nivel = grado.getNivel();
        boolean hayEnElNivel = gradoRepository.findByNivelAndAccesoNot(nivel, eliminado).stream()
                .anyMatch(g -> Objects.equals(g.getOrden(), grado.getOrden() + 1));
        if (hayEnElNivel) {
            return Optional.of(new Posicion(nivel.getIdNivel(), grado.getOrden() + 1));
        }
        return nivelRepository.findByAccesoNot(eliminado).stream()
                .filter(n -> n.getOrden() != null && n.getOrden() > nivel.getOrden())
                .sorted(POR_ORDEN_NIVEL)
                .flatMap(n -> gradoRepository.findByNivelAndAccesoNot(n, eliminado).stream()
                        .map(Grado::getOrden)
                        .filter(Objects::nonNull)
                        .min(Integer::compare)
                        .map(orden -> new Posicion(n.getIdNivel(), orden))
                        .stream())
                .findFirst();
    }

    private Acceso eliminado() {
        return accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow();
    }
}
