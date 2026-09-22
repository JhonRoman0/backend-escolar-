package com.example.Escolar.Service;

import com.example.Escolar.Dto.GradoRequest;
import com.example.Escolar.Dto.GradoResponse;
import com.example.Escolar.Dto.SeccionResponse;
import com.example.Escolar.Exception.ResourceNotFoundException;
import com.example.Escolar.Model.*;
import com.example.Escolar.Repository.AccesoRepository;
import com.example.Escolar.Repository.GradoRepository;
import com.example.Escolar.Repository.GradoSeccionRepository;
import com.example.Escolar.Repository.NivelRepository;
import com.example.Escolar.Repository.SeccionRepository;
import com.example.Escolar.Repository.TurnoRepository;
import com.example.Escolar.Repository.AnioEscolarRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GradoService {

    private final GradoRepository gradoRepository;
    private final SeccionRepository seccionRepository;
    private final GradoSeccionRepository gradoSeccionRepository;
    private final TurnoRepository turnoRepository;
    private final AnioEscolarRepository anioEscolarRepository;
    private final NivelRepository nivelRepository;
    private final AccesoRepository accesoRepository;

    public List<GradoResponse> getAll() {
        return gradoRepository.findByAccesoNot(accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow()).stream()
                .map(this::toResponse)
                .toList();
    }

    public List<GradoResponse> getAllByNivel(Integer idNivel) {
        Nivel nivel = findNivel(idNivel);
        return gradoRepository.findByNivelAndAccesoNot(nivel, accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow()).stream()
                .map(this::toResponse)
                .toList();
    }

    public GradoResponse getById(Integer id) {
        return toResponse(findGrado(id));
    }

    @Transactional
    public GradoResponse create(GradoRequest request) {
        Nivel nivel = findNivel(request.getIdNivel());
        Turno turno = findTurno(request.getIdTurno());
        AnioEscolar anioEscolar = findAnio(request.getIdAnio());

        validarCombinacionUnica(request.getNombre(), nivel.getIdNivel(), request.getIdTurno(), request.getIdAnio(), null);

        Grado grado = new Grado();
        grado.setNombre(request.getNombre().trim());
        grado.setNivel(nivel);
        grado.setAcceso(accesoRepository.findById(request.getAccesoId() == null ? AccesoConstants.ACTIVO : request.getAccesoId()).orElseThrow());
        grado = gradoRepository.save(grado);

        if (request.getSecciones() != null && !request.getSecciones().isEmpty()) {
            crearSecciones(grado, turno, anioEscolar, request.getSecciones());
        } else {
            crearGradoSeccionSinSeccion(grado, turno, anioEscolar);
        }
        return toResponse(grado);
    }

    @Transactional
    public GradoResponse update(Integer id, GradoRequest request) {
        Grado grado = findGrado(id);

        if (request.getNombre() != null && !request.getNombre().isBlank()) {
            grado.setNombre(request.getNombre().trim());
        }
        if (request.getIdNivel() != null) {
            grado.setNivel(findNivel(request.getIdNivel()));
        }
        if (request.getAccesoId() != null) {
            grado.setAcceso(accesoRepository.findById(request.getAccesoId()).orElseThrow());
        }

        if (request.getIdTurno() != null && request.getIdAnio() != null) {
            sincronizarSecciones(grado, request);
        }

        Integer idNivelFinal = grado.getNivel().getIdNivel();
        Integer turnoFinal = request.getIdTurno() != null
                ? request.getIdTurno()
                : gradoSeccionRepository.findByGradoAndAccesoNot(grado, accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow()).stream().findFirst()
                        .map(gs -> gs.getTurno().getIdTurno()).orElse(null);
        Integer anioFinal = request.getIdAnio() != null
                ? request.getIdAnio()
                : gradoSeccionRepository.findByGradoAndAccesoNot(grado, accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow()).stream().findFirst()
                        .map(gs -> gs.getAnioEscolar().getIdAnio()).orElse(null);

        validarCombinacionUnica(grado.getNombre(), idNivelFinal, turnoFinal, anioFinal, id);
        return toResponse(gradoRepository.save(grado));
    }

    private void sincronizarSecciones(Grado grado, GradoRequest request) {
        Turno turno = findTurno(request.getIdTurno());
        AnioEscolar anioEscolar = findAnio(request.getIdAnio());
        List<GradoSeccion> actuales = gradoSeccionRepository.findByGradoAndAccesoNot(grado, accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow());

        Set<String> nombresActuales = actuales.stream()
                .filter(gs -> gs.getSeccion() != null)
                .map(gs -> gs.getSeccion().getNombre().trim())
                .collect(Collectors.toSet());

        if (request.getSecciones() == null || request.getSecciones().isEmpty()) {
            actuales.forEach(gs -> gs.setAcceso(accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow()));
            gradoSeccionRepository.saveAll(actuales);
            crearGradoSeccionSinSeccion(grado, turno, anioEscolar);
            return;
        }

        Set<String> nombresRequest = new HashSet<>();
        for (String nombre : request.getSecciones()) {
            String limpio = nombre.trim();
            if (!nombresRequest.add(limpio)) {
                throw new IllegalArgumentException("No se pueden repetir secciones dentro del mismo grado");
            }
        }

        for (GradoSeccion gs : actuales) {
            if (gs.getSeccion() == null) {
                gs.setAcceso(accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow());
            } else {
                String nombre = gs.getSeccion().getNombre().trim();
                if (nombresRequest.contains(nombre)) {
                    gs.setTurno(turno);
                    gs.setAnioEscolar(anioEscolar);
                } else {
                    gs.setAcceso(accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow());
                }
            }
        }
        gradoSeccionRepository.saveAll(actuales);

        nombresActuales.retainAll(nombresRequest);
        nombresRequest.removeAll(nombresActuales);
        for (String nuevo : nombresRequest) {
            crearSeccion(grado, turno, anioEscolar, nuevo);
        }
    }

    private void crearGradoSeccionSinSeccion(Grado grado, Turno turno, AnioEscolar anioEscolar) {
        GradoSeccion gs = new GradoSeccion();
        gs.setGrado(grado);
        gs.setSeccion(null);
        gs.setTurno(turno);
        gs.setAnioEscolar(anioEscolar);
        gs.setAcceso(accesoRepository.findById(AccesoConstants.ACTIVO).orElseThrow());
        gradoSeccionRepository.save(gs);
    }

    private void crearSeccion(Grado grado, Turno turno, AnioEscolar anioEscolar, String nombre) {
        Seccion seccion = new Seccion();
        seccion.setNombre(nombre.trim());
        seccion.setAcceso(accesoRepository.findById(AccesoConstants.ACTIVO).orElseThrow());
        seccion = seccionRepository.save(seccion);

        GradoSeccion gradoSeccion = new GradoSeccion();
        gradoSeccion.setGrado(grado);
        gradoSeccion.setSeccion(seccion);
        gradoSeccion.setTurno(turno);
        gradoSeccion.setAnioEscolar(anioEscolar);
        gradoSeccion.setAcceso(accesoRepository.findById(AccesoConstants.ACTIVO).orElseThrow());
        gradoSeccionRepository.save(gradoSeccion);
    }

    private void crearSecciones(Grado grado, Turno turno, AnioEscolar anioEscolar, List<String> nombres) {
        Set<String> vistos = new HashSet<>();
        for (String nombre : nombres) {
            String limpio = nombre.trim();
            if (!vistos.add(limpio)) {
                throw new IllegalArgumentException("No se pueden repetir secciones dentro del mismo grado");
            }
            crearSeccion(grado, turno, anioEscolar, limpio);
        }
    }

    @Transactional
    public void delete(Integer id) {
        Grado grado = findGrado(id);
        grado.setAcceso(accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow());
        gradoSeccionRepository.findByGradoAndAccesoNot(grado, accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow())
                .forEach(gs -> gs.setAcceso(accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow()));
        gradoRepository.save(grado);
    }

    private void validarCombinacionUnica(String nombre, Integer idNivel, Integer idTurno, Integer idAnio, Integer idExcluir) {
        if (nombre == null || nombre.isBlank()) {
            return;
        }
        for (Grado g : gradoRepository.findByNombreAndAccesoNot(nombre.trim(), accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow())) {
            if (idExcluir != null && g.getIdGrado().equals(idExcluir)) {
                continue;
            }
            if (!g.getNivel().getIdNivel().equals(idNivel)) {
                continue;
            }
            boolean coincideTurnoAnio = gradoSeccionRepository.findByGradoAndAccesoNot(g, accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow()).stream()
                    .anyMatch(gs -> gs.getTurno().getIdTurno().equals(idTurno)
                            && gs.getAnioEscolar().getIdAnio().equals(idAnio));
            if (coincideTurnoAnio) {
                GradoSeccion gs0 = gradoSeccionRepository.findByGradoAndAccesoNot(g, accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow()).get(0);
                throw new IllegalArgumentException("Ya existe el grado " + nombre + " en el nivel "
                        + g.getNivel().getNombre() + ", turno " + gs0.getTurno().getNombre()
                        + " para el año " + gs0.getAnioEscolar().getAnio());
            }
        }
    }

    private Grado findGrado(Integer id) {
        return gradoRepository.findByIdGradoAndAccesoNot(id, accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow())
                .orElseThrow(() -> new ResourceNotFoundException("Grado no encontrado con id " + id));
    }

    private Nivel findNivel(Integer id) {
        return nivelRepository.findByIdNivelAndAccesoNot(id, accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow())
                .orElseThrow(() -> new ResourceNotFoundException("Nivel no encontrado con id " + id));
    }

    private Turno findTurno(Integer id) {
        return turnoRepository.findByIdTurnoAndAccesoNot(id, accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow())
                .orElseThrow(() -> new ResourceNotFoundException("Turno no encontrado con id " + id));
    }

    private AnioEscolar findAnio(Integer id) {
        return anioEscolarRepository.findByIdAnioAndAccesoNot(id, accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow())
                .orElseThrow(() -> new ResourceNotFoundException("Año escolar no encontrado con id " + id));
    }

    private GradoResponse toResponse(Grado grado) {
        GradoResponse response = new GradoResponse();
        response.setIdGrado(grado.getIdGrado());
        response.setNombre(grado.getNombre());
        response.setIdNivel(grado.getNivel().getIdNivel());
        response.setNivel(grado.getNivel().getNombre());
        response.setAccesoId(grado.getAcceso().getIdAcceso().longValue());
        List<GradoSeccion> filas = gradoSeccionRepository.findByGradoAndAccesoNot(grado, accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow());
        if (!filas.isEmpty()) {
            GradoSeccion primera = filas.get(0);
            response.setIdAnio(primera.getAnioEscolar().getIdAnio());
            response.setAnio(primera.getAnioEscolar().getAnio());
            response.setIdTurno(primera.getTurno().getIdTurno());
            response.setTurno(primera.getTurno().getNombre());
        }
        response.setSecciones(filas.stream()
                .filter(gs -> gs.getSeccion() != null)
                .map(this::toSeccionResponse)
                .toList());
        if (response.getSecciones().isEmpty() && !filas.isEmpty()) {
            response.setIdGradoSeccionDefault(filas.get(0).getIdGradoSeccion());
        }
        return response;
    }

    private SeccionResponse toSeccionResponse(GradoSeccion gradoSeccion) {
        SeccionResponse response = new SeccionResponse();
        response.setIdGradoSeccion(gradoSeccion.getIdGradoSeccion());
        if (gradoSeccion.getSeccion() != null) {
            response.setIdSeccion(gradoSeccion.getSeccion().getIdSeccion());
            response.setNombre(gradoSeccion.getSeccion().getNombre());
        }
        return response;
    }
}
