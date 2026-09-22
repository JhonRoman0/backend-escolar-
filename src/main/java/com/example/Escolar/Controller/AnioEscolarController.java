package com.example.Escolar.Controller;

import com.example.Escolar.Dto.AnioEscolarRequest;
import com.example.Escolar.Dto.AnioEscolarResponse;
import com.example.Escolar.Service.AnioEscolarService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/anios-escolares")
@RequiredArgsConstructor
public class AnioEscolarController {

    private final AnioEscolarService anioEscolarService;

    @GetMapping
    public List<AnioEscolarResponse> getAll() {
        return anioEscolarService.getAll();
    }

    @GetMapping("/{id}")
    public AnioEscolarResponse getById(@PathVariable Integer id) {
        return anioEscolarService.getById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AnioEscolarResponse create(@Valid @RequestBody AnioEscolarRequest request) {
        return anioEscolarService.create(request);
    }

    @PutMapping("/{id}")
    public AnioEscolarResponse update(@PathVariable Integer id, @Valid @RequestBody AnioEscolarRequest request) {
        return anioEscolarService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        anioEscolarService.delete(id);
        return ResponseEntity.noContent().build();
    }
}