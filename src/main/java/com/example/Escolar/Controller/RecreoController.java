package com.example.Escolar.Controller;

import com.example.Escolar.Dto.RecreoRequest;
import com.example.Escolar.Dto.RecreoResponse;
import com.example.Escolar.Dto.RecreoUpdateRequest;
import com.example.Escolar.Service.RecreoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/recreos")
@RequiredArgsConstructor
public class RecreoController {

    private final RecreoService recreoService;

    @GetMapping
    public List<RecreoResponse> getAll(@RequestParam(required = false) Integer idAnio,
                                       @RequestParam(required = false) Integer idNivel,
                                       @RequestParam(required = false) Integer idTurno) {
        return recreoService.getAll(idAnio, idNivel, idTurno);
    }

    @GetMapping("/{id}")
    public RecreoResponse getById(@PathVariable Integer id) {
        return recreoService.getById(id);
    }

    /** Crea un recreo por cada grado, dia y franja del formulario. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public List<RecreoResponse> create(@Valid @RequestBody RecreoRequest request) {
        return recreoService.create(request);
    }

    @PutMapping("/{id}")
    public RecreoResponse update(@PathVariable Integer id, @Valid @RequestBody RecreoUpdateRequest request) {
        return recreoService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        recreoService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/anio/{idAnio}/turno/{idTurno}/grado/{idGrado}")
    public ResponseEntity<Void> deleteDelGrado(@PathVariable Integer idAnio, @PathVariable Integer idTurno,
                                               @PathVariable Integer idGrado) {
        recreoService.deleteDelGrado(idAnio, idTurno, idGrado);
        return ResponseEntity.noContent().build();
    }
}
