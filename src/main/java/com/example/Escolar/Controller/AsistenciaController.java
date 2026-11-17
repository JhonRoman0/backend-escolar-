package com.example.Escolar.Controller;

import com.example.Escolar.Dto.AnulacionAsistenciaRequest;
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
import com.example.Escolar.Service.AsistenciaService.Filtro;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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
        return asistenciaService.previsualizar(request, usuario(), roles());
    }

    /** datos: el alumno y, si entra justificado, motivo y acompañante; foto: evidencia del ingreso justificado. */
    @PostMapping(value = "/confirmar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public AsistenciaResponse confirmar(@Valid @RequestPart("datos") AsistenciaRequest request,
                                        @RequestPart(value = "foto", required = false) MultipartFile foto) {
        return asistenciaService.confirmar(request, foto, usuario(), roles());
    }

    @PutMapping("/{id}/justificar")
    public AsistenciaResponse justificar(@PathVariable Integer id, @Valid @RequestBody JustificarRequest request) {
        return asistenciaService.justificar(id, request, usuario(), roles());
    }

    /** Anula una marca del día con su motivo. */
    @PostMapping("/{id}/anulacion")
    public ResponseEntity<Void> anular(@PathVariable Integer id, @Valid @RequestBody AnulacionAsistenciaRequest request) {
        asistenciaService.anular(id, request.motivo(), usuario(), roles());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/hoy")
    public Page<AsistenciaDiaResponse> asistenciasHoy(
            @RequestParam(required = false) Integer idNivel,
            @RequestParam(required = false) Integer idGrado,
            @RequestParam(required = false) Integer idSeccion,
            @RequestParam(required = false) Integer idTurno,
            @RequestParam(required = false) Integer idGradoSeccion,
            @RequestParam(required = false) Integer idAnio,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String estado,
            @PageableDefault(size = 10, sort = "idMatricula") Pageable pageable) {
        return asistenciaService.asistenciasHoy(usuario(), roles(),
                new Filtro(idAnio, idNivel, idGrado, idSeccion, idTurno, idGradoSeccion, search, estado), pageable);
    }

    @GetMapping("/semana")
    public Page<MatrizSemanalResponse> matrizSemanal(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestParam(required = false) Integer idNivel,
            @RequestParam(required = false) Integer idGrado,
            @RequestParam(required = false) Integer idSeccion,
            @RequestParam(required = false) Integer idTurno,
            @RequestParam(required = false) Integer idGradoSeccion,
            @RequestParam(required = false) Integer idAnio,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 10, sort = "idMatricula") Pageable pageable) {
        return asistenciaService.matrizSemanal(fecha, pageable, usuario(), roles(),
                new Filtro(idAnio, idNivel, idGrado, idSeccion, idTurno, idGradoSeccion, search, null));
    }

    @GetMapping("/mes")
    public Page<ResumenMensualResponse> resumenMensual(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestParam(required = false) Integer idNivel,
            @RequestParam(required = false) Integer idGrado,
            @RequestParam(required = false) Integer idSeccion,
            @RequestParam(required = false) Integer idTurno,
            @RequestParam(required = false) Integer idGradoSeccion,
            @RequestParam(required = false) Integer idAnio,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 10, sort = "idMatricula") Pageable pageable) {
        return asistenciaService.resumenMensual(fecha, pageable, usuario(), roles(),
                new Filtro(idAnio, idNivel, idGrado, idSeccion, idTurno, idGradoSeccion, search, null));
    }

    @GetMapping("/estadisticas")
    public EstadisticasResponse estadisticas(
            @RequestParam(defaultValue = "hoy") String rango,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestParam(required = false) Integer idAnio) {
        return asistenciaService.estadisticas(rango, fecha, idAnio, usuario(), roles());
    }

    /** Para el portal del apoderado. */
    @GetMapping("/hijos")
    public List<ResumenMensualResponse> asistenciasDeHijos() {
        return asistenciaService.asistenciasDeHijos(usuario(), roles());
    }

    @GetMapping("/estados")
    public List<EstadoAsistenciaResponse> listarEstados() {
        return asistenciaService.listarEstados();
    }

    private Integer usuario() {
        return accesoContextoService.idUsuarioAutenticado();
    }

    private List<String> roles() {
        return accesoContextoService.rolesAutenticados();
    }
}
