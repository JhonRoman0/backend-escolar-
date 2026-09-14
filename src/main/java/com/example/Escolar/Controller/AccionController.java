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

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AccionResponse create(@Valid @RequestBody AccionRequest request) {
        return accionService.create(request);
    }

    @PutMapping("/{id}")
    public AccionResponse update(@PathVariable Integer id, @Valid @RequestBody AccionRequest request) {
        return accionService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        accionService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
