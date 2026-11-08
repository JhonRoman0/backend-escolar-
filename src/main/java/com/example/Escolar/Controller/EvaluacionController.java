package com.example.Escolar.Controller;

import com.example.Escolar.Dto.EvaluacionRequest;
import com.example.Escolar.Dto.EvaluacionResponse;
import com.example.Escolar.Dto.NotasEvaluacionRequest;
import com.example.Escolar.Service.AccesoContextoService;
import com.example.Escolar.Service.EvaluacionService;
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

    @GetMapping("/cursos")
    public List<EvaluacionResponse.Curso> cursos(@RequestParam Integer idAnio) {
        return evaluacionService.cursos(idAnio, usuario(), roles());
    }

    @GetMapping
    public List<EvaluacionResponse.Resumen> listar(@RequestParam Integer idAsignacion, @RequestParam Integer periodo) {
        return evaluacionService.listar(idAsignacion, periodo, usuario(), roles());
    }

    @GetMapping("/avance-finales")
    public List<EvaluacionResponse.AvanceFinal> avance(@RequestParam Integer idAsignacion, @RequestParam Integer periodo) {
        return evaluacionService.avance(idAsignacion, periodo, usuario(), roles());
    }

    @GetMapping("/registro")
    public EvaluacionResponse.Registro registro(@RequestParam Integer idAsignacion, @RequestParam Integer periodo) {
        return evaluacionService.registro(idAsignacion, periodo, usuario(), roles());
    }

    @GetMapping("/{id}")
    public EvaluacionResponse.Detalle detalle(@PathVariable Integer id) {
        return evaluacionService.detalle(id, usuario(), roles());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EvaluacionResponse.Resumen crear(@Valid @RequestBody EvaluacionRequest request) {
        return evaluacionService.crear(request, usuario(), roles());
    }

    @PutMapping("/{id}/notas")
    public EvaluacionResponse.Detalle registrarNotas(@PathVariable Integer id,
                                                     @Valid @RequestBody NotasEvaluacionRequest request) {
        return evaluacionService.registrarNotas(id, request, usuario(), roles());
    }

    @PutMapping("/{id}")
    public EvaluacionResponse.Resumen editar(@PathVariable Integer id, @Valid @RequestBody EvaluacionRequest request) {
        return evaluacionService.editar(id, request, usuario(), roles());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
        evaluacionService.eliminar(id, usuario(), roles());
        return ResponseEntity.noContent().build();
    }

    private Integer usuario() {
        return accesoContextoService.idUsuarioAutenticado();
    }

    private List<String> roles() {
        return accesoContextoService.rolesAutenticados();
    }
}
