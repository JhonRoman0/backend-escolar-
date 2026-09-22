package com.example.Escolar.Controller;

import com.example.Escolar.Dto.PermisosRolResponse;
import com.example.Escolar.Dto.RolPermisoRequest;
import com.example.Escolar.Dto.RolPermisoResponse;
import com.example.Escolar.Service.RolPermisoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/roles-permiso")
@RequiredArgsConstructor
public class RolPermisoController {

    private final RolPermisoService rolPermisoService;

    @GetMapping
    public List<RolPermisoResponse> getAll() {
        return rolPermisoService.getAll();
    }

    @GetMapping("/{id}")
    public RolPermisoResponse getById(@PathVariable Integer id) {
        return rolPermisoService.getById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RolPermisoResponse create(@Valid @RequestBody RolPermisoRequest request) {
        return rolPermisoService.create(request);
    }

    @PutMapping("/{id}")
    public RolPermisoResponse update(@PathVariable Integer id, @Valid @RequestBody RolPermisoRequest request) {
        return rolPermisoService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        rolPermisoService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/rol/{idRol}")
    public PermisosRolResponse obtenerPermisosDelRol(@PathVariable Integer idRol) {
        return rolPermisoService.obtenerPermisosDelRol(idRol);
    }
}
