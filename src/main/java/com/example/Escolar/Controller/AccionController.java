package com.example.Escolar.Controller;

import com.example.Escolar.Dto.AccionRequest;
import com.example.Escolar.Dto.AccionResponse;
import com.example.Escolar.Service.AccionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Catálogo de solo lectura: lo define el sistema (CatalogoPermisos), no se edita por API. */
@RestController
@RequestMapping("/acciones")
@RequiredArgsConstructor
public class AccionController {

    private final AccionService accionService;

    @GetMapping
    public List<AccionResponse> getAll() {
        return accionService.getAll();
    }

    @GetMapping("/{id}")
    public AccionResponse getById(@PathVariable Integer id) {
        return accionService.getById(id);
    }
}
