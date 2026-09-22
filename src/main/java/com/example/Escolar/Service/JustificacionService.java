package com.example.Escolar.Service;

import com.example.Escolar.Dto.JustificacionRequest;
import com.example.Escolar.Dto.JustificacionResponse;
import com.example.Escolar.Exception.ResourceNotFoundException;
import com.example.Escolar.Model.Justificacion;
import com.example.Escolar.Repository.AccesoRepository;
import com.example.Escolar.Repository.JustificacionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class JustificacionService {

    private final JustificacionRepository justificacionRepository;
    private final AccesoRepository accesoRepository;

    public List<JustificacionResponse> getAll() {
        return justificacionRepository.findByAccesoNot(accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow()).stream()
                .map(this::toResponse)
                .toList();
    }

    public JustificacionResponse getById(Integer id) {
        return toResponse(findJustificacion(id));
    }

    @Transactional
    public JustificacionResponse create(JustificacionRequest request) {
        Justificacion justificacion = new Justificacion();
        justificacion.setMotivo(request.getMotivo().trim());
        justificacion.setDocumentoUrl(request.getDocumentoUrl());
        justificacion.setFechaJustificacion(request.getFechaJustificacion());
        justificacion.setAcceso(accesoRepository.findById(request.getAccesoId() == null ? AccesoConstants.ACTIVO : request.getAccesoId()).orElseThrow());
        return toResponse(justificacionRepository.save(justificacion));
    }

    @Transactional
    public JustificacionResponse update(Integer id, JustificacionRequest request) {
        Justificacion justificacion = findJustificacion(id);
        justificacion.setMotivo(request.getMotivo().trim());
        justificacion.setDocumentoUrl(request.getDocumentoUrl());
        justificacion.setFechaJustificacion(request.getFechaJustificacion());
        if (request.getAccesoId() != null) {
            justificacion.setAcceso(accesoRepository.findById(request.getAccesoId()).orElseThrow());
        }
        return toResponse(justificacionRepository.save(justificacion));
    }

    @Transactional
    public void delete(Integer id) {
        Justificacion justificacion = findJustificacion(id);
        justificacion.setAcceso(accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow());
        justificacionRepository.save(justificacion);
    }

    private Justificacion findJustificacion(Integer id) {
        return justificacionRepository.findByIdJustificacionAndAccesoNot(id, accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow())
                .orElseThrow(() -> new ResourceNotFoundException("Motivo de justificación no encontrado con id " + id));
    }

    private JustificacionResponse toResponse(Justificacion justificacion) {
        JustificacionResponse response = new JustificacionResponse();
        response.setIdJustificacion(justificacion.getIdJustificacion());
        response.setMotivo(justificacion.getMotivo());
        response.setDocumentoUrl(justificacion.getDocumentoUrl());
        response.setFechaJustificacion(justificacion.getFechaJustificacion());
        response.setAccesoId(justificacion.getAcceso().getIdAcceso().longValue());
        return response;
    }
}
