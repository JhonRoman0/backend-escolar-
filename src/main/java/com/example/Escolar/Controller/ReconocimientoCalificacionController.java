package com.example.Escolar.Controller;

import com.example.Escolar.Dto.AnulacionReconocimientoRequest;
import com.example.Escolar.Dto.HistorialCalificacionesResponse;
import com.example.Escolar.Dto.ReconocimientoCalificacionRequest;
import com.example.Escolar.Dto.ReconocimientoCalificacionResponse;
import com.example.Escolar.Dto.TrasladoResponse;
import com.example.Escolar.Service.AccesoContextoService;
import com.example.Escolar.Service.ReconocimientoCalificacionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

/** Notas de alumnos trasladados de sección. */
@RestController
@RequestMapping("/evaluaciones")
@RequiredArgsConstructor
public class ReconocimientoCalificacionController {
    private final ReconocimientoCalificacionService service;
    private final AccesoContextoService contexto;

    @GetMapping("/historial-calificaciones")
    public List<HistorialCalificacionesResponse> historial(@RequestParam Integer idMatricula, @RequestParam Integer idAsignacion) {
        return service.historial(idMatricula, idAsignacion, contexto.idUsuarioAutenticado(), contexto.idRolActivoAutenticado());
    }

    /** Alumnos que traen notas utilizables en una evaluación con esas competencias. */
    @GetMapping("/traslados")
    public List<TrasladoResponse> traslados(@RequestParam Integer idAsignacion, @RequestParam Integer periodo,
                                            @RequestParam Set<Integer> competencias,
                                            @RequestParam(required = false) Integer idEvaluacion) {
        return service.traslados(idAsignacion, periodo, competencias, idEvaluacion,
                contexto.idUsuarioAutenticado(), contexto.idRolActivoAutenticado());
    }

    @PostMapping("/reconocimientos")
    public ReconocimientoCalificacionResponse reconocer(@Valid @RequestBody ReconocimientoCalificacionRequest request) {
        return service.reconocer(request, contexto.idUsuarioAutenticado(), contexto.idRolActivoAutenticado());
    }

    @PostMapping("/reconocimientos/{id}/anulacion")
    public void anular(@PathVariable Integer id, @Valid @RequestBody AnulacionReconocimientoRequest request) {
        service.anular(id, request.motivo(), contexto.idUsuarioAutenticado(), contexto.idRolActivoAutenticado());
    }
}
