package com.example.Escolar.Controller;

import com.example.Escolar.Dto.AsignarSustitutoRequest;
import com.example.Escolar.Dto.AsignacionResponse;
import com.example.Escolar.Dto.CambioDocenteRequest;
import com.example.Escolar.Dto.CambioDocenteResponse;
import com.example.Escolar.Dto.EditarCambioVigenteRequest;
import com.example.Escolar.Dto.ResolverIncidenciaRequest;
import com.example.Escolar.Service.CambioDocenteService;
import com.example.Escolar.Service.AccesoContextoService;
import com.example.Escolar.Service.AsignacionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/cambios-docente")
@RequiredArgsConstructor
public class CambioDocenteController {

    private final CambioDocenteService cambioDocenteService;
    private final AsignacionService asignacionService;
    private final AccesoContextoService accesoContextoService;

    @GetMapping
    public List<CambioDocenteResponse> getAll() {
        return cambioDocenteService.getAll();
    }

    @GetMapping("/{id}")
    public CambioDocenteResponse getById(@PathVariable Integer id) {
        return cambioDocenteService.getById(id);
    }

    @GetMapping("/docente/{idDocente}")
    public List<CambioDocenteResponse> getByDocente(@PathVariable Integer idDocente) {
        return cambioDocenteService.getByDocente(idDocente);
    }

    @GetMapping("/docente/{idDocente}/asignaciones")
    public List<AsignacionResponse> asignacionesParaCambio(@PathVariable Integer idDocente) {
        return asignacionService.asignacionesParaCambio(idDocente,
                accesoContextoService.idUsuarioAutenticado(), accesoContextoService.rolesAutenticados());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CambioDocenteResponse create(@Valid @RequestBody CambioDocenteRequest request) {
        return cambioDocenteService.create(request);
    }

    @PutMapping("/{id}")
    public CambioDocenteResponse actualizar(@PathVariable Integer id,
                                            @Valid @RequestBody CambioDocenteRequest request) {
        return cambioDocenteService.actualizar(id, request);
    }

    @PutMapping("/{id}/edicion-vigente")
    public CambioDocenteResponse editarVigente(@PathVariable Integer id,
                                               @Valid @RequestBody EditarCambioVigenteRequest request) {
        return cambioDocenteService.editarVigente(id, request);
    }

    @PutMapping("/{id}/asignar-sustituto")
    public CambioDocenteResponse asignarSustituto(@PathVariable Integer id,
                                                  @Valid @RequestBody AsignarSustitutoRequest request) {
        return cambioDocenteService.asignarSustituto(id, request);
    }

    @PutMapping("/{id}/resolver-incidencia")
    public CambioDocenteResponse resolverIncidencia(@PathVariable Integer id,
                                                    @Valid @RequestBody ResolverIncidenciaRequest request) {
        return cambioDocenteService.resolverIncidencia(id, request);
    }

    @PutMapping("/{id}/finalizar")
    public CambioDocenteResponse finalizar(@PathVariable Integer id) {
        return cambioDocenteService.finalizarManual(id);
    }

    @PutMapping("/{id}/cancelar")
    public CambioDocenteResponse cancelar(@PathVariable Integer id,
                                          @RequestParam(required = false) String motivo) {
        return cambioDocenteService.cancelar(id, motivo);
    }
}
