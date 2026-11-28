package com.example.Escolar.Controller;

import com.example.Escolar.Dto.EvaluacionRequest;
import com.example.Escolar.Dto.EvaluacionResponse;
import com.example.Escolar.Dto.NotasEvaluacionRequest;
import com.example.Escolar.Service.AccesoContextoService;
import com.example.Escolar.Service.EvaluacionService;
import com.example.Escolar.Service.ReconocimientoCalificacionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/evaluaciones")
@RequiredArgsConstructor
public class EvaluacionController {

    private final EvaluacionService evaluacionService;
    private final AccesoContextoService accesoContextoService;
    /** Crear y editar pasan por aquí: también registran a los trasladados que no rinden. */
    private final ReconocimientoCalificacionService reconocimientoService;

    @GetMapping("/cursos")
    public List<EvaluacionResponse.Curso> cursos(@RequestParam Integer idAnio) {
        return evaluacionService.cursos(idAnio, usuario(), idRolActivo());
    }

    @GetMapping
    public List<EvaluacionResponse.Resumen> listar(@RequestParam Integer idAsignacion, @RequestParam Integer periodo) {
        return evaluacionService.listar(idAsignacion, periodo, usuario(), idRolActivo());
    }

    @GetMapping("/avance-finales")
    public List<EvaluacionResponse.AvanceFinal> avance(@RequestParam Integer idAsignacion, @RequestParam Integer periodo) {
        return evaluacionService.avance(idAsignacion, periodo, usuario(), idRolActivo());
    }

    @GetMapping("/registro")
    public EvaluacionResponse.Registro registro(@RequestParam Integer idAsignacion, @RequestParam Integer periodo) {
        return evaluacionService.registro(idAsignacion, periodo, usuario(), idRolActivo());
    }

    @GetMapping("/{id}")
    public EvaluacionResponse.Detalle detalle(@PathVariable Integer id) {
        return evaluacionService.detalle(id, usuario(), idRolActivo());
    }

    /** Pide la acción VER_RESUMEN (ver ApiPermisoRegistro). */
    @GetMapping("/{id}/resumen")
    public EvaluacionResponse.Ficha resumen(@PathVariable Integer id) {
        return evaluacionService.resumen(id, usuario(), idRolActivo());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EvaluacionResponse.Resumen crear(@Valid @RequestBody EvaluacionRequest request) {
        return reconocimientoService.crear(request, usuario(), idRolActivo());
    }

    @PutMapping("/{id}/notas")
    public EvaluacionResponse.Detalle registrarNotas(@PathVariable Integer id,
                                                     @Valid @RequestBody NotasEvaluacionRequest request) {
        return evaluacionService.registrarNotas(id, request, usuario(), idRolActivo());
    }

    @PutMapping("/{id}")
    public EvaluacionResponse.Resumen editar(@PathVariable Integer id, @Valid @RequestBody EvaluacionRequest request) {
        return reconocimientoService.editar(id, request, usuario(), idRolActivo());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
        evaluacionService.eliminar(id, usuario(), idRolActivo());
        return ResponseEntity.noContent().build();
    }

    private Integer usuario() {
        return accesoContextoService.idUsuarioAutenticado();
    }

    private Integer idRolActivo() {
        return accesoContextoService.idRolActivoAutenticado();
    }
}
