package com.example.Escolar.Controller;

import com.example.Escolar.Dto.GradoSeccionResponse;
import com.example.Escolar.Dto.SeccionRequest;
import com.example.Escolar.Service.GradoSeccionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/secciones")
@RequiredArgsConstructor
public class GradoSeccionController {

    private final GradoSeccionService gradoSeccionService;

    /**
     * Flujo documentado: /niveles -> /grados?idNivel -> /secciones?idGrado -> /turnos.
     * Cada Grado tiene sus propias Secciones (agregado Grado). No existe catálogo /secciones independiente.
     */
    @GetMapping
    public List<GradoSeccionResponse> getSeccionesByGrado(@RequestParam Integer idGrado) {
        return gradoSeccionService.getSeccionesByGrado(idGrado);
    }

    /**
     * Agrega una seccion a un grado que ya existe. El anio lo resuelve el
     * servicio: si el cuerpo no lo manda se usa el vigente.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public GradoSeccionResponse create(@Valid @RequestBody SeccionRequest request) {
        return gradoSeccionService.create(request);
    }

    /** Borra una sola seccion. El servicio la rechaza si tiene alumnos o cursos. */
    @DeleteMapping("/{idGradoSeccion}")
    public ResponseEntity<Void> delete(@PathVariable Integer idGradoSeccion) {
        gradoSeccionService.delete(idGradoSeccion);
        return ResponseEntity.noContent().build();
    }
}
