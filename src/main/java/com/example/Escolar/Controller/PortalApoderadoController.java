package com.example.Escolar.Controller;

import com.example.Escolar.Dto.HorarioPlanoResponse;
import com.example.Escolar.Dto.PortalApoderadoDtos.*;
import com.example.Escolar.Dto.RecreoHorarioResponse;
import com.example.Escolar.Service.AccesoContextoService;
import com.example.Escolar.Service.PortalApoderadoService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.YearMonth;
import java.util.List;

/** Portal del apoderado: solo lectura y solo sus hijos. */
@RestController
@RequestMapping("/portal-apoderado")
@RequiredArgsConstructor
public class PortalApoderadoController {

    private final PortalApoderadoService portalService;
    private final AccesoContextoService accesoContextoService;

    @GetMapping("/hijos")
    public List<Hijo> hijos() {
        return portalService.hijos(usuario(), rol());
    }

    @GetMapping("/{idMatricula}/resumen")
    public Resumen resumen(@PathVariable Integer idMatricula) {
        return portalService.resumen(idMatricula, usuario(), rol());
    }

    @GetMapping("/{idMatricula}/notas")
    public List<CursoNotas> notas(@PathVariable Integer idMatricula) {
        return portalService.notas(idMatricula, usuario(), rol());
    }

    /** mes: YYYY-MM; sin él, el mes actual dentro del año escolar. */
    @GetMapping("/{idMatricula}/asistencia")
    public AsistenciaMes asistencia(@PathVariable Integer idMatricula,
                                    @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth mes) {
        return portalService.asistencia(idMatricula, mes, usuario(), rol());
    }

    @GetMapping("/{idMatricula}/horario")
    public List<HorarioPlanoResponse> horario(@PathVariable Integer idMatricula) {
        return portalService.horario(idMatricula, usuario(), rol());
    }

    @GetMapping("/{idMatricula}/recreos")
    public List<RecreoHorarioResponse> recreos(@PathVariable Integer idMatricula) {
        return portalService.recreos(idMatricula, usuario(), rol());
    }

    @GetMapping("/{idMatricula}/pagos")
    public Pagos pagos(@PathVariable Integer idMatricula) {
        return portalService.pagos(idMatricula, usuario(), rol());
    }

    private Integer usuario() {
        return accesoContextoService.idUsuarioAutenticado();
    }

    private Integer rol() {
        return accesoContextoService.idRolActivoAutenticado();
    }
}
