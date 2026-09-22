package com.example.Escolar.Service;

import com.example.Escolar.Dto.AulaRequest;
import com.example.Escolar.Dto.AulaResponse;
import com.example.Escolar.Exception.ResourceNotFoundException;
import com.example.Escolar.Model.Aula;
import com.example.Escolar.Repository.AccesoRepository;
import com.example.Escolar.Repository.AulaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AulaService {

    private final AulaRepository aulaRepository;
    private final AccesoRepository accesoRepository;

    public List<AulaResponse> getAll() {
        return aulaRepository.findByAccesoNot(accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow()).stream()
                .map(this::toResponse)
                .toList();
    }

    public AulaResponse getById(Integer id) {
        return toResponse(findAula(id));
    }

    @Transactional
    public AulaResponse create(AulaRequest request) {
        validarNombreUnico(request.getNombre(), null);
        Aula aula = new Aula();
        aula.setNombre(request.getNombre().trim());
        aula.setCapacidad(request.getCapacidad());
        aula.setAcceso(accesoRepository.findById(request.getAccesoId() == null ? AccesoConstants.ACTIVO : request.getAccesoId()).orElseThrow());
        return toResponse(aulaRepository.save(aula));
    }

    @Transactional
    public AulaResponse update(Integer id, AulaRequest request) {
        Aula aula = findAula(id);
        if (request.getNombre() != null && !request.getNombre().isBlank()) {
            validarNombreUnico(request.getNombre(), id);
            aula.setNombre(request.getNombre().trim());
        }
        if (request.getCapacidad() != null) {
            aula.setCapacidad(request.getCapacidad());
        }
        if (request.getAccesoId() != null) {
            aula.setAcceso(accesoRepository.findById(request.getAccesoId()).orElseThrow());
        }
        return toResponse(aulaRepository.save(aula));
    }

    @Transactional
    public void delete(Integer id) {
        Aula aula = findAula(id);
        aula.setAcceso(accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow());
        aulaRepository.save(aula);
    }

    private void validarNombreUnico(String nombre, Integer idExcluir) {
        if (nombre == null || nombre.isBlank()) {
            return;
        }
        aulaRepository.findByNombreAndAccesoNot(nombre.trim(), accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow())
                .filter(a -> idExcluir == null || !a.getIdAula().equals(idExcluir))
                .ifPresent(a -> {
                    throw new IllegalArgumentException("Ya existe un aula con ese nombre");
                });
    }

    private Aula findAula(Integer id) {
        return aulaRepository.findByIdAulaAndAccesoNot(id, accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow())
                .orElseThrow(() -> new ResourceNotFoundException("Aula no encontrada con id " + id));
    }

    private AulaResponse toResponse(Aula aula) {
        AulaResponse response = new AulaResponse();
        response.setIdAula(aula.getIdAula());
        response.setNombre(aula.getNombre());
        response.setCapacidad(aula.getCapacidad());
        response.setAccesoId(aula.getAcceso().getIdAcceso().longValue());
        return response;
    }
}
