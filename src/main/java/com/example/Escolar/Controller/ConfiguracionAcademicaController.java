package com.example.Escolar.Controller;

import com.example.Escolar.Dto.ConfiguracionAcademicaRequest;
import com.example.Escolar.Dto.ConfiguracionAcademicaResponse;
import com.example.Escolar.Service.ConfiguracionAcademicaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/configuracion-academica")
@RequiredArgsConstructor
public class ConfiguracionAcademicaController {

    private final ConfiguracionAcademicaService configuracionService;

    /** Una fila por nivel con la configuracion vigente del año. */
    @GetMapping
    public List<ConfiguracionAcademicaResponse> listar(@RequestParam Integer idAnio) {
        return configuracionService.listar(idAnio);
    }

    @PutMapping
    public ConfiguracionAcademicaResponse guardar(@Valid @RequestBody ConfiguracionAcademicaRequest request) {
        return configuracionService.guardar(request);
    }
}
