package com.example.Escolar.Controller;

import com.example.Escolar.Dto.ModuloRequest;
import com.example.Escolar.Dto.ModuloResponse;
import com.example.Escolar.Service.ModuloService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/modulos")
@RequiredArgsConstructor
public class ModuloController {

    private final ModuloService moduloService;

    @GetMapping
    public List<ModuloResponse> getAll() {
        return moduloService.getAll();
    }

    @GetMapping("/{id}")
    public ModuloResponse getById(@PathVariable Integer id) {
        return moduloService.getById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ModuloResponse create(@Valid @RequestBody ModuloRequest request) {
        return moduloService.create(request);
    }

    @PutMapping("/{id}")
    public ModuloResponse update(@PathVariable Integer id, @Valid @RequestBody ModuloRequest request) {
        return moduloService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        moduloService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
