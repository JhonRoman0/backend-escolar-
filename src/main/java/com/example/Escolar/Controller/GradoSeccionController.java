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

    @GetMapping
    public List<GradoSeccionResponse> getSeccionesByGrado(@RequestParam Integer idGrado) {
        return gradoSeccionService.getSeccionesByGrado(idGrado);
    }
}
