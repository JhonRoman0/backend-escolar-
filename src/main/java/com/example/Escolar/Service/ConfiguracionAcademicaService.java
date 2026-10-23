package com.example.Escolar.Service;

import com.example.Escolar.Dto.ConfiguracionAcademicaRequest;
import com.example.Escolar.Dto.ConfiguracionAcademicaResponse;
import com.example.Escolar.Exception.ResourceNotFoundException;
import com.example.Escolar.Model.Acceso;
import com.example.Escolar.Model.AnioEscolar;
import com.example.Escolar.Model.ConfiguracionAcademica;
import com.example.Escolar.Model.Nivel;
import com.example.Escolar.Repository.AccesoRepository;
import com.example.Escolar.Repository.AnioEscolarRepository;
import com.example.Escolar.Repository.AsignacionRepository;
import com.example.Escolar.Repository.ConfiguracionAcademicaRepository;
import com.example.Escolar.Repository.NivelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Duracion de la hora academica por año y nivel. Un año sin configuracion
 * guardada hereda la del año anterior de ese nivel, y si no hay ninguna usa
 * los valores de siempre: inicial libre, primaria y secundaria 45 minutos.
 */
@Service
@RequiredArgsConstructor
public class ConfiguracionAcademicaService {

    /** Hora pedagogica del MINEDU para primaria y secundaria. */
    public static final int MINUTOS_POR_DEFECTO = 45;
    public static final int MINUTOS_MINIMO = 30;
    public static final int MINUTOS_MAXIMO = 60;

    private final ConfiguracionAcademicaRepository configuracionRepository;
    private final AnioEscolarRepository anioEscolarRepository;
    private final NivelRepository nivelRepository;
    private final AsignacionRepository asignacionRepository;
    private final AccesoRepository accesoRepository;

    /** Una fila por nivel, con la duracion vigente y de donde sale. */
    @Transactional(readOnly = true)
    public List<ConfiguracionAcademicaResponse> listar(Integer idAnio) {
        AnioEscolar anio = findAnio(idAnio);
        return nivelRepository.findByAccesoNot(accesoEliminado()).stream()
                .sorted(Comparator.comparing(Nivel::getIdNivel))
                .map(nivel -> resolver(anio, nivel))
                .toList();
    }

    @Transactional(readOnly = true)
    public ConfiguracionAcademicaResponse obtener(Integer idAnio, Integer idNivel) {
        return resolver(findAnio(idAnio), findNivel(idNivel));
    }

    /** Minutos de la hora academica para validar horarios; null: libre. */
    @Transactional(readOnly = true)
    public Integer minutosHoraAcademica(AnioEscolar anio, Nivel nivel) {
        return resolver(anio, nivel).getMinutosHoraAcademica();
    }

    /**
     * Solo en un año por comenzar, y no si el nivel ya tiene asignaciones: sus
     * horarios se armaron con la duracion anterior y quedarian invalidos.
     */
    @Transactional
    public ConfiguracionAcademicaResponse guardar(ConfiguracionAcademicaRequest request) {
        AnioEscolar anio = findAnio(request.getIdAnio());
        Nivel nivel = findNivel(request.getIdNivel());
        if (anio.getEstado() != AnioEscolarService.ESTADO_POR_COMENZAR) {
            throw new IllegalArgumentException("El año " + anio.getAnio() + " está "
                    + (anio.getEstado() == AnioEscolarService.ESTADO_VIGENTE ? "vigente" : "cerrado")
                    + ": su configuración académica ya no se puede cambiar");
        }
        Integer minutos = request.getMinutosHoraAcademica();
        if (minutos != null && (minutos < MINUTOS_MINIMO || minutos > MINUTOS_MAXIMO || minutos % 5 != 0)) {
            throw new IllegalArgumentException("La hora académica debe durar entre " + MINUTOS_MINIMO + " y "
                    + MINUTOS_MAXIMO + " minutos, de 5 en 5");
        }

        ConfiguracionAcademicaResponse actual = resolver(anio, nivel);
        if (!Objects.equals(actual.getMinutosHoraAcademica(), minutos) && actual.getAsignaciones() > 0) {
            throw new IllegalArgumentException(nivel.getNombre() + " ya tiene " + actual.getAsignaciones()
                    + (actual.getAsignaciones() == 1 ? " asignación" : " asignaciones")
                    + " en el año " + anio.getAnio() + ": sus horarios se armaron con la duración actual");
        }

        ConfiguracionAcademica configuracion = configuracionRepository
                .findByAnioEscolarIdAnioAndNivelIdNivelAndAccesoNot(anio.getIdAnio(), nivel.getIdNivel(), accesoEliminado())
                .orElseGet(() -> {
                    ConfiguracionAcademica nueva = new ConfiguracionAcademica();
                    nueva.setAnioEscolar(anio);
                    nueva.setNivel(nivel);
                    nueva.setAcceso(accesoRepository.findById(AccesoConstants.ACTIVO).orElseThrow());
                    return nueva;
                });
        configuracion.setMinutosHoraAcademica(minutos);
        configuracionRepository.save(configuracion);
        return resolver(anio, nivel);
    }

