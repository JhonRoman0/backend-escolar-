package com.example.Escolar.Controller;

import com.example.Escolar.Dto.NotaReporteResponse;
import com.example.Escolar.Dto.NotaResponse;
import com.example.Escolar.Service.AccesoContextoService;
import com.example.Escolar.Service.NotaService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/notas")
@RequiredArgsConstructor
public class NotaController {

    private final NotaService notaService;
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


    @GetMapping("/reporte")
    public List<NotaReporteResponse> reporte(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fin,
            @RequestParam(required = false) Integer idGradoSeccion,
            @RequestParam(required = false) Integer bimestre) {
        return notaService.reporte(inicio, fin, idGradoSeccion, bimestre,
                accesoContextoService.idUsuarioAutenticado(), accesoContextoService.rolesAutenticados());
    }

}
