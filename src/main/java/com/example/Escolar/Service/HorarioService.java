package com.example.Escolar.Service;

import com.example.Escolar.Dto.HorarioPlanoResponse;
import com.example.Escolar.Dto.MiHorarioResponse;
import com.example.Escolar.Dto.RecreoHorarioResponse;
import com.example.Escolar.Model.GradoSeccion;
import com.example.Escolar.Model.Recreo;
import com.example.Escolar.Repository.GradoSeccionRepository;
import com.example.Escolar.Repository.RecreoRepository;
import com.example.Escolar.Exception.ResourceNotFoundException;
import com.example.Escolar.Model.HorarioClase;
import com.example.Escolar.Model.Matricula;
import com.example.Escolar.Repository.AccesoRepository;
import com.example.Escolar.Repository.AnioEscolarRepository;
import com.example.Escolar.Repository.HorarioClaseRepository;
import com.example.Escolar.Repository.MatriculaRepository;
import com.example.Escolar.Service.AccesoContextoService.Alcance;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class HorarioService {

    private static final String PERMISO = "HORARIOS";
    public static final byte ESTADO_ACTIVO = 1;

    private final HorarioClaseRepository horarioClaseRepository;
    private final AnioEscolarRepository anioEscolarRepository;
    private final MatriculaRepository matriculaRepository;
    private final AccesoRepository accesoRepository;
    private final AccesoContextoService accesoContextoService;
    private final RecreoRepository recreoRepository;
    private final GradoSeccionRepository gradoSeccionRepository;

    /**
     * Con VER_COLEGIO (o ADMIN) se ven todos los horarios. Sin ella, el docente
     * ve los suyos, el apoderado los de sus hijos y quien no tiene vínculo, nada.
     */
    private Alcance alcance(Integer idUsuario, List<String> roles) {
        if (accesoContextoService.tieneAccion(PERMISO, "VER_COLEGIO", idUsuario, roles)) {
            return Alcance.TODO;
        }
        return accesoContextoService.alcance(PERMISO, idUsuario, roles);
    }

    public org.springframework.data.domain.Page<HorarioPlanoResponse> listar(
            String grado, String seccion,
            Integer idDocente, Integer idAnio,
            Integer idNivel, Integer idGrado, Integer idSeccion, Integer idTurno, Integer idGradoSeccion,
            Integer idUsuario, List<String> roles,
            org.springframework.data.domain.Pageable pageable) {
        Integer anio = resolverAnio(idAnio);
        if (alcance(idUsuario, roles) == Alcance.TODO) {
            org.springframework.data.domain.Page<HorarioClase> page = horarioClaseRepository.findFiltrados(
                    accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow(), anio,
                    idNivel, idGrado, idSeccion, idTurno, idGradoSeccion, idDocente, grado, seccion, pageable);
            return page.map(this::toPlanoResponse);
        } else {
            List<HorarioClase> filtrados = horarioClaseRepository.findFiltradosList(
                    accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow(), anio,
                    idNivel, idGrado, idSeccion, idTurno, idGradoSeccion, idDocente, grado, seccion);
            List<HorarioClase> visibles = filtrarPorVisibilidad(filtrados, idUsuario, roles);
            return paginarHorarios(visibles, pageable);
        }
    }

    // Compatibilidad sin paginación (deprecated)
    public List<HorarioPlanoResponse> listar(String grado, String seccion,
                                              Integer idDocente, Integer idAnio,
                                              Integer idUsuario, List<String> roles) {
        return listar(grado, seccion, idDocente, idAnio, null, null, null, null, null, idUsuario, roles,
                org.springframework.data.domain.Pageable.unpaged()).getContent();
    }

    private org.springframework.data.domain.Page<HorarioPlanoResponse> paginarHorarios(List<HorarioClase> lista, org.springframework.data.domain.Pageable pageable) {
        if (pageable.isUnpaged()) {
            return new org.springframework.data.domain.PageImpl<>(lista.stream().map(this::toPlanoResponse).toList());
        }
        int total = lista.size();
        int start = Math.min((int) pageable.getOffset(), total);
        int end = Math.min(start + pageable.getPageSize(), total);
        List<HorarioPlanoResponse> content = lista.subList(start, end).stream().map(this::toPlanoResponse).toList();
        return new org.springframework.data.domain.PageImpl<>(content, pageable, total);
    }

    public List<HorarioPlanoResponse> porDocente(Integer idDocente, Integer idAnio,
                                                   Integer idUsuario, List<String> roles) {
        Integer anio = resolverAnio(idAnio);
        List<HorarioClase> horarios;

        Alcance alcance = alcance(idUsuario, roles);
        if (alcance == Alcance.TODO) {
            horarios = horarioClaseRepository.findPlanosPorDocenteYAnio(accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow(), anio, idDocente);
        } else if (alcance.incluyeDocente()) {
            Integer idPropio = accesoContextoService.docenteDeUsuario(idUsuario)
                    .map(d -> d.getIdDocente())
                    .orElse(null);
            if (idPropio == null || !idPropio.equals(idDocente)) {
                throw new ResourceNotFoundException("Docente no encontrado con id " + idDocente);
            }
            horarios = horarioClaseRepository.findPlanosPorDocenteYAnio(accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow(), anio, idDocente);
        } else {
            horarios = horarioClaseRepository.findPlanosPorDocenteYAnio(accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow(), anio, idDocente);
            horarios = filtrarPorVisibilidad(horarios, idUsuario, roles);
        }
        return horarios.stream().filter(h -> alcance == Alcance.TODO || asignacionActiva(h))
                .map(this::toPlanoResponse).toList();
    }

    /**
     * Horario del docente vinculado al usuario: todas sus clases del año, en
     * cualquier nivel, grado, sección o turno. Sin año, el vigente.
     */
    public MiHorarioResponse mio(Integer idAnio, Integer idUsuario) {
        Integer idDocente = accesoContextoService.docenteDeUsuario(idUsuario)
                .map(d -> d.getIdDocente())
                .orElseThrow(() -> new IllegalArgumentException("Tu usuario no está vinculado a un docente"));
        Integer anio = resolverAnio(idAnio);
        List<HorarioClase> clases = horarioClaseRepository.findPlanosPorDocenteYAnio(
                accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow(), anio, idDocente);
        clases = clases.stream().filter(HorarioService::asignacionActiva).toList();
        // Por turno: con un solo grado (aunque sean varias secciones) su recreo aplica a todas sus clases.
        List<RecreoHorarioResponse> recreos = clases.stream()
                .map(h -> h.getAsignacion().getGradoSeccion())
                .collect(java.util.stream.Collectors.groupingBy(gs -> gs.getTurno().getIdTurno()))
                .values().stream()
                .filter(secciones -> secciones.stream().map(gs -> gs.getGrado().getIdGrado()).distinct().count() == 1)
                .flatMap(secciones -> recreosDe(anio, secciones.get(0)).stream())
                .toList();
        return new MiHorarioResponse(clases.stream().map(this::toPlanoResponse).toList(), recreos);
    }

    /** Recreos del grado y turno de una sección, en su año. */
    public List<RecreoHorarioResponse> recreosDeSeccion(Integer idGradoSeccion) {
        GradoSeccion gs = gradoSeccionRepository.findById(idGradoSeccion)
                .orElseThrow(() -> new ResourceNotFoundException("Sección no encontrada"));
        return recreosDe(gs.getAnioEscolar().getIdAnio(), gs);
    }

    private List<RecreoHorarioResponse> recreosDe(Integer idAnio, GradoSeccion gs) {
        return recreoRepository.findByAnioEscolarIdAnioAndTurnoIdTurnoAndGradoIdGradoAndAccesoNot(idAnio,
                        gs.getTurno().getIdTurno(), gs.getGrado().getIdGrado(),
                        accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow()).stream()
                .map(HorarioService::aRecreo)
                .toList();
    }

    private static RecreoHorarioResponse aRecreo(Recreo r) {
        return new RecreoHorarioResponse(r.getTurno().getIdTurno(), r.getTurno().getNombre(), r.getDiaSemana(),
                r.getHoraInicio(), r.getHoraFin(), r.getGrado().getNombre());
    }

    public List<HorarioPlanoResponse> porAlumno(Integer idAlumno, Integer idAnio,
                                                  Integer idUsuario, List<String> roles) {
        Integer anio = resolverAnio(idAnio);
        List<Matricula> matriculas = matriculaRepository.findActivasPorAlumnoId(idAlumno, accesoRepository.findById(AccesoConstants.ACTIVO).orElseThrow()).stream()
                .filter(m -> m.getGradoSeccion().getAnioEscolar().getIdAnio().equals(anio))
                .toList();
        if (matriculas.isEmpty()) {
            throw new ResourceNotFoundException("El alumno con id " + idAlumno + " no tiene matrícula activa");
        }

        Alcance alcance = alcance(idUsuario, roles);
        if (alcance != Alcance.TODO && (alcance == null || !alcance.incluyeApoderado()
                || !accesoContextoService.esApoderadoDe(idUsuario, matriculas.get(0)))) {
            throw new ResourceNotFoundException("Alumno no encontrado con id " + idAlumno);
        }

        Integer idGradoSeccion = matriculas.get(0).getGradoSeccion().getIdGradoSeccion();
        List<HorarioClase> horarios = horarioClaseRepository
                .findPlanosPorGradoSeccionYAnio(accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow(), anio, idGradoSeccion);
        return horarios.stream().map(this::toPlanoResponse).toList();
    }

    /** Horario de una sección en un año; quien llama ya verificó el acceso (portal del apoderado). */
    public List<HorarioPlanoResponse> planoDeSeccion(Integer idAnio, Integer idGradoSeccion) {
        return horarioClaseRepository
                .findPlanosPorGradoSeccionYAnio(accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow(), idAnio, idGradoSeccion)
                .stream().map(this::toPlanoResponse).toList();
    }

    private Integer resolverAnio(Integer idAnioParam) {
        if (idAnioParam != null) {
            return idAnioParam;
        }
        return anioEscolarRepository.findByEstadoAndAccesoNot(ESTADO_ACTIVO, accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow())
                .map(a -> a.getIdAnio())
                .orElseThrow(() -> new ResourceNotFoundException("No hay un año escolar activo"));
    }

    private static boolean asignacionActiva(HorarioClase horario) {
        return horario.getAsignacion().getAcceso() != null
                && AccesoConstants.ACTIVO.equals(horario.getAsignacion().getAcceso().getIdAcceso());
    }

    private List<HorarioClase> filtrarPorVisibilidad(List<HorarioClase> horarios,
                                                      Integer idUsuario, List<String> roles) {
        Alcance alcance = alcance(idUsuario, roles);
        if (alcance == Alcance.TODO) {
            return horarios;
        }
        Set<Integer> propias = alcance != null && alcance.incluyeDocente()
                ? accesoContextoService.asignacionesActivasDeDocente(idUsuario).stream()
                        .map(a -> a.getIdAsignacion()).collect(java.util.stream.Collectors.toSet())
                : Set.of();
        Set<Integer> seccionesHijos = alcance != null && alcance.incluyeApoderado()
                ? accesoContextoService.gradoSeccionIdsDeApoderado(
                        accesoContextoService.apoderadoDeUsuario(idUsuario).orElse(null))
                : Set.of();
        return horarios.stream().filter(HorarioService::asignacionActiva)
                .filter(h -> propias.contains(h.getAsignacion().getIdAsignacion())
                        || seccionesHijos.contains(h.getAsignacion().getGradoSeccion().getIdGradoSeccion()))
                .toList();
    }

    private List<HorarioClase> aplicarFiltros(List<HorarioClase> horarios,
                                               String grado, String seccion, Integer idDocente) {
        return horarios.stream()
                .filter(h -> grado == null || grado.equalsIgnoreCase(
                        h.getAsignacion().getGradoSeccion().getGrado().getNombre()))
                .filter(h -> seccion == null || seccion.equalsIgnoreCase(
                        h.getAsignacion().getGradoSeccion().getSeccion() != null
                                ? h.getAsignacion().getGradoSeccion().getSeccion().getNombre()
                                : "Única"))
                .filter(h -> idDocente == null || idDocente.equals(
                        h.getAsignacion().getDocente().getIdDocente()))
                .toList();
    }

    private HorarioPlanoResponse toPlanoResponse(HorarioClase h) {
        HorarioPlanoResponse r = new HorarioPlanoResponse();
        r.setIdHorario(h.getIdHorario());
        r.setDiaSemana(h.getDiaSemana());
        r.setHoraInicio(h.getHoraInicio());
        r.setHoraFin(h.getHoraFin());
        r.setIdAula(h.getAula().getIdAula());
        r.setAula(h.getAula().getNombre());
        r.setCurso(h.getAsignacion().getCurso().getNombre());
        r.setIdDocente(h.getAsignacion().getDocente().getIdDocente());
        r.setDocente(h.getAsignacion().getDocente().getUsuario().getNombre() + " "
                + h.getAsignacion().getDocente().getUsuario().getApellidoPat() + " "
                + h.getAsignacion().getDocente().getUsuario().getApellidoMat());
        r.setIdGradoSeccion(h.getAsignacion().getGradoSeccion().getIdGradoSeccion());
        r.setGrado(h.getAsignacion().getGradoSeccion().getGrado().getNombre());
        r.setSeccion(h.getAsignacion().getGradoSeccion().getSeccion() != null
                ? h.getAsignacion().getGradoSeccion().getSeccion().getNombre()
                : "Única");
        r.setTurno(h.getAsignacion().getGradoSeccion().getTurno().getNombre());
        r.setIdNivel(h.getAsignacion().getGradoSeccion().getGrado().getNivel().getIdNivel());
        r.setNivel(h.getAsignacion().getGradoSeccion().getGrado().getNivel().getNombre());
        r.setIdAnio(h.getAsignacion().getAnioEscolar().getIdAnio());
        return r;
    }
}
