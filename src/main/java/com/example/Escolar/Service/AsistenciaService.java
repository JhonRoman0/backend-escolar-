package com.example.Escolar.Service;

import com.example.Escolar.Dto.AsistenciaDiaResponse;
import com.example.Escolar.Dto.AsistenciaRequest;
import com.example.Escolar.Dto.AsistenciaResponse;
import com.example.Escolar.Dto.EstadisticasResponse;
import com.example.Escolar.Dto.EstadoAsistenciaResponse;
import com.example.Escolar.Dto.JustificarRequest;
import com.example.Escolar.Dto.MatrizSemanalResponse;
import com.example.Escolar.Dto.NotificacionAsistenciaDto;
import com.example.Escolar.Dto.ReporteAlumnoResponse;
import com.example.Escolar.Dto.ReporteGeneralResponse;
import com.example.Escolar.Dto.ReporteUsuarioResponse;
import com.example.Escolar.Dto.ResumenMensualResponse;
import com.example.Escolar.Dto.UploadResponse;
import com.example.Escolar.Exception.ResourceNotFoundException;
import com.example.Escolar.Model.*;
import com.example.Escolar.Repository.*;
import com.example.Escolar.Service.AccesoContextoService.Alcance;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.text.Normalizer;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Asistencia de entrada al colegio: una marca por alumno y día de clase, según
 * el horario de su turno. Todo se mira dentro de un año escolar (el vigente por
 * defecto) y solo en sus días de clase (ver {@link CalendarioEscolar}). Sin
 * marca, el alumno está Pendiente hasta el límite de tardanza de su turno y luego
 * en Inasistencia (falta). Cada uno ve y actúa según su vínculo: el docente,
 * sobre las secciones donde dicta; lo que puede hacer lo dan sus permisos.
 */
@Service
@RequiredArgsConstructor
public class AsistenciaService {

    private static final String PERMISO = "ASISTENCIAS";
    public static final byte PRINCIPAL = 1;

    public static final String ESTADO_PUNTUAL = "Puntual";
    public static final String ESTADO_TARDANZA = "Tardanza";
    public static final String ESTADO_INASISTENCIA = "Inasistencia";
    public static final String ESTADO_JUSTIFICADA = "Justificada";
    /** Sin marca, aún dentro del límite de tardanza de su turno. */
    public static final String ESTADO_PENDIENTE = "Pendiente";
    /** Feriado, fin de semana, vacaciones o fuera del año. */
    public static final String ESTADO_SIN_CLASES = "Sin clases";

    public static final String ACCION_REGISTRO = "REGISTRO";
    public static final String ACCION_JUSTIFICACION = "JUSTIFICACION";
    public static final String ACCION_ANULACION = "ANULACION";

    static final String CARPETA_EVIDENCIAS = "asistencias/evidencias";
    static final int ACOMPANANTE_MAXIMO = 150;

    private final AsistenciaAlumnoRepository asistenciaAlumnoRepository;
    private final EstadoAsistenciaRepository estadoAsistenciaRepository;
    private final JustificacionRepository justificacionRepository;
    private final HistorialAsistenciaRepository historialAsistenciaRepository;
    private final AlumnoRepository alumnoRepository;
    private final AlumnoApoderadoRepository alumnoApoderadoRepository;
    private final MatriculaRepository matriculaRepository;
    private final AnioEscolarRepository anioEscolarRepository;
    private final UsuarioRepository usuarioRepository;
    private final NotificacionService notificacionService;
    private final AccesoRepository accesoRepository;
    private final AccesoContextoService accesoContextoService;
    private final CalendarioEscolar calendario;
    private final CloudinaryService cloudinaryService;
    private final AvisoFaltaRepository avisoFaltaRepository;

    /** Filtros de las vistas. Sin año: el vigente. estado solo aplica a la vista de hoy. */
    public record Filtro(Integer idAnio, Integer idNivel, Integer idGrado, Integer idSeccion, Integer idTurno,
                         Integer idGradoSeccion, String search, String estado) {
        public static Filtro delAnio(Integer idAnio) {
            return new Filtro(idAnio, null, null, null, null, null, null, null);
        }
    }

    // ============================= REGISTRO =============================

    @Transactional(readOnly = true)
    public AsistenciaResponse previsualizar(AsistenciaRequest request, Integer idUsuario, List<String> roles) {
        validarEscritura(idUsuario, roles);
        Alumno alumno = buscarAlumno(request);
        Matricula matricula = matriculaDelAnioVigente(alumno);
        validarAlcance(matricula, idUsuario, roles);
        LocalDate hoy = LocalDate.now();
        validarDiaDeClase(matricula, hoy);
        Turno turno = matricula.getGradoSeccion().getTurno();
        LocalTime ahora = LocalTime.now();
        if (marcaDelDia(matricula, hoy).isPresent()) {
            throw new IllegalArgumentException("El alumno ya registró su asistencia el día de hoy.");
        }
        validarVentanaIngreso(ahora, turno);
        EstadoAsistencia estado = calcularEstado(ahora, turno);
        return toResponse(null, matricula, estado,
                request.getIdJustificacion() != null ? justificacion(request.getIdJustificacion()) : null);
    }

