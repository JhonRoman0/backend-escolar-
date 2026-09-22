package com.example.Escolar.Controller;

import com.example.Escolar.Dto.CursoRequest;
import com.example.Escolar.Dto.CursoResponse;
import com.example.Escolar.Service.CursoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/cursos")
@RequiredArgsConstructor
public class CursoController {

    private final CursoService cursoService;

    @GetMapping
    public List<CursoResponse> getAll() {
        return cursoService.getAll();
    }

    @GetMapping("/{id}")
    public CursoResponse getById(@PathVariable Integer id) {
        return cursoService.getById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CursoResponse create(@Valid @RequestBody CursoRequest request) {
        return cursoService.create(request);
    }

    @PutMapping("/{id}")
    public CursoResponse update(@PathVariable Integer id, @Valid @RequestBody CursoRequest request) {
        return cursoService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        cursoService.delete(id);
        return ResponseEntity.noContent().build();
    }
}