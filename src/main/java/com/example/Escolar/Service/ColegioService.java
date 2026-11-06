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

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ColegioService {

    private static final String CARPETA_FOTO = "portal/colegio/foto";
    private static final String CARPETA_PORTADA = "portal/colegio/portada";

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

    /** Todo o nada: el colegio no existe sin su foto, y si algo falla no quedan imágenes sueltas. */
    @Transactional
    public ColegioResponse create(ColegioRequest request, MultipartFile foto, MultipartFile portada) {
        colegioRepository.findFirstByAccesoNot(accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow())
                .ifPresent(c -> {
                    throw new IllegalArgumentException("Ya existe un colegio registrado; actualícelo en lugar de crear otro");
                });
        exigirFoto(foto);
        Colegio colegio = new Colegio();
        aplicarDatos(colegio, request);
        colegio.setAcceso(accesoRepository.findById(AccesoConstants.ACTIVO).orElseThrow());
        List<String> subidas = new ArrayList<>();
        try {
            UploadResponse subidaFoto = cloudinaryService.upload(foto, CARPETA_FOTO);
            subidas.add(subidaFoto.getPublicId());
            colegio.setUrlFoto(subidaFoto.getUrl());
            colegio.setPkUrlFoto(subidaFoto.getPublicId());
            if (portada != null && !portada.isEmpty()) {
                UploadResponse subidaPortada = cloudinaryService.upload(portada, CARPETA_PORTADA);
                subidas.add(subidaPortada.getPublicId());
                colegio.setUrlPortal(subidaPortada.getUrl());
                colegio.setPkUrlPortada(subidaPortada.getPublicId());
            }
            return toResponse(colegioRepository.save(colegio));
        } catch (RuntimeException e) {
            subidas.forEach(cloudinaryService::delete);
            throw e;
        }
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

    /** Primero sube la nueva: si falla, la anterior sigue en su sitio. */
    @Transactional
    public ColegioResponse subirFoto(Integer id, MultipartFile file) {
        Colegio colegio = findColegio(id);
        exigirFoto(file);
        UploadResponse upload = cloudinaryService.upload(file, CARPETA_FOTO);
        String anterior = colegio.getPkUrlFoto();
        colegio.setUrlFoto(upload.getUrl());
        colegio.setPkUrlFoto(upload.getPublicId());
        ColegioResponse response = toResponse(colegioRepository.save(colegio));
        borrarDeCloudinary(anterior);
        return response;
    }

    @Transactional
    public ColegioResponse subirPortada(Integer id, MultipartFile file) {
        Colegio colegio = findColegio(id);
        UploadResponse upload = cloudinaryService.upload(file, CARPETA_PORTADA);
        String anterior = colegio.getPkUrlPortada();
        colegio.setUrlPortal(upload.getUrl());
        colegio.setPkUrlPortada(upload.getPublicId());
        ColegioResponse response = toResponse(colegioRepository.save(colegio));
        borrarDeCloudinary(anterior);
        return response;
    }

    private void exigirFoto(MultipartFile foto) {
        if (foto == null || foto.isEmpty()) {
            throw new IllegalArgumentException("La foto del colegio es obligatoria");
        }
    }

    private void borrarDeCloudinary(String publicId) {
        if (publicId != null && !publicId.isBlank()) {
            cloudinaryService.delete(publicId);
        }
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
        // Opcional: vacío se guarda como sin código.
        String codigo = request.getCodigoColegio();
        colegio.setCodigoColegio(codigo == null || codigo.isBlank() ? null : codigo.trim());
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