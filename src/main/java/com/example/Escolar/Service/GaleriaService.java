package com.example.Escolar.Service;

import com.example.Escolar.Dto.GaleriaDetalleRequest;
import com.example.Escolar.Dto.GaleriaDetalleResponse;
import com.example.Escolar.Dto.GaleriaRequest;
import com.example.Escolar.Dto.GaleriaResponse;
import com.example.Escolar.Dto.UploadResponse;
import com.example.Escolar.Exception.ResourceNotFoundException;
import com.example.Escolar.Model.GaleriaDetalle;
import com.example.Escolar.Model.GaleriaImagen;
import com.example.Escolar.Repository.AccesoRepository;
import com.example.Escolar.Repository.GaleriaDetalleRepository;
import com.example.Escolar.Repository.GaleriaImagenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GaleriaService {

    private final GaleriaImagenRepository galeriaImagenRepository;
    private final GaleriaDetalleRepository galeriaDetalleRepository;
    private final CloudinaryService cloudinaryService;
    private final AccesoRepository accesoRepository;

    public List<GaleriaResponse> getAll() {
        return galeriaImagenRepository.findByAccesoNot(accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow()).stream()
                .map(this::toResponse)
                .toList();
    }

    public GaleriaResponse getById(Integer id) {
        return toResponse(findGaleria(id));
    }

    @Transactional
    public GaleriaResponse create(GaleriaRequest request) {
        GaleriaImagen galeria = new GaleriaImagen();
        galeria.setTitulo(request.getTitulo());
        galeria.setDescripcion(request.getDescripcion());
        galeria.setFecha(request.getFecha());
        galeria.setAcceso(accesoRepository.findById(request.getAccesoId() == null ? AccesoConstants.ACTIVO : request.getAccesoId()).orElseThrow());
        GaleriaImagen saved = galeriaImagenRepository.save(galeria);
        crearDetalles(saved, request.getDetalles());
        return toResponse(saved);
    }

    @Transactional
    public GaleriaResponse update(Integer id, GaleriaRequest request) {
        GaleriaImagen galeria = findGaleria(id);
        galeria.setTitulo(request.getTitulo());
        galeria.setDescripcion(request.getDescripcion());
        galeria.setFecha(request.getFecha());
        if (request.getAccesoId() != null) {
            galeria.setAcceso(accesoRepository.findById(request.getAccesoId()).orElseThrow());
        }
        galeriaImagenRepository.save(galeria);
        galeriaDetalleRepository.findByGaleriaIdGaleria(galeria.getIdGaleria())
                .forEach(d -> d.setAcceso(accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow()));
        crearDetalles(galeria, request.getDetalles());
        return toResponse(galeria);
    }

    @Transactional
    public void delete(Integer id) {
        GaleriaImagen galeria = findGaleria(id);
        galeria.setAcceso(accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow());
        galeriaDetalleRepository.findByGaleriaIdGaleria(galeria.getIdGaleria())
                .forEach(d -> d.setAcceso(accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow()));
        galeriaImagenRepository.save(galeria);
    }

    @Transactional
    public GaleriaResponse agregarFotos(Integer idGaleria, List<MultipartFile> fotos) {
        GaleriaImagen galeria = findGaleria(idGaleria);
        int orden = galeriaDetalleRepository.findByGaleriaIdGaleria(idGaleria).size();
        for (MultipartFile foto : fotos) {
            UploadResponse upload = cloudinaryService.upload(foto, "portal/galerias");
            GaleriaDetalle detalle = new GaleriaDetalle();
            detalle.setGaleria(galeria);
            detalle.setImagenUrl(upload.getUrl());
            detalle.setPkUrlFoto(upload.getPublicId());
            detalle.setOrden(++orden);
            detalle.setAcceso(accesoRepository.findById(AccesoConstants.ACTIVO).orElseThrow());
            galeriaDetalleRepository.save(detalle);
        }
        return toResponse(galeria);
    }

    @Transactional
    public void eliminarDetalle(Integer idDetalle) {
        GaleriaDetalle detalle = galeriaDetalleRepository.findById(idDetalle)
                .orElseThrow(() -> new ResourceNotFoundException("Detalle de galería no encontrado con id " + idDetalle));
        if (detalle.getPkUrlFoto() != null && !detalle.getPkUrlFoto().isBlank()) {
            cloudinaryService.delete(detalle.getPkUrlFoto());
        }
        detalle.setAcceso(accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow());
        galeriaDetalleRepository.save(detalle);
    }

    private void crearDetalles(GaleriaImagen galeria, List<GaleriaDetalleRequest> detalles) {
        if (detalles == null || detalles.isEmpty()) {
            return;
        }
        for (GaleriaDetalleRequest detalleRequest : detalles) {
            GaleriaDetalle detalle = new GaleriaDetalle();
            detalle.setGaleria(galeria);
            detalle.setImagenUrl(detalleRequest.getImagenUrl());
            detalle.setOrden(detalleRequest.getOrden());
            detalle.setAcceso(accesoRepository.findById(AccesoConstants.ACTIVO).orElseThrow());
            galeriaDetalleRepository.save(detalle);
        }
    }

    private GaleriaImagen findGaleria(Integer id) {
        return galeriaImagenRepository.findByIdGaleriaAndAccesoNot(id, accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow())
                .orElseThrow(() -> new ResourceNotFoundException("Galería no encontrada con id " + id));
    }

    private GaleriaResponse toResponse(GaleriaImagen galeria) {
        GaleriaResponse response = new GaleriaResponse();
        response.setIdGaleria(galeria.getIdGaleria());
        response.setTitulo(galeria.getTitulo());
        response.setDescripcion(galeria.getDescripcion());
        response.setFecha(galeria.getFecha());
        response.setAccesoId(galeria.getAcceso().getIdAcceso().longValue());
        response.setDetalles(galeriaDetalleRepository
                .findByGaleriaIdGaleriaAndAccesoNot(galeria.getIdGaleria(), accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow()).stream()
                .map(this::toDetalleResponse)
                .toList());
        return response;
    }

    private GaleriaDetalleResponse toDetalleResponse(GaleriaDetalle detalle) {
        GaleriaDetalleResponse response = new GaleriaDetalleResponse();
        response.setIdDetalle(detalle.getIdDetalle());
        response.setImagenUrl(detalle.getImagenUrl());
        response.setOrden(detalle.getOrden());
        return response;
    }
}