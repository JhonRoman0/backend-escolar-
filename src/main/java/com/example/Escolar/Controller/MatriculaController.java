package com.example.Escolar.Controller;

import com.example.Escolar.Dto.MatriculaReporteResponse;
import com.example.Escolar.Dto.MatriculaRequest;
import com.example.Escolar.Dto.MatriculaResponse;
import com.example.Escolar.Service.AccesoContextoService;
import com.example.Escolar.Service.MatriculaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/matriculas")
@RequiredArgsConstructor
public class MatriculaController {

    private final MatriculaService matriculaService;
    private final AccesoContextoService accesoContextoService;

    @GetMapping
    public Page<MatriculaResponse> getAll(@PageableDefault(size = 10, sort = "idMatricula") Pageable pageable) {
        return matriculaService.getAll(pageable, accesoContextoService.idUsuarioAutenticado(), accesoContextoService.rolesAutenticados());
    }

    @GetMapping("/{id}")
    public MatriculaResponse getById(@PathVariable Integer id) {
        return matriculaService.getById(id, accesoContextoService.idUsuarioAutenticado(), accesoContextoService.rolesAutenticados());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MatriculaResponse create(@Valid @RequestBody MatriculaRequest request) {
        return matriculaService.create(request, accesoContextoService.idUsuarioAutenticado());
    }

    @PostMapping("/{id}/cambio-seccion")
    public MatriculaResponse cambioSeccion(@PathVariable Integer id,
                                           @RequestParam Integer idGradoSeccion,
                                           @RequestParam(required = false) String motivo) {
        return matriculaService.cambioSeccion(id, idGradoSeccion, motivo);
    }

    @PutMapping("/{id}")
    public MatriculaResponse update(@PathVariable Integer id, @Valid @RequestBody MatriculaRequest request) {
        return matriculaService.update(id, request);
    }

    @PostMapping("/{id}/aprobar")
    public MatriculaResponse aprobar(@PathVariable Integer id, @RequestParam(required = false) String observaciones) {
        return matriculaService.aprobar(id, observaciones);
    }

    @PostMapping("/{id}/rechazar")
    public MatriculaResponse rechazar(@PathVariable Integer id, @RequestParam(required = false) String observaciones) {
        return matriculaService.rechazar(id, observaciones);
    }

    @PostMapping("/{id}/matricular")
    public MatriculaResponse matricular(@PathVariable Integer id,
                                        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaPago,
                                        @RequestParam java.math.BigDecimal montoPago) {
        return matriculaService.matricular(id, fechaPago, montoPago);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        matriculaService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/reporte")
    public List<MatriculaReporteResponse> reporte(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fin,
            @RequestParam(required = false) Integer idAnio,
            @RequestParam(required = false) Integer idGradoSeccion) {
        return matriculaService.reporte(inicio, fin, idAnio, idGradoSeccion);
    }
}