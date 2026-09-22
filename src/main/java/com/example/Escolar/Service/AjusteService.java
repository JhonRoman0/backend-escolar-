package com.example.Escolar.Service;

import com.example.Escolar.Dto.AjusteRequest;
import com.example.Escolar.Dto.AjusteResponse;
import com.example.Escolar.Exception.ResourceNotFoundException;
import com.example.Escolar.Model.AjustePortal;
import com.example.Escolar.Repository.AccesoRepository;
import com.example.Escolar.Repository.AjustePortalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AjusteService {

    private final AjustePortalRepository ajustePortalRepository;
    private final AccesoRepository accesoRepository;

    public List<AjusteResponse> getAll() {
        return ajustePortalRepository.findByAccesoNot(accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow()).stream()
                .map(this::toResponse)
                .toList();
    }

    public AjusteResponse getById(Integer id) {
        return toResponse(findAjuste(id));
    }

    @Transactional
    public AjusteResponse create(AjusteRequest request) {
        ajustePortalRepository.findByClaveAndAccesoNot(request.getClave(), accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow())
                .ifPresent(a -> {
                    throw new IllegalArgumentException("Ya existe un ajuste con la clave " + request.getClave());
                });
        AjustePortal ajuste = new AjustePortal();
        ajuste.setClave(request.getClave());
        ajuste.setValor(request.getValor());
        ajuste.setAcceso(accesoRepository.findById(request.getAccesoId() == null ? AccesoConstants.ACTIVO : request.getAccesoId()).orElseThrow());
        return toResponse(ajustePortalRepository.save(ajuste));
    }

    @Transactional
    public AjusteResponse update(Integer id, AjusteRequest request) {
        AjustePortal ajuste = findAjuste(id);
        ajustePortalRepository.findByClaveAndAccesoNot(request.getClave(), accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow())
                .filter(a -> !a.getIdAjuste().equals(id))
                .ifPresent(a -> {
                    throw new IllegalArgumentException("Ya existe un ajuste con la clave " + request.getClave());
                });
        ajuste.setClave(request.getClave());
        ajuste.setValor(request.getValor());
        if (request.getAccesoId() != null) {
            ajuste.setAcceso(accesoRepository.findById(request.getAccesoId()).orElseThrow());
        }
        return toResponse(ajustePortalRepository.save(ajuste));
    }

    @Transactional
    public void delete(Integer id) {
        AjustePortal ajuste = findAjuste(id);
        ajuste.setAcceso(accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow());
        ajustePortalRepository.save(ajuste);
    }

    private AjustePortal findAjuste(Integer id) {
        return ajustePortalRepository.findByIdAjusteAndAccesoNot(id, accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow())
                .orElseThrow(() -> new ResourceNotFoundException("Ajuste del portal no encontrado con id " + id));
    }

    private AjusteResponse toResponse(AjustePortal ajuste) {
        AjusteResponse response = new AjusteResponse();
        response.setIdAjuste(ajuste.getIdAjuste());
        response.setClave(ajuste.getClave());
        response.setValor(ajuste.getValor());
        response.setAccesoId(ajuste.getAcceso().getIdAcceso().longValue());
        return response;
    }
}