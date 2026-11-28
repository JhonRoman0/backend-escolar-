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
import com.example.Escolar.Model.Acceso;
import com.example.Escolar.Model.Nivel;
import com.example.Escolar.Model.Recreo;
import com.example.Escolar.Model.TipoCurso;
import com.example.Escolar.Model.TipoAula;
import com.example.Escolar.Repository.AccesoRepository;
import com.example.Escolar.Repository.AnioEscolarRepository;
import com.example.Escolar.Repository.AsignacionRepository;
import com.example.Escolar.Repository.AulaRepository;
import com.example.Escolar.Repository.CursoRepository;
import com.example.Escolar.Repository.DocenteNivelRepository;
import com.example.Escolar.Repository.DocenteRepository;
import com.example.Escolar.Repository.GradoSeccionRepository;
import com.example.Escolar.Repository.HorarioClaseRepository;
import com.example.Escolar.Repository.RecreoRepository;
import com.example.Escolar.Security.ConcesionesService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

@Service
@RequiredArgsConstructor
public class AsignacionService {

    public static final double SEMANAS_POR_MES = 4.33;
    private static final String QUE = "asignaciones";
    private static final String PERMISO = "ASIGNACIONES";

    private final AsignacionRepository asignacionRepository;
    private final com.example.Escolar.Repository.ReconocimientoCalificacionRepository reconocimientos;
    private final BloqueoCalificaciones bloqueo;
    private final HorarioClaseRepository horarioClaseRepository;
    private final CursoRepository cursoRepository;
    private final DocenteRepository docenteRepository;
    private final GradoSeccionRepository gradoSeccionRepository;
    private final AnioEscolarRepository anioEscolarRepository;
    private final AulaRepository aulaRepository;
    private final AccesoRepository accesoRepository;
    private final AccesoContextoService accesoContextoService;
    private final ConcesionesService concesionesService;
    private final DocenteNivelRepository docenteNivelRepository;
    private final RecreoRepository recreoRepository;
    private final ConfiguracionAcademicaService configuracionAcademicaService;

    // En una transacción, los horarios reutilizan las asignaciones ya cargadas.
    @Transactional(readOnly = true)
    public List<AsignacionResponse> getAll(Integer idUsuario, Integer idRolActivo) {
        Predicate<Asignacion> visible = visibles(idUsuario, idRolActivo);
        List<Asignacion> asignaciones = asignacionRepository.findByAccesoNot(accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow()).stream()
                .filter(visible)
                .toList();
        Map<Integer, List<HorarioClase>> horarios = horariosDe(asignaciones);
        return asignaciones.stream()
                .map(a -> toResponse(a, horarios.getOrDefault(a.getIdAsignacion(), List.of())))
                .toList();
    }

    /** Horarios de varias asignaciones con una sola consulta (antes era una por fila). */
    private Map<Integer, List<HorarioClase>> horariosDe(List<Asignacion> asignaciones) {
        if (asignaciones.isEmpty()) {
            return Map.of();
        }
        return horarioClaseRepository.findByAsignacionIdAsignacionInAndAccesoNot(
                        asignaciones.stream().map(Asignacion::getIdAsignacion).toList(),
                        accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow()).stream()
                .collect(java.util.stream.Collectors.groupingBy(h -> h.getAsignacion().getIdAsignacion()));
    }

    public List<AsignacionResponse> getByDocente(Integer idDocente, Integer idUsuario, Integer idRolActivo) {
        findDocente(idDocente);
        Predicate<Asignacion> visible = visibles(idUsuario, idRolActivo);
        return asignacionesVigentes(idDocente).stream().filter(visible)
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AsignacionResponse> asignacionesParaCambio(Integer idDocente, Integer idUsuario, Integer idRolActivo) {
        if (!accesoContextoService.tieneAccion("CAMBIOS_DOCENTE", "CREAR", idUsuario, idRolActivo)
                && !accesoContextoService.tieneAccion("CAMBIOS_DOCENTE", "ACTUALIZAR", idUsuario, idRolActivo)) {
            throw new org.springframework.security.access.AccessDeniedException("Sin permiso para gestionar cambios de docente");
        }
        findDocente(idDocente);
        List<Asignacion> asignaciones = asignacionesVigentes(idDocente).stream()
                .filter(a -> AccesoConstants.ACTIVO.equals(a.getAcceso().getIdAcceso()))
                .toList();
        Map<Integer, List<HorarioClase>> horarios = horariosDe(asignaciones);
        return asignaciones.stream()
                .map(a -> toResponse(a, horarios.getOrDefault(a.getIdAsignacion(), List.of())))
                .toList();
    }

    private List<Asignacion> asignacionesVigentes(Integer idDocente) {
        return asignacionRepository.findByDocenteIdDocenteAndAnioEscolarEstadoAndAccesoNot(
                idDocente, AnioEscolarService.ESTADO_VIGENTE,
                accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow());
    }

    public AsignacionResponse getById(Integer id, Integer idUsuario, Integer idRolActivo) {
        Asignacion asignacion = findAsignacion(id);
        if (!visibles(idUsuario, idRolActivo).test(asignacion)) {
            throw new ResourceNotFoundException("Asignación no encontrada con id " + id);
        }
        return toResponse(asignacion);
    }

    /**
     * En un año vigente solo se crea lo que falta (un curso de la seccion sin
     * docente); eso ya lo garantiza la regla de un docente por curso y seccion.
     */
    @Transactional
    public AsignacionResponse create(AsignacionRequest request) {
        Datos datos = validarDatos(request, null);
        ReglasAnioEscolar.validarCreable(datos.anio(), true, QUE);
        List<HorarioClase> nuevos = prepararHorarios(request, datos, null);

        Asignacion asignacion = new Asignacion();
        asignacion.setCurso(datos.curso());
        asignacion.setDocente(datos.docente());
        asignacion.setGradoSeccion(datos.gradoSeccion());
        asignacion.setAnioEscolar(datos.anio());
        asignacion.setAcceso(accesoRepository.findById(request.getAccesoId() == null ? AccesoConstants.ACTIVO : request.getAccesoId()).orElseThrow());
        asignacion = asignacionRepository.save(asignacion);
        guardarHorarios(asignacion, nuevos);
        if (AccesoConstants.ACTIVO.equals(asignacion.getAcceso().getIdAcceso())) invalidarDocente(datos.docente());
        return toResponse(asignacion);
    }

    /** El formulario siempre manda la asignacion completa: se valida como al crear. */
    @Transactional
    public AsignacionResponse update(Integer id, AsignacionRequest request) {
        bloqueo.asignacion(id);
        Asignacion asignacion = findAsignacion(id);
        if (asignacion.getDocente() != null && request.getIdDocente() != null
                && !request.getIdDocente().equals(asignacion.getDocente().getIdDocente())) {
            throw new IllegalArgumentException("El docente de una asignación se cambia desde Cambios de docente");
        }
        validarBloqueoHorarios(asignacion.getAnioEscolar());
        ReglasAnioEscolar.validarEditable(asignacion.getAnioEscolar(), QUE);
        Datos datos = validarDatos(request, id);
        if (reconocimientos.tieneAsignacionActiva(id, com.example.Escolar.Model.ReconocimientoCalificacion.VIGENTE) &&
                (!asignacion.getCurso().getIdCurso().equals(datos.curso().getIdCurso())
                || !asignacion.getGradoSeccion().getIdGradoSeccion().equals(datos.gradoSeccion().getIdGradoSeccion())
                || !asignacion.getAnioEscolar().getIdAnio().equals(datos.anio().getIdAnio())
                || (request.getAccesoId() != null && !AccesoConstants.ACTIVO.equals(request.getAccesoId())))) {
            throw new IllegalArgumentException("La asignación participa en reconocimientos activos");
        }
        ReglasAnioEscolar.validarEditable(datos.anio(), QUE);
        List<HorarioClase> nuevos = prepararHorarios(request, datos, id);

        boolean cambioAlcance = !asignacion.getCurso().getIdCurso().equals(datos.curso().getIdCurso())
                || !asignacion.getGradoSeccion().getIdGradoSeccion().equals(datos.gradoSeccion().getIdGradoSeccion())
                || !asignacion.getAnioEscolar().getIdAnio().equals(datos.anio().getIdAnio())
                || request.getAccesoId() != null && !request.getAccesoId().equals(asignacion.getAcceso().getIdAcceso());
        asignacion.setCurso(datos.curso());
        asignacion.setDocente(datos.docente());
        asignacion.setGradoSeccion(datos.gradoSeccion());
        asignacion.setAnioEscolar(datos.anio());
        if (request.getAccesoId() != null) {
            asignacion.setAcceso(accesoRepository.findById(request.getAccesoId()).orElseThrow());
        }
        asignacion = asignacionRepository.save(asignacion);
        reemplazarHorarios(asignacion, nuevos);
        if (cambioAlcance) invalidarDocente(datos.docente());
        return toResponse(asignacion);
    }

    @Transactional
    public void delete(Integer id) {
        bloqueo.asignacion(id);
        if (reconocimientos.tieneAsignacionActiva(id, com.example.Escolar.Model.ReconocimientoCalificacion.VIGENTE)) throw new IllegalArgumentException("La asignación participa en reconocimientos activos");
        Asignacion asignacion = findAsignacion(id);
        validarBloqueoHorarios(asignacion.getAnioEscolar());
        ReglasAnioEscolar.validarEditable(asignacion.getAnioEscolar(), QUE);
        asignacion.setAcceso(accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow());
        horarioClaseRepository.findByAsignacionIdAsignacionAndAccesoNot(id, accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow())
                .forEach(h -> h.setAcceso(accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow()));
        asignacionRepository.save(asignacion);
        invalidarDocente(asignacion.getDocente());
    }

    private void invalidarDocente(Docente docente) {
        if (docente != null && docente.getUsuario() != null && docente.getUsuario().getIdUsuario() != null) {
            concesionesService.invalidarVinculoDespuesDeCommit(Set.of(docente.getUsuario().getIdUsuario()));
        }
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

    private record Datos(AnioEscolar anio, GradoSeccion gradoSeccion, Curso curso, Docente docente) {
    }

    /** Que el año, la seccion, el curso y el docente encajen entre si. */
    private Datos validarDatos(AsignacionRequest request, Integer idExcluido) {
        AnioEscolar anio = findAnio(request.getIdAnio());
        GradoSeccion gradoSeccion = findGradoSeccion(request.getIdGradoSeccion());
        Curso curso = findCurso(request.getIdCurso());
        Docente docente = findDocente(request.getIdDocente());
        Nivel nivel = gradoSeccion.getGrado().getNivel();

        if (!gradoSeccion.getAnioEscolar().getIdAnio().equals(anio.getIdAnio())) {
            throw new IllegalArgumentException("La sección " + nombreSeccion(gradoSeccion)
                    + " no es del año " + anio.getAnio());
        }
        if (!curso.getAnioEscolar().getIdAnio().equals(anio.getIdAnio())
                || !curso.getNivel().getIdNivel().equals(nivel.getIdNivel())) {
            throw new IllegalArgumentException("El curso " + curso.getNombre() + " no es de la malla de "
                    + nivel.getNombre() + " del año " + anio.getAnio());
        }
        if (!AccesoConstants.ACTIVO.equals(curso.getAcceso().getIdAcceso())) {
            throw new IllegalArgumentException("El curso " + curso.getNombre() + " está inactivo");
        }
        // Se asignan las areas sin subcursos y los subcursos; no las transversales
        // ni un area que se dicta dividida en subcursos.
        if (curso.getTipo() == TipoCurso.TRANSVERSAL) {
            throw new IllegalArgumentException(curso.getNombre() + " es una competencia transversal: no se asigna a un docente");
        }
        if (curso.getTipo() == TipoCurso.AREA && curso.isTieneSubcursos()) {
            throw new IllegalArgumentException(curso.getNombre() + " se dicta en subcursos: asigna cada subcurso");
        }
        if (!AccesoConstants.ACTIVO.equals(docente.getAcceso().getIdAcceso())) {
            throw new IllegalArgumentException("El docente está inactivo");
        }
        boolean ensenaEnElNivel = docenteNivelRepository.findByDocenteAndAccesoNot(docente, accesoEliminado()).stream()
                .anyMatch(dn -> dn.getNivel().getIdNivel().equals(nivel.getIdNivel()));
        if (!ensenaEnElNivel) {
            throw new IllegalArgumentException("El docente no enseña en el nivel " + nivel.getNombre());
        }
        boolean yaTieneDocente = asignacionRepository
                .findByCursoIdCursoAndGradoSeccionIdGradoSeccionAndAccesoNot(
                        curso.getIdCurso(), gradoSeccion.getIdGradoSeccion(), accesoEliminado()).stream()
                .anyMatch(a -> !a.getIdAsignacion().equals(idExcluido));
        if (yaTieneDocente) {
            throw new IllegalArgumentException(curso.getNombre() + " de " + nombreSeccion(gradoSeccion)
                    + " ya tiene un docente asignado");
        }
        return new Datos(anio, gradoSeccion, curso, docente);
    }

    /**
     * Cada horario cae dentro del turno de la seccion, en un dia que tiene
     * recreo y sin cruzarse con el recreo, con los otros horarios de la misma
     * asignacion ni con las otras clases del año de la seccion, del docente o
     * del aula (salvo que el aula sea de uso compartido).
     */
    private List<HorarioClase> prepararHorarios(AsignacionRequest request, Datos datos, Integer idExcluido) {
        GradoSeccion gs = datos.gradoSeccion();
        Integer idAnio = datos.anio().getIdAnio();
        Acceso eliminado = accesoEliminado();
        if (gs.getAula() == null) {
            throw new IllegalArgumentException("Asigna primero el salón de " + nombreSeccion(gs) + " en Grados y secciones");
        }
        // La clase dura bloques de la hora academica configurada para el año y
        // nivel (Configuracion academica). Null: libre, como inicial por defecto.
        Integer horaAcademica = configuracionAcademicaService.minutosHoraAcademica(datos.anio(), gs.getGrado().getNivel());
        List<Recreo> recreos = recreoRepository.findByAnioEscolarIdAnioAndTurnoIdTurnoAndGradoIdGradoAndAccesoNot(
                idAnio, gs.getTurno().getIdTurno(), gs.getGrado().getIdGrado(), eliminado);
        List<HorarioClase> deLaSeccion = vigentes(horarioClaseRepository
                .findByAsignacionGradoSeccionIdGradoSeccionInAndAccesoNot(List.of(gs.getIdGradoSeccion()), eliminado), idExcluido);
        List<HorarioClase> delDocente = vigentes(horarioClaseRepository
                .findByAsignacionDocenteIdDocenteAndAsignacionAnioEscolarIdAnioAndAccesoNot(
                        datos.docente().getIdDocente(), idAnio, eliminado), idExcluido);

        List<HorarioClase> horarios = new ArrayList<>();
        for (HorarioRequest hr : request.getHorarios()) {
            HorarioUtil.validarDia(hr.getDiaSemana());
            byte dia = hr.getDiaSemana();
            HorarioUtil.validarDentroDelTurno(hr.getHoraInicio(), hr.getHoraFin(), gs.getTurno());
            if (horaAcademica != null
                    && Duration.between(hr.getHoraInicio(), hr.getHoraFin()).toMinutes() % horaAcademica != 0) {
                throw new IllegalArgumentException("La clase debe durar bloques de " + horaAcademica + " minutos ("
                        + horaAcademica + ", " + horaAcademica * 2 + ", " + horaAcademica * 3 + "…): "
                        + HorarioUtil.rango(hr.getHoraInicio(), hr.getHoraFin()) + " no cumple");
            }
            String cuando = "el " + HorarioUtil.dia(dia) + " de " + HorarioUtil.rango(hr.getHoraInicio(), hr.getHoraFin());

            List<Recreo> recreosDelDia = recreos.stream().filter(r -> r.getDiaSemana() == dia).toList();
            if (recreosDelDia.isEmpty()) {
                throw new IllegalArgumentException("Primero configura el recreo de " + gs.getGrado().getNombre()
                        + " – turno " + gs.getTurno().getNombre() + " para el " + HorarioUtil.dia(dia));
            }
            for (Recreo recreo : recreosDelDia) {
                if (HorarioUtil.seCruzan(hr.getHoraInicio(), hr.getHoraFin(), recreo.getHoraInicio(), recreo.getHoraFin())) {
                    throw new IllegalArgumentException("La clase " + cuando + " choca con el recreo del "
                            + HorarioUtil.dia(dia) + " (" + HorarioUtil.rango(recreo.getHoraInicio(), recreo.getHoraFin()) + ")");
                }
            }
            for (HorarioClase otro : horarios) {
                if (otro.getDiaSemana() == dia
                        && HorarioUtil.seCruzan(hr.getHoraInicio(), hr.getHoraFin(), otro.getHoraInicio(), otro.getHoraFin())) {
                    throw new IllegalArgumentException("Los horarios de esta asignación se cruzan " + cuando);
                }
            }
            for (HorarioClase otra : deLaSeccion) {
                if (seCruza(hr, otra)) {
                    throw new IllegalArgumentException(nombreSeccion(gs) + " ya tiene " + otra.getAsignacion().getCurso().getNombre()
                            + " el " + HorarioUtil.dia(dia) + " de " + HorarioUtil.rango(otra.getHoraInicio(), otra.getHoraFin()));
                }
            }
            for (HorarioClase otra : delDocente) {
                if (seCruza(hr, otra)) {
                    throw new IllegalArgumentException("El docente ya tiene " + otra.getAsignacion().getCurso().getNombre()
                            + " en " + nombreSeccion(otra.getAsignacion().getGradoSeccion()) + " el " + HorarioUtil.dia(dia)
                            + " de " + HorarioUtil.rango(otra.getHoraInicio(), otra.getHoraFin()));
                }
            }
            Aula aula = findAula(hr.getIdAula());
            // Su salon, un ambiente especial o un espacio compartido; nunca el salon de otra seccion.
            if (aula.getTipo() == TipoAula.SALON && !aula.getIdAula().equals(gs.getAula().getIdAula())) {
                throw new IllegalArgumentException("El " + aula.getNombre() + " es el salón de otra sección: elige el "
                        + gs.getAula().getNombre() + ", un ambiente especial o un espacio compartido");
            }
            if (aula.getTipo() != TipoAula.COMPARTIDO) {
                for (HorarioClase otra : vigentes(horarioClaseRepository
                        .findByAulaIdAulaAndAsignacionAnioEscolarIdAnioAndAccesoNot(aula.getIdAula(), idAnio, eliminado), idExcluido)) {
                    if (seCruza(hr, otra)) {
                        throw new IllegalArgumentException("El aula " + aula.getNombre() + " ya está ocupada el "
                                + HorarioUtil.dia(dia) + " de " + HorarioUtil.rango(otra.getHoraInicio(), otra.getHoraFin())
                                + " por " + nombreSeccion(otra.getAsignacion().getGradoSeccion()));
                    }
                }
            }

            HorarioClase horario = new HorarioClase();
            horario.setAula(aula);
            horario.setDiaSemana(dia);
            horario.setHoraInicio(hr.getHoraInicio());
            horario.setHoraFin(hr.getHoraFin());
            horario.setAcceso(accesoRepository.findById(AccesoConstants.ACTIVO).orElseThrow());
            horarios.add(horario);
        }
        return horarios;
    }

    /** Horarios de asignaciones no eliminadas, sin los de la asignacion que se edita. */
    private List<HorarioClase> vigentes(List<HorarioClase> horarios, Integer idExcluido) {
        return horarios.stream()
                .filter(h -> !AccesoConstants.ELIMINADO.equals(h.getAsignacion().getAcceso().getIdAcceso()))
                .filter(h -> idExcluido == null || !h.getAsignacion().getIdAsignacion().equals(idExcluido))
                .toList();
    }

    private boolean seCruza(HorarioRequest hr, HorarioClase existente) {
        return hr.getDiaSemana() == existente.getDiaSemana()
                && HorarioUtil.seCruzan(hr.getHoraInicio(), hr.getHoraFin(), existente.getHoraInicio(), existente.getHoraFin());
    }

    private String nombreSeccion(GradoSeccion gs) {
        return gs.getGrado().getNombre() + " " + (gs.getSeccion() != null ? gs.getSeccion().getNombre() : "Única");
    }

    private Acceso accesoEliminado() {
        return accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow();
    }

    private void guardarHorarios(Asignacion asignacion, List<HorarioClase> horarios) {
        for (HorarioClase horario : horarios) {
            horario.setAsignacion(asignacion);
            horarioClaseRepository.save(horario);
        }
    }

    private void reemplazarHorarios(Asignacion asignacion, List<HorarioClase> nuevos) {
        horarioClaseRepository.findByAsignacionIdAsignacionAndAccesoNot(asignacion.getIdAsignacion(), accesoEliminado())
                .forEach(h -> h.setAcceso(accesoEliminado()));
        guardarHorarios(asignacion, nuevos);
    }

    private double horas(LocalTime inicio, LocalTime fin) {
        return Duration.between(inicio, fin).toMinutes() / 60.0;
    }

    private double redondear(double valor) {
        return Math.round(valor * 100.0) / 100.0;
    }

    // Se calcula una vez por consulta: el alcance lee los permisos del usuario.
    private Predicate<Asignacion> visibles(Integer idUsuario, Integer idRolActivo) {
        AccesoContextoService.Alcance alcance = accesoContextoService.alcance(PERMISO, idUsuario, idRolActivo);
        if (alcance == AccesoContextoService.Alcance.TODO) {
            return asignacion -> true;
        }
        Integer idDocente = alcance.incluyeDocente() ? accesoContextoService.docenteDeUsuario(idUsuario)
                .map(Docente::getIdDocente).orElse(null) : null;
        Set<Integer> secciones = alcance.incluyeApoderado()
                ? accesoContextoService.gradoSeccionIdsDeApoderado(
                        accesoContextoService.apoderadoDeUsuario(idUsuario).orElse(null)) : Set.of();
        return asignacion -> AccesoConstants.ACTIVO.equals(asignacion.getAcceso().getIdAcceso())
                && (asignacion.getDocente().getIdDocente().equals(idDocente)
                        || secciones.contains(asignacion.getGradoSeccion().getIdGradoSeccion()));
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
        return toResponse(asignacion, horarioClaseRepository.findByAsignacionIdAsignacionAndAccesoNot(
                asignacion.getIdAsignacion(), accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow()));
    }

    private AsignacionResponse toResponse(Asignacion asignacion, List<HorarioClase> horarios) {
        AsignacionResponse response = new AsignacionResponse();
        response.setIdAsignacion(asignacion.getIdAsignacion());
        response.setIdCurso(asignacion.getCurso().getIdCurso());
        response.setCurso(asignacion.getCurso().getNombre());
        response.setIdDocente(asignacion.getDocente().getIdDocente());
        response.setDocente(asignacion.getDocente().getUsuario().getNombre() + " " + asignacion.getDocente().getUsuario().getApellidoPat());
        response.setIdGradoSeccion(asignacion.getGradoSeccion().getIdGradoSeccion());
        response.setIdNivel(asignacion.getGradoSeccion().getGrado().getNivel().getIdNivel());
        response.setNivel(asignacion.getGradoSeccion().getGrado().getNivel().getNombre());
        response.setIdGrado(asignacion.getGradoSeccion().getGrado().getIdGrado());
        response.setIdTurno(asignacion.getGradoSeccion().getTurno().getIdTurno());
        response.setGrado(asignacion.getGradoSeccion().getGrado().getNombre());
        response.setSeccion(asignacion.getGradoSeccion().getSeccion() != null
                ? asignacion.getGradoSeccion().getSeccion().getNombre()
                : "Única");
        response.setTurno(asignacion.getGradoSeccion().getTurno().getNombre());
        response.setIdAnio(asignacion.getAnioEscolar().getIdAnio());
        response.setAnio(asignacion.getAnioEscolar().getAnio());
        response.setAccesoId(asignacion.getAcceso().getIdAcceso().longValue());
        response.setHorarios(horarios.stream().map(this::toHorarioResponse).toList());
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
