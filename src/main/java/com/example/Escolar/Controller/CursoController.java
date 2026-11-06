package com.example.Escolar.Controller;

import com.example.Escolar.Dto.CursoCopiaRequest;
import com.example.Escolar.Dto.CursoEstadoRequest;
import com.example.Escolar.Dto.CursoImportResponse;
import com.example.Escolar.Dto.CursoResponse;
import com.example.Escolar.Dto.CursoSubcursosRequest;
import com.example.Escolar.Dto.CursoVistaPreviaResponse;
import com.example.Escolar.Dto.SubcursoRequest;
import com.example.Escolar.Service.CursoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/cursos")
@RequiredArgsConstructor
public class CursoController {

    private final CursoService cursoService;

    @GetMapping
    public List<CursoResponse> getAll(@RequestParam(required = false) Integer idAnio,
                                      @RequestParam(required = false) Integer idNivel) {
        return cursoService.getAll(idAnio, idNivel);
    }

    @GetMapping("/{id}")
    public CursoResponse getById(@PathVariable Integer id) {
        return cursoService.getById(id);
    }

    @PostMapping(value = "/importar/vista-previa", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public CursoVistaPreviaResponse vistaPrevia(@RequestParam("archivo") MultipartFile archivo,
                                                @RequestParam("idAnio") Integer idAnio) {
        return cursoService.vistaPrevia(archivo, idAnio);
    }

    @PostMapping(value = "/importar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public CursoImportResponse importar(@RequestParam("archivo") MultipartFile archivo,
                                        @RequestParam("idAnio") Integer idAnio) {
        return cursoService.importar(archivo, idAnio);
    }

    @PostMapping("/copiar")
    @ResponseStatus(HttpStatus.CREATED)
    public List<CursoResponse> copiar(@Valid @RequestBody CursoCopiaRequest request) {
        return cursoService.copiar(request);
    }

    @PatchMapping("/{id}/estado")
    public CursoResponse cambiarEstado(@PathVariable Integer id, @Valid @RequestBody CursoEstadoRequest request) {
        return cursoService.cambiarEstado(id, request.getAccesoId());
    }

    @PatchMapping("/{id}/subcursos")
    public CursoResponse cambiarSubcursos(@PathVariable Integer id, @Valid @RequestBody CursoSubcursosRequest request) {
        return cursoService.cambiarSubcursos(id, request.getTieneSubcursos());
    }

    @PostMapping("/{id}/subcursos")
    @ResponseStatus(HttpStatus.CREATED)
    public CursoResponse crearSubcurso(@PathVariable Integer id, @Valid @RequestBody SubcursoRequest request) {
        return cursoService.crearSubcurso(id, request);
    }

    /** Solo subcursos: las areas se actualizan volviendo a importar. */
    @PutMapping("/{id}")
    public CursoResponse actualizarSubcurso(@PathVariable Integer id, @Valid @RequestBody SubcursoRequest request) {
        return cursoService.actualizarSubcurso(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        cursoService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
