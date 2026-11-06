package com.example.Escolar.Controller;

import com.example.Escolar.Dto.ReporteAlumnoResponse;
import com.example.Escolar.Dto.ReporteGeneralResponse;
import com.example.Escolar.Dto.ReporteUsuarioResponse;
import com.example.Escolar.Service.AccesoContextoService;
import com.example.Escolar.Service.AsistenciaService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/reportes")
@RequiredArgsConstructor
public class ReporteController {

    private final AsistenciaService asistenciaService;
    private final AccesoContextoService accesoContextoService;

    @GetMapping("/general")
    public Page<ReporteGeneralResponse> reporteGeneral(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fin,
            @RequestParam(required = false) Integer idNivel,
            @RequestParam(required = false) Integer idGrado,
            @RequestParam(required = false) Integer idSeccion,
            @RequestParam(required = false) Integer idTurno,
            @RequestParam(required = false) Integer idGradoSeccion,
            @RequestParam(required = false) Integer idAnio,
            @PageableDefault(size = 10, sort = "fecha") Pageable pageable) {
        return asistenciaService.reporteGeneral(inicio, fin, pageable,
                accesoContextoService.idUsuarioAutenticado(), accesoContextoService.rolesAutenticados(),
                idNivel, idGrado, idSeccion, idTurno, idGradoSeccion, idAnio);
    }

    @GetMapping("/alumno/{idAlumno}")
    public List<ReporteAlumnoResponse> reportePorAlumno(
            @PathVariable Integer idAlumno,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fin) {
        return asistenciaService.reportePorAlumno(idAlumno, inicio, fin,
                accesoContextoService.idUsuarioAutenticado(), accesoContextoService.rolesAutenticados());
    }

    @GetMapping("/usuario/{idUsuario}")
    public List<ReporteUsuarioResponse> reportePorUsuario(
            @PathVariable Integer idUsuario,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fin) {
        return asistenciaService.reportePorUsuario(idUsuario, inicio, fin,
                accesoContextoService.idUsuarioAutenticado(), accesoContextoService.rolesAutenticados());
    }
}
