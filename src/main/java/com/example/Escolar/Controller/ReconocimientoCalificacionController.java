package com.example.Escolar.Controller;
import com.example.Escolar.Dto.*;
import com.example.Escolar.Service.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/evaluaciones")
@RequiredArgsConstructor
public class ReconocimientoCalificacionController {
    private final ReconocimientoCalificacionService service;
    private final AccesoContextoService contexto;
    @GetMapping("/historial-calificaciones")
    public List<HistorialCalificacionesResponse> historial(@RequestParam Integer idMatricula, @RequestParam Integer idAsignacion) {
        return service.historial(idMatricula, idAsignacion, contexto.idUsuarioAutenticado(), contexto.rolesAutenticados());
    }
    @GetMapping("/reconocimientos/candidatas")
    public List<ReconocimientoCalificacionResponse.Candidata> candidatas(@RequestParam Integer idNotaOrigen, @RequestParam Integer idAsignacion) {
        return service.candidatas(idNotaOrigen, idAsignacion, contexto.idUsuarioAutenticado(), contexto.rolesAutenticados());
    }
    @PostMapping("/reconocimientos")
    public ReconocimientoCalificacionResponse reconocer(@Valid @RequestBody ReconocimientoCalificacionRequest request) {
        return service.reconocer(request, contexto.idUsuarioAutenticado(), contexto.rolesAutenticados());
    }
    @PostMapping("/reconocimientos/{id}/anulacion")
    public void anular(@PathVariable Integer id, @Valid @RequestBody AnulacionReconocimientoRequest request) {
        service.anular(id, request.motivo(), contexto.idUsuarioAutenticado(), contexto.rolesAutenticados());
    }
}