    private ConfiguracionAcademicaResponse resolver(AnioEscolar anio, Nivel nivel) {
        Acceso eliminado = accesoEliminado();
        ConfiguracionAcademicaResponse response = new ConfiguracionAcademicaResponse();
        response.setIdAnio(anio.getIdAnio());
        response.setAnio(anio.getAnio());
        response.setIdNivel(nivel.getIdNivel());
        response.setNivel(nivel.getNombre());
        response.setAsignaciones(asignacionRepository.countByAnioEscolarIdAnioAndGradoSeccionGradoNivelIdNivelAndAccesoNot(
                anio.getIdAnio(), nivel.getIdNivel(), eliminado));

        Optional<ConfiguracionAcademica> propia = configuracionRepository
                .findByAnioEscolarIdAnioAndNivelIdNivelAndAccesoNot(anio.getIdAnio(), nivel.getIdNivel(), eliminado);
        if (propia.isPresent()) {
            response.setMinutosHoraAcademica(propia.get().getMinutosHoraAcademica());
            response.setOrigen("CONFIGURADO");
            return response;
        }
        // La del año anterior mas cercano de ese nivel.
        Optional<ConfiguracionAcademica> anterior = configuracionRepository
                .findByNivelIdNivelAndAccesoNot(nivel.getIdNivel(), eliminado).stream()
                .filter(c -> c.getAnioEscolar().getAnio().compareTo(anio.getAnio()) < 0)
                .max(Comparator.comparing(c -> c.getAnioEscolar().getAnio()));
        if (anterior.isPresent()) {
            response.setMinutosHoraAcademica(anterior.get().getMinutosHoraAcademica());
            response.setOrigen("HEREDADO");
            response.setAnioOrigen(anterior.get().getAnioEscolar().getAnio());
            return response;
        }
        // Inicial se organiza por actividades: sin bloques fijos.
        boolean inicial = PlantillaCursosParser.clave(nivel.getNombre()).equals("INICIAL");
        response.setMinutosHoraAcademica(inicial ? null : MINUTOS_POR_DEFECTO);
        response.setOrigen("DEFECTO");
        return response;
    }

    private AnioEscolar findAnio(Integer idAnio) {
        return anioEscolarRepository.findByIdAnioAndAccesoNot(idAnio, accesoEliminado())
                .orElseThrow(() -> new ResourceNotFoundException("Año escolar no encontrado con id " + idAnio));
    }

    private Nivel findNivel(Integer idNivel) {
        return nivelRepository.findByIdNivelAndAccesoNot(idNivel, accesoEliminado())
                .orElseThrow(() -> new ResourceNotFoundException("Nivel no encontrado con id " + idNivel));
    }

    private Acceso accesoEliminado() {
        return accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow();
    }
}
