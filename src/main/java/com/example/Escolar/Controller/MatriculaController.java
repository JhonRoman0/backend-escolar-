package com.example.Escolar.Controller;

import com.example.Escolar.Dto.ApoderadoBusquedaResponse;
import com.example.Escolar.Dto.InscripcionAlumnoResponse;
import com.example.Escolar.Dto.InscripcionRequest;
import com.example.Escolar.Dto.MatriculaEdicionRequest;
import com.example.Escolar.Dto.MatriculaOpcionesResponse;
import com.example.Escolar.Dto.MatriculaReporteResponse;
import com.example.Escolar.Dto.MatriculaResponse;
import com.example.Escolar.Dto.PagoMatriculaResponse;
import com.example.Escolar.Service.AccesoContextoService;
import com.example.Escolar.Service.InscripcionService;
import com.example.Escolar.Service.MatriculaReglasService;
import com.example.Escolar.Service.MatriculaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/matriculas")
@RequiredArgsConstructor
public class MatriculaController {

    private final MatriculaService matriculaService;
    private final InscripcionService inscripcionService;
    private final MatriculaReglasService matriculaReglasService;
    private final AccesoContextoService accesoContextoService;

    @GetMapping
    public Page<MatriculaResponse> getAll(@PageableDefault(size = 10, sort = "idMatricula") Pageable pageable,
                                          @RequestParam(required = false) Integer idNivel,
                                          @RequestParam(required = false) Integer idGrado,
                                          @RequestParam(required = false) Integer idSeccion,
                                          @RequestParam(required = false) Integer idTurno,
                                          @RequestParam(required = false) Integer idGradoSeccion,
                                          @RequestParam(required = false) Integer idAnio) {
        return matriculaService.getAll(pageable, accesoContextoService.idUsuarioAutenticado(), accesoContextoService.rolesAutenticados(),
                idNivel, idGrado, idSeccion, idTurno, idGradoSeccion, idAnio);
    }

    @GetMapping("/{id}")
    public MatriculaResponse getById(@PathVariable Integer id) {
        return matriculaService.getById(id, accesoContextoService.idUsuarioAutenticado(), accesoContextoService.rolesAutenticados());
    }

    @GetMapping("/{id}/pagos")
    public List<PagoMatriculaResponse> pagos(@PathVariable Integer id) {
        return matriculaService.pagos(id, accesoContextoService.idUsuarioAutenticado(), accesoContextoService.rolesAutenticados());
    }

    @GetMapping("/inscripcion/alumno/{dni}")
    public InscripcionAlumnoResponse buscarAlumno(@PathVariable String dni) {
        return inscripcionService.buscarAlumno(dni);
    }

    @GetMapping("/inscripcion/apoderado/{dni}")
    public ApoderadoBusquedaResponse buscarApoderado(@PathVariable String dni,
                                                     @RequestParam(required = false) Integer idAlumno) {
        return inscripcionService.buscarApoderado(dni, idAlumno);
    }

    /** Grados y secciones permitidos; idMatricula excluye la propia al editar. */
    @GetMapping("/opciones")
    public MatriculaOpcionesResponse opciones(@RequestParam Integer idAnio,
                                              @RequestParam(required = false) Integer idAlumno,
                                              @RequestParam(required = false) Integer idMatricula) {
        return matriculaReglasService.opciones(idAnio, idAlumno, idMatricula);
    }

    @PostMapping("/inscripcion/validar")
    public ResponseEntity<Void> validar(@Valid @RequestBody InscripcionRequest request) {
        inscripcionService.validar(request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/inscripcion", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public MatriculaResponse inscribir(@Valid @RequestPart("datos") InscripcionRequest request,
                                       @RequestPart(value = "foto", required = false) MultipartFile foto,
                                       @RequestPart(value = "comprobante", required = false) MultipartFile comprobante) {
        return inscripcionService.inscribir(request, foto, comprobante, accesoContextoService.idUsuarioAutenticado());
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public MatriculaResponse editar(@PathVariable Integer id,
                                    @Valid @RequestPart("datos") MatriculaEdicionRequest request,
                                    @RequestPart(value = "comprobante", required = false) MultipartFile comprobante) {
        return inscripcionService.editar(id, request, comprobante, accesoContextoService.idUsuarioAutenticado());
    }

    @PostMapping("/{id}/cambio-seccion")
    public MatriculaResponse cambioSeccion(@PathVariable Integer id,
                                           @RequestParam Integer idGradoSeccion,
                                           @RequestParam(required = false) String motivo) {
        return matriculaService.cambioSeccion(id, idGradoSeccion, motivo);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        inscripcionService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/reporte")
    public List<MatriculaReporteResponse> reporte(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fin,
            @RequestParam(required = false) Integer idAnio,
            @RequestParam(required = false) Integer idGradoSeccion) {
        return matriculaService.reporte(inicio, fin, idAnio, idGradoSeccion);
    }
}
