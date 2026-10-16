package com.example.Escolar.Controller;

import com.example.Escolar.Dto.CursoEstadoRequest;
import com.example.Escolar.Dto.CursoImportResponse;
import com.example.Escolar.Dto.CursoRequest;
import com.example.Escolar.Dto.CursoResponse;
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
    public List<CursoResponse> getAll() {
        return cursoService.getAll();
    }

    @GetMapping("/{id}")
    public CursoResponse getById(@PathVariable Integer id) {
        return cursoService.getById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CursoResponse create(@Valid @RequestBody CursoRequest request) {
        return cursoService.create(request);
    }

    @PostMapping(value = "/importar-excel", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public CursoImportResponse importarExcel(@RequestParam("archivo") MultipartFile archivo) {
        return cursoService.importarDesdeExcel(archivo);
    }

    @PutMapping("/{id}")
    public CursoResponse update(@PathVariable Integer id, @Valid @RequestBody CursoRequest request) {
        return cursoService.update(id, request);
    }

    @PatchMapping("/{id}/estado")
    public CursoResponse cambiarEstado(@PathVariable Integer id, @Valid @RequestBody CursoEstadoRequest request) {
        return cursoService.cambiarEstado(id, request.getAccesoId());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        cursoService.delete(id);
        return ResponseEntity.noContent().build();
    }
}