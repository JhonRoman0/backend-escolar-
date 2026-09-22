package com.example.Escolar.Controller;

import com.example.Escolar.Dto.ContactoActualizarRequest;
import com.example.Escolar.Dto.ContactoMensajeResponse;
import com.example.Escolar.Service.AccesoContextoService;
import com.example.Escolar.Service.ContactoMensajeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/contactos")
@RequiredArgsConstructor
public class ContactoController {

    private final ContactoMensajeService contactoMensajeService;
    private final AccesoContextoService accesoContextoService;

    @GetMapping
    public List<ContactoMensajeResponse> getAll() {
        return contactoMensajeService.getAll();
    }

    @GetMapping("/{id}")
    public ContactoMensajeResponse getById(@PathVariable Integer id) {
        return contactoMensajeService.getById(id);
    }

    @PutMapping("/{id}")
    public ContactoMensajeResponse actualizarEstado(@PathVariable Integer id,
                                                    @Valid @RequestBody ContactoActualizarRequest request) {
        return contactoMensajeService.actualizarEstado(id, request, accesoContextoService.idUsuarioAutenticado());
    }
}