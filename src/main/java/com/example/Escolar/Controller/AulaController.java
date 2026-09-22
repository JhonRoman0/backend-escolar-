package com.example.Escolar.Controller;

import com.example.Escolar.Dto.AulaRequest;
import com.example.Escolar.Dto.AulaResponse;
import com.example.Escolar.Service.AulaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/aulas")
@RequiredArgsConstructor
public class AulaController {

    private final AulaService aulaService;

    @GetMapping
    public List<AulaResponse> getAll() {
        return aulaService.getAll();
    }

    @GetMapping("/{id}")
    public AulaResponse getById(@PathVariable Integer id) {
        return aulaService.getById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AulaResponse create(@Valid @RequestBody AulaRequest request) {
        return aulaService.create(request);
    }

    @PutMapping("/{id}")
    public AulaResponse update(@PathVariable Integer id, @Valid @RequestBody AulaRequest request) {
        return aulaService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        aulaService.delete(id);
        return ResponseEntity.noContent().build();
    }
}