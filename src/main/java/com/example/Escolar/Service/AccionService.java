package com.example.Escolar.Service;

import com.example.Escolar.Dto.AccionRequest;
import com.example.Escolar.Dto.AccionResponse;
import com.example.Escolar.Exception.ResourceNotFoundException;
import com.example.Escolar.Model.Accion;
import com.example.Escolar.Repository.AccionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AccionService {

    public static final byte ACCESO_ACTIVO = 1;
    public static final byte ACCESO_ELIMINADO = 2;

    private final AccionRepository accionRepository;

    public List<AccionResponse> getAll() {
        return accionRepository.findByAccesoNot(ACCESO_ELIMINADO).stream().map(this::toResponse).toList();
    }

    public AccionResponse getById(Integer id) {
        return toResponse(findAccion(id));
    }

    @Transactional
    public AccionResponse create(AccionRequest request) {
        validarCodigoUnico(request.getCodigo(), null);
        Accion accion = new Accion();
        applyRequest(accion, request);
        accion.setAcceso(request.getAcceso() != null ? request.getAcceso() : ACCESO_ACTIVO);
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
        accion.setAcceso(ACCESO_ELIMINADO);
        accionRepository.save(accion);
    }

    private Accion findAccion(Integer id) {
        return accionRepository.findByIdAccionAndAccesoNot(id, ACCESO_ELIMINADO)
                .orElseThrow(() -> new ResourceNotFoundException("Acción no encontrada con id " + id));
    }

    private void validarCodigoUnico(String codigo, Integer idExcluido) {
        accionRepository.findByCodigoAndAccesoNot(codigo, ACCESO_ELIMINADO)
                .filter(a -> idExcluido == null || !a.getIdAccion().equals(idExcluido))
                .ifPresent(a -> {
                    throw new IllegalArgumentException("Ya existe una acción con el código " + codigo);
                });
    }

    private void applyRequest(Accion accion, AccionRequest request) {
        accion.setCodigo(request.getCodigo());
        accion.setNombre(request.getNombre());
        if (request.getAcceso() != null) {
            accion.setAcceso(request.getAcceso());
        }
    }

    private AccionResponse toResponse(Accion accion) {
        AccionResponse response = new AccionResponse();
        response.setIdAccion(accion.getIdAccion());
        response.setCodigo(accion.getCodigo());
        response.setNombre(accion.getNombre());
        response.setAcceso(accion.getAcceso());
        return response;
    }
}