    /**
     * Registra la entrada. Pasado el límite de tardanza solo entra justificado:
     * con motivo, nombre de quien lo trajo y foto de evidencia (obligatorios).
     */
    @Transactional
    public AsistenciaResponse confirmar(AsistenciaRequest request, MultipartFile foto, Integer idUsuario, List<String> roles) {
        validarEscritura(idUsuario, roles);
        Alumno alumno = buscarAlumno(request);
        Matricula matricula = matriculaDelAnioVigente(alumno);
        validarAlcance(matricula, idUsuario, roles);
        LocalDate hoy = LocalDate.now();
        validarDiaDeClase(matricula, hoy);
        Turno turno = matricula.getGradoSeccion().getTurno();
        LocalTime ahora = LocalTime.now();
        validarVentanaIngreso(ahora, turno);
        if (marcaDelDia(matricula, hoy).isPresent()) {
            throw new IllegalArgumentException("El alumno ya registró su asistencia el día de hoy.");
        }

        EstadoAsistencia estado = calcularEstado(ahora, turno);
        Justificacion justificacion = null;
        String acompanante = null;
        UploadResponse evidencia = null;
        if (ESTADO_JUSTIFICADA.equals(estado.getNombre())) {
            if (request.getIdJustificacion() == null) {
                throw new IllegalArgumentException("Pasado el límite de tardanza el alumno solo ingresa justificado: elige el motivo.");
            }
            acompanante = request.getAcompanante() == null ? "" : request.getAcompanante().trim();
            if (acompanante.length() < 3 || acompanante.length() > ACOMPANANTE_MAXIMO) {
                throw new IllegalArgumentException("Escribe el nombre de la persona que trajo al alumno.");
            }
            if (foto == null || foto.isEmpty() || foto.getContentType() == null || !foto.getContentType().startsWith("image/")) {
                throw new IllegalArgumentException("Toma la foto de evidencia del ingreso justificado.");
            }
            justificacion = justificacion(request.getIdJustificacion());
            evidencia = cloudinaryService.upload(foto, CARPETA_EVIDENCIAS);
            borrarSiNoSeGuarda(evidencia);
        }

        Usuario usuario = findUsuario(idUsuario);
        AsistenciaAlumno asistencia = new AsistenciaAlumno();
        asistencia.setMatricula(matricula);
        asistencia.setEstado(estado);
        asistencia.setJustificacion(justificacion);
        asistencia.setUsuarioRegistro(usuario);
        asistencia.setFecha(hoy);
        asistencia.setHoraEntrada(ahora);
        asistencia.setAcompanante(acompanante);
        asistencia.setUrlEvidencia(evidencia != null ? evidencia.getUrl() : null);
        asistencia.setPkEvidencia(evidencia != null ? evidencia.getPublicId() : null);
        asistencia.setAcceso(acceso(AccesoConstants.ACTIVO));
        asistencia = asistenciaAlumnoRepository.save(asistencia);

        guardarHistorial(asistencia, usuario, ACCION_REGISTRO, null, estado.getNombre(),
                acompanante != null ? "Ingreso justificado, acompañado de " + acompanante : "Marcado de asistencia");
        avisar(NotificacionAsistenciaDto.Tipo.INGRESO, alumno, hoy, ahora, estado.getNombre(),
                justificacion != null ? justificacion.getMotivo() : null, acompanante);
        return toResponse(asistencia, matricula, estado, justificacion);
    }

    @Transactional
    public AsistenciaResponse justificar(Integer idAsistencia, JustificarRequest request, Integer idUsuario, List<String> roles) {
        validarEscritura(idUsuario, roles);
        AsistenciaAlumno asistencia = findAsistencia(idAsistencia);
        validarModificable(asistencia, idUsuario, roles);
        Usuario usuario = findUsuario(idUsuario);
        Justificacion justificacion = justificacion(request.getIdJustificacion());

        String estadoAnterior = asistencia.getEstado().getNombre();
        EstadoAsistencia estadoJustificado = estado(ESTADO_JUSTIFICADA);
        asistencia.setEstado(estadoJustificado);
        asistencia.setJustificacion(justificacion);
        asistencia = asistenciaAlumnoRepository.save(asistencia);

        guardarHistorial(asistencia, usuario, ACCION_JUSTIFICACION, estadoAnterior, estadoJustificado.getNombre(),
                "Justificación retrospectiva: " + justificacion.getMotivo());
        avisar(NotificacionAsistenciaDto.Tipo.JUSTIFICACION, alumnoDe(asistencia.getMatricula()), asistencia.getFecha(),
                asistencia.getHoraEntrada(), estadoJustificado.getNombre(), justificacion.getMotivo(), null);
        return toResponse(asistencia, asistencia.getMatricula(), estadoJustificado, justificacion);
    }

    /**
     * Anula una marca equivocada (por ejemplo, el QR de otro alumno): solo el mismo
     * día y con motivo, que queda en el historial. El alumno vuelve a Pendiente o
     * Falta, pero sin aviso de falta: sus apoderados ya recibieron el de ingreso.
     */
    @Transactional
    public void anular(Integer idAsistencia, String motivo, Integer idUsuario, List<String> roles) {
        validarEscritura(idUsuario, roles);
        AsistenciaAlumno asistencia = findAsistencia(idAsistencia);
        validarModificable(asistencia, idUsuario, roles);
        if (!asistencia.getFecha().equals(LocalDate.now())) {
            throw new IllegalArgumentException("Solo se anulan marcas del mismo día.");
        }
        String texto = motivo == null ? "" : motivo.trim();
        if (texto.length() < 5) {
            throw new IllegalArgumentException("Escribe el motivo de la anulación.");
        }
        Usuario usuario = findUsuario(idUsuario);
        String estadoAnterior = asistencia.getEstado().getNombre();
        asistencia.setAcceso(acceso(AccesoConstants.ELIMINADO));
        asistenciaAlumnoRepository.save(asistencia);
        guardarHistorial(asistencia, usuario, ACCION_ANULACION, estadoAnterior, null, "Marca anulada: " + texto);
        // Se deja como ya avisado para que la tarea de faltas no le escriba.
        if (avisoFaltaRepository.findByFecha(asistencia.getFecha()).stream()
                .noneMatch(a -> a.getMatricula().getIdMatricula().equals(asistencia.getMatricula().getIdMatricula()))) {
            AvisoFalta aviso = new AvisoFalta();
            aviso.setMatricula(asistencia.getMatricula());
            aviso.setFecha(asistencia.getFecha());
            aviso.setFechaEnvio(LocalDateTime.now());
            avisoFaltaRepository.save(aviso);
        }
    }

    @Transactional(readOnly = true)
    public List<EstadoAsistenciaResponse> listarEstados() {
        return estadoAsistenciaRepository.findByAccesoNot(eliminado()).stream()
                .map(e -> {
                    EstadoAsistenciaResponse r = new EstadoAsistenciaResponse();
                    r.setIdEstado(e.getIdEstado());
                    r.setNombre(e.getNombre());
                    return r;
                })
                .toList();
    }

    // ============================= VISTAS =============================

