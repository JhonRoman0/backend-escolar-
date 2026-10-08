package com.example.Escolar.Controller;

import com.example.Escolar.Dto.GradoRequest;
import com.example.Escolar.Dto.GradoResponse;
import com.example.Escolar.Service.GradoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/grados")
@RequiredArgsConstructor
public class GradoController {

    private final GradoService gradoService;

    @GetMapping
    public List<GradoResponse> getAll(@RequestParam(required = false) Integer idNivel,
            @RequestParam(required = false) Integer idAnio) {
        if (idNivel != null || idAnio != null) {
            return gradoService.getAllByNivelYAnio(idNivel, idAnio);
        }
        return gradoService.getAll();
    }

    @GetMapping("/{id}")
    public GradoResponse getById(@PathVariable Integer id) {
        return gradoService.getById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public GradoResponse create(@Valid @RequestBody GradoRequest request) {
        return gradoService.create(request);
    }

    @PutMapping("/{id}")
    public GradoResponse update(@PathVariable Integer id, @Valid @RequestBody GradoRequest request) {
        return gradoService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        gradoService.delete(id);
        return ResponseEntity.noContent().build();
    }
}