package com.example.Escolar.Controller;

import com.example.Escolar.Dto.DocenteReporteResponse;
import com.example.Escolar.Dto.DocenteRequest;
import com.example.Escolar.Dto.DocenteResponse;
import com.example.Escolar.Service.DocenteService;
import com.example.Escolar.Service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.dao.ConcurrencyFailureException;
import com.example.Escolar.Service.ExclusividadDniService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/docentes")
@RequiredArgsConstructor
public class DocenteController {

    private final ExclusividadDniService exclusividadDni;
    private final DocenteService docenteService;
    private final UsuarioService usuarioService;

    @GetMapping("/validar-dni/{dni}")
    public ResponseEntity<Void> validarDni(@PathVariable String dni) {
        exclusividadDni.validarUsuario(dni);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public List<DocenteResponse> getAll() {
        return docenteService.getAll();
    }

    @GetMapping("/{id}")
    public DocenteResponse getById(@PathVariable Integer id) {
        return docenteService.getById(id);
    }

    @PutMapping("/{id}")
    public DocenteResponse update(@PathVariable Integer id, @Valid @RequestBody DocenteRequest request) {
        DocenteResponse response;
        try {
            response = docenteService.update(id, request);
        } catch (ConcurrencyFailureException ex) {
            throw new IllegalArgumentException("No se pudo completar el guardado porque hubo cambios concurrentes. Reintenta.", ex);
        }
        // Va después de guardar: si el correo falla, los datos ya quedaron guardados.
        if (request.isRestablecerContrasena()) {
            response.setCredencialesEnviadas(usuarioService.restablecerContrasena(response.getIdUsuario()));
        }
        return response;
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        docenteService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/reporte")
    public List<DocenteReporteResponse> reporte(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fin,
            @RequestParam(required = false) Integer tipoContratoId,
            @RequestParam(required = false) Integer idNivel,
            @RequestParam(required = false) Long idAcceso) {
        return docenteService.reporte(inicio, fin, tipoContratoId, idNivel, idAcceso);
    }
}