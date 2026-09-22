package com.example.Escolar.Controller;

import com.example.Escolar.Dto.SuspensionDocenteRequest;
import com.example.Escolar.Dto.SuspensionDocenteResponse;
import com.example.Escolar.Service.SuspensionDocenteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/suspensiones")
@RequiredArgsConstructor
public class SuspensionDocenteController {

    private final SuspensionDocenteService suspensionDocenteService;

    @GetMapping
    public List<SuspensionDocenteResponse> getAll() {
        return suspensionDocenteService.getAll();
    }

    @GetMapping("/{id}")
    public SuspensionDocenteResponse getById(@PathVariable Integer id) {
        return suspensionDocenteService.getById(id);
    }

    @GetMapping("/docente/{idDocente}")
    public List<SuspensionDocenteResponse> getByDocente(@PathVariable Integer idDocente) {
        return suspensionDocenteService.getByDocente(idDocente);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SuspensionDocenteResponse create(@Valid @RequestBody SuspensionDocenteRequest request) {
        return suspensionDocenteService.create(request);
    }

    @PutMapping("/{id}/finalizar")
    public SuspensionDocenteResponse finalizar(@PathVariable Integer id) {
        return suspensionDocenteService.finalizar(id);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Integer id) {
        suspensionDocenteService.delete(id);
    }
}
