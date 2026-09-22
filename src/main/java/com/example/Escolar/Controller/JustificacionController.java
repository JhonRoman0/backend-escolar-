package com.example.Escolar.Controller;

import com.example.Escolar.Dto.JustificacionRequest;
import com.example.Escolar.Dto.JustificacionResponse;
import com.example.Escolar.Service.JustificacionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/justificaciones")
@RequiredArgsConstructor
public class JustificacionController {

    private final JustificacionService justificacionService;

    @GetMapping
    public List<JustificacionResponse> getAll() {
        return justificacionService.getAll();
    }

    @GetMapping("/{id}")
    public JustificacionResponse getById(@PathVariable Integer id) {
        return justificacionService.getById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public JustificacionResponse create(@Valid @RequestBody JustificacionRequest request) {
        return justificacionService.create(request);
    }

    @PutMapping("/{id}")
    public JustificacionResponse update(@PathVariable Integer id, @Valid @RequestBody JustificacionRequest request) {
        return justificacionService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        justificacionService.delete(id);
        return ResponseEntity.noContent().build();
    }
}