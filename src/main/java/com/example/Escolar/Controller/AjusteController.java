package com.example.Escolar.Controller;

import com.example.Escolar.Dto.AjusteRequest;
import com.example.Escolar.Dto.AjusteResponse;
import com.example.Escolar.Service.AjusteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/ajustes")
@RequiredArgsConstructor
public class AjusteController {

    private final AjusteService ajusteService;

    @GetMapping
    public List<AjusteResponse> getAll() {
        return ajusteService.getAll();
    }

    @GetMapping("/{id}")
    public AjusteResponse getById(@PathVariable Integer id) {
        return ajusteService.getById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AjusteResponse create(@Valid @RequestBody AjusteRequest request) {
        return ajusteService.create(request);
    }

    @PutMapping("/{id}")
    public AjusteResponse update(@PathVariable Integer id, @Valid @RequestBody AjusteRequest request) {
        return ajusteService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        ajusteService.delete(id);
        return ResponseEntity.noContent().build();
    }
}