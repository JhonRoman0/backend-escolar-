package com.example.Escolar.Controller;

import com.example.Escolar.Dto.ApoderadoEdicionRequest;
import com.example.Escolar.Dto.ApoderadoResponse;
import com.example.Escolar.Dto.InscripcionApoderadoRequest;
import com.example.Escolar.Service.AccesoContextoService;
import com.example.Escolar.Service.ApoderadoService;
import com.example.Escolar.Service.InscripcionService;
import com.example.Escolar.Service.UsuarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import jakarta.validation.Valid;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/apoderados")
@RequiredArgsConstructor
public class ApoderadoController {

    private final ApoderadoService apoderadoService;
    private final InscripcionService inscripcionService;
    private final UsuarioService usuarioService;
    private final AccesoContextoService accesoContextoService;

    @GetMapping
    public List<ApoderadoResponse> getAll() {
        return apoderadoService.getAll(
                accesoContextoService.idUsuarioAutenticado(),
                accesoContextoService.rolesAutenticados());
    }

    @GetMapping("/{id}")
    public ApoderadoResponse getById(@PathVariable Integer id) {
        return apoderadoService.getById(id,
                accesoContextoService.idUsuarioAutenticado(),
                accesoContextoService.rolesAutenticados());
    }

    @GetMapping("/dni/{documento}")
    public ApoderadoResponse getByDocumento(@PathVariable String documento) {
        return apoderadoService.getByDocumento(documento,
                accesoContextoService.idUsuarioAutenticado(),
                accesoContextoService.rolesAutenticados());
    }

    // Segundo apoderado de un alumno ya matriculado.
    @PostMapping("/alumno/{idAlumno}")
    @ResponseStatus(HttpStatus.CREATED)
    public ApoderadoResponse agregar(@PathVariable Integer idAlumno, @Valid @RequestBody InscripcionApoderadoRequest request) {
        try {
            return inscripcionService.agregarApoderado(idAlumno, request);
        } catch (ConcurrencyFailureException ex) {
            throw new IllegalArgumentException("No se pudo completar el guardado porque hubo cambios concurrentes. Reintenta.", ex);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> quitar(@PathVariable Integer id, @RequestParam Integer idAlumno) {
        inscripcionService.quitarApoderado(id, idAlumno);
        return ResponseEntity.noContent().build();
    }

    /** Otra persona ocupa su lugar con este alumno; el backend comprueba quién es ese DNI. */
    @PutMapping("/{id}/reemplazar")
    public ApoderadoResponse reemplazar(@PathVariable Integer id, @RequestParam Integer idAlumno,
                                        @Valid @RequestBody InscripcionApoderadoRequest request) {
        try {
            return inscripcionService.reemplazarApoderado(id, idAlumno, request);
        } catch (ConcurrencyFailureException ex) {
            throw new IllegalArgumentException("No se pudo completar el guardado porque hubo cambios concurrentes. Reintenta.", ex);
        }
    }

    @PutMapping("/{id}")
    public ApoderadoResponse actualizar(@PathVariable Integer id, @Valid @RequestBody ApoderadoEdicionRequest request) {
        ApoderadoResponse response;
        try {
            response = apoderadoService.actualizar(id, request);
        } catch (ConcurrencyFailureException ex) {
            throw new IllegalArgumentException("No se pudo completar el guardado porque hubo cambios concurrentes. Reintenta.", ex);
        }
        // Va después de guardar: si el correo falla, los datos ya quedaron guardados.
        if (request.isRestablecerContrasena()) {
            response.setCredencialesEnviadas(usuarioService.restablecerContrasena(response.getIdUsuario()));
        }
        return response;
    }
}
