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
    public org.springframework.data.domain.Page<HorarioPlanoResponse> listar(
            @RequestParam(required = false) String grado,
            @RequestParam(required = false) String seccion,
            @RequestParam(required = false) Integer docente,
            @RequestParam(required = false) Integer anioEscolar,
            @RequestParam(required = false) Integer idNivel,
            @RequestParam(required = false) Integer idGrado,
            @RequestParam(required = false) Integer idSeccion,
            @RequestParam(required = false) Integer idTurno,
            @RequestParam(required = false) Integer idGradoSeccion,
            @RequestParam(required = false) Integer idAnio,
            @org.springframework.data.web.PageableDefault(size = 20, sort = "idHorario") org.springframework.data.domain.Pageable pageable) {
        // anioEscolar (legacy) tiene prioridad sobre idAnio
        Integer anioEfectivo = anioEscolar != null ? anioEscolar : idAnio;
        return horarioService.listar(grado, seccion, docente, anioEfectivo,
                idNivel, idGrado, idSeccion, idTurno, idGradoSeccion,
                accesoContextoService.idUsuarioAutenticado(), accesoContextoService.rolesAutenticados(), pageable);
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
