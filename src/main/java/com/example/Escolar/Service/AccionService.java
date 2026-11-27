package com.example.Escolar.Service;

import com.example.Escolar.Dto.AccionRequest;
import com.example.Escolar.Dto.AccionResponse;
import com.example.Escolar.Exception.ResourceNotFoundException;
import com.example.Escolar.Model.Accion;
import com.example.Escolar.Repository.AccionRepository;
import com.example.Escolar.Repository.AccesoRepository;
import com.example.Escolar.Security.ConcesionesService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AccionService {

    private final AccionRepository accionRepository;
    private final AccesoRepository accesoRepository;
    private final ConcesionesService concesionesService;

    public List<AccionResponse> getAll() {
        return accionRepository.findByAccesoNot(accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow()).stream().map(this::toResponse).toList();
    }

    public AccionResponse getById(Integer id) {
        return toResponse(findAccion(id));
    }

    private Accion findAccion(Integer id) {
        return accionRepository.findByIdAccionAndAccesoNot(id, accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow())
                .orElseThrow(() -> new ResourceNotFoundException("Acción no encontrada con id " + id));
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
