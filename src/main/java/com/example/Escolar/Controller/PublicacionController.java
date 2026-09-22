package com.example.Escolar.Controller;

import com.example.Escolar.Dto.PublicacionRequest;
import com.example.Escolar.Dto.PublicacionResponse;
import com.example.Escolar.Service.AccesoContextoService;
import com.example.Escolar.Service.PublicacionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/publicaciones")
@RequiredArgsConstructor
public class PublicacionController {

    private final PublicacionService publicacionService;
    private final AccesoContextoService accesoContextoService;

    @GetMapping
    public List<PublicacionResponse> getAll() {
        return publicacionService.getAll();
    }

    @GetMapping("/{id}")
    public PublicacionResponse getById(@PathVariable Integer id) {
        return publicacionService.getById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PublicacionResponse create(@Valid @RequestBody PublicacionRequest request) {
        return publicacionService.create(request, accesoContextoService.idUsuarioAutenticado());
    }

    @PutMapping("/{id}")
    public PublicacionResponse update(@PathVariable Integer id, @Valid @RequestBody PublicacionRequest request) {
        return publicacionService.update(id, request, accesoContextoService.idUsuarioAutenticado());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        publicacionService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/{id}/imagen", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public PublicacionResponse subirImagen(@PathVariable Integer id, @RequestPart("imagen") MultipartFile imagen) {
        return publicacionService.subirImagen(id, imagen);
    }

    @DeleteMapping("/{id}/imagen")
    public ResponseEntity<Void> eliminarFoto(@PathVariable Integer id) {
        publicacionService.eliminarFoto(id);
        return ResponseEntity.noContent().build();
    }
}