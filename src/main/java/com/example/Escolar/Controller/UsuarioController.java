package com.example.Escolar.Controller;

import com.example.Escolar.Dto.RolResponse;
import com.example.Escolar.Dto.UsuarioReporteResponse;
import com.example.Escolar.Dto.UsuarioRequest;
import com.example.Escolar.Dto.UsuarioResponse;
import com.example.Escolar.Service.UsuarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/usuarios")
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioService usuarioService;

    @GetMapping
    public Page<UsuarioResponse> getAll(@PageableDefault(size = 10, sort = "idUsuario") Pageable pageable) {
        return usuarioService.getAll(pageable);
    }

    @GetMapping("/{id}")
    public UsuarioResponse getById(@PathVariable Integer id) {
        return usuarioService.getById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioResponse create(@Valid @RequestBody UsuarioRequest request) {
        return usuarioService.create(request);
    }

    @PutMapping("/{id}")
    public UsuarioResponse update(@PathVariable Integer id, @Valid @RequestBody UsuarioRequest request) {
        return usuarioService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        usuarioService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/rol/{idRol}")
    public List<UsuarioResponse> getUsuariosPorRol(@PathVariable Integer idRol) {
        return usuarioService.getUsuariosPorRol(idRol);
    }

    @GetMapping("/{id}/roles")
    public List<RolResponse> getRolesDelUsuario(@PathVariable Integer id) {
        return usuarioService.getRolesDelUsuario(id);
    }

    @PostMapping(value = "/{id}/foto", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public UsuarioResponse subirFoto(@PathVariable Integer id, @RequestPart("foto") MultipartFile foto) {
        return usuarioService.subirFoto(id, foto);
    }

    @DeleteMapping("/{id}/foto")
    public ResponseEntity<Void> eliminarFoto(@PathVariable Integer id) {
        usuarioService.eliminarFoto(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/reporte")
    public List<UsuarioReporteResponse> reporte(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fin,
            @RequestParam(required = false) Integer idRol) {
        return usuarioService.reporte(inicio, fin, idRol);
    }
}
