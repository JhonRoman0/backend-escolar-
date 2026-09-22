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
import com.example.Escolar.Exception.ResourceNotFoundException;
import com.example.Escolar.Model.Alumno;
import com.example.Escolar.Model.AlumnoApoderado;
import com.example.Escolar.Model.Apoderado;
import com.example.Escolar.Model.AsistenciaAlumno;
import com.example.Escolar.Model.DiaFeriado;
import com.example.Escolar.Model.EstadoAsistencia;
import com.example.Escolar.Model.GradoSeccion;
import com.example.Escolar.Model.HistorialAsistencia;
import com.example.Escolar.Model.Justificacion;
import com.example.Escolar.Model.Matricula;
import com.example.Escolar.Model.Turno;
import com.example.Escolar.Model.Usuario;
import com.example.Escolar.Repository.AccesoRepository;
import com.example.Escolar.Repository.AlumnoApoderadoRepository;
import com.example.Escolar.Repository.AlumnoRepository;
import com.example.Escolar.Repository.ApoderadoRepository;
import com.example.Escolar.Repository.AsistenciaAlumnoRepository;
import com.example.Escolar.Repository.DiaFeriadoRepository;
import com.example.Escolar.Repository.EstadoAsistenciaRepository;
import com.example.Escolar.Repository.HistorialAsistenciaRepository;
import com.example.Escolar.Repository.JustificacionRepository;
import com.example.Escolar.Repository.MatriculaRepository;
import com.example.Escolar.Repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AsistenciaService {

    public static final byte PRINCIPAL = 1;

    public static final String ESTADO_PUNTUAL = "Puntual";
    public static final String ESTADO_TARDANZA = "Tardanza";
    public static final String ESTADO_INASISTENCIA = "Inasistencia";
    public static final String ESTADO_JUSTIFICADA = "Justificada";

    public static final String ACCION_REGISTRO = "REGISTRO";
    public static final String ACCION_JUSTIFICACION = "JUSTIFICACION";
    public static final String ACCION_ELIMINACION = "ELIMINACION";

    private final AsistenciaAlumnoRepository asistenciaAlumnoRepository;
    private final EstadoAsistenciaRepository estadoAsistenciaRepository;
    private final JustificacionRepository justificacionRepository;
    private final HistorialAsistenciaRepository historialAsistenciaRepository;
    private final DiaFeriadoRepository diaFeriadoRepository;
    private final AlumnoRepository alumnoRepository;
    private final AlumnoApoderadoRepository alumnoApoderadoRepository;
    private final MatriculaRepository matriculaRepository;
    private final ApoderadoRepository apoderadoRepository;
    private final UsuarioRepository usuarioRepository;
    private final NotificacionService notificacionService;
    private final AccesoRepository accesoRepository;
    private final AccesoContextoService accesoContextoService;

    @Transactional(readOnly = true)
    public AsistenciaResponse previsualizar(AsistenciaRequest request) {
        Alumno alumno = buscarAlumno(request);
        Matricula matricula = resolverMatriculaVigente(alumno);
        validarDiaHabil(LocalDate.now(), matricula);

        Turno turno = matricula.getGradoSeccion().getTurno();
        LocalTime ahora = LocalTime.now();

        Optional<AsistenciaAlumno> asistenciaHoy = asistenciaAlumnoRepository
                .findByMatriculaIdMatriculaAndFechaAndAccesoNot(matricula.getIdMatricula(), LocalDate.now(), accesoRepository.findById(AccesoConstants.ACTIVO).orElseThrow());
        if (asistenciaHoy.isPresent()) {
            throw new IllegalArgumentException("El alumno ya registró su asistencia el día de hoy.");
        }

        validarVentanaIngreso(ahora, turno);
        EstadoAsistencia estado = calcularEstado(ahora, turno);

        return toResponse(null, matricula, estado,
                request.getIdJustificacion() != null ? justificacion(request.getIdJustificacion()) : null);
    }

    @Transactional
    public AsistenciaResponse confirmar(AsistenciaRequest request, Integer idUsuarioRegistro) {
        Alumno alumno = buscarAlumno(request);
        Matricula matricula = resolverMatriculaVigente(alumno);
        validarDiaHabil(LocalDate.now(), matricula);

        Turno turno = matricula.getGradoSeccion().getTurno();
        LocalTime ahora = LocalTime.now();
        validarVentanaIngreso(ahora, turno);

        LocalDate hoy = LocalDate.now();

        Optional<AsistenciaAlumno> asistenciaHoy = asistenciaAlumnoRepository
                .findByMatriculaIdMatriculaAndFechaAndAccesoNot(matricula.getIdMatricula(), hoy, accesoRepository.findById(AccesoConstants.ACTIVO).orElseThrow());
        if (asistenciaHoy.isPresent()) {
            throw new IllegalArgumentException("El alumno ya registró su asistencia el día de hoy.");
        }

        EstadoAsistencia estado = calcularEstado(ahora, turno);
        Justificacion justificacion = null;
        if (ESTADO_JUSTIFICADA.equals(estado.getNombre())) {
            if (request.getIdJustificacion() == null) {
                throw new IllegalArgumentException("Debe seleccionar un motivo de justificación para registrar el ingreso.");
            }
            justificacion = justificacion(request.getIdJustificacion());
        }

        Usuario usuario = findUsuario(idUsuarioRegistro);

        AsistenciaAlumno asistencia = new AsistenciaAlumno();
        asistencia.setMatricula(matricula);
        asistencia.setEstado(estado);
        asistencia.setJustificacion(justificacion);
        asistencia.setUsuarioRegistro(usuario);
        asistencia.setFecha(hoy);
        asistencia.setHoraEntrada(ahora);
        asistencia.setAcceso(accesoRepository.findById(AccesoConstants.ACTIVO).orElseThrow());

        try {
            asistencia = asistenciaAlumnoRepository.save(asistencia);
        } catch (DataIntegrityViolationException e) {
            throw new IllegalArgumentException("El alumno ya registró su asistencia el día de hoy.");
        }

        guardarHistorial(asistencia, usuario, ACCION_REGISTRO, null, estado.getNombre(), "Marcado de asistencia");

        notificacionService.notificarMarcado(new NotificacionAsistenciaDto(
                alumno.getNombre() + " " + alumno.getApellidoPat() + " " + alumno.getApellidoMat(),
                hoy, ahora, estado.getNombre(), celularesDe(alumno)));

        return toResponse(asistencia, matricula, estado, justificacion);
    }

    @Transactional
    public AsistenciaResponse justificar(Integer idAsistencia, JustificarRequest request, Integer idUsuarioRegistro) {
        AsistenciaAlumno asistencia = findAsistencia(idAsistencia);
        Usuario usuario = findUsuario(idUsuarioRegistro);
        Justificacion justificacion = justificacion(request.getIdJustificacion());

        String estadoAnterior = asistencia.getEstado().getNombre();
        EstadoAsistencia estadoJustificado = estado(ESTADO_JUSTIFICADA);
        asistencia.setEstado(estadoJustificado);
        asistencia.setJustificacion(justificacion);
        asistencia = asistenciaAlumnoRepository.save(asistencia);

        guardarHistorial(asistencia, usuario, ACCION_JUSTIFICACION, estadoAnterior, estadoJustificado.getNombre(),
                "Justificación retrospectiva: " + justificacion.getMotivo());

        return toResponse(asistencia, asistencia.getMatricula(), estadoJustificado, justificacion);
    }

    @Transactional
    public void eliminar(Integer idAsistencia, Integer idUsuarioRegistro) {
        AsistenciaAlumno asistencia = findAsistencia(idAsistencia);
        Usuario usuario = findUsuario(idUsuarioRegistro);

        String estadoAnterior = asistencia.getEstado().getNombre();
        asistencia.setAcceso(accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow());
        asistenciaAlumnoRepository.save(asistencia);

        guardarHistorial(asistencia, usuario, ACCION_ELIMINACION, estadoAnterior, null, "Eliminación lógica de la marca");
    }

    @Transactional(readOnly = true)
    public List<EstadoAsistenciaResponse> listarEstados() {
        return estadoAsistenciaRepository.findByAccesoNot(accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow()).stream()
                .map(e -> {
                    EstadoAsistenciaResponse r = new EstadoAsistenciaResponse();
                    r.setIdEstado(e.getIdEstado());
                    r.setNombre(e.getNombre());
                    return r;
                })
                .toList();
    }

    private List<Matricula> matriculasVisibles(Integer idUsuario, List<String> roles) {
        List<Matricula> todas = matriculaRepository.findByAccesoNot(accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow());
        if (accesoContextoService.esGestion(roles)) {
            return todas;
        }
        if (accesoContextoService.esPersonalDocente(roles)) {
            Set<Integer> gradoSeccionIds = accesoContextoService.gradoSeccionIdsDeDocente(idUsuario);
            return todas.stream()
                    .filter(m -> gradoSeccionIds.contains(m.getGradoSeccion().getIdGradoSeccion()))
                    .toList();
        }
        if (accesoContextoService.esApoderado(roles)) {
            Apoderado apoderado = accesoContextoService.apoderadoDeUsuario(idUsuario).orElse(null);
            if (apoderado == null) {
                return List.of();
            }
            List<Matricula> visibles = new ArrayList<>();
            for (AlumnoApoderado vinculo : alumnoApoderadoRepository.findByApoderado(apoderado)) {
                matriculaRepository.findByAlumnoApoderadoAndAccesoNot(vinculo, accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow())
                        .ifPresent(visibles::add);
            }
            return visibles;
        }
        return List.of();
    }

    private boolean esVisibleAlumno(Integer idAlumno, Integer idUsuario, List<String> roles) {
        return matriculasVisibles(idUsuario, roles).stream()
                .anyMatch(m -> alumnoDe(m).getIdAlumno().equals(idAlumno));
    }

    private boolean esVisibleUsuarioRegistrador(Integer idUsuarioConsulta, Integer idUsuario, List<String> roles) {
        if (accesoContextoService.esGestion(roles)) {
            return true;
        }
        return idUsuarioConsulta != null && idUsuarioConsulta.equals(idUsuario);
    }

    @Transactional(readOnly = true)
    public List<AsistenciaDiaResponse> asistenciasHoy(Integer idUsuario, List<String> roles) {
        LocalDate hoy = LocalDate.now();
        return matriculasVisibles(idUsuario, roles).stream().map(matricula -> {
            Alumno alumno = alumnoDe(matricula);
            GradoSeccion gs = matricula.getGradoSeccion();

            AsistenciaDiaResponse row = new AsistenciaDiaResponse();
            row.setIdAlumno(alumno.getIdAlumno());
            row.setAlumno(nombreCompleto(alumno));
            row.setGrado(gs.getGrado().getNombre());
            row.setSeccion(gs.getSeccion() != null ? gs.getSeccion().getNombre() : "Única");

            asistenciaAlumnoRepository
                    .findByMatriculaIdMatriculaAndFechaAndAccesoNot(matricula.getIdMatricula(), hoy, accesoRepository.findById(AccesoConstants.ACTIVO).orElseThrow())
                    .ifPresent(a -> {
                        row.setIdAsistencia(a.getIdAsistencia());
                        row.setEstado(a.getEstado().getNombre());
                        row.setHoraEntrada(a.getHoraEntrada());
                        row.setMarcadoPor(a.getUsuarioRegistro().getNombre() + " " + a.getUsuarioRegistro().getApellidoPat());
                    });
            if (row.getEstado() == null) {
                row.setEstado(ESTADO_INASISTENCIA);
            }
            return row;
        }).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<MatrizSemanalResponse> matrizSemanal(LocalDate fecha, Pageable pageable, Integer idUsuario, List<String> roles) {
        LocalDate lunes = fecha.with(DayOfWeek.MONDAY);
        List<MatrizSemanalResponse> contenido = matriculasVisibles(idUsuario, roles).stream().map(matricula -> {
            Alumno alumno = alumnoDe(matricula);
            GradoSeccion gs = matricula.getGradoSeccion();

            MatrizSemanalResponse row = new MatrizSemanalResponse();
            row.setIdAlumno(alumno.getIdAlumno());
            row.setAlumno(nombreCompleto(alumno));
            row.setGrado(gs.getGrado().getNombre());
            row.setSeccion(gs.getSeccion() != null ? gs.getSeccion().getNombre() : "Única");

            List<String> estados = new ArrayList<>();
            for (int i = 0; i < 5; i++) {
                LocalDate dia = lunes.plusDays(i);
                estados.add(asistenciaAlumnoRepository
                        .findByMatriculaIdMatriculaAndFechaAndAccesoNot(matricula.getIdMatricula(), dia, accesoRepository.findById(AccesoConstants.ACTIVO).orElseThrow())
                        .map(a -> a.getEstado().getNombre())
                        .orElse(ESTADO_INASISTENCIA));
            }
            row.setEstados(estados);
            return row;
        }).collect(Collectors.toList());
        return paginarEnMemoria(contenido, pageable);
    }

    @Transactional(readOnly = true)
    public Page<ResumenMensualResponse> resumenMensual(LocalDate fecha, Pageable pageable, Integer idUsuario, List<String> roles) {
        LocalDate inicio = fecha.withDayOfMonth(1);
        LocalDate fin = fecha.withDayOfMonth(fecha.lengthOfMonth());

        List<Matricula> matriculas = matriculasVisibles(idUsuario, roles);
        List<AsistenciaAlumno> asistenciasMes = asistenciaAlumnoRepository.findByFechaBetweenAndAccesoNot(inicio, fin, accesoRepository.findById(AccesoConstants.ACTIVO).orElseThrow());
        Map<Integer, List<AsistenciaAlumno>> porMatricula = asistenciasMes.stream()
                .collect(Collectors.groupingBy(a -> a.getMatricula().getIdMatricula()));

        List<ResumenMensualResponse> contenido = matriculas.stream().map(matricula ->
                resumenDeMatricula(matricula, inicio, fin, porMatricula.getOrDefault(matricula.getIdMatricula(), List.of())))
                .collect(Collectors.toList());
        return paginarEnMemoria(contenido, pageable);
    }

    @Transactional(readOnly = true)
    public List<ResumenMensualResponse> asistenciasDeHijos(Integer idUsuarioAutenticado, List<String> roles) {
        if (!accesoContextoService.esApoderado(roles) && !accesoContextoService.esGestion(roles)) {
            return List.of();
        }
        Apoderado apoderado = apoderadoRepository
                .findByUsuarioIdUsuarioAndAccesoNot(idUsuarioAutenticado, accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Apoderado no encontrado para el usuario " + idUsuarioAutenticado));

        LocalDate hoy = LocalDate.now();
        LocalDate inicio = hoy.withDayOfMonth(1);
        LocalDate fin = hoy.withDayOfMonth(hoy.lengthOfMonth());

        List<ResumenMensualResponse> resultado = new ArrayList<>();
        for (AlumnoApoderado vinculo : alumnoApoderadoRepository.findByApoderado(apoderado)) {
            Alumno alumno = vinculo.getAlumno();
            Optional<Matricula> matriculaOpt = matriculaRepository
                    .findByAlumnoApoderadoAndAccesoNot(vinculo, accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow());
            if (matriculaOpt.isEmpty()) {
                continue;
            }
            Matricula matricula = matriculaOpt.get();
            List<AsistenciaAlumno> asistencias = asistenciaAlumnoRepository
                    .findByMatriculaIdMatriculaAndFechaBetweenAndAccesoNot(matricula.getIdMatricula(), inicio, fin, accesoRepository.findById(AccesoConstants.ACTIVO).orElseThrow());
            resultado.add(resumenDeMatricula(matricula, inicio, fin, asistencias));
        }
        return resultado;
    }

    @Transactional(readOnly = true)
    public EstadisticasResponse estadisticas(String rango, LocalDate fecha, Integer idUsuario, List<String> roles) {
        LocalDate inicio = fecha;
        LocalDate fin = fecha;
        if ("semana".equalsIgnoreCase(rango)) {
            inicio = fecha.with(DayOfWeek.MONDAY);
            fin = fecha.with(DayOfWeek.FRIDAY);
        } else if ("mes".equalsIgnoreCase(rango)) {
            inicio = fecha.withDayOfMonth(1);
            fin = fecha.withDayOfMonth(fecha.lengthOfMonth());
        }

        List<Matricula> matriculas = matriculasVisibles(idUsuario, roles);
        Set<Integer> matriculaIdsVisibles = matriculas.stream()
                .map(Matricula::getIdMatricula)
                .collect(Collectors.toSet());
        long universoEsperado = 0;
        for (Matricula matricula : matriculas) {
            universoEsperado += contarDiasHabiles(inicio, fin, matricula);
        }

        List<AsistenciaAlumno> registros = asistenciaAlumnoRepository
                .findByFechaBetweenAndAccesoNot(inicio, fin, accesoRepository.findById(AccesoConstants.ACTIVO).orElseThrow()).stream()
                .filter(a -> matriculaIdsVisibles.contains(a.getMatricula().getIdMatricula()))
                .collect(Collectors.toList());
        long presentes = registros.stream().filter(a -> ESTADO_PUNTUAL.equals(a.getEstado().getNombre())).count();
        long tardanzas = registros.stream().filter(a -> ESTADO_TARDANZA.equals(a.getEstado().getNombre())).count();
        long justificados = registros.stream().filter(a -> ESTADO_JUSTIFICADA.equals(a.getEstado().getNombre())).count();
        long totalRegistrados = registros.size();
        long inasistencias = Math.max(0, universoEsperado - totalRegistrados);

        EstadisticasResponse response = new EstadisticasResponse();
        response.setPresentes(presentes);
        response.setTardanzas(tardanzas);
        response.setJustificados(justificados);
        response.setInasistencias(inasistencias);
        response.setTotalEsperado(universoEsperado);
        response.setPorcentajeAsistencia(universoEsperado == 0 ? 0.0
                : Math.round(((double) totalRegistrados / universoEsperado) * 10000.0) / 100.0);
        return response;
    }

    // ============================= REPORTES =============================

    @Transactional(readOnly = true)
    public Page<ReporteGeneralResponse> reporteGeneral(LocalDate inicio, LocalDate fin, Pageable pageable,
                                                       Integer idUsuario, List<String> roles) {
        if (inicio == null) inicio = LocalDate.now();
        if (fin == null) fin = LocalDate.now();
        List<Matricula> matriculas = matriculasVisibles(idUsuario, roles);
        Set<Integer> matriculaIdsVisibles = matriculas.stream()
                .map(Matricula::getIdMatricula)
                .collect(Collectors.toSet());

        List<AsistenciaAlumno> asistenciasReales = asistenciaAlumnoRepository
                .findByFechaBetweenAndAccesoNotOrderByFechaAscHoraEntradaAsc(inicio, fin, accesoRepository.findById(AccesoConstants.ACTIVO).orElseThrow())
                .stream().filter(a -> matriculaIdsVisibles.contains(a.getMatricula().getIdMatricula()))
                .toList();

        List<ReporteGeneralResponse> contenido = asistenciasReales.stream()
                .map(a -> {
                    Matricula matricula = a.getMatricula();
                    Alumno alumno = alumnoDe(matricula);
                    GradoSeccion gs = matricula.getGradoSeccion();

                    ReporteGeneralResponse r = new ReporteGeneralResponse();
                    r.setFecha(a.getFecha());
                    r.setHoraEntrada(a.getHoraEntrada());
                    r.setCodigo(alumno.getCodigo());
                    r.setAlumno(nombreCompleto(alumno));
                    r.setGradoSeccion(gs.getGrado().getNombre() + " - " + (gs.getSeccion() != null ? gs.getSeccion().getNombre() : "Única"));
                    r.setEstado(a.getEstado().getNombre());
                    r.setJustificacion(a.getJustificacion() != null ? a.getJustificacion().getMotivo() : "-");
                    r.setRegistradoPor(marcadoPor(a));
                    return r;
                }).collect(Collectors.toList());

        if (!matriculas.isEmpty()) {
            Integer idAnio = matriculas.get(0).getGradoSeccion().getAnioEscolar().getIdAnio();

            Set<LocalDate> fechasFeriado = diaFeriadoRepository
                    .findByFechaBetweenAndAccesoNot(inicio, fin, accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow()).stream()
                    .filter(f -> f.getAnioEscolar() == null || f.getAnioEscolar().getIdAnio().equals(idAnio))
                    .map(DiaFeriado::getFecha)
                    .collect(Collectors.toSet());

            Set<String> clavesReales = new HashSet<>();
            for (AsistenciaAlumno a : asistenciasReales) {
                clavesReales.add(a.getMatricula().getIdMatricula() + ":" + a.getFecha());
            }

            for (Matricula matricula : matriculas) {
                Alumno alumno = alumnoDe(matricula);
                GradoSeccion gs = matricula.getGradoSeccion();
                Integer idMat = matricula.getIdMatricula();
                String codigo = alumno.getCodigo();
                String nombreAlumno = nombreCompleto(alumno);
                String gsTexto = gs.getGrado().getNombre() + " - " + (gs.getSeccion() != null ? gs.getSeccion().getNombre() : "Única");

                for (LocalDate fecha : inicio.datesUntil(fin.plusDays(1)).toList()) {
                    if (fecha.getDayOfWeek() == DayOfWeek.SATURDAY || fecha.getDayOfWeek() == DayOfWeek.SUNDAY) {
                        continue;
                    }
                    if (fechasFeriado.contains(fecha)) {
                        continue;
                    }
                    String clave = idMat + ":" + fecha;
                    if (clavesReales.contains(clave)) {
                        continue;
                    }

                    ReporteGeneralResponse r = new ReporteGeneralResponse();
                    r.setFecha(fecha);
                    r.setHoraEntrada(null);
                    r.setCodigo(codigo);
                    r.setAlumno(nombreAlumno);
                    r.setGradoSeccion(gsTexto);
                    r.setEstado(ESTADO_INASISTENCIA);
                    r.setJustificacion("-");
                    r.setRegistradoPor("-");
                    contenido.add(r);
                }
            }
        }

        contenido.sort((a, b) -> {
            int cmp = a.getFecha().compareTo(b.getFecha());
            if (cmp != 0) return cmp;
            if (a.getHoraEntrada() == null && b.getHoraEntrada() == null) return 0;
            if (a.getHoraEntrada() == null) return -1;
            if (b.getHoraEntrada() == null) return 1;
            return a.getHoraEntrada().compareTo(b.getHoraEntrada());
        });

        return paginarEnMemoria(contenido, pageable);
    }

    @Transactional(readOnly = true)
    public List<ReporteAlumnoResponse> reportePorAlumno(Integer idAlumno, LocalDate inicio, LocalDate fin,
                                                        Integer idUsuario, List<String> roles) {
        if (inicio == null) inicio = LocalDate.of(2000, 1, 1);
        if (fin == null) fin = LocalDate.now();
        Alumno alumno = alumnoRepository.findByIdAlumnoAndAccesoNot(idAlumno, accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow())
                .orElseThrow(() -> new ResourceNotFoundException("Alumno no encontrado con id " + idAlumno));
        if (!esVisibleAlumno(idAlumno, idUsuario, roles)) {
            throw new ResourceNotFoundException("Alumno no encontrado con id " + idAlumno);
        }
        Matricula matricula = resolverMatriculaVigente(alumno);

        return asistenciaAlumnoRepository
                .findByMatriculaIdMatriculaAndFechaBetweenAndAccesoNot(matricula.getIdMatricula(), inicio, fin, accesoRepository.findById(AccesoConstants.ACTIVO).orElseThrow())
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
        if (inicio == null) inicio = LocalDate.of(2000, 1, 1);
        if (fin == null) fin = LocalDate.now();
        if (!esVisibleUsuarioRegistrador(idUsuarioConsulta, idUsuario, roles)) {
            throw new ResourceNotFoundException("Usuario registrador no encontrado con id " + idUsuarioConsulta);
        }
        usuarioRepository.findByIdUsuarioAndAccesoNot(idUsuarioConsulta, accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario registrador no encontrado con id " + idUsuarioConsulta));

        return asistenciaAlumnoRepository
                .findByUsuarioRegistroIdUsuarioAndFechaBetweenAndAccesoNot(idUsuarioConsulta, inicio, fin, accesoRepository.findById(AccesoConstants.ACTIVO).orElseThrow())
                .stream().map(a -> {
                    Matricula matricula = a.getMatricula();
                    GradoSeccion gs = matricula.getGradoSeccion();

                    ReporteUsuarioResponse r = new ReporteUsuarioResponse();
                    r.setFecha(a.getFecha());
                    r.setHoraEntrada(a.getHoraEntrada());
                    r.setAlumno(nombreCompleto(alumnoDe(matricula)));
                    r.setGradoSeccion(gs.getGrado().getNombre() + " - " + (gs.getSeccion() != null ? gs.getSeccion().getNombre() : "Única"));
                    r.setEstado(a.getEstado().getNombre());
                    r.setJustificacion(a.getJustificacion() != null ? a.getJustificacion().getMotivo() : "-");
                    return r;
                }).collect(Collectors.toList());
    }

    // ======================== MÉTODOS DE APOYO ========================

    private <T> Page<T> paginarEnMemoria(List<T> contenido, Pageable pageable) {
        int total = contenido.size();
        int inicio = Math.min(Math.toIntExact(pageable.getOffset()), total);
        int fin = Math.min(inicio + pageable.getPageSize(), total);
        return new PageImpl<>(contenido.subList(inicio, fin), pageable, total);
    }

    private ResumenMensualResponse resumenDeMatricula(Matricula matricula, LocalDate inicio, LocalDate fin,
                                                      List<AsistenciaAlumno> asistencias) {
        Alumno alumno = alumnoDe(matricula);
        GradoSeccion gs = matricula.getGradoSeccion();
        long diasHabiles = contarDiasHabiles(inicio, fin, matricula);
        long asistenciasRegistradas = asistencias.size();
        long inasistencias = Math.max(0, diasHabiles - asistenciasRegistradas);
        double porcentaje = diasHabiles == 0 ? 0.0
                : Math.round(((double) asistenciasRegistradas / diasHabiles) * 10000.0) / 100.0;

        ResumenMensualResponse r = new ResumenMensualResponse();
        r.setIdAlumno(alumno.getIdAlumno());
        r.setAlumno(nombreCompleto(alumno));
        r.setGrado(gs.getGrado().getNombre());
        r.setSeccion(gs.getSeccion() != null ? gs.getSeccion().getNombre() : "Única");
        r.setIdMatricula(matricula.getIdMatricula());
        r.setDiasAsistidos(asistenciasRegistradas);
        r.setInasistencias(inasistencias);
        r.setPorcentajeAsistencia(porcentaje);
        return r;
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
        historial.setAcceso(accesoRepository.findById(AccesoConstants.ACTIVO).orElseThrow());
        historialAsistenciaRepository.save(historial);
    }

    private boolean esDiaHabil(LocalDate fecha, Matricula matricula) {
        DayOfWeek dia = fecha.getDayOfWeek();
        if (dia == DayOfWeek.SATURDAY || dia == DayOfWeek.SUNDAY) {
            return false;
        }
        Integer idAnio = matricula.getGradoSeccion().getAnioEscolar().getIdAnio();
        return diaFeriadoRepository.findByFechaBetweenAndAccesoNot(fecha, fecha, accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow()).stream()
                .noneMatch(f -> f.getAnioEscolar() == null
                        || f.getAnioEscolar().getIdAnio().equals(idAnio));
    }

    private long contarDiasHabiles(LocalDate inicio, LocalDate fin, Matricula matricula) {
        if (inicio.isAfter(fin)) {
            return 0;
        }
        return inicio.datesUntil(fin.plusDays(1))
                .filter(d -> esDiaHabil(d, matricula))
                .count();
    }

    private void validarDiaHabil(LocalDate fecha, Matricula matricula) {
        if (!esDiaHabil(fecha, matricula)) {
            throw new IllegalArgumentException("Hoy no es un día de clases (fin de semana o feriado). No se registra asistencia.");
        }
    }

    static void validarVentanaIngreso(LocalTime ahora, Turno turno) {
        if (ahora.isBefore(turno.getHoraEntrada())) {
            throw new IllegalArgumentException("Registro denegado: aún no inicia la hora de entrada para el turno " + turno.getNombre());
        }
    }

    private EstadoAsistencia calcularEstado(LocalTime ahora, Turno turno) {
        return estado(calcularNombreEstado(ahora, turno));
    }

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

    private EstadoAsistencia estado(String nombre) {
        return estadoAsistenciaRepository.findByNombreIgnoreCaseAndAccesoNot(nombre, accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow())
                .orElseThrow(() -> new IllegalStateException("Catálogo de estados de asistencia no configurado: " + nombre));
    }

    private Justificacion justificacion(Integer idJustificacion) {
        return justificacionRepository.findByIdJustificacionAndAccesoNot(idJustificacion, accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow())
                .orElseThrow(() -> new ResourceNotFoundException("Motivo de justificación no encontrado con id " + idJustificacion));
    }

    private Alumno buscarAlumno(AsistenciaRequest request) {
        boolean tieneCodigo = request.getCodigo() != null && !request.getCodigo().isBlank();
        boolean tieneHash = request.getCodigoHash() != null && !request.getCodigoHash().isBlank();
        if (!tieneCodigo && !tieneHash) {
            throw new IllegalArgumentException("Debe escanear un QR o ingresar el código manual del alumno.");
        }
        Alumno alumno;
        if (tieneCodigo) {
            alumno = alumnoRepository.findByCodigoAndAccesoNot(request.getCodigo().trim(), accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow())
                    .orElseThrow(() -> new ResourceNotFoundException("Código de alumno no válido o alumno dado de baja."));
        } else {
            alumno = alumnoRepository.findByCodigoHashAndAccesoNot(request.getCodigoHash().trim(), accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow())
                    .orElseThrow(() -> new ResourceNotFoundException("Código QR no válido o alumno dado de baja."));
        }
        if (alumno.getAcceso().getIdAcceso().equals(AccesoConstants.ELIMINADO)) {
            throw new IllegalArgumentException("Acceso denegado: el alumno ha sido dado de baja del sistema.");
        }
        return alumno;
    }

    private Matricula resolverMatriculaVigente(Alumno alumno) {
        AlumnoApoderado principal = alumnoApoderadoRepository.findByAlumno(alumno).stream()
                .filter(v -> v.getApoPrincipal() == PRINCIPAL)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("El alumno no tiene un apoderado principal registrado"));
        return matriculaRepository.findByAlumnoApoderadoAndAccesoNot(principal, accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow())
                .orElseThrow(() -> new IllegalArgumentException("El alumno no tiene una matrícula vigente"));
    }

    private AsistenciaAlumno findAsistencia(Integer idAsistencia) {
        return asistenciaAlumnoRepository.findByIdAsistenciaAndAccesoNot(idAsistencia, accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow())
                .orElseThrow(() -> new ResourceNotFoundException("Registro de asistencia no encontrado con id " + idAsistencia));
    }

    private Usuario findUsuario(Integer idUsuario) {
        if (idUsuario == null) {
            throw new IllegalArgumentException("No se pudo identificar al usuario que registra la asistencia");
        }
        return usuarioRepository.findByIdUsuarioAndAccesoNot(idUsuario, accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con id " + idUsuario));
    }

    private List<String> celularesDe(Alumno alumno) {
        return alumnoApoderadoRepository.findByAlumno(alumno).stream()
                .map(v -> v.getApoderado().getCelular())
                .filter(celular -> celular != null && !celular.isBlank())
                .collect(Collectors.toList());
    }

    private Alumno alumnoDe(Matricula matricula) {
        return matricula.getAlumnoApoderado().getAlumno();
    }

    private String nombreCompleto(Alumno alumno) {
        return alumno.getNombre() + " " + alumno.getApellidoPat() + " " + alumno.getApellidoMat();
    }

    private String marcadoPor(AsistenciaAlumno asistencia) {
        Usuario u = asistencia.getUsuarioRegistro();
        return u.getNombre() + " " + u.getApellidoPat();
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
        response.setSeccion(gs.getSeccion() != null ? gs.getSeccion().getNombre() : "Única");
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
        } else if (justificacion != null) {
            response.setIdJustificacion(justificacion.getIdJustificacion());
            response.setJustificacion(justificacion.getMotivo());
        }
        return response;
    }
}
