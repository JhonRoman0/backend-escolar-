package com.example.Escolar.Controller;

import com.example.Escolar.Dto.HorarioPlanoResponse;
import com.example.Escolar.Service.AccesoContextoService;
import com.example.Escolar.Service.HorarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/horarios")
@RequiredArgsConstructor
public class HorarioController {

    private final HorarioService horarioService;
    private final AccesoContextoService accesoContextoService;

    @GetMapping
    public List<HorarioPlanoResponse> listar(
            @RequestParam(required = false) String grado,
            @RequestParam(required = false) String seccion,
            @RequestParam(required = false) Integer docente,
            @RequestParam(required = false) Integer anioEscolar) {
        return horarioService.listar(grado, seccion, docente, anioEscolar,
                accesoContextoService.idUsuarioAutenticado(), accesoContextoService.rolesAutenticados());
    }

    @GetMapping("/docente/{idDocente}")
    public List<HorarioPlanoResponse> porDocente(
            @PathVariable Integer idDocente,
            @RequestParam(required = false) Integer anioEscolar) {
        return horarioService.porDocente(idDocente, anioEscolar,
                accesoContextoService.idUsuarioAutenticado(), accesoContextoService.rolesAutenticados());
    }

    @GetMapping("/alumno/{idAlumno}")
    public List<HorarioPlanoResponse> porAlumno(
            @PathVariable Integer idAlumno,
            @RequestParam(required = false) Integer anioEscolar) {
        return horarioService.porAlumno(idAlumno, anioEscolar,
                accesoContextoService.idUsuarioAutenticado(), accesoContextoService.rolesAutenticados());
    }
}
