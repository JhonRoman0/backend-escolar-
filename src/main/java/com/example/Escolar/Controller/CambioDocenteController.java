package com.example.Escolar.Controller;

import com.example.Escolar.Dto.CambioDocenteResponse;
import com.example.Escolar.Service.CambioDocenteService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/cambios-docente")
@RequiredArgsConstructor
public class CambioDocenteController {

    private final CambioDocenteService cambioDocenteService;

    @GetMapping
    public List<CambioDocenteResponse> getAll() {
        return cambioDocenteService.getAll();
    }

    @GetMapping("/{id}")
    public CambioDocenteResponse getById(@PathVariable Integer id) {
        return cambioDocenteService.getById(id);
    }

    @GetMapping("/docente/{idDocente}")
    public List<CambioDocenteResponse> getByDocente(@PathVariable Integer idDocente) {
        return cambioDocenteService.getByDocente(idDocente);
    }
}
