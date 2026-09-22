package com.example.Escolar.Service;

import com.example.Escolar.Dto.TurnoRequest;
import com.example.Escolar.Dto.TurnoResponse;
import com.example.Escolar.Exception.ResourceNotFoundException;
import com.example.Escolar.Model.Turno;
import com.example.Escolar.Repository.AccesoRepository;
import com.example.Escolar.Repository.TurnoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TurnoService {

    private final TurnoRepository turnoRepository;
    private final AccesoRepository accesoRepository;

    public List<TurnoResponse> getAll() {
        return turnoRepository.findByAccesoNot(accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow()).stream()
                .map(this::toResponse)
                .toList();
    }

    public TurnoResponse getById(Integer id) {
        return toResponse(findTurno(id));
    }

    @Transactional
    public TurnoResponse create(TurnoRequest request) {
        validarNombreUnico(request.getNombre(), null);
        Turno turno = new Turno();
        turno.setNombre(request.getNombre().trim());
        aplicarDatos(turno, request);
        turno.setAcceso(accesoRepository.findById(request.getAccesoId() == null ? AccesoConstants.ACTIVO : request.getAccesoId()).orElseThrow());
        return toResponse(turnoRepository.save(turno));
    }

    @Transactional
    public TurnoResponse update(Integer id, TurnoRequest request) {
        Turno turno = findTurno(id);
        if (request.getNombre() != null && !request.getNombre().isBlank()) {
            validarNombreUnico(request.getNombre(), id);
            turno.setNombre(request.getNombre().trim());
        }
        aplicarDatos(turno, request);
        if (request.getAccesoId() != null) {
            turno.setAcceso(accesoRepository.findById(request.getAccesoId()).orElseThrow());
        }
        return toResponse(turnoRepository.save(turno));
    }

    @Transactional
    public void delete(Integer id) {
        Turno turno = findTurno(id);
        turno.setAcceso(accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow());
        turnoRepository.save(turno);
    }

    private void aplicarDatos(Turno turno, TurnoRequest request) {
        turno.setHoraEntrada(request.getHoraEntrada());
        turno.setHoraEntradaLimite(request.getHoraEntradaLimite());
        turno.setHoraFaltaLimite(request.getHoraFaltaLimite());
        turno.setHoraSalida(request.getHoraSalida());
    }

    private void validarNombreUnico(String nombre, Integer idExcluir) {
        if (nombre == null || nombre.isBlank()) {
            return;
        }
        turnoRepository.findByNombreAndAccesoNot(nombre.trim(), accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow())
                .filter(t -> idExcluir == null || !t.getIdTurno().equals(idExcluir))
                .ifPresent(t -> {
                    throw new IllegalArgumentException("Ya existe un turno con ese nombre");
                });
    }

    private Turno findTurno(Integer id) {
        return turnoRepository.findByIdTurnoAndAccesoNot(id, accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow())
                .orElseThrow(() -> new ResourceNotFoundException("Turno no encontrado con id " + id));
    }

    private TurnoResponse toResponse(Turno turno) {
        TurnoResponse response = new TurnoResponse();
        response.setIdTurno(turno.getIdTurno());
        response.setNombre(turno.getNombre());
        response.setHoraEntrada(turno.getHoraEntrada());
        response.setHoraEntradaLimite(turno.getHoraEntradaLimite());
        response.setHoraFaltaLimite(turno.getHoraFaltaLimite());
        response.setHoraSalida(turno.getHoraSalida());
        response.setAccesoId(turno.getAcceso().getIdAcceso().longValue());
        return response;
    }
}