    @Transactional(readOnly = true)
    public Page<AsistenciaDiaResponse> asistenciasHoy(Integer idUsuario, List<String> roles, Filtro filtro, Pageable pageable) {
        LocalDate hoy = LocalDate.now();
        List<Matricula> matriculas = matriculasVisibles(idUsuario, roles, filtro);
        Map<Integer, AsistenciaAlumno> marcas = marcasPorMatricula(matriculas, hoy, hoy).entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().get(0)));
        Map<Integer, Set<LocalDate>> dias = new HashMap<>();
        List<AsistenciaDiaResponse> contenido = matriculas.stream().map(m -> {
            AsistenciaDiaResponse row = filaDia(m);
            AsistenciaAlumno a = marcas.get(m.getIdMatricula());
            if (a != null) {
                row.setIdAsistencia(a.getIdAsistencia());
                row.setEstado(a.getEstado().getNombre());
                row.setHoraEntrada(a.getHoraEntrada());
                row.setMarcadoPor(marcadoPor(a));
                row.setJustificacion(a.getJustificacion() != null ? a.getJustificacion().getMotivo() : null);
                row.setAcompanante(a.getAcompanante());
                row.setUrlEvidencia(a.getUrlEvidencia());
            } else {
                row.setEstado(estadoSinMarca(m, hoy, diasDe(m, hoy, hoy, dias)));
            }
            return row;
        }).filter(r -> filtro.estado() == null || filtro.estado().isBlank() || filtro.estado().equalsIgnoreCase(r.getEstado()))
                .collect(Collectors.toList());
        return paginar(contenido, pageable);
    }

    @Transactional(readOnly = true)
    public Page<MatrizSemanalResponse> matrizSemanal(LocalDate fecha, Pageable pageable, Integer idUsuario, List<String> roles,
                                                      Filtro filtro) {
        LocalDate lunes = fecha.with(DayOfWeek.MONDAY);
        LocalDate viernes = lunes.plusDays(4);
        List<Matricula> matriculas = matriculasVisibles(idUsuario, roles, filtro);
        Map<Integer, List<AsistenciaAlumno>> marcas = marcasPorMatricula(matriculas, lunes, viernes);
        Map<Integer, Set<LocalDate>> dias = new HashMap<>();
        List<MatrizSemanalResponse> contenido = matriculas.stream().map(m -> {
            Alumno alumno = alumnoDe(m);
            GradoSeccion gs = m.getGradoSeccion();
            MatrizSemanalResponse row = new MatrizSemanalResponse();
            row.setIdAlumno(alumno.getIdAlumno());
            row.setAlumno(nombreCompleto(alumno));
            row.setGrado(gs.getGrado().getNombre());
            row.setSeccion(nombreSeccion(gs));
            completarAcademico(gs, row::setIdGradoSeccion, row::setIdTurno, row::setTurno, row::setIdAnio, row::setAnio,
                    row::setIdGrado, row::setIdNivel, row::setNivel, row::setIdSeccion);
            Map<LocalDate, String> delAlumno = marcas.getOrDefault(m.getIdMatricula(), List.of()).stream()
                    .collect(Collectors.toMap(AsistenciaAlumno::getFecha, a -> a.getEstado().getNombre(), (x, y) -> x));
            Set<LocalDate> deClase = diasDe(m, lunes, viernes, dias);
            List<String> estados = new ArrayList<>();
            for (int i = 0; i < 5; i++) {
                LocalDate dia = lunes.plusDays(i);
                estados.add(delAlumno.getOrDefault(dia, estadoSinMarca(m, dia, deClase)));
            }
            row.setEstados(estados);
            return row;
        }).collect(Collectors.toList());
        return paginar(contenido, pageable);
    }

    @Transactional(readOnly = true)
    public Page<ResumenMensualResponse> resumenMensual(LocalDate fecha, Pageable pageable, Integer idUsuario, List<String> roles,
                                                        Filtro filtro) {
        LocalDate inicio = fecha.withDayOfMonth(1);
        LocalDate fin = fecha.withDayOfMonth(fecha.lengthOfMonth());
        List<Matricula> matriculas = matriculasVisibles(idUsuario, roles, filtro);
        Map<Integer, List<AsistenciaAlumno>> marcas = marcasPorMatricula(matriculas, inicio, fin);
        Map<Integer, Set<LocalDate>> dias = new HashMap<>();
        List<ResumenMensualResponse> contenido = matriculas.stream()
                .map(m -> resumenDeMatricula(m, inicio, fin, marcas.getOrDefault(m.getIdMatricula(), List.of()), dias))
                .collect(Collectors.toList());
        return paginar(contenido, pageable);
    }

    /** Resumen del mes de cada hijo (para el portal del apoderado). */
    @Transactional(readOnly = true)
    public List<ResumenMensualResponse> asistenciasDeHijos(Integer idUsuarioAutenticado, List<String> roles) {
        Apoderado apoderado = accesoContextoService.apoderadoDeUsuario(idUsuarioAutenticado).orElse(null);
        if (apoderado == null) {
            return List.of();
        }
        LocalDate hoy = LocalDate.now();
        LocalDate inicio = hoy.withDayOfMonth(1);
        LocalDate fin = hoy.withDayOfMonth(hoy.lengthOfMonth());
        Map<Integer, Set<LocalDate>> dias = new HashMap<>();
        List<ResumenMensualResponse> resultado = new ArrayList<>();
        for (AlumnoApoderado vinculo : alumnoApoderadoRepository.findByApoderado(apoderado)) {
            accesoContextoService.matriculaActual(vinculo.getAlumno().getIdAlumno()).ifPresent(m -> {
                List<AsistenciaAlumno> marcas = asistenciaAlumnoRepository
                        .findByMatriculaIdMatriculaAndFechaBetweenAndAccesoNot(m.getIdMatricula(), inicio, fin, eliminado());
                resultado.add(resumenDeMatricula(m, inicio, fin, marcas, dias));
            });
        }
        return resultado;
    }

    /** KPIs del rango (hoy, semana o mes) en el año. Los días futuros no cuentan. */
    @Transactional(readOnly = true)
    public EstadisticasResponse estadisticas(String rango, LocalDate fecha, Integer idAnio, Integer idUsuario, List<String> roles) {
        LocalDate inicio = fecha;
        LocalDate fin = fecha;
        if ("semana".equalsIgnoreCase(rango)) {
            inicio = fecha.with(DayOfWeek.MONDAY);
            fin = fecha.with(DayOfWeek.FRIDAY);
        } else if ("mes".equalsIgnoreCase(rango)) {
            inicio = fecha.withDayOfMonth(1);
            fin = fecha.withDayOfMonth(fecha.lengthOfMonth());
        }
        List<Matricula> matriculas = matriculasVisibles(idUsuario, roles, Filtro.delAnio(idAnio));
        Map<Integer, List<AsistenciaAlumno>> marcas = marcasPorMatricula(matriculas, inicio, fin);
        Map<Integer, Set<LocalDate>> dias = new HashMap<>();
        long esperados = 0;
        long pendientes = 0;
        List<AsistenciaAlumno> registros = new ArrayList<>();
        for (Matricula m : matriculas) {
            Cuenta c = contar(m, inicio, fin, marcas.getOrDefault(m.getIdMatricula(), List.of()), dias);
            esperados += c.esperados();
            pendientes += c.pendientes();
            registros.addAll(c.registros());
        }
        long presentes = contarEstado(registros, ESTADO_PUNTUAL);
        long tardanzas = contarEstado(registros, ESTADO_TARDANZA);
        long justificados = contarEstado(registros, ESTADO_JUSTIFICADA);

        EstadisticasResponse response = new EstadisticasResponse();
        response.setPresentes(presentes);
        response.setTardanzas(tardanzas);
        response.setJustificados(justificados);
        response.setInasistencias(Math.max(0, esperados - registros.size()));
        response.setPendientes(pendientes);
        response.setTotalEsperado(esperados);
        response.setPorcentajeAsistencia(esperados == 0 ? 0.0
                : Math.round(((double) registros.size() / esperados) * 10000.0) / 100.0);
        return response;
    }

    // ============================= REPORTES =============================

    @Transactional(readOnly = true)
    public Page<ReporteGeneralResponse> reporteGeneral(LocalDate inicio, LocalDate fin, Pageable pageable,
                                                       Integer idUsuario, List<String> roles,
                                                       Integer idNivel, Integer idGrado, Integer idSeccion,
                                                       Integer idTurno, Integer idGradoSeccion, Integer idAnio) {
        LocalDate desde = inicio != null ? inicio : LocalDate.now();
        LocalDate hasta = fin != null ? fin : LocalDate.now();
        List<Matricula> matriculas = matriculasVisibles(idUsuario, roles,
                new Filtro(idAnio, idNivel, idGrado, idSeccion, idTurno, idGradoSeccion, null, null));
        Map<Integer, List<AsistenciaAlumno>> marcas = marcasPorMatricula(matriculas, desde, hasta);
        Map<Integer, Set<LocalDate>> dias = new HashMap<>();
        List<ReporteGeneralResponse> contenido = new ArrayList<>();
        for (Matricula m : matriculas) {
            Cuenta c = contar(m, desde, hasta, marcas.getOrDefault(m.getIdMatricula(), List.of()), dias);
            c.registros().forEach(a -> contenido.add(filaReporte(m, a.getFecha(), a)));
            // Los días de clase ya vencidos sin marca son faltas.
            c.faltas().forEach(dia -> contenido.add(filaReporte(m, dia, null)));
        }
        contenido.sort(Comparator.comparing(ReporteGeneralResponse::getFecha)
                .thenComparing(ReporteGeneralResponse::getHoraEntrada, Comparator.nullsFirst(Comparator.naturalOrder())));
        return paginar(contenido, pageable);
    }

    @Transactional(readOnly = true)
    public List<ReporteAlumnoResponse> reportePorAlumno(Integer idAlumno, LocalDate inicio, LocalDate fin,
                                                        Integer idUsuario, List<String> roles) {
        Alumno alumno = alumnoRepository.findByIdAlumnoAndAccesoNot(idAlumno, eliminado())
                .orElseThrow(() -> new ResourceNotFoundException("Alumno no encontrado con id " + idAlumno));
        Matricula matricula = accesoContextoService.matriculaActual(alumno.getIdAlumno())
                .orElseThrow(() -> new ResourceNotFoundException("El alumno no tiene matrícula"));
        if (!esVisible(matricula, idUsuario, roles)) {
            throw new ResourceNotFoundException("Alumno no encontrado con id " + idAlumno);
        }
        AnioEscolar anio = matricula.getGradoSeccion().getAnioEscolar();
        LocalDate desde = inicio != null ? inicio : (anio.getFechaInicio() != null ? anio.getFechaInicio() : LocalDate.of(2000, 1, 1));
        LocalDate hasta = fin != null ? fin : LocalDate.now();
        return asistenciaAlumnoRepository
                .findByMatriculaIdMatriculaAndFechaBetweenAndAccesoNot(matricula.getIdMatricula(), desde, hasta, eliminado())
                .stream().map(a -> {
                    ReporteAlumnoResponse r = new ReporteAlumnoResponse();
                    r.setFecha(a.getFecha());
                    r.setHoraEntrada(a.getHoraEntrada());
                    r.setEstado(a.getEstado().getNombre());
                    r.setJustificacion(a.getJustificacion() != null ? a.getJustificacion().getMotivo() : "-");
                    r.setRegistradoPor(marcadoPor(a));
                    return r;
                }).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ReporteUsuarioResponse> reportePorUsuario(Integer idUsuarioConsulta, LocalDate inicio, LocalDate fin,
                                                          Integer idUsuario, List<String> roles) {
        LocalDate desde = inicio != null ? inicio : LocalDate.of(2000, 1, 1);
        LocalDate hasta = fin != null ? fin : LocalDate.now();
        if (alcanceLectura(idUsuario, roles) != Alcance.TODO && !Objects.equals(idUsuarioConsulta, idUsuario)) {
            throw new ResourceNotFoundException("Usuario registrador no encontrado con id " + idUsuarioConsulta);
        }
        usuarioRepository.findByIdUsuarioAndAccesoNot(idUsuarioConsulta, eliminado())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario registrador no encontrado con id " + idUsuarioConsulta));
        return asistenciaAlumnoRepository
                .findByUsuarioRegistroIdUsuarioAndFechaBetweenAndAccesoNot(idUsuarioConsulta, desde, hasta, eliminado())
                .stream().map(a -> {
                    GradoSeccion gs = a.getMatricula().getGradoSeccion();
                    ReporteUsuarioResponse r = new ReporteUsuarioResponse();
                    r.setFecha(a.getFecha());
                    r.setHoraEntrada(a.getHoraEntrada());
                    r.setAlumno(nombreCompleto(alumnoDe(a.getMatricula())));
                    r.setGradoSeccion(gs.getGrado().getNombre() + " - " + nombreSeccion(gs));
                    r.setEstado(a.getEstado().getNombre());
                    r.setJustificacion(a.getJustificacion() != null ? a.getJustificacion().getMotivo() : "-");
                    return r;
                }).collect(Collectors.toList());
    }

    // ======================== REGLAS ========================

    /** Esperados (días de clase ya vencidos), registros en ellos, faltas y pendiente de hoy. */
    record Cuenta(long esperados, long pendientes, List<AsistenciaAlumno> registros, List<LocalDate> faltas) {
    }

    private Cuenta contar(Matricula m, LocalDate inicio, LocalDate fin, List<AsistenciaAlumno> marcas,
                          Map<Integer, Set<LocalDate>> cache) {
        LocalDate hoy = LocalDate.now();
        LocalDate hasta = fin.isAfter(hoy) ? hoy : fin;
        if (inicio.isAfter(hasta)) {
            return new Cuenta(0, 0, List.of(), List.of());
        }
        Set<LocalDate> deClase = diasDe(m, inicio, hasta, cache);
        Map<LocalDate, AsistenciaAlumno> porDia = marcas.stream()
                .filter(a -> deClase.contains(a.getFecha()))
                .collect(Collectors.toMap(AsistenciaAlumno::getFecha, Function.identity(), (x, y) -> x));
        long esperados = 0;
        long pendientes = 0;
        List<LocalDate> faltas = new ArrayList<>();
        for (LocalDate dia : deClase.stream().sorted().toList()) {
            boolean marcado = porDia.containsKey(dia);
            if (!marcado && ESTADO_PENDIENTE.equals(estadoSinMarca(m, dia, deClase))) {
                pendientes++;
                continue;
            }
            esperados++;
            if (!marcado) {
                faltas.add(dia);
            }
        }
        return new Cuenta(esperados, pendientes, List.copyOf(porDia.values()), faltas);
    }

    /**
     * Estado de un día sin marca: Sin clases si no es día de clase; null si aún no
     * llega; Pendiente hoy hasta el límite de tardanza de su turno; si no, falta.
     */
    String estadoSinMarca(Matricula m, LocalDate dia, Set<LocalDate> diasDeClase) {
        if (!diasDeClase.contains(dia)) {
            return ESTADO_SIN_CLASES;
        }
        LocalDate hoy = LocalDate.now();
        if (dia.isAfter(hoy)) {
            return null;
        }
        if (dia.equals(hoy) && !LocalTime.now().isAfter(m.getGradoSeccion().getTurno().getHoraFaltaLimite())) {
            return ESTADO_PENDIENTE;
        }
        return ESTADO_INASISTENCIA;
    }

    static void validarVentanaIngreso(LocalTime ahora, Turno turno) {
        if (ahora.isBefore(turno.getHoraEntrada())) {
            throw new IllegalArgumentException("Registro denegado: aún no inicia la hora de entrada para el turno " + turno.getNombre());
        }
    }

    /** Puntual hasta el límite de puntualidad; tardanza hasta el de tardanza; luego justificado hasta la salida. */
    static String calcularNombreEstado(LocalTime ahora, Turno turno) {
        if (!ahora.isAfter(turno.getHoraEntradaLimite())) {
            return ESTADO_PUNTUAL;
        }
        if (!ahora.isAfter(turno.getHoraFaltaLimite())) {
            return ESTADO_TARDANZA;
        }
        if (!ahora.isAfter(turno.getHoraSalida())) {
            return ESTADO_JUSTIFICADA;
        }
        throw new IllegalArgumentException("El límite de ingreso ha expirado. El alumno queda como " + ESTADO_INASISTENCIA + ".");
    }

    private void validarDiaDeClase(Matricula matricula, LocalDate fecha) {
        GradoSeccion gs = matricula.getGradoSeccion();
        if (!calendario.esDiaDeClase(gs.getAnioEscolar(), gs.getGrado().getNivel(), fecha)) {
            throw new IllegalArgumentException("Hoy no es un día de clases (fin de semana, feriado, vacaciones o fuera del año escolar). No se registra asistencia.");
        }
    }

    /** Lo que se ve: el docente, sus secciones; el apoderado, sus hijos; el resto, todo. */
    private Alcance alcanceLectura(Integer idUsuario, List<String> roles) {
        if (accesoContextoService.esAdmin(roles)) {
            return Alcance.TODO;
        }
        if (accesoContextoService.docenteDeUsuario(idUsuario).isPresent()) {
            return Alcance.DOCENTE;
        }
        return accesoContextoService.alcance(PERMISO, idUsuario, roles);
    }

    /**
     * Registrar, justificar y anular dependen de los permisos (la ruta ya pide cada
     * acción). El apoderado nunca: la asistencia la registra el colegio.
     */
    private void validarEscritura(Integer idUsuario, List<String> roles) {
        if (alcanceLectura(idUsuario, roles) == Alcance.APODERADO) {
            throw new IllegalArgumentException("La asistencia la registra el colegio.");
        }
    }

    /** Solo sobre alumnos que el usuario ve: el docente, los de sus secciones. */
    private void validarAlcance(Matricula matricula, Integer idUsuario, List<String> roles) {
        if (!esVisible(matricula, idUsuario, roles)) {
            throw new IllegalArgumentException("El alumno no pertenece a tus secciones.");
        }
    }

    private void validarModificable(AsistenciaAlumno asistencia, Integer idUsuario, List<String> roles) {
        if (!esVisible(asistencia.getMatricula(), idUsuario, roles)) {
            throw new ResourceNotFoundException("Registro de asistencia no encontrado con id " + asistencia.getIdAsistencia());
        }
        if (asistencia.getMatricula().getGradoSeccion().getAnioEscolar().getEstado() == AnioEscolarService.ESTADO_CERRADO) {
            throw new IllegalArgumentException("El año escolar está cerrado: su asistencia ya no se modifica.");
        }
    }

    private boolean esVisible(Matricula matricula, Integer idUsuario, List<String> roles) {
        return switch (alcanceLectura(idUsuario, roles)) {
            case TODO -> true;
            case DOCENTE -> accesoContextoService.gradoSeccionIdsDeDocente(idUsuario)
                    .contains(matricula.getGradoSeccion().getIdGradoSeccion());
            case APODERADO -> accesoContextoService.apoderadoDeUsuario(idUsuario)
                    .map(a -> accesoContextoService.matriculasDeHijos(a).stream()
                            .anyMatch(m -> m.getIdMatricula().equals(matricula.getIdMatricula())))
                    .orElse(false);
        };
    }

    /** Matrículas activas del año (el vigente si no se elige) que el usuario puede ver, con los filtros. */
    private List<Matricula> matriculasVisibles(Integer idUsuario, List<String> roles, Filtro filtro) {
        Optional<AnioEscolar> anio = resolverAnio(filtro.idAnio());
        if (anio.isEmpty()) {
            return List.of();
        }
        List<Matricula> delAnio = matriculaRepository.findByGradoSeccionAnioEscolarIdAnioAndAccesoNot(anio.get().getIdAnio(), eliminado())
                .stream().filter(m -> AccesoConstants.ACTIVO.equals(m.getAcceso().getIdAcceso())).toList();
        List<Matricula> porRol = switch (alcanceLectura(idUsuario, roles)) {
            case TODO -> delAnio;
            case DOCENTE -> {
                Set<Integer> secciones = accesoContextoService.gradoSeccionIdsDeDocente(idUsuario);
                yield delAnio.stream().filter(m -> secciones.contains(m.getGradoSeccion().getIdGradoSeccion())).toList();
            }
            case APODERADO -> {
                Apoderado apoderado = accesoContextoService.apoderadoDeUsuario(idUsuario).orElse(null);
                Set<Integer> hijos = apoderado == null ? Set.of() : accesoContextoService.matriculasDeHijos(apoderado).stream()
                        .map(Matricula::getIdMatricula).collect(Collectors.toSet());
                yield delAnio.stream().filter(m -> hijos.contains(m.getIdMatricula())).toList();
            }
        };
        String buscado = normalizar(filtro.search());
        return porRol.stream().filter(m -> {
            GradoSeccion gs = m.getGradoSeccion();
            if (filtro.idGradoSeccion() != null && !filtro.idGradoSeccion().equals(gs.getIdGradoSeccion())) return false;
            if (filtro.idTurno() != null && !filtro.idTurno().equals(gs.getTurno().getIdTurno())) return false;
            if (filtro.idGrado() != null && !filtro.idGrado().equals(gs.getGrado().getIdGrado())) return false;
            if (filtro.idNivel() != null && !filtro.idNivel().equals(gs.getGrado().getNivel().getIdNivel())) return false;
            if (filtro.idSeccion() != null && (gs.getSeccion() == null || !filtro.idSeccion().equals(gs.getSeccion().getIdSeccion()))) return false;
            if (!buscado.isEmpty()) {
                Alumno a = alumnoDe(m);
                String texto = normalizar(nombreCompleto(a) + " " + a.getCodigo() + " " + Objects.toString(a.getDocumentoIdentidad(), ""));
                return Arrays.stream(buscado.split("\\s+")).allMatch(texto::contains);
            }
            return true;
        }).sorted(MatriculaService.ORDEN_REPORTE).toList();
    }

    /** El año elegido; sin elegir, el vigente. */
    private Optional<AnioEscolar> resolverAnio(Integer idAnio) {
        if (idAnio != null) {
            return anioEscolarRepository.findByIdAnioAndAccesoNot(idAnio, eliminado());
        }
        return anioEscolarRepository.findByEstadoAndAccesoNot(AnioEscolarService.ESTADO_VIGENTE, eliminado());
    }

    /** La matrícula del alumno en el año vigente, activa. */
    private Matricula matriculaDelAnioVigente(Alumno alumno) {
        Matricula m = accesoContextoService.matriculaActual(alumno.getIdAlumno())
                .orElseThrow(() -> new IllegalArgumentException("El alumno no tiene matrícula."));
        if (m.getGradoSeccion().getAnioEscolar().getEstado() != AnioEscolarService.ESTADO_VIGENTE) {
            throw new IllegalArgumentException("El alumno no tiene matrícula en el año escolar vigente.");
        }
        if (!AccesoConstants.ACTIVO.equals(m.getAcceso().getIdAcceso())) {
            throw new IllegalArgumentException("La matrícula del alumno no está activa.");
        }
        return m;
    }

    // ======================== APOYO ========================

    private Optional<AsistenciaAlumno> marcaDelDia(Matricula m, LocalDate dia) {
        return asistenciaAlumnoRepository.findByMatriculaIdMatriculaAndFechaAndAccesoNot(m.getIdMatricula(), dia, eliminado());
    }

    private Map<Integer, List<AsistenciaAlumno>> marcasPorMatricula(List<Matricula> matriculas, LocalDate inicio, LocalDate fin) {
        if (matriculas.isEmpty()) {
            return Map.of();
        }
        return asistenciaAlumnoRepository.findByMatriculaIdMatriculaInAndFechaBetweenAndAccesoNot(
                        matriculas.stream().map(Matricula::getIdMatricula).toList(), inicio, fin, eliminado()).stream()
                .collect(Collectors.groupingBy(a -> a.getMatricula().getIdMatricula()));
    }

    /** Días de clase del nivel de la matrícula, calculados una vez por nivel en cada consulta. */
    private Set<LocalDate> diasDe(Matricula m, LocalDate inicio, LocalDate fin, Map<Integer, Set<LocalDate>> cache) {
        GradoSeccion gs = m.getGradoSeccion();
        return cache.computeIfAbsent(gs.getGrado().getNivel().getIdNivel(),
                id -> calendario.diasDeClase(gs.getAnioEscolar(), gs.getGrado().getNivel(), inicio, fin));
    }

    private ResumenMensualResponse resumenDeMatricula(Matricula matricula, LocalDate inicio, LocalDate fin,
                                                      List<AsistenciaAlumno> marcas, Map<Integer, Set<LocalDate>> cache) {
        Alumno alumno = alumnoDe(matricula);
        GradoSeccion gs = matricula.getGradoSeccion();
        Cuenta c = contar(matricula, inicio, fin, marcas, cache);
        ResumenMensualResponse r = new ResumenMensualResponse();
        r.setIdAlumno(alumno.getIdAlumno());
        r.setAlumno(nombreCompleto(alumno));
        r.setGrado(gs.getGrado().getNombre());
        r.setSeccion(nombreSeccion(gs));
        r.setIdMatricula(matricula.getIdMatricula());
        r.setDiasAsistidos((long) c.registros().size());
        r.setInasistencias((long) c.faltas().size());
        r.setPorcentajeAsistencia(c.esperados() == 0 ? 0.0
                : Math.round(((double) c.registros().size() / c.esperados()) * 10000.0) / 100.0);
        completarAcademico(gs, r::setIdGradoSeccion, r::setIdTurno, r::setTurno, r::setIdAnio, r::setAnio,
                r::setIdGrado, r::setIdNivel, r::setNivel, r::setIdSeccion);
        return r;
    }

    private AsistenciaDiaResponse filaDia(Matricula m) {
        Alumno alumno = alumnoDe(m);
        GradoSeccion gs = m.getGradoSeccion();
        AsistenciaDiaResponse row = new AsistenciaDiaResponse();
        row.setIdAlumno(alumno.getIdAlumno());
        row.setAlumno(nombreCompleto(alumno));
        row.setCodigo(alumno.getCodigo());
        row.setUrlFoto(alumno.getUrlFoto());
        row.setGrado(gs.getGrado().getNombre());
        row.setSeccion(nombreSeccion(gs));
        completarAcademico(gs, row::setIdGradoSeccion, row::setIdTurno, row::setTurno, row::setIdAnio, row::setAnio,
                row::setIdGrado, row::setIdNivel, row::setNivel, row::setIdSeccion);
        return row;
    }

    private ReporteGeneralResponse filaReporte(Matricula m, LocalDate fecha, AsistenciaAlumno a) {
        Alumno alumno = alumnoDe(m);
        GradoSeccion gs = m.getGradoSeccion();
        ReporteGeneralResponse r = new ReporteGeneralResponse();
        r.setFecha(fecha);
        r.setHoraEntrada(a != null ? a.getHoraEntrada() : null);
        r.setCodigo(alumno.getCodigo());
        r.setAlumno(nombreCompleto(alumno));
        r.setGradoSeccion(gs.getGrado().getNombre() + " - " + nombreSeccion(gs));
        r.setEstado(a != null ? a.getEstado().getNombre() : ESTADO_INASISTENCIA);
        r.setJustificacion(a != null && a.getJustificacion() != null ? a.getJustificacion().getMotivo() : "-");
        r.setRegistradoPor(a != null ? marcadoPor(a) : "-");
        r.setGrado(gs.getGrado().getNombre());
        if (gs.getSeccion() != null) {
            r.setSeccion(gs.getSeccion().getNombre());
        }
        completarAcademico(gs, r::setIdGradoSeccion, r::setIdTurno, r::setTurno, r::setIdAnio, r::setAnio,
                r::setIdGrado, r::setIdNivel, r::setNivel, r::setIdSeccion);
        return r;
    }

    /** Los datos académicos que todas las filas repiten. */
    private static void completarAcademico(GradoSeccion gs,
                                           java.util.function.Consumer<Integer> idGradoSeccion,
                                           java.util.function.Consumer<Integer> idTurno, java.util.function.Consumer<String> turno,
                                           java.util.function.Consumer<Integer> idAnio, java.util.function.Consumer<String> anio,
                                           java.util.function.Consumer<Integer> idGrado,
                                           java.util.function.Consumer<Integer> idNivel, java.util.function.Consumer<String> nivel,
                                           java.util.function.Consumer<Integer> idSeccion) {
        idGradoSeccion.accept(gs.getIdGradoSeccion());
        idTurno.accept(gs.getTurno().getIdTurno());
        turno.accept(gs.getTurno().getNombre());
        idAnio.accept(gs.getAnioEscolar().getIdAnio());
        anio.accept(gs.getAnioEscolar().getAnio());
        idGrado.accept(gs.getGrado().getIdGrado());
        idNivel.accept(gs.getGrado().getNivel().getIdNivel());
        nivel.accept(gs.getGrado().getNivel().getNombre());
        idSeccion.accept(gs.getSeccion() != null ? gs.getSeccion().getIdSeccion() : null);
    }

    /** Aviso a los apoderados, recién confirmado el guardado. */
    private void avisar(NotificacionAsistenciaDto.Tipo tipo, Alumno alumno, LocalDate fecha, LocalTime hora, String estado,
                        String motivo, String acompanante) {
        List<AlumnoApoderado> vinculos = alumnoApoderadoRepository.findByAlumno(alumno);
        NotificacionAsistenciaDto evento = new NotificacionAsistenciaDto(tipo, nombreCompleto(alumno), fecha, hora, estado,
                motivo, acompanante,
                vinculos.stream().map(v -> v.getApoderado().getCelular()).filter(c -> c != null && !c.isBlank()).toList(),
                correosDe(vinculos));
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    notificacionService.notificar(evento);
                }
            });
        } else {
            notificacionService.notificar(evento);
        }
    }

    static List<String> correosDe(List<AlumnoApoderado> vinculos) {
        return vinculos.stream()
                .map(v -> v.getApoderado().getUsuario() != null ? v.getApoderado().getUsuario().getGmail() : null)
                .filter(c -> c != null && !c.isBlank())
                .distinct()
                .toList();
    }

    /** Si el guardado no llega a confirmarse, la foto subida se borra para no dejarla suelta. */
    private void borrarSiNoSeGuarda(UploadResponse subida) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED) {
                    cloudinaryService.delete(subida.getPublicId());
                }
            }
        });
    }

    private void guardarHistorial(AsistenciaAlumno asistencia, Usuario usuario, String accion,
                                  String estadoAnterior, String estadoNuevo, String detalle) {
        HistorialAsistencia historial = new HistorialAsistencia();
        historial.setAsistenciaAlumno(asistencia);
        historial.setUsuario(usuario);
        historial.setFechaHora(LocalDateTime.now());
        historial.setAccion(accion);
        historial.setEstadoAnterior(estadoAnterior);
        historial.setEstadoNuevo(estadoNuevo);
        historial.setDetalle(detalle);
        historial.setAcceso(acceso(AccesoConstants.ACTIVO));
        historialAsistenciaRepository.save(historial);
    }

    private EstadoAsistencia calcularEstado(LocalTime ahora, Turno turno) {
        return estado(calcularNombreEstado(ahora, turno));
    }

    private EstadoAsistencia estado(String nombre) {
        return estadoAsistenciaRepository.findByNombreIgnoreCaseAndAccesoNot(nombre, eliminado())
                .orElseThrow(() -> new IllegalStateException("Catálogo de estados de asistencia no configurado: " + nombre));
    }

    private Justificacion justificacion(Integer idJustificacion) {
        return justificacionRepository.findByIdJustificacionAndAccesoNot(idJustificacion, eliminado())
                .orElseThrow(() -> new ResourceNotFoundException("Motivo de justificación no encontrado con id " + idJustificacion));
    }

    private Alumno buscarAlumno(AsistenciaRequest request) {
        boolean tieneCodigo = request.getCodigo() != null && !request.getCodigo().isBlank();
        boolean tieneHash = request.getCodigoHash() != null && !request.getCodigoHash().isBlank();
        if (!tieneCodigo && !tieneHash) {
            throw new IllegalArgumentException("Debe escanear un QR o ingresar el código manual del alumno.");
        }
        Alumno alumno = tieneCodigo
                ? alumnoRepository.findByCodigoAndAccesoNot(request.getCodigo().trim(), eliminado())
                        .orElseThrow(() -> new ResourceNotFoundException("Código de alumno no válido o alumno dado de baja."))
                : alumnoRepository.findByCodigoHashAndAccesoNot(request.getCodigoHash().trim(), eliminado())
                        .orElseThrow(() -> new ResourceNotFoundException("Código QR no válido o alumno dado de baja."));
        if (alumno.getAcceso().getIdAcceso().equals(AccesoConstants.ELIMINADO)) {
            throw new IllegalArgumentException("Acceso denegado: el alumno ha sido dado de baja del sistema.");
        }
        return alumno;
    }

    private AsistenciaAlumno findAsistencia(Integer idAsistencia) {
        return asistenciaAlumnoRepository.findByIdAsistenciaAndAccesoNot(idAsistencia, eliminado())
                .orElseThrow(() -> new ResourceNotFoundException("Registro de asistencia no encontrado con id " + idAsistencia));
    }

    private Usuario findUsuario(Integer idUsuario) {
        if (idUsuario == null) {
            throw new IllegalArgumentException("No se pudo identificar al usuario que registra la asistencia");
        }
        return usuarioRepository.findByIdUsuarioAndAccesoNot(idUsuario, eliminado())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con id " + idUsuario));
    }

    private static long contarEstado(List<AsistenciaAlumno> registros, String estado) {
        return registros.stream().filter(a -> estado.equals(a.getEstado().getNombre())).count();
    }

    private static <T> Page<T> paginar(List<T> contenido, Pageable pageable) {
        if (pageable.isUnpaged()) {
            return new PageImpl<>(contenido);
        }
        int total = contenido.size();
        int inicio = Math.min(Math.toIntExact(pageable.getOffset()), total);
        int fin = Math.min(inicio + pageable.getPageSize(), total);
        return new PageImpl<>(contenido.subList(inicio, fin), pageable, total);
    }

    private static String normalizar(String texto) {
        if (texto == null) {
            return "";
        }
        return Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase().trim();
    }

    private static Alumno alumnoDe(Matricula matricula) {
        return matricula.getAlumnoApoderado().getAlumno();
    }

    private static String nombreCompleto(Alumno alumno) {
        return alumno.getNombre() + " " + alumno.getApellidoPat() + " " + alumno.getApellidoMat();
    }

    private static String nombreSeccion(GradoSeccion gs) {
        return gs.getSeccion() != null ? gs.getSeccion().getNombre() : "Única";
    }

    private static String marcadoPor(AsistenciaAlumno asistencia) {
        Usuario u = asistencia.getUsuarioRegistro();
        return u.getNombre() + " " + u.getApellidoPat();
    }

    private Acceso acceso(Long id) {
        return accesoRepository.findById(id).orElseThrow();
    }

    private Acceso eliminado() {
        return acceso(AccesoConstants.ELIMINADO);
    }

    private AsistenciaResponse toResponse(AsistenciaAlumno asistencia, Matricula matricula,
                                          EstadoAsistencia estado, Justificacion justificacion) {
        Alumno alumno = alumnoDe(matricula);
        GradoSeccion gs = matricula.getGradoSeccion();
        AsistenciaResponse response = new AsistenciaResponse();
        response.setIdAsistencia(asistencia != null ? asistencia.getIdAsistencia() : null);
        response.setIdMatricula(matricula.getIdMatricula());
        response.setIdAlumno(alumno.getIdAlumno());
        response.setCodigo(alumno.getCodigo());
        response.setAlumno(nombreCompleto(alumno));
        response.setGrado(gs.getGrado().getNombre());
        response.setSeccion(nombreSeccion(gs));
        response.setTurno(gs.getTurno().getNombre());
        response.setAnio(gs.getAnioEscolar().getAnio());
        response.setUrlFoto(alumno.getUrlFoto());
        response.setEstado(estado.getNombre());
        if (asistencia != null) {
            response.setHoraEntrada(asistencia.getHoraEntrada());
            response.setHoraSalida(asistencia.getHoraSalida());
            response.setIdJustificacion(asistencia.getJustificacion() != null ? asistencia.getJustificacion().getIdJustificacion() : null);
            response.setJustificacion(asistencia.getJustificacion() != null ? asistencia.getJustificacion().getMotivo() : null);
            response.setIdUsuarioRegistro(asistencia.getUsuarioRegistro().getIdUsuario());
            response.setUsuarioRegistro(marcadoPor(asistencia));
            response.setAcompanante(asistencia.getAcompanante());
            response.setUrlEvidencia(asistencia.getUrlEvidencia());
        } else if (justificacion != null) {
            response.setIdJustificacion(justificacion.getIdJustificacion());
            response.setJustificacion(justificacion.getMotivo());
        }
        return response;
    }
}
