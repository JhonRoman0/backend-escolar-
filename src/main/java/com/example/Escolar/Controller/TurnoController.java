package com.example.Escolar.Controller;

import com.example.Escolar.Dto.TurnoRequest;
import com.example.Escolar.Dto.TurnoResponse;
import com.example.Escolar.Service.TurnoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/turnos")
@RequiredArgsConstructor
public class TurnoController {

    private final TurnoService turnoService;

    @GetMapping
    public List<TurnoResponse> getAll() {
        return turnoService.getAll();
    }

    @GetMapping("/{id}")
    public TurnoResponse getById(@PathVariable Integer id) {
        return turnoService.getById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TurnoResponse create(@Valid @RequestBody TurnoRequest request) {
        return turnoService.create(request);
    }

    @PutMapping("/{id}")
    public TurnoResponse update(@PathVariable Integer id, @Valid @RequestBody TurnoRequest request) {
        return turnoService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        turnoService.delete(id);
        return ResponseEntity.noContent().build();
    }
}