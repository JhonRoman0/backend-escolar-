package com.example.Escolar.Controller;

import com.example.Escolar.Dto.NotaFinalRequest;
import com.example.Escolar.Dto.NotaFinalResponse;
import com.example.Escolar.Dto.ReaperturaNotaRequest;
import com.example.Escolar.Service.AccesoContextoService;
import com.example.Escolar.Service.NotaFinalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/notas/finales")
@RequiredArgsConstructor
public class NotaFinalController {

    private final NotaFinalService notaFinalService;
    private final AccesoContextoService accesoContextoService;

    @GetMapping("/cursos")
    public List<NotaFinalResponse.Curso> cursos(@RequestParam Integer idAnio) {
        return notaFinalService.cursos(idAnio, usuario(), idRolActivo());
    }

    @GetMapping
    public NotaFinalResponse.Detalle detalle(@RequestParam Integer idGradoSeccion, @RequestParam Integer idArea,
                                             @RequestParam Integer periodo) {
        return notaFinalService.detalle(idGradoSeccion, idArea, periodo, usuario(), idRolActivo());
    }

    @PutMapping
    public NotaFinalResponse.Detalle registrar(@Valid @RequestBody NotaFinalRequest request) {
        return notaFinalService.registrar(request, usuario(), idRolActivo());
    }

    @PutMapping("/correccion")
    public NotaFinalResponse.Detalle corregir(@Valid @RequestBody NotaFinalRequest request) {
        return notaFinalService.corregir(request, usuario(), idRolActivo());
    }

    @PostMapping("/reapertura")
    public NotaFinalResponse.Detalle reabrir(@Valid @RequestBody ReaperturaNotaRequest request) {
        return notaFinalService.reabrir(request, usuario(), idRolActivo());
    }

    private Integer usuario() {
        return accesoContextoService.idUsuarioAutenticado();
    }

    private Integer idRolActivo() {
        return accesoContextoService.idRolActivoAutenticado();
    }
}
