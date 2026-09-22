package com.example.Escolar.Service;

import com.example.Escolar.Dto.AsignacionRequest;
import com.example.Escolar.Dto.AsignacionResponse;
import com.example.Escolar.Dto.HorarioRequest;
import com.example.Escolar.Dto.HorarioResponse;
import com.example.Escolar.Dto.HorasDocenteResponse;
import com.example.Escolar.Exception.ResourceNotFoundException;
import com.example.Escolar.Model.AnioEscolar;
import com.example.Escolar.Model.Asignacion;
import com.example.Escolar.Model.Aula;
import com.example.Escolar.Model.Curso;
import com.example.Escolar.Model.Docente;
import com.example.Escolar.Model.GradoSeccion;
import com.example.Escolar.Model.HorarioClase;
import com.example.Escolar.Repository.AccesoRepository;
import com.example.Escolar.Repository.AnioEscolarRepository;
import com.example.Escolar.Repository.AsignacionRepository;
import com.example.Escolar.Repository.AulaRepository;
import com.example.Escolar.Repository.CursoRepository;
import com.example.Escolar.Repository.DocenteRepository;
import com.example.Escolar.Repository.GradoSeccionRepository;
import com.example.Escolar.Repository.HorarioClaseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AsignacionService {

    public static final double SEMANAS_POR_MES = 4.33;

    private final AsignacionRepository asignacionRepository;
    private final HorarioClaseRepository horarioClaseRepository;
    private final CursoRepository cursoRepository;
    private final DocenteRepository docenteRepository;
    private final GradoSeccionRepository gradoSeccionRepository;
    private final AnioEscolarRepository anioEscolarRepository;
    private final AulaRepository aulaRepository;
    private final AccesoRepository accesoRepository;
    private final AccesoContextoService accesoContextoService;

    public List<AsignacionResponse> getAll(Integer idUsuario, List<String> roles) {
        return asignacionRepository.findByAccesoNot(accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow()).stream()
                .filter(a -> esVisible(a, idUsuario, roles))
                .map(this::toResponse)
                .toList();
    }

    public List<AsignacionResponse> getByDocente(Integer idDocente, Integer idUsuario, List<String> roles) {
        if (accesoContextoService.esPersonalDocente(roles)) {
            Integer idDocentePropio = accesoContextoService.docenteDeUsuario(idUsuario)
                    .map(Docente::getIdDocente)
                    .orElse(null);
            if (idDocentePropio == null || !idDocentePropio.equals(idDocente)) {
                throw new ResourceNotFoundException("Docente no encontrado con id " + idDocente);
            }
        }
        findDocente(idDocente);
        return asignacionRepository.findByDocenteIdDocenteAndAccesoNot(idDocente, accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow()).stream()
                .map(this::toResponse)
                .toList();
    }

    public AsignacionResponse getById(Integer id, Integer idUsuario, List<String> roles) {
        Asignacion asignacion = findAsignacion(id);
        if (!esVisible(asignacion, idUsuario, roles)) {
            throw new ResourceNotFoundException("Asignación no encontrada con id " + id);
        }
        return toResponse(asignacion);
    }

    @Transactional
    public AsignacionResponse create(AsignacionRequest request) {
        Curso curso = findCurso(request.getIdCurso());
        Docente docente = findDocente(request.getIdDocente());
        GradoSeccion gradoSeccion = findGradoSeccion(request.getIdGradoSeccion());
        AnioEscolar anioEscolar = findAnio(request.getIdAnio());

        validarCombinacionUnica(request.getIdCurso(), request.getIdDocente(), request.getIdGradoSeccion(), request.getIdAnio(), null);
        List<HorarioClase> nuevos = prepararHorarios(request, null);
        validarHorariosInternos(nuevos);

        Asignacion asignacion = new Asignacion();
        asignacion.setCurso(curso);
        asignacion.setDocente(docente);
        asignacion.setGradoSeccion(gradoSeccion);
        asignacion.setAnioEscolar(anioEscolar);
        asignacion.setAcceso(accesoRepository.findById(request.getAccesoId() == null ? AccesoConstants.ACTIVO : request.getAccesoId()).orElseThrow());
        asignacion = asignacionRepository.save(asignacion);
        guardarHorarios(asignacion, nuevos);
        return toResponse(asignacion);
    }

    @Transactional
    public AsignacionResponse update(Integer id, AsignacionRequest request) {
        Asignacion asignacion = findAsignacion(id);
        validarBloqueoHorarios(asignacion.getAnioEscolar());
        boolean cursoCambio = request.getIdCurso() != null && !request.getIdCurso().equals(asignacion.getCurso().getIdCurso());
        boolean docenteCambio = request.getIdDocente() != null && !request.getIdDocente().equals(asignacion.getDocente().getIdDocente());
        boolean gsCambio = request.getIdGradoSeccion() != null && !request.getIdGradoSeccion().equals(asignacion.getGradoSeccion().getIdGradoSeccion());
        boolean anioCambio = request.getIdAnio() != null && !request.getIdAnio().equals(asignacion.getAnioEscolar().getIdAnio());

        if (cursoCambio) {
            asignacion.setCurso(findCurso(request.getIdCurso()));
        }
        if (docenteCambio) {
            asignacion.setDocente(findDocente(request.getIdDocente()));
        }
        if (gsCambio) {
            asignacion.setGradoSeccion(findGradoSeccion(request.getIdGradoSeccion()));
        }
        if (anioCambio) {
            asignacion.setAnioEscolar(findAnio(request.getIdAnio()));
        }
        if (request.getAccesoId() != null) {
            asignacion.setAcceso(accesoRepository.findById(request.getAccesoId()).orElseThrow());
        }

        if (request.getHorarios() != null) {
            validarCombinacionUnica(
                    asignacion.getCurso().getIdCurso(),
                    asignacion.getDocente().getIdDocente(),
                    asignacion.getGradoSeccion().getIdGradoSeccion(),
                    asignacion.getAnioEscolar().getIdAnio(),
                    id);
            List<HorarioClase> nuevos = prepararHorarios(request, id);
            validarHorariosInternos(nuevos);
            asignacion = asignacionRepository.save(asignacion);
            reemplazarHorarios(asignacion, nuevos);
        } else {
            asignacion = asignacionRepository.save(asignacion);
        }
        return toResponse(asignacion);
    }

    @Transactional
    public void delete(Integer id) {
        Asignacion asignacion = findAsignacion(id);
        validarBloqueoHorarios(asignacion.getAnioEscolar());
        asignacion.setAcceso(accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow());
        horarioClaseRepository.findByAsignacionIdAsignacionAndAccesoNot(id, accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow())
                .forEach(h -> h.setAcceso(accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow()));
        asignacionRepository.save(asignacion);
    }

    public HorasDocenteResponse calcularHorasDocente(Integer idDocente, String periodo) {
        if (periodo != null && !periodo.isBlank()
                && !"semana".equalsIgnoreCase(periodo) && !"mes".equalsIgnoreCase(periodo)) {
            throw new IllegalArgumentException("El periodo debe ser 'semana' o 'mes'");
        }
        Docente docente = findDocente(idDocente);
        double horasSemana = horarioClaseRepository
                .findByAsignacionDocenteIdDocenteAndAccesoNot(idDocente, accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow()).stream()
                .filter(h -> !h.getAsignacion().getAcceso().getIdAcceso().equals(AccesoConstants.ELIMINADO))
                .mapToDouble(h -> horas(h.getHoraInicio(), h.getHoraFin()))
                .sum();

        HorasDocenteResponse response = new HorasDocenteResponse();
        response.setIdDocente(docente.getIdDocente());
        response.setDocente(docente.getUsuario().getNombre() + " " + docente.getUsuario().getApellidoPat());
        response.setHorasSemana(redondear(horasSemana));
        response.setHorasMes(redondear(horasSemana * SEMANAS_POR_MES));
        return response;
    }

    private List<HorarioClase> prepararHorarios(AsignacionRequest request, Integer idAsignacionExcluido) {
        List<HorarioClase> horarios = new ArrayList<>();
        for (HorarioRequest hr : request.getHorarios()) {
            validarRangoHorario(hr.getHoraInicio(), hr.getHoraFin());
            validarDiaSemana(hr.getDiaSemana());
            Aula aula = findAula(hr.getIdAula());
            validarChoqueDocente(hr, request.getIdDocente(), idAsignacionExcluido);
            validarChoqueAula(hr, idAsignacionExcluido);
            HorarioClase horario = new HorarioClase();
            horario.setAula(aula);
            horario.setDiaSemana(hr.getDiaSemana());
            horario.setHoraInicio(hr.getHoraInicio());
            horario.setHoraFin(hr.getHoraFin());
            horario.setAcceso(accesoRepository.findById(AccesoConstants.ACTIVO).orElseThrow());
            horarios.add(horario);
        }
        return horarios;
    }

    private void guardarHorarios(Asignacion asignacion, List<HorarioClase> horarios) {
        for (HorarioClase horario : horarios) {
            horario.setAsignacion(asignacion);
            horarioClaseRepository.save(horario);
        }
    }

    private void reemplazarHorarios(Asignacion asignacion, List<HorarioClase> nuevos) {
        horarioClaseRepository.findByAsignacionIdAsignacionAndAccesoNot(asignacion.getIdAsignacion(), accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow())
                .forEach(h -> h.setAcceso(accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow()));
        guardarHorarios(asignacion, nuevos);
    }

    private void validarCombinacionUnica(Integer idCurso, Integer idDocente, Integer idGradoSeccion, Integer idAnio, Integer idExcluido) {
        asignacionRepository
                .findByCursoIdCursoAndDocenteIdDocenteAndGradoSeccionIdGradoSeccionAndAnioEscolarIdAnioAndAccesoNot(
                        idCurso, idDocente, idGradoSeccion, idAnio, accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow())
                .filter(a -> idExcluido == null || !a.getIdAsignacion().equals(idExcluido))
                .ifPresent(a -> {
                    throw new IllegalArgumentException("Ya existe una asignación con ese curso, docente y grado-sección para el año indicado");
                });
    }

    private void validarHorariosInternos(List<HorarioClase> horarios) {
        for (int i = 0; i < horarios.size(); i++) {
            for (int j = i + 1; j < horarios.size(); j++) {
                HorarioClase a = horarios.get(i);
                HorarioClase b = horarios.get(j);
                if (seSolapan(a, b)) {
                    throw new IllegalArgumentException("Los horarios de la misma asignación se superponen: día "
                            + a.getDiaSemana() + " " + a.getHoraInicio() + "-" + a.getHoraFin()
                            + " y " + b.getHoraInicio() + "-" + b.getHoraFin());
                }
            }
        }
    }

    private void validarChoqueDocente(HorarioRequest hr, Integer idDocente, Integer idAsignacionExcluido) {
        List<HorarioClase> delDocente = horarioClaseRepository
                .findByAsignacionDocenteIdDocenteAndAccesoNot(idDocente, accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow()).stream()
                .filter(h -> !h.getAsignacion().getAcceso().getIdAcceso().equals(AccesoConstants.ELIMINADO))
                .toList();
        for (HorarioClase existente : delDocente) {
            if (idAsignacionExcluido != null && existente.getAsignacion().getIdAsignacion().equals(idAsignacionExcluido)) {
                continue;
            }
            if (seSolapan(hr.getDiaSemana(), hr.getHoraInicio(), hr.getHoraFin(), existente)) {
                throw new IllegalArgumentException("El docente ya tiene clase en ese horario: día "
                        + existente.getDiaSemana() + " " + existente.getHoraInicio() + "-" + existente.getHoraFin());
            }
        }
    }

    private void validarChoqueAula(HorarioRequest hr, Integer idAsignacionExcluido) {
        List<HorarioClase> delAula = horarioClaseRepository.findByAulaIdAulaAndAccesoNot(hr.getIdAula(), accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow()).stream()
                .filter(h -> !h.getAsignacion().getAcceso().getIdAcceso().equals(AccesoConstants.ELIMINADO))
                .toList();
        for (HorarioClase existente : delAula) {
            if (idAsignacionExcluido != null && existente.getAsignacion().getIdAsignacion().equals(idAsignacionExcluido)) {
                continue;
            }
            if (seSolapan(hr.getDiaSemana(), hr.getHoraInicio(), hr.getHoraFin(), existente)) {
                throw new IllegalArgumentException("El aula " + existente.getAula().getNombre()
                        + " ya está ocupada en ese horario: día " + existente.getDiaSemana()
                        + " " + existente.getHoraInicio() + "-" + existente.getHoraFin());
            }
        }
    }

    private void validarRangoHorario(LocalTime inicio, LocalTime fin) {
        if (!inicio.isBefore(fin)) {
            throw new IllegalArgumentException("La hora de fin debe ser posterior a la hora de inicio");
        }
    }

    private void validarDiaSemana(Byte dia) {
        if (dia == null || dia < 1 || dia > 7) {
            throw new IllegalArgumentException("El día de la semana debe ser un valor entre 1 (Lunes) y 7 (Domingo)");
        }
    }

    private boolean seSolapan(HorarioClase a, HorarioClase b) {
        return a.getDiaSemana() == b.getDiaSemana()
                && a.getHoraInicio().isBefore(b.getHoraFin())
                && b.getHoraInicio().isBefore(a.getHoraFin());
    }

    private boolean seSolapan(Byte diaSemana, LocalTime inicio, LocalTime fin, HorarioClase existente) {
        return diaSemana == existente.getDiaSemana()
                && inicio.isBefore(existente.getHoraFin())
                && existente.getHoraInicio().isBefore(fin);
    }

    private double horas(LocalTime inicio, LocalTime fin) {
        return Duration.between(inicio, fin).toMinutes() / 60.0;
    }

    private double redondear(double valor) {
        return Math.round(valor * 100.0) / 100.0;
    }

    private boolean esVisible(Asignacion asignacion, Integer idUsuario, List<String> roles) {
        if (accesoContextoService.esGestion(roles)) {
            return true;
        }
        if (accesoContextoService.esPersonalDocente(roles)) {
            return accesoContextoService.docenteDeUsuario(idUsuario)
                    .map(d -> d.getIdDocente().equals(asignacion.getDocente().getIdDocente()))
                    .orElse(false);
        }
        if (accesoContextoService.esApoderado(roles)) {
            return accesoContextoService.gradoSeccionIdsDeApoderado(
                    accesoContextoService.apoderadoDeUsuario(idUsuario).orElse(null))
                    .contains(asignacion.getGradoSeccion().getIdGradoSeccion());
        }
        return false;
    }

    private Asignacion findAsignacion(Integer id) {
        return asignacionRepository.findByIdAsignacionAndAccesoNot(id, accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow())
                .orElseThrow(() -> new ResourceNotFoundException("Asignación no encontrada con id " + id));
    }

    private Curso findCurso(Integer id) {
        return cursoRepository.findByIdCursoAndAccesoNot(id, accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow())
                .orElseThrow(() -> new ResourceNotFoundException("Curso no encontrado con id " + id));
    }

    private Docente findDocente(Integer id) {
        return docenteRepository.findByIdDocenteAndAccesoNot(id, accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow())
                .orElseThrow(() -> new ResourceNotFoundException("Docente no encontrado con id " + id));
    }

    private GradoSeccion findGradoSeccion(Integer id) {
        return gradoSeccionRepository.findByIdGradoSeccionAndAccesoNot(id, accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow())
                .orElseThrow(() -> new ResourceNotFoundException("Grado-sección no encontrado con id " + id));
    }

    private AnioEscolar findAnio(Integer id) {
        return anioEscolarRepository.findByIdAnioAndAccesoNot(id, accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow())
                .orElseThrow(() -> new ResourceNotFoundException("Año escolar no encontrado con id " + id));
    }

    private Aula findAula(Integer id) {
        return aulaRepository.findByIdAulaAndAccesoNot(id, accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow())
                .orElseThrow(() -> new ResourceNotFoundException("Aula no encontrada con id " + id));
    }

    private AsignacionResponse toResponse(Asignacion asignacion) {
        AsignacionResponse response = new AsignacionResponse();
        response.setIdAsignacion(asignacion.getIdAsignacion());
        response.setIdCurso(asignacion.getCurso().getIdCurso());
        response.setCurso(asignacion.getCurso().getNombre());
        response.setIdDocente(asignacion.getDocente().getIdDocente());
        response.setDocente(asignacion.getDocente().getUsuario().getNombre() + " " + asignacion.getDocente().getUsuario().getApellidoPat());
        response.setIdGradoSeccion(asignacion.getGradoSeccion().getIdGradoSeccion());
        response.setGrado(asignacion.getGradoSeccion().getGrado().getNombre());
        response.setSeccion(asignacion.getGradoSeccion().getSeccion() != null
                ? asignacion.getGradoSeccion().getSeccion().getNombre()
                : "Única");
        response.setTurno(asignacion.getGradoSeccion().getTurno().getNombre());
        response.setIdAnio(asignacion.getAnioEscolar().getIdAnio());
        response.setAnio(asignacion.getAnioEscolar().getAnio());
        response.setAccesoId(asignacion.getAcceso().getIdAcceso().longValue());
        response.setHorarios(horarioClaseRepository
                .findByAsignacionIdAsignacionAndAccesoNot(asignacion.getIdAsignacion(), accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow()).stream()
                .map(this::toHorarioResponse)
                .toList());
        return response;
    }

    private HorarioResponse toHorarioResponse(HorarioClase horario) {
        HorarioResponse response = new HorarioResponse();
        response.setIdHorario(horario.getIdHorario());
        response.setIdAula(horario.getAula().getIdAula());
        response.setAula(horario.getAula().getNombre());
        response.setDiaSemana(horario.getDiaSemana());
        response.setHoraInicio(horario.getHoraInicio());
        response.setHoraFin(horario.getHoraFin());
        return response;
    }

    private void validarBloqueoHorarios(AnioEscolar anioEscolar) {
        if (Boolean.TRUE.equals(anioEscolar.getBloqueoHorariosPorFecha())
                && anioEscolar.getFechaInicio() != null
                && !anioEscolar.getFechaInicio().isAfter(LocalDate.now())) {
            throw new IllegalArgumentException(
                "No se pueden modificar horarios porque la fecha de inicio del ciclo ya paso"
            );
        }
    }
}
