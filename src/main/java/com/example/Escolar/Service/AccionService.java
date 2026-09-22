package com.example.Escolar.Service;

import com.example.Escolar.Dto.AccionRequest;
import com.example.Escolar.Dto.AccionResponse;
import com.example.Escolar.Exception.ResourceNotFoundException;
import com.example.Escolar.Model.Accion;
import com.example.Escolar.Repository.AccionRepository;
import com.example.Escolar.Repository.AccesoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AccionService {

    private final AccionRepository accionRepository;
    private final AccesoRepository accesoRepository;

    public List<AccionResponse> getAll() {
        return accionRepository.findByAccesoNot(accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow()).stream().map(this::toResponse).toList();
    }

    public AccionResponse getById(Integer id) {
        return toResponse(findAccion(id));
    }

    @Transactional
    public AccionResponse create(AccionRequest request) {
        validarCodigoUnico(request.getCodigo(), null);
        Accion accion = new Accion();
        applyRequest(accion, request);
        accion.setAcceso(accesoRepository.findById(request.getAccesoId() != null ? request.getAccesoId() : AccesoConstants.ACTIVO).orElseThrow());
        return toResponse(accionRepository.save(accion));
    }

    @Transactional
    public AccionResponse update(Integer id, AccionRequest request) {
        Accion accion = findAccion(id);
        validarCodigoUnico(request.getCodigo(), accion.getIdAccion());
        applyRequest(accion, request);
        return toResponse(accionRepository.save(accion));
    }

    @Transactional
    public void delete(Integer id) {
        Accion accion = findAccion(id);
        accion.setAcceso(accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow());
        accionRepository.save(accion);
    }

    private Accion findAccion(Integer id) {
        return accionRepository.findByIdAccionAndAccesoNot(id, accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow())
                .orElseThrow(() -> new ResourceNotFoundException("Acción no encontrada con id " + id));
    }

    private void validarCodigoUnico(String codigo, Integer idExcluido) {
        accionRepository.findByCodigoAndAccesoNot(codigo, accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow())
                .filter(a -> idExcluido == null || !a.getIdAccion().equals(idExcluido))
                .ifPresent(a -> {
                    throw new IllegalArgumentException("Ya existe una acción con el código " + codigo);
                });
    }

    private void applyRequest(Accion accion, AccionRequest request) {
        accion.setCodigo(request.getCodigo());
        accion.setNombre(request.getNombre());
        if (request.getAccesoId() != null) {
            accion.setAcceso(accesoRepository.findById(request.getAccesoId()).orElseThrow());
        }
    }

    private AccionResponse toResponse(Accion accion) {
        AccionResponse response = new AccionResponse();
        response.setIdAccion(accion.getIdAccion());
        response.setCodigo(accion.getCodigo());
        response.setNombre(accion.getNombre());
        response.setAccesoId(accion.getAcceso().getIdAcceso().longValue());
        return response;
    }
}
