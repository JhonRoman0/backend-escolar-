package com.example.Escolar.Controller;

import com.example.Escolar.Dto.RolRequest;
import com.example.Escolar.Dto.RolResponse;
import com.example.Escolar.Service.RolService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/roles")
@RequiredArgsConstructor
public class RolController {

    private final RolService rolService;

    @GetMapping
    public List<RolResponse> getAll() {
        return rolService.getAll();
    }

    @GetMapping("/{id}")
    public RolResponse getById(@PathVariable Integer id) {
        return rolService.getById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RolResponse create(@Valid @RequestBody RolRequest request) {
        return rolService.create(request);
    }

    @PutMapping("/{id}")
    public RolResponse update(@PathVariable Integer id, @Valid @RequestBody RolRequest request) {
        return rolService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        rolService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
