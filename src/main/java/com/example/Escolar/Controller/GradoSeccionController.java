package com.example.Escolar.Controller;

import com.example.Escolar.Dto.GradoSeccionResponse;
import com.example.Escolar.Dto.SeccionActualizarRequest;
import com.example.Escolar.Dto.SeccionRequest;
import com.example.Escolar.Dto.SeccionesBatchRequest;
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

    /**
     * Agrega varias secciones de una vez a un grado que ya existe. Todas
     * comparten turno y anio, asi que la pantalla "Nueva seccion" alcanza con
     * elegir esa combinacion una sola vez.
     *
     * <p>El servicio valida el lote entero antes de guardar la primera letra: si
     * alguna se repite o ya existe, no se crea ninguna.
     */
    @PostMapping("/lote")
    @ResponseStatus(HttpStatus.CREATED)
    public List<GradoSeccionResponse> crearLote(@Valid @RequestBody SeccionesBatchRequest request) {
        return gradoSeccionService.crearLote(request);
    }

    /** Borra una sola seccion. El servicio la rechaza si tiene alumnos o cursos. */
    @DeleteMapping("/{idGradoSeccion}")
    public ResponseEntity<Void> delete(@PathVariable Integer idGradoSeccion) {
        gradoSeccionService.delete(idGradoSeccion);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{idGradoSeccion}")
    public ResponseEntity<GradoSeccionResponse> update(@PathVariable Integer idGradoSeccion,
                                                       @jakarta.validation.Valid @RequestBody SeccionActualizarRequest request) {
        GradoSeccionResponse resp = gradoSeccionService.update(idGradoSeccion, request);
        return ResponseEntity.ok(resp);
    }

    @DeleteMapping("/lote")
    public ResponseEntity<Void> deleteLote(@jakarta.validation.Valid @RequestBody com.example.Escolar.Dto.DeleteSeccionesLoteRequest request) {
        gradoSeccionService.deleteLote(request.getIds());
        return ResponseEntity.noContent().build();
    }
}
