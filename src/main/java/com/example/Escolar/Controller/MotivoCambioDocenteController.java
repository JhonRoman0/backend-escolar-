package com.example.Escolar.Controller;

import com.example.Escolar.Dto.MotivoCambioDocenteRequest;
import com.example.Escolar.Dto.MotivoCambioDocenteResponse;
import com.example.Escolar.Model.TipoCambioDocente;
import com.example.Escolar.Service.MotivoCambioDocenteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/motivos-cambio-docente")
@RequiredArgsConstructor
public class MotivoCambioDocenteController {

    private final MotivoCambioDocenteService motivoService;

    @GetMapping
    public List<MotivoCambioDocenteResponse> getAll(@RequestParam(required = false) TipoCambioDocente tipo) {
        return tipo == null ? motivoService.getAll() : motivoService.getByTipo(tipo);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MotivoCambioDocenteResponse create(@Valid @RequestBody MotivoCambioDocenteRequest request) {
        return motivoService.create(request);
    }

    @PutMapping("/{id}")
    public MotivoCambioDocenteResponse update(@PathVariable Integer id,
                                              @Valid @RequestBody MotivoCambioDocenteRequest request) {
        return motivoService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Integer id) {
        motivoService.delete(id);
    }
}
