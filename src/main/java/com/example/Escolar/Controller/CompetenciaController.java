package com.example.Escolar.Controller;

import com.example.Escolar.Dto.CompetenciaResponse;
import com.example.Escolar.Dto.CursoEstadoRequest;
import com.example.Escolar.Service.CompetenciaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/competencias")
@RequiredArgsConstructor
public class CompetenciaController {

    private final CompetenciaService competenciaService;

    @GetMapping
    public List<CompetenciaResponse> listar(@RequestParam(required = false) Integer idCurso) {
        return competenciaService.listar(idCurso);
    }

    @PatchMapping("/{id}/estado")
    public CompetenciaResponse cambiarEstado(@PathVariable Integer id, @Valid @RequestBody CursoEstadoRequest request) {
        return competenciaService.cambiarEstado(id, request.getAccesoId());
    }
}
