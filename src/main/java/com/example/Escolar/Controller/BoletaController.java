package com.example.Escolar.Controller;

import com.example.Escolar.Dto.BoletaResponse;
import com.example.Escolar.Service.AccesoContextoService;
import com.example.Escolar.Service.BoletaService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/notas/boletas")
@RequiredArgsConstructor
public class BoletaController {

    private final BoletaService boletaService;
    private final AccesoContextoService accesoContextoService;

    @GetMapping
    public BoletaResponse boletas(@RequestParam Integer idGradoSeccion, @RequestParam Integer periodo) {
        return boletaService.boletas(idGradoSeccion, periodo,
                accesoContextoService.idUsuarioAutenticado(), accesoContextoService.rolesAutenticados());
    }
}
