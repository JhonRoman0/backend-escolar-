package com.example.Escolar.Controller;

import com.example.Escolar.Dto.AutorizacionRegistroResponse;
import com.example.Escolar.Dto.AutorizacionRequest;
import com.example.Escolar.Dto.AutorizacionResponse;
import com.example.Escolar.Dto.NotaBatchRequest;
import com.example.Escolar.Dto.NotaConsolidadoResponse;
import com.example.Escolar.Dto.NotaReporteResponse;
import com.example.Escolar.Dto.NotaRequest;
import com.example.Escolar.Dto.NotaResponse;
import com.example.Escolar.Dto.PromedioResponse;
import com.example.Escolar.Dto.ValidarAutorizacionRequest;
import com.example.Escolar.Service.AccesoContextoService;
import com.example.Escolar.Service.ExportService;
import com.example.Escolar.Service.NotaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/notas")
@RequiredArgsConstructor
public class NotaController {

    private final NotaService notaService;
    private final ExportService exportService;
    private final AccesoContextoService accesoContextoService;

    @GetMapping("/competencia/{idCompetencia}")
    public List<NotaResponse> getPorCompetencia(
            @PathVariable Integer idCompetencia,
            @RequestParam(required = false) Byte bimestre) {
        return notaService.getPorCompetencia(idCompetencia, bimestre,
                accesoContextoService.idUsuarioAutenticado(), accesoContextoService.rolesAutenticados());
    }

    @GetMapping("/matricula/{idMatricula}")
    public List<NotaResponse> getPorMatricula(@PathVariable Integer idMatricula) {
        return notaService.getPorMatricula(idMatricula,
                accesoContextoService.idUsuarioAutenticado(), accesoContextoService.rolesAutenticados());
    }

    @GetMapping("/consolidado")
    public List<NotaConsolidadoResponse> getConsolidado(
            @RequestParam Integer idGradoSeccion,
            @RequestParam Byte bimestre,
            @RequestParam(required = false) Integer idCurso) {
        return notaService.getConsolidado(idGradoSeccion, bimestre, idCurso,
                accesoContextoService.idUsuarioAutenticado(), accesoContextoService.rolesAutenticados());
    }

    @GetMapping("/matricula/{idMatricula}/promedio")
    public PromedioResponse promedioPorBimestre(@PathVariable Integer idMatricula) {
        return notaService.promedioPorBimestre(idMatricula,
                accesoContextoService.idUsuarioAutenticado(), accesoContextoService.rolesAutenticados());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public NotaResponse registrar(@Valid @RequestBody NotaRequest request) {
        return notaService.registrar(request,
                accesoContextoService.idUsuarioAutenticado(), accesoContextoService.rolesAutenticados());
    }

    @PostMapping("/batch")
    @ResponseStatus(HttpStatus.CREATED)
    public List<NotaResponse> registrarPorLote(@Valid @RequestBody NotaBatchRequest request) {
        return notaService.registrarPorLote(request,
                accesoContextoService.idUsuarioAutenticado(), accesoContextoService.rolesAutenticados());
    }

    @PostMapping("/autorizaciones")
    @ResponseStatus(HttpStatus.CREATED)
    public AutorizacionResponse generarAutorizacion(@Valid @RequestBody AutorizacionRequest request) {
        return notaService.generarAutorizacion(
                accesoContextoService.idUsuarioAutenticado(), accesoContextoService.rolesAutenticados(), request);
    }

    @GetMapping("/autorizaciones")
    public List<AutorizacionRegistroResponse> listarAutorizaciones() {
        return notaService.listarAutorizaciones(
                accesoContextoService.idUsuarioAutenticado(), accesoContextoService.rolesAutenticados());
    }

    @PostMapping("/autorizaciones/validar")
    public Map<String, Object> validarAutorizacion(@Valid @RequestBody ValidarAutorizacionRequest request) {
        notaService.validarAutorizacion(request.getCodigo());
        return Map.of("valido", true);
    }

    @GetMapping("/reporte")
    public List<NotaReporteResponse> reporte(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fin,
            @RequestParam(required = false) Integer idGradoSeccion,
            @RequestParam(required = false) Integer bimestre) {
        return notaService.reporte(inicio, fin, idGradoSeccion, bimestre);
    }

    @GetMapping("/exportar")
    public ResponseEntity<byte[]> exportarSiagie(
            @RequestParam Integer idGradoSeccion,
            @RequestParam Byte bimestre) {
        byte[] excel = exportService.exportarSiagie(idGradoSeccion, bimestre);
        String filename = exportService.nombreArchivoSiagie(idGradoSeccion, bimestre);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData("attachment", filename);
        headers.setContentLength(excel.length);
        return new ResponseEntity<>(excel, headers, HttpStatus.OK);
    }
}
