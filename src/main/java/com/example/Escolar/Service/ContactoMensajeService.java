package com.example.Escolar.Service;

import com.example.Escolar.Dto.ContactoActualizarRequest;
import com.example.Escolar.Dto.ContactoMensajeRequest;
import com.example.Escolar.Dto.ContactoMensajeResponse;
import com.example.Escolar.Exception.ResourceNotFoundException;
import com.example.Escolar.Model.ContactoMensaje;
import com.example.Escolar.Model.Usuario;
import com.example.Escolar.Repository.AccesoRepository;
import com.example.Escolar.Repository.ContactoMensajeRepository;
import com.example.Escolar.Repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ContactoMensajeService {

    public static final byte PENDIENTE = 1;

    private final ContactoMensajeRepository contactoMensajeRepository;
    private final UsuarioRepository usuarioRepository;
    private final AccesoRepository accesoRepository;

    public List<ContactoMensajeResponse> getAll() {
        return contactoMensajeRepository.findByAccesoNot(accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow()).stream()
                .map(this::toResponse)
                .toList();
    }

    public ContactoMensajeResponse getById(Integer id) {
        return toResponse(findMensaje(id));
    }

    @Transactional
    public ContactoMensajeResponse crearPublico(ContactoMensajeRequest request) {
        ContactoMensaje mensaje = new ContactoMensaje();
        mensaje.setNombreRemitente(request.getNombreRemitente());
        mensaje.setCorreo(request.getCorreo());
        mensaje.setCelular(request.getCelular());
        mensaje.setAsunto(request.getAsunto());
        mensaje.setMensaje(request.getMensaje());
        mensaje.setFechaEnvio(LocalDateTime.now());
        mensaje.setEstado(PENDIENTE);
        mensaje.setAcceso(accesoRepository.findById(AccesoConstants.ACTIVO).orElseThrow());
        return toResponse(contactoMensajeRepository.save(mensaje));
    }

    @Transactional
    public ContactoMensajeResponse actualizarEstado(Integer id, ContactoActualizarRequest request, Integer idUsuario) {
        ContactoMensaje mensaje = findMensaje(id);
        if (request.getEstado() != null) {
            mensaje.setEstado(request.getEstado());
        }
        if (idUsuario != null) {
            mensaje.setUsuario(findUsuario(idUsuario));
        }
        return toResponse(contactoMensajeRepository.save(mensaje));
    }

    @Transactional
    public void delete(Integer id) {
        ContactoMensaje mensaje = findMensaje(id);
        mensaje.setAcceso(accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow());
        contactoMensajeRepository.save(mensaje);
    }

    private ContactoMensaje findMensaje(Integer id) {
        return contactoMensajeRepository.findByIdMensajeAndAccesoNot(id, accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow())
                .orElseThrow(() -> new ResourceNotFoundException("Mensaje de contacto no encontrado con id " + id));
    }

    private Usuario findUsuario(Integer id) {
        return usuarioRepository.findByIdUsuarioAndAccesoNot(id, accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con id " + id));
    }

    private ContactoMensajeResponse toResponse(ContactoMensaje mensaje) {
        ContactoMensajeResponse response = new ContactoMensajeResponse();
        response.setIdMensaje(mensaje.getIdMensaje());
        response.setNombreRemitente(mensaje.getNombreRemitente());
        response.setCorreo(mensaje.getCorreo());
        response.setCelular(mensaje.getCelular());
        response.setAsunto(mensaje.getAsunto());
        response.setMensaje(mensaje.getMensaje());
        response.setFechaEnvio(mensaje.getFechaEnvio());
        response.setEstado(mensaje.getEstado());
        if (mensaje.getUsuario() != null) {
            response.setIdUsuario(mensaje.getUsuario().getIdUsuario());
            response.setAtendidoPor(mensaje.getUsuario().getNombre() + " " + mensaje.getUsuario().getApellidoPat());
        }
        response.setAccesoId(mensaje.getAcceso().getIdAcceso().longValue());
        return response;
    }
}