package com.example.Escolar.Controller;

import com.example.Escolar.Dto.AsistenciaDiaResponse;
import com.example.Escolar.Dto.AsistenciaRequest;
import com.example.Escolar.Dto.AsistenciaResponse;
import com.example.Escolar.Dto.EstadisticasResponse;
import com.example.Escolar.Dto.EstadoAsistenciaResponse;
import com.example.Escolar.Dto.JustificarRequest;
import com.example.Escolar.Dto.MatrizSemanalResponse;
import com.example.Escolar.Dto.ResumenMensualResponse;
import com.example.Escolar.Service.AccesoContextoService;
import com.example.Escolar.Service.AsistenciaService;
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
@RequestMapping("/asistencias")
@RequiredArgsConstructor
public class AsistenciaController {

    private final AsistenciaService asistenciaService;
    private final AccesoContextoService accesoContextoService;

    @PostMapping("/previsualizar")
    public AsistenciaResponse previsualizar(@Valid @RequestBody AsistenciaRequest request) {
        return asistenciaService.previsualizar(request);
    }

    @PostMapping("/confirmar")
    @ResponseStatus(HttpStatus.CREATED)
    public AsistenciaResponse confirmar(@Valid @RequestBody AsistenciaRequest request) {
        return asistenciaService.confirmar(request, accesoContextoService.idUsuarioAutenticado());
    }

    @PutMapping("/{id}/justificar")
    public AsistenciaResponse justificar(@PathVariable Integer id, @Valid @RequestBody JustificarRequest request) {
        return asistenciaService.justificar(id, request, accesoContextoService.idUsuarioAutenticado());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
        asistenciaService.eliminar(id, accesoContextoService.idUsuarioAutenticado());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/hoy")
    public List<AsistenciaDiaResponse> asistenciasHoy() {
        return asistenciaService.asistenciasHoy(accesoContextoService.idUsuarioAutenticado(), accesoContextoService.rolesAutenticados());
    }

    @GetMapping("/semana")
    public Page<MatrizSemanalResponse> matrizSemanal(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @PageableDefault(size = 10, sort = "idMatricula") Pageable pageable) {
        return asistenciaService.matrizSemanal(fecha, pageable, accesoContextoService.idUsuarioAutenticado(), accesoContextoService.rolesAutenticados());
    }

    @GetMapping("/mes")
    public Page<ResumenMensualResponse> resumenMensual(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @PageableDefault(size = 10, sort = "idMatricula") Pageable pageable) {
        return asistenciaService.resumenMensual(fecha, pageable, accesoContextoService.idUsuarioAutenticado(), accesoContextoService.rolesAutenticados());
    }

    @GetMapping("/estadisticas")
    public EstadisticasResponse estadisticas(
            @RequestParam(defaultValue = "hoy") String rango,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return asistenciaService.estadisticas(rango, fecha, accesoContextoService.idUsuarioAutenticado(), accesoContextoService.rolesAutenticados());
    }

    @GetMapping("/hijos")
    public List<ResumenMensualResponse> asistenciasDeHijos() {
        return asistenciaService.asistenciasDeHijos(accesoContextoService.idUsuarioAutenticado(), accesoContextoService.rolesAutenticados());
    }

    @GetMapping("/estados")
    public List<EstadoAsistenciaResponse> listarEstados() {
        return asistenciaService.listarEstados();
    }
}