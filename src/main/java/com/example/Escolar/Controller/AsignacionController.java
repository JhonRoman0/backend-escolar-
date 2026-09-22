package com.example.Escolar.Controller;

import com.example.Escolar.Dto.AsignacionRequest;
import com.example.Escolar.Dto.AsignacionResponse;
import com.example.Escolar.Dto.HorasDocenteResponse;
import com.example.Escolar.Service.AccesoContextoService;
import com.example.Escolar.Service.AsignacionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/asignaciones")
@RequiredArgsConstructor
public class AsignacionController {

    private final AsignacionService asignacionService;
    private final AccesoContextoService accesoContextoService;

    @GetMapping
    public List<AsignacionResponse> getAll() {
        return asignacionService.getAll(accesoContextoService.idUsuarioAutenticado(), accesoContextoService.rolesAutenticados());
    }

    @GetMapping("/{id}")
    public AsignacionResponse getById(@PathVariable Integer id) {
        return asignacionService.getById(id, accesoContextoService.idUsuarioAutenticado(), accesoContextoService.rolesAutenticados());
    }

    @GetMapping("/docente/{idDocente}")
    public List<AsignacionResponse> getByDocente(@PathVariable Integer idDocente) {
        return asignacionService.getByDocente(idDocente, accesoContextoService.idUsuarioAutenticado(), accesoContextoService.rolesAutenticados());
    }

    @GetMapping("/docente/{idDocente}/horas")
    public HorasDocenteResponse horasDocente(@PathVariable Integer idDocente,
                                             @RequestParam(required = false) String periodo) {
        return asignacionService.calcularHorasDocente(idDocente, periodo);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AsignacionResponse create(@Valid @RequestBody AsignacionRequest request) {
        return asignacionService.create(request);
    }

    @PutMapping("/{id}")
    public AsignacionResponse update(@PathVariable Integer id, @Valid @RequestBody AsignacionRequest request) {
        return asignacionService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        asignacionService.delete(id);
        return ResponseEntity.noContent().build();
    }
}