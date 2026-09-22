package com.example.Escolar.Service;

import com.example.Escolar.Dto.ColegioRequest;
import com.example.Escolar.Dto.ColegioResponse;
import com.example.Escolar.Dto.UploadResponse;
import com.example.Escolar.Exception.ResourceNotFoundException;
import com.example.Escolar.Model.Colegio;
import com.example.Escolar.Repository.AccesoRepository;
import com.example.Escolar.Repository.ColegioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ColegioService {

    private final ColegioRepository colegioRepository;
    private final AccesoRepository accesoRepository;
    private final CloudinaryService cloudinaryService;

    public List<ColegioResponse> getAll() {
        return colegioRepository.findByAccesoNot(accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow()).stream()
                .map(this::toResponse)
                .toList();
    }

    public ColegioResponse getById(Integer id) {
        return toResponse(findColegio(id));
    }

    public ColegioResponse getActual() {
        return colegioRepository.findFirstByAccesoNot(accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow())
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("No existe un colegio registrado"));
    }

    @Transactional
    public ColegioResponse create(ColegioRequest request) {
        colegioRepository.findFirstByAccesoNot(accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow())
                .ifPresent(c -> {
                    throw new IllegalArgumentException("Ya existe un colegio registrado; actualícelo en lugar de crear otro");
                });
        Colegio colegio = new Colegio();
        aplicarDatos(colegio, request);
        colegio.setAcceso(accesoRepository.findById(AccesoConstants.ACTIVO).orElseThrow());
        return toResponse(colegioRepository.save(colegio));
    }

    @Transactional
    public ColegioResponse update(Integer id, ColegioRequest request) {
        Colegio colegio = findColegio(id);
        aplicarDatos(colegio, request);
        if (request.getAccesoId() != null) {
            colegio.setAcceso(accesoRepository.findById(request.getAccesoId()).orElseThrow());
        }
        return toResponse(colegioRepository.save(colegio));
    }

    @Transactional
    public void delete(Integer id) {
        Colegio colegio = findColegio(id);
        colegio.setAcceso(accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow());
        colegioRepository.save(colegio);
    }

    @Transactional
    public ColegioResponse subirFoto(Integer id, MultipartFile file) {
        Colegio colegio = findColegio(id);
        if (colegio.getPkUrlFoto() != null && !colegio.getPkUrlFoto().isBlank()) {
            cloudinaryService.delete(colegio.getPkUrlFoto());
        }
        UploadResponse upload = cloudinaryService.upload(file, "portal/colegio/foto");
        colegio.setUrlFoto(upload.getUrl());
        colegio.setPkUrlFoto(upload.getPublicId());
        return toResponse(colegioRepository.save(colegio));
    }

    @Transactional
    public void eliminarFoto(Integer id) {
        Colegio colegio = findColegio(id);
        if (colegio.getPkUrlFoto() != null && !colegio.getPkUrlFoto().isBlank()) {
            cloudinaryService.delete(colegio.getPkUrlFoto());
            colegio.setUrlFoto(null);
            colegio.setPkUrlFoto(null);
            colegioRepository.save(colegio);
        }
    }

    @Transactional
    public ColegioResponse subirPortada(Integer id, MultipartFile file) {
        Colegio colegio = findColegio(id);
        if (colegio.getPkUrlPortada() != null && !colegio.getPkUrlPortada().isBlank()) {
            cloudinaryService.delete(colegio.getPkUrlPortada());
        }
        UploadResponse upload = cloudinaryService.upload(file, "portal/colegio/portada");
        colegio.setUrlPortal(upload.getUrl());
        colegio.setPkUrlPortada(upload.getPublicId());
        return toResponse(colegioRepository.save(colegio));
    }

    @Transactional
    public void eliminarPortada(Integer id) {
        Colegio colegio = findColegio(id);
        if (colegio.getPkUrlPortada() != null && !colegio.getPkUrlPortada().isBlank()) {
            cloudinaryService.delete(colegio.getPkUrlPortada());
            colegio.setUrlPortal(null);
            colegio.setPkUrlPortada(null);
            colegioRepository.save(colegio);
        }
    }

    private void aplicarDatos(Colegio colegio, ColegioRequest request) {
        colegio.setNombre(request.getNombre());
        colegio.setCelular(request.getCelular());
        colegio.setTelefono(request.getTelefono());
        colegio.setDireccion(request.getDireccion());
        colegio.setCodigoColegio(request.getCodigoColegio());
        colegio.setUrlFoto(request.getUrlFoto());
        colegio.setUrlPortal(request.getUrlPortal());
    }

    private Colegio findColegio(Integer id) {
        return colegioRepository.findByIdColegioAndAccesoNot(id, accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow())
                .orElseThrow(() -> new ResourceNotFoundException("Colegio no encontrado con id " + id));
    }

    private ColegioResponse toResponse(Colegio colegio) {
        ColegioResponse response = new ColegioResponse();
        response.setIdColegio(colegio.getIdColegio());
        response.setNombre(colegio.getNombre());
        response.setCelular(colegio.getCelular());
        response.setTelefono(colegio.getTelefono());
        response.setDireccion(colegio.getDireccion());
        response.setCodigoColegio(colegio.getCodigoColegio());
        response.setUrlFoto(colegio.getUrlFoto());
        response.setUrlPortal(colegio.getUrlPortal());
        response.setAccesoId(colegio.getAcceso().getIdAcceso().longValue());
        return response;
    }
}