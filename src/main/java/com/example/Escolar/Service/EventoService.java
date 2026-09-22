package com.example.Escolar.Service;

import com.example.Escolar.Dto.EventoRequest;
import com.example.Escolar.Dto.EventoResponse;
import com.example.Escolar.Dto.UploadResponse;
import com.example.Escolar.Exception.ResourceNotFoundException;
import com.example.Escolar.Model.EventoEscolar;
import com.example.Escolar.Repository.AccesoRepository;
import com.example.Escolar.Repository.EventoEscolarRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EventoService {

    private final EventoEscolarRepository eventoEscolarRepository;
    private final AccesoRepository accesoRepository;
    private final CloudinaryService cloudinaryService;

    public List<EventoResponse> getAll() {
        return eventoEscolarRepository.findByAccesoNot(accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow()).stream()
                .map(this::toResponse)
                .toList();
    }

    public List<EventoResponse> getPublicos() {
        return eventoEscolarRepository.findByEsPublicoAndAccesoNot((byte) 1, accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow()).stream()
                .map(this::toResponse)
                .toList();
    }

    public EventoResponse getById(Integer id) {
        return toResponse(findEvento(id));
    }

    @Transactional
    public EventoResponse create(EventoRequest request) {
        EventoEscolar evento = new EventoEscolar();
        aplicarDatos(evento, request);
        evento.setAcceso(accesoRepository.findById(AccesoConstants.ACTIVO).orElseThrow());
        return toResponse(eventoEscolarRepository.save(evento));
    }

    @Transactional
    public EventoResponse update(Integer id, EventoRequest request) {
        EventoEscolar evento = findEvento(id);
        aplicarDatos(evento, request);
        if (request.getAccesoId() != null) {
            evento.setAcceso(accesoRepository.findById(request.getAccesoId()).orElseThrow());
        }
        return toResponse(eventoEscolarRepository.save(evento));
    }

    @Transactional
    public void delete(Integer id) {
        EventoEscolar evento = findEvento(id);
        evento.setAcceso(accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow());
        eventoEscolarRepository.save(evento);
    }

    @Transactional
    public EventoResponse subirImagen(Integer id, MultipartFile file) {
        EventoEscolar evento = findEvento(id);
        if (evento.getPkUrlFoto() != null && !evento.getPkUrlFoto().isBlank()) {
            cloudinaryService.delete(evento.getPkUrlFoto());
        }
        UploadResponse upload = cloudinaryService.upload(file, "portal/eventos");
        evento.setImagenUrl(upload.getUrl());
        evento.setPkUrlFoto(upload.getPublicId());
        return toResponse(eventoEscolarRepository.save(evento));
    }

    @Transactional
    public void eliminarFoto(Integer id) {
        EventoEscolar evento = findEvento(id);
        if (evento.getPkUrlFoto() != null && !evento.getPkUrlFoto().isBlank()) {
            cloudinaryService.delete(evento.getPkUrlFoto());
            evento.setImagenUrl(null);
            evento.setPkUrlFoto(null);
            eventoEscolarRepository.save(evento);
        }
    }

    private void aplicarDatos(EventoEscolar evento, EventoRequest request) {
        evento.setTitulo(request.getTitulo());
        evento.setDescripcion(request.getDescripcion());
        evento.setLugar(request.getLugar());
        evento.setFechaInicio(request.getFechaInicio());
        evento.setFechaFin(request.getFechaFin());
        evento.setEsPublico(request.getEsPublico() == null ? 1 : request.getEsPublico());
    }

    private EventoEscolar findEvento(Integer id) {
        return eventoEscolarRepository.findByIdEventoAndAccesoNot(id, accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow())
                .orElseThrow(() -> new ResourceNotFoundException("Evento no encontrado con id " + id));
    }

    private EventoResponse toResponse(EventoEscolar evento) {
        EventoResponse response = new EventoResponse();
        response.setIdEvento(evento.getIdEvento());
        response.setTitulo(evento.getTitulo());
        response.setDescripcion(evento.getDescripcion());
        response.setLugar(evento.getLugar());
        response.setFechaInicio(evento.getFechaInicio());
        response.setFechaFin(evento.getFechaFin());
        response.setEsPublico(evento.getEsPublico());
        response.setImagenUrl(evento.getImagenUrl());
        response.setAccesoId(evento.getAcceso().getIdAcceso().longValue());
        return response;
    }
}
