package com.example.Escolar.Controller;

import com.example.Escolar.Dto.RecreoRequest;
import com.example.Escolar.Dto.RecreoResponse;
import com.example.Escolar.Service.RecreoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/recreos")
@RequiredArgsConstructor
public class RecreoController {

    private final RecreoService recreoService;

    @GetMapping
    public List<RecreoResponse> getAll() {
        return recreoService.getAll();
    }

    @GetMapping("/{id}")
    public RecreoResponse getById(@PathVariable Integer id) {
        return recreoService.getById(id);
    }

    @GetMapping("/nivel/{idNivel}")
    public List<RecreoResponse> getByNivel(@PathVariable Integer idNivel) {
        return recreoService.getByNivel(idNivel);
    }

    @GetMapping("/nivel/{idNivel}/dia/{diaSemana}")
    public List<RecreoResponse> getByNivelAndDia(@PathVariable Integer idNivel, @PathVariable Byte diaSemana) {
        return recreoService.getByNivelAndDia(idNivel, diaSemana);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RecreoResponse create(@Valid @RequestBody RecreoRequest request) {
        return recreoService.create(request);
    }

    @PutMapping("/{id}")
    public RecreoResponse update(@PathVariable Integer id, @Valid @RequestBody RecreoRequest request) {
        return recreoService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Integer id) {
        recreoService.delete(id);
    }
}
