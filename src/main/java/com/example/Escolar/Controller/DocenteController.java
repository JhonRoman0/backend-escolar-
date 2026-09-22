package com.example.Escolar.Controller;

import com.example.Escolar.Dto.DocenteReporteResponse;
import com.example.Escolar.Dto.DocenteRequest;
import com.example.Escolar.Dto.DocenteResponse;
import com.example.Escolar.Service.DocenteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/docentes")
@RequiredArgsConstructor
public class DocenteController {

    private final DocenteService docenteService;

    @GetMapping
    public List<DocenteResponse> getAll() {
        return docenteService.getAll();
    }

    @GetMapping("/{id}")
    public DocenteResponse getById(@PathVariable Integer id) {
        return docenteService.getById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DocenteResponse create(@Valid @RequestBody DocenteRequest request) {
        return docenteService.create(request);
    }

    @PutMapping("/{id}")
    public DocenteResponse update(@PathVariable Integer id, @Valid @RequestBody DocenteRequest request) {
        return docenteService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        docenteService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/reporte")
    public List<DocenteReporteResponse> reporte(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fin,
            @RequestParam(required = false) String especialidad,
            @RequestParam(required = false) String tipoContrato) {
        return docenteService.reporte(inicio, fin, especialidad, tipoContrato);
    }
}