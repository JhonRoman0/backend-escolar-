package com.example.Escolar.Controller;

import com.example.Escolar.Dto.PermisoRequest;
import com.example.Escolar.Dto.PermisoResponse;
import com.example.Escolar.Service.PermisoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Catálogo de solo lectura: lo define el sistema (CatalogoPermisos), no se edita por API. */
@RestController
@RequestMapping("/permisos")
@RequiredArgsConstructor
public class PermisoController {

    private final PermisoService permisoService;

    @GetMapping
    public List<PermisoResponse> getAll() {
        return permisoService.getAll();
    }

    @GetMapping("/{id}")
    public PermisoResponse getById(@PathVariable Integer id) {
        return permisoService.getById(id);
    }
}
