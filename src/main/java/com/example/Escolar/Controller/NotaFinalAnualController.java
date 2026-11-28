package com.example.Escolar.Controller;

import com.example.Escolar.Dto.NotaAnualRequest;
import com.example.Escolar.Dto.NotaAnualResponse;
import com.example.Escolar.Dto.NotaFinalResponse;
import com.example.Escolar.Dto.ReaperturaNotaRequest;
import com.example.Escolar.Service.AccesoContextoService;
import com.example.Escolar.Service.NotaFinalAnualService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Notas finales del año por competencia (permiso NOTAS_ANUALES). */
@RestController
@RequestMapping("/notas/anuales")
@RequiredArgsConstructor
public class NotaFinalAnualController {

    private final NotaFinalAnualService service;
    private final AccesoContextoService accesoContextoService;

    @GetMapping("/cursos")
    public List<NotaFinalResponse.Curso> cursos(@RequestParam Integer idAnio) {
        return service.cursos(idAnio, usuario(), idRolActivo());
    }

    @GetMapping
    public NotaAnualResponse.Detalle detalle(@RequestParam Integer idGradoSeccion, @RequestParam Integer idArea) {
        return service.detalle(idGradoSeccion, idArea, usuario(), idRolActivo());
    }

    @PutMapping
    public NotaAnualResponse.Detalle registrar(@Valid @RequestBody NotaAnualRequest request) {
        return service.registrar(request, usuario(), idRolActivo());
    }

    @PutMapping("/correccion")
    public NotaAnualResponse.Detalle corregir(@Valid @RequestBody NotaAnualRequest request) {
        return service.corregir(request, usuario(), idRolActivo());
    }

    @PostMapping("/reapertura")
    public NotaAnualResponse.Detalle reabrir(@Valid @RequestBody ReaperturaNotaRequest request) {
        return service.reabrir(request, usuario(), idRolActivo());
    }

    private Integer usuario() {
        return accesoContextoService.idUsuarioAutenticado();
    }

    private Integer idRolActivo() {
        return accesoContextoService.idRolActivoAutenticado();
    }
}
