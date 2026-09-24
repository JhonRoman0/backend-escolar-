package com.example.Escolar.Controller;

import com.example.Escolar.Dto.GradoSeccionResponse;
import com.example.Escolar.Service.GradoSeccionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/secciones")
@RequiredArgsConstructor
public class GradoSeccionController {

    private final GradoSeccionService gradoSeccionService;

    /**
     * Flujo documentado: /niveles -> /grados?idNivel -> /secciones?idGrado -> /turnos.
     * Cada Grado tiene sus propias Secciones (agregado Grado). No existe catálogo /secciones independiente.
     */
    @GetMapping
    public List<GradoSeccionResponse> getSeccionesByGrado(@RequestParam Integer idGrado) {
        return gradoSeccionService.getSeccionesByGrado(idGrado);
    }
}
