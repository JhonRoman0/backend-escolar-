package com.example.Escolar.Service;

import com.example.Escolar.Dto.HorarioPlanoResponse;
import com.example.Escolar.Exception.ResourceNotFoundException;
import com.example.Escolar.Model.HorarioClase;
import com.example.Escolar.Model.Matricula;
import com.example.Escolar.Repository.AccesoRepository;
import com.example.Escolar.Repository.AnioEscolarRepository;
import com.example.Escolar.Repository.HorarioClaseRepository;
import com.example.Escolar.Repository.MatriculaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class HorarioService {

    public static final byte ESTADO_ACTIVO = 1;

    private final HorarioClaseRepository horarioClaseRepository;
    private final AnioEscolarRepository anioEscolarRepository;
    private final MatriculaRepository matriculaRepository;
    private final AccesoRepository accesoRepository;
    private final AccesoContextoService accesoContextoService;

    public org.springframework.data.domain.Page<HorarioPlanoResponse> listar(
            String grado, String seccion,
            Integer idDocente, Integer idAnio,
            Integer idNivel, Integer idGrado, Integer idSeccion, Integer idTurno, Integer idGradoSeccion,
            Integer idUsuario, List<String> roles,
            org.springframework.data.domain.Pageable pageable) {
        Integer anio = resolverAnio(idAnio);
        boolean esGestion = accesoContextoService.esGestion(roles);
        if (esGestion) {
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

        if (accesoContextoService.esGestion(roles)) {
            horarios = horarioClaseRepository.findPlanosPorDocenteYAnio(accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow(), anio, idDocente);
        } else if (accesoContextoService.esPersonalDocente(roles)) {
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
        return horarios.stream().map(this::toPlanoResponse).toList();
    }

    public List<HorarioPlanoResponse> porAlumno(Integer idAlumno, Integer idAnio,
                                                  Integer idUsuario, List<String> roles) {
        Integer anio = resolverAnio(idAnio);
        List<Matricula> matriculas = matriculaRepository.findActivasPorAlumnoId(idAlumno, accesoRepository.findById(AccesoConstants.ACTIVO).orElseThrow());
        if (matriculas.isEmpty()) {
            throw new ResourceNotFoundException("El alumno con id " + idAlumno + " no tiene matrícula activa");
        }

        if (accesoContextoService.esApoderado(roles)) {
            boolean esHijo = matriculas.stream().anyMatch(m ->
                    accesoContextoService.apoderadoDeUsuario(idUsuario)
                            .map(ap -> ap.getIdApoderado().equals(
                                    m.getAlumnoApoderado().getApoderado().getIdApoderado()))
                            .orElse(false));
            if (!esHijo) {
                throw new ResourceNotFoundException("El alumno con id " + idAlumno + " no es hijo del apoderado autenticado");
            }
        }

        Integer idGradoSeccion = matriculas.get(0).getGradoSeccion().getIdGradoSeccion();
        List<HorarioClase> horarios = horarioClaseRepository
                .findPlanosPorGradoSeccionYAnio(accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow(), anio, idGradoSeccion);
        return horarios.stream().map(this::toPlanoResponse).toList();
    }

    private Integer resolverAnio(Integer idAnioParam) {
        if (idAnioParam != null) {
            return idAnioParam;
        }
        return anioEscolarRepository.findByEstadoAndAccesoNot(ESTADO_ACTIVO, accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow())
                .map(a -> a.getIdAnio())
                .orElseThrow(() -> new ResourceNotFoundException("No hay un año escolar activo"));
    }

    private List<HorarioClase> filtrarPorVisibilidad(List<HorarioClase> horarios,
                                                      Integer idUsuario, List<String> roles) {
        if (accesoContextoService.esGestion(roles)) {
            return horarios;
        }
        if (accesoContextoService.esPersonalDocente(roles)) {
            Set<Integer> gradoSeccionIds = accesoContextoService.gradoSeccionIdsDeDocente(idUsuario);
            return horarios.stream()
                    .filter(h -> gradoSeccionIds.contains(
                            h.getAsignacion().getGradoSeccion().getIdGradoSeccion()))
                    .toList();
        }
        if (accesoContextoService.esApoderado(roles)) {
            Set<Integer> gradoSeccionIds = accesoContextoService.gradoSeccionIdsDeApoderado(
                    accesoContextoService.apoderadoDeUsuario(idUsuario).orElse(null));
            return horarios.stream()
                    .filter(h -> gradoSeccionIds.contains(
                            h.getAsignacion().getGradoSeccion().getIdGradoSeccion()))
                    .toList();
        }
        return List.of();
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
        r.setIdAnio(h.getAsignacion().getAnioEscolar().getIdAnio());
        return r;
    }
}
