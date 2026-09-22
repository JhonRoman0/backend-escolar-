package com.example.Escolar.Service;

import com.example.Escolar.Dto.PublicacionRequest;
import com.example.Escolar.Dto.PublicacionResponse;
import com.example.Escolar.Dto.UploadResponse;
import com.example.Escolar.Exception.ResourceNotFoundException;
import com.example.Escolar.Model.Publicacion;
import com.example.Escolar.Model.Usuario;
import com.example.Escolar.Repository.AccesoRepository;
import com.example.Escolar.Repository.PublicacionRepository;
import com.example.Escolar.Repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class PublicacionService {

    public static final byte ESTADO_PUBLICADO = 1;

    private final PublicacionRepository publicacionRepository;
    private final UsuarioRepository usuarioRepository;
    private final CloudinaryService cloudinaryService;
    private final AccesoRepository accesoRepository;

    public List<PublicacionResponse> getAll() {
        return publicacionRepository.findByAccesoNot(accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow()).stream()
                .map(this::toResponse)
                .toList();
    }

    public List<PublicacionResponse> getPublicas() {
        return publicacionRepository.findByEstadoAndAccesoNot(ESTADO_PUBLICADO, accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow()).stream()
                .map(this::toResponse)
                .toList();
    }

    public PublicacionResponse getById(Integer id) {
        return toResponse(findPublicacion(id));
    }

    @Transactional
    public PublicacionResponse create(PublicacionRequest request, Integer idUsuario) {
        Publicacion publicacion = new Publicacion();
        publicacion.setUsuario(findUsuario(idUsuario));
        aplicarDatos(publicacion, request);
        publicacion.setFechaPublicacion(LocalDateTime.now());
        publicacion.setAcceso(accesoRepository.findById(AccesoConstants.ACTIVO).orElseThrow());
        return toResponse(publicacionRepository.save(publicacion));
    }

    @Transactional
    public PublicacionResponse update(Integer id, PublicacionRequest request, Integer idUsuario) {
        Publicacion publicacion = findPublicacion(id);
        aplicarDatos(publicacion, request);
        publicacion.setFechaActualizacion(LocalDateTime.now());
        publicacion.setUsuario(findUsuario(idUsuario));
        if (request.getAccesoId() != null) {
            publicacion.setAcceso(accesoRepository.findById(request.getAccesoId()).orElseThrow());
        }
        return toResponse(publicacionRepository.save(publicacion));
    }

    @Transactional
    public void delete(Integer id) {
        Publicacion publicacion = findPublicacion(id);
        publicacion.setAcceso(accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow());
        publicacionRepository.save(publicacion);
    }

    @Transactional
    public PublicacionResponse subirImagen(Integer id, MultipartFile file) {
        Publicacion publicacion = findPublicacion(id);
        if (publicacion.getPkUrlFoto() != null && !publicacion.getPkUrlFoto().isBlank()) {
            cloudinaryService.delete(publicacion.getPkUrlFoto());
        }
        UploadResponse upload = cloudinaryService.upload(file, "portal/publicaciones");
        publicacion.setImagenPortadaUrl(upload.getUrl());
        publicacion.setPkUrlFoto(upload.getPublicId());
        return toResponse(publicacionRepository.save(publicacion));
    }

    @Transactional
    public void eliminarFoto(Integer id) {
        Publicacion publicacion = findPublicacion(id);
        if (publicacion.getPkUrlFoto() != null && !publicacion.getPkUrlFoto().isBlank()) {
            cloudinaryService.delete(publicacion.getPkUrlFoto());
            publicacion.setImagenPortadaUrl(null);
            publicacion.setPkUrlFoto(null);
            publicacionRepository.save(publicacion);
        }
    }

    private void aplicarDatos(Publicacion publicacion, PublicacionRequest request) {
        publicacion.setTitulo(request.getTitulo());
        publicacion.setSlug(request.getSlug() == null || request.getSlug().isBlank()
                ? generarSlug(request.getTitulo()) : request.getSlug());
        publicacion.setContenido(request.getContenido());
        publicacion.setImagenPortadaUrl(request.getImagenPortadaUrl());
        publicacion.setCategoria(request.getCategoria());
        publicacion.setEsDestacado(request.getEsDestacado() == null ? 0 : request.getEsDestacado());
        publicacion.setEstado(request.getEstado() == null ? ESTADO_PUBLICADO : request.getEstado());
    }

    private String generarSlug(String titulo) {
        return titulo.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9\\s]", "")
                .trim()
                .replaceAll("\\s+", "-");
    }

    private Publicacion findPublicacion(Integer id) {
        return publicacionRepository.findByIdPublicacionAndAccesoNot(id, accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow())
                .orElseThrow(() -> new ResourceNotFoundException("Publicación no encontrada con id " + id));
    }

    private Usuario findUsuario(Integer id) {
        if (id == null) {
            throw new IllegalArgumentException("No se pudo identificar al usuario que realiza la operación");
        }
        return usuarioRepository.findByIdUsuarioAndAccesoNot(id, accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con id " + id));
    }

    private PublicacionResponse toResponse(Publicacion publicacion) {
        PublicacionResponse response = new PublicacionResponse();
        response.setIdPublicacion(publicacion.getIdPublicacion());
        response.setIdUsuario(publicacion.getUsuario().getIdUsuario());
        response.setAutor(publicacion.getUsuario().getNombre() + " " + publicacion.getUsuario().getApellidoPat());
        response.setTitulo(publicacion.getTitulo());
        response.setSlug(publicacion.getSlug());
        response.setContenido(publicacion.getContenido());
        response.setImagenPortadaUrl(publicacion.getImagenPortadaUrl());
        response.setCategoria(publicacion.getCategoria());
        response.setEsDestacado(publicacion.getEsDestacado());
        response.setEstado(publicacion.getEstado());
        response.setFechaPublicacion(publicacion.getFechaPublicacion());
        response.setFechaActualizacion(publicacion.getFechaActualizacion());
        response.setAccesoId(publicacion.getAcceso().getIdAcceso().longValue());
        return response;
    }
}