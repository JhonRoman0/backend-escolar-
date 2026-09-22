package com.example.Escolar.Controller;

import com.example.Escolar.Dto.DiaFeriadoRequest;
import com.example.Escolar.Dto.DiaFeriadoResponse;
import com.example.Escolar.Service.DiaFeriadoService;
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
@RequestMapping("/dias-feriados")
@RequiredArgsConstructor
public class DiaFeriadoController {

    private final DiaFeriadoService diaFeriadoService;

    @GetMapping
    public List<DiaFeriadoResponse> getAll() {
        return diaFeriadoService.getAll();
    }

    @GetMapping("/{id}")
    public DiaFeriadoResponse getById(@PathVariable Integer id) {
        return diaFeriadoService.getById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DiaFeriadoResponse create(@Valid @RequestBody DiaFeriadoRequest request) {
        return diaFeriadoService.create(request);
    }

    @PutMapping("/{id}")
    public DiaFeriadoResponse update(@PathVariable Integer id, @Valid @RequestBody DiaFeriadoRequest request) {
        return diaFeriadoService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        diaFeriadoService.delete(id);
        return ResponseEntity.noContent().build();
    }
}