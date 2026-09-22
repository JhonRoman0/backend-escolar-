package com.example.Escolar.Service;

import com.example.Escolar.Dto.AnioEscolarRequest;
import com.example.Escolar.Dto.AnioEscolarResponse;
import com.example.Escolar.Exception.ResourceNotFoundException;
import com.example.Escolar.Model.AnioEscolar;
import com.example.Escolar.Repository.AccesoRepository;
import com.example.Escolar.Repository.AnioEscolarRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AnioEscolarService {

    public static final byte ESTADO_ACTIVO = 1;
    public static final byte ESTADO_CERRADO = 2;

    private final AnioEscolarRepository anioEscolarRepository;
    private final AccesoRepository accesoRepository;

    public List<AnioEscolarResponse> getAll() {
        return anioEscolarRepository.findByAccesoNot(accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow()).stream()
                .map(this::toResponse)
                .toList();
    }

    public AnioEscolarResponse getById(Integer id) {
        return toResponse(findAnio(id));
    }

    @Transactional
    public AnioEscolarResponse create(AnioEscolarRequest request) {
        validarAnioUnico(request.getAnio(), null);
        validarFechas(request.getFechaInicio(), request.getFechaFin());
        AnioEscolar anioEscolar = new AnioEscolar();
        anioEscolar.setAnio(request.getAnio().trim());
        byte estado = request.getEstado() == null ? ESTADO_ACTIVO : request.getEstado();
        anioEscolar.setEstado(estado);
        anioEscolar.setFechaInicio(request.getFechaInicio());
        anioEscolar.setFechaFin(request.getFechaFin());
        anioEscolar.setBloqueoHorariosPorFecha(
            request.getBloqueoHorariosPorFecha() == null ? true : request.getBloqueoHorariosPorFecha()
        );
        anioEscolar.setAcceso(accesoRepository.findById(request.getAccesoId() == null ? AccesoConstants.ACTIVO : request.getAccesoId()).orElseThrow());
        if (estado == ESTADO_ACTIVO) {
            cerrarDemasActivos(null);
        }
        return toResponse(anioEscolarRepository.save(anioEscolar));
    }

    @Transactional
    public AnioEscolarResponse update(Integer id, AnioEscolarRequest request) {
        AnioEscolar anioEscolar = findAnio(id);
        if (request.getAnio() != null && !request.getAnio().isBlank()) {
            validarAnioUnico(request.getAnio(), id);
            anioEscolar.setAnio(request.getAnio().trim());
        }
        if (request.getEstado() != null) {
            anioEscolar.setEstado(request.getEstado());
            if (request.getEstado() == ESTADO_ACTIVO) {
                cerrarDemasActivos(id);
            }
        }
        if (request.getFechaInicio() != null) {
            anioEscolar.setFechaInicio(request.getFechaInicio());
        }
        if (request.getFechaFin() != null) {
            anioEscolar.setFechaFin(request.getFechaFin());
        }
        if (request.getBloqueoHorariosPorFecha() != null) {
            anioEscolar.setBloqueoHorariosPorFecha(request.getBloqueoHorariosPorFecha());
        }
        if (request.getAccesoId() != null) {
            anioEscolar.setAcceso(accesoRepository.findById(request.getAccesoId()).orElseThrow());
        }
        // Validar fechas despues de aplicar cambios
        validarFechas(anioEscolar.getFechaInicio(), anioEscolar.getFechaFin());
        return toResponse(anioEscolarRepository.save(anioEscolar));
    }

    @Transactional
    public void delete(Integer id) {
        AnioEscolar anioEscolar = findAnio(id);
        anioEscolar.setAcceso(accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow());
        anioEscolarRepository.save(anioEscolar);
    }

    private void cerrarDemasActivos(Integer idExcluir) {
        anioEscolarRepository.findByAccesoNot(accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow()).stream()
                .filter(a -> a.getEstado() == ESTADO_ACTIVO)
                .filter(a -> idExcluir == null || !a.getIdAnio().equals(idExcluir))
                .forEach(a -> a.setEstado(ESTADO_CERRADO));
    }

    private void validarAnioUnico(String anio, Integer idExcluir) {
        if (anio == null || anio.isBlank()) {
            return;
        }
        anioEscolarRepository.findByAnioAndAccesoNot(anio.trim(), accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow())
                .filter(a -> idExcluir == null || !a.getIdAnio().equals(idExcluir))
                .ifPresent(a -> {
                    throw new IllegalArgumentException("Ya existe un año escolar con ese año");
                });
    }

    private AnioEscolar findAnio(Integer id) {
        return anioEscolarRepository.findByIdAnioAndAccesoNot(id, accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow())
                .orElseThrow(() -> new ResourceNotFoundException("Año escolar no encontrado con id " + id));
    }

    private void validarFechas(java.time.LocalDate fechaInicio, java.time.LocalDate fechaFin) {
        if (fechaInicio != null && fechaFin != null) {
            if (!fechaFin.isAfter(fechaInicio)) {
                throw new IllegalArgumentException("La fecha de fin debe ser posterior a la fecha de inicio");
            }
        }
    }

    private AnioEscolarResponse toResponse(AnioEscolar anioEscolar) {
        AnioEscolarResponse response = new AnioEscolarResponse();
        response.setIdAnio(anioEscolar.getIdAnio());
        response.setAnio(anioEscolar.getAnio());
        response.setEstado(anioEscolar.getEstado());
        response.setFechaInicio(anioEscolar.getFechaInicio());
        response.setFechaFin(anioEscolar.getFechaFin());
        response.setBloqueoHorariosPorFecha(anioEscolar.getBloqueoHorariosPorFecha());
        response.setAccesoId(anioEscolar.getAcceso().getIdAcceso().longValue());
        return response;
    }
}