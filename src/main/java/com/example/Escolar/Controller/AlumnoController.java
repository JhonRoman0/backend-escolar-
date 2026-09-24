package com.example.Escolar.Controller;

import com.example.Escolar.Dto.AlumnoReporteResponse;
import com.example.Escolar.Dto.AlumnoRequest;
import com.example.Escolar.Dto.AlumnoResponse;
import com.example.Escolar.Service.AccesoContextoService;
import com.example.Escolar.Service.AlumnoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/alumnos")
@RequiredArgsConstructor
public class AlumnoController {

    private final AlumnoService alumnoService;
    private final AccesoContextoService accesoContextoService;

    @GetMapping
    public Page<AlumnoResponse> getAll(@PageableDefault(size = 10, sort = "idAlumno") Pageable pageable,
                                       @RequestParam(required = false) Integer idNivel,
                                       @RequestParam(required = false) Integer idGrado,
                                       @RequestParam(required = false) Integer idSeccion,
                                       @RequestParam(required = false) Integer idTurno,
                                       @RequestParam(required = false) Integer idGradoSeccion,
                                       @RequestParam(required = false) Integer idAnio) {
        return alumnoService.getAll(pageable, accesoContextoService.idUsuarioAutenticado(), accesoContextoService.rolesAutenticados(),
                idNivel, idGrado, idSeccion, idTurno, idGradoSeccion, idAnio);
    }

    @GetMapping("/{id}")
    public AlumnoResponse getById(@PathVariable Integer id) {
        return alumnoService.getById(id, accesoContextoService.idUsuarioAutenticado(), accesoContextoService.rolesAutenticados());
    }

    @GetMapping("/dni/{documento}")
    public AlumnoResponse getByDocumento(@PathVariable String documento) {
        return alumnoService.getByDocumento(documento, accesoContextoService.idUsuarioAutenticado(), accesoContextoService.rolesAutenticados());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AlumnoResponse create(@Valid @RequestBody AlumnoRequest request) {
        return alumnoService.create(request);
    }

    @PutMapping("/{id}")
    public AlumnoResponse update(@PathVariable Integer id, @Valid @RequestBody AlumnoRequest request) {
        return alumnoService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        alumnoService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/{id}/foto", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public AlumnoResponse subirFoto(@PathVariable Integer id, @RequestPart("foto") MultipartFile foto) {
        return alumnoService.subirFoto(id, foto);
    }

    @DeleteMapping("/{id}/foto")
    public ResponseEntity<Void> eliminarFoto(@PathVariable Integer id) {
        alumnoService.eliminarFoto(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/reporte")
    public List<AlumnoReporteResponse> reporte(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fin,
            @RequestParam(required = false) Integer idGradoSeccion) {
        return alumnoService.reporte(inicio, fin, idGradoSeccion);
    }
}