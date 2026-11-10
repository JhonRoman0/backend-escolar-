package com.example.Escolar.Service;

import com.example.Escolar.Dto.FichaMatriculaResponse;
import com.example.Escolar.Dto.HistorialResponse;
import com.example.Escolar.Dto.MatriculaReporteResponse;
import com.example.Escolar.Dto.PagoMatriculaResponse;
import com.example.Escolar.Dto.MatriculaResponse;
import com.example.Escolar.Exception.ResourceNotFoundException;
import com.example.Escolar.Model.Acceso;
import com.example.Escolar.Model.Alumno;
import com.example.Escolar.Model.AlumnoApoderado;
import com.example.Escolar.Model.Apoderado;
import com.example.Escolar.Model.GradoSeccion;
import com.example.Escolar.Model.HistorialMatricula;
import com.example.Escolar.Model.Matricula;
import com.example.Escolar.Model.PagoMatricula;
import com.example.Escolar.Model.TipoPago;
import com.example.Escolar.Repository.AccesoRepository;
import com.example.Escolar.Repository.AlumnoApoderadoRepository;
import com.example.Escolar.Repository.GradoSeccionRepository;
import com.example.Escolar.Repository.HistorialMatriculaRepository;
import com.example.Escolar.Repository.MatriculaRepository;
import com.example.Escolar.Repository.PagoMatriculaRepository;
import com.example.Escolar.Service.AccesoContextoService.Alcance;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MatriculaService {

    private static final String PERMISO = "MATRICULAS";

    private static final Comparator<Integer> ORDEN_NUMERO = Comparator.nullsLast(Comparator.naturalOrder());

    /** Orden de los reportes: año, nivel, grado, sección y apellidos. */
    public static final Comparator<Matricula> ORDEN_REPORTE = Comparator
            .comparing((Matricula m) -> m.getGradoSeccion().getAnioEscolar().getAnio())
            .thenComparing(m -> m.getGradoSeccion().getGrado().getNivel().getOrden(), ORDEN_NUMERO)
            .thenComparing(m -> m.getGradoSeccion().getGrado().getOrden(), ORDEN_NUMERO)
            .thenComparing(m -> m.getGradoSeccion().getSeccion() != null
                    ? m.getGradoSeccion().getSeccion().getNombre() : "")
            .thenComparing(m -> {
                Alumno a = m.getAlumnoApoderado().getAlumno();
                return a.getApellidoPat() + " " + a.getApellidoMat() + " " + a.getNombre();
            }, String.CASE_INSENSITIVE_ORDER);

    private final MatriculaRepository matriculaRepository;
    private final HistorialMatriculaRepository historialMatriculaRepository;
    private final AlumnoApoderadoRepository alumnoApoderadoRepository;
    private final GradoSeccionRepository gradoSeccionRepository;
    private final PagoMatriculaRepository pagoMatriculaRepository;
    private final AccesoRepository accesoRepository;
    private final AccesoContextoService accesoContextoService;
    private final BloqueoCalificaciones bloqueo;

    public Page<MatriculaResponse> getAll(Pageable pageable, Integer idUsuario, List<String> roles,
                                         Integer idNivel, Integer idGrado, Integer idSeccion,
                                         Integer idTurno, Integer idGradoSeccion, Integer idAnio,
                                         Long idAcceso) {
        if (idAcceso != null && !AccesoConstants.ACTIVO.equals(idAcceso)
                && !AccesoConstants.INACTIVO.equals(idAcceso)) {
            throw new IllegalArgumentException("El estado debe ser activo o inactivo");
        }
        boolean filtrando = idNivel != null || idGrado != null || idSeccion != null || idTurno != null || idGradoSeccion != null || idAnio != null;
        Set<Integer> visibles = idsMatriculasVisibles(idUsuario, roles);
        boolean esGestion = visibles == null;

        if (filtrando) {
            if (esGestion) {
                Page<Matricula> page = matriculaRepository.findFiltradas(
                        AccesoConstants.ELIMINADO, idNivel, idGrado, idSeccion, idTurno, idGradoSeccion, idAnio, idAcceso, pageable);
                return new PageImpl<>(toResponses(page.getContent()), pageable, page.getTotalElements());
            } else {
                List<Matricula> filtradas = matriculaRepository.findFiltradasList(
                        AccesoConstants.ELIMINADO, idNivel, idGrado, idSeccion, idTurno, idGradoSeccion, idAnio, idAcceso);
                List<Matricula> visiblesList = filtradas.stream()
                        .filter(m -> visibles.contains(m.getIdMatricula()))
                        .toList();
                return paginarMatriculasEnMemoria(visiblesList, pageable);
            }
        }

        if (esGestion) {
            Page<Matricula> page = idAcceso == null
                    ? matriculaRepository.findByAccesoNot(eliminado(), pageable)
                    : matriculaRepository.findByAcceso(accesoRepository.findById(idAcceso).orElseThrow(), pageable);
            return new PageImpl<>(toResponses(page.getContent()), pageable, page.getTotalElements());
        } else {
            List<Matricula> todas = idAcceso == null
                    ? matriculaRepository.findByAccesoNot(eliminado())
                    : matriculaRepository.findByAcceso(accesoRepository.findById(idAcceso).orElseThrow());
            List<Matricula> visiblesList = todas.stream()
                    .filter(m -> visibles.contains(m.getIdMatricula()))
                    .toList();
            return paginarMatriculasEnMemoria(visiblesList, pageable);
        }
    }

    public Page<MatriculaResponse> getAll(Pageable pageable, Integer idUsuario, List<String> roles,
                                         Integer idNivel, Integer idGrado, Integer idSeccion,
                                         Integer idTurno, Integer idGradoSeccion, Integer idAnio) {
        return getAll(pageable, idUsuario, roles, idNivel, idGrado, idSeccion,
                idTurno, idGradoSeccion, idAnio, null);
    }

    public Page<MatriculaResponse> getAll(Pageable pageable, Integer idUsuario, List<String> roles) {
        return getAll(pageable, idUsuario, roles, null, null, null, null, null, null);
    }

    private Page<MatriculaResponse> paginarMatriculasEnMemoria(List<Matricula> lista, Pageable pageable) {
        int total = lista.size();
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), total);
        List<Matricula> slice = start >= total ? List.of() : lista.subList(start, end);
        List<MatriculaResponse> respuestas = toResponses(slice);
        return new PageImpl<>(respuestas, pageable, total);
    }

    public MatriculaResponse getById(Integer id, Integer idUsuario, List<String> roles) {
        Matricula matricula = findMatricula(id);
        if (!esVisible(matricula.getIdMatricula(), idUsuario, roles)) {
            throw new ResourceNotFoundException("Matrícula no encontrada con id " + id);
        }
        return toResponse(matricula);
    }

    /**
     * Cambio de sección en un año vigente: mismo grado, con aula y cupo. Deja el
     * historial con el motivo. En un año por comenzar se edita la matrícula.
     */
    @Transactional
    public MatriculaResponse cambioSeccion(Integer idMatricula, Integer idGradoSeccion, String motivo) {
        bloqueo.matricula(idMatricula);
        Matricula matricula = findMatricula(idMatricula);
        GradoSeccion actual = matricula.getGradoSeccion();
        if (actual.getAnioEscolar().getEstado() != AnioEscolarService.ESTADO_VIGENTE) {
            throw new IllegalArgumentException("El cambio de sección solo se hace en un año vigente");
        }
        GradoSeccion nuevoGradoSeccion = findGradoSeccion(idGradoSeccion);
        // Mismo grado aunque sea otra fila (una por turno): mismo nivel y orden.
        boolean mismoGrado = nuevoGradoSeccion.getGrado().getNivel().getIdNivel().equals(actual.getGrado().getNivel().getIdNivel())
                && java.util.Objects.equals(nuevoGradoSeccion.getGrado().getOrden(), actual.getGrado().getOrden());
        if (!mismoGrado
                || !nuevoGradoSeccion.getAnioEscolar().getIdAnio().equals(actual.getAnioEscolar().getIdAnio())) {
            throw new IllegalArgumentException("El cambio de sección debe ser dentro del mismo grado y año");
        }
        if (nuevoGradoSeccion.getAula() == null) {
            throw new IllegalArgumentException("La sección " + GradoSeccionService.nombreSeccion(nuevoGradoSeccion)
                    + " no tiene aula asignada");
        }
        gradoSeccionRepository.bloquear(nuevoGradoSeccion.getIdGradoSeccion());
        if (matriculaRepository.countByGradoSeccionIdGradoSeccionAndAccesoNot(idGradoSeccion, eliminado())
                >= nuevoGradoSeccion.getAula().getCapacidad()) {
            throw new IllegalArgumentException("La sección " + GradoSeccionService.nombreSeccion(nuevoGradoSeccion)
                    + " ya no tiene cupo");
        }

        cerrarHistorialVigente(matricula);
        matricula.setGradoSeccion(nuevoGradoSeccion);
        matricula = matriculaRepository.save(matricula);

        crearHistorial(matricula, nuevoGradoSeccion, motivo);
        return toResponse(matricula);
    }

    public List<PagoMatriculaResponse> pagos(Integer idMatricula, Integer idUsuario, List<String> roles) {
        Matricula matricula = findMatricula(idMatricula);
        if (!esVisible(matricula.getIdMatricula(), idUsuario, roles)) {
            throw new ResourceNotFoundException("Matrícula no encontrada con id " + idMatricula);
        }
        return pagoMatriculaRepository.findByMatriculaIdMatriculaAndAccesoNotOrderByFecha(idMatricula, eliminado()).stream()
                .map(pago -> {
                    PagoMatriculaResponse response = new PagoMatriculaResponse();
                    response.setIdPago(pago.getIdPagoMatricula());
                    response.setTipo(pago.getTipo().name());
                    response.setMonto(pago.getMonto());
                    response.setMetodoPago(pago.getMetodoPago().getNombre());
                    response.setCodigoTransaccion(pago.getCodigoTransaccion());
                    response.setUrlComprobante(pago.getUrlComprobante());
                    response.setFecha(pago.getFecha());
                    response.setRegistradoPor(pago.getUsuario().getNombre() + " " + pago.getUsuario().getApellidoPat());
                    return response;
                })
                .toList();
    }

    /** Pagos y diferencias menos devoluciones. */
    public BigDecimal pagadoNeto(Integer idMatricula) {
        return pagoMatriculaRepository.findByMatriculaIdMatriculaAndAccesoNotOrderByFecha(idMatricula, eliminado()).stream()
                .map(p -> p.getTipo() == TipoPago.DEVOLUCION ? p.getMonto().negate() : p.getMonto())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Transactional(readOnly = true)
    public List<MatriculaReporteResponse> reporte(LocalDate inicio, LocalDate fin, Integer idAnio, Integer idNivel,
            Integer idGrado, Integer idGradoSeccion, Long idAcceso, Integer idUsuario, List<String> roles) {
        Set<Integer> visibles = idsMatriculasVisibles(idUsuario, roles);
        return matriculaRepository.findFiltradasList(AccesoConstants.ELIMINADO, idNivel, idGrado, null, null,
                        idGradoSeccion, idAnio, idAcceso).stream()
                .filter(m -> visibles == null || visibles.contains(m.getIdMatricula()))
                .filter(m -> RangoFechas.contiene(m.getFechaRegistro(), inicio, fin))
                .sorted(ORDEN_REPORTE)
                .map(this::aReporte)
                .toList();
    }

    public FichaMatriculaResponse ficha(Integer id, Integer idUsuario, List<String> roles) {
        Matricula matricula = findMatricula(id);
        if (!esVisible(matricula.getIdMatricula(), idUsuario, roles)) {
            throw new ResourceNotFoundException("Matrícula no encontrada con id " + id);
        }
        Alumno alumno = matricula.getAlumnoApoderado().getAlumno();
        return new FichaMatriculaResponse(
                new FichaMatriculaResponse.AlumnoFicha(alumno.getCodigo(), alumno.getDocumentoIdentidad(),
                        alumno.getNombre() + " " + alumno.getApellidoPat() + " " + alumno.getApellidoMat(),
                        alumno.getSexo() != null ? alumno.getSexo().name() : null,
                        alumno.getFechaNacimiento(), alumno.getDireccion(),
                        Boolean.TRUE.equals(alumno.getTieneDiscapacidad()) ? alumno.getDiscapacidad() : null,
                        alumno.getUrlFoto()),
                aTrayectoria(matricula),
                matricula.getFechaRegistro(),
                matricula.getUsuario().getNombre() + " " + matricula.getUsuario().getApellidoPat(),
                alumnoApoderadoRepository.findByAlumno(alumno).stream()
                        .sorted(Comparator.comparing(AlumnoApoderado::getApoPrincipal).reversed())
                        .map(MatriculaService::aApoderadoFicha)
                        .toList(),
                matriculaRepository.findByAlumnoApoderadoAlumnoIdAlumnoAndAccesoNot(alumno.getIdAlumno(), eliminado())
                        .stream()
                        .sorted(Comparator.comparing((Matricula m) -> m.getGradoSeccion().getAnioEscolar().getAnio()))
                        .map(MatriculaService::aTrayectoria)
                        .toList());
    }

    private static FichaMatriculaResponse.TrayectoriaFicha aTrayectoria(Matricula m) {
        GradoSeccion gs = m.getGradoSeccion();
        return new FichaMatriculaResponse.TrayectoriaFicha(m.getIdMatricula(), gs.getAnioEscolar().getAnio(),
                gs.getGrado().getNivel().getNombre(), gradoYSeccion(gs), gs.getTurno().getNombre(),
                m.getAcceso().getNombre());
    }

    private static FichaMatriculaResponse.ApoderadoFicha aApoderadoFicha(AlumnoApoderado vinculo) {
        Apoderado apoderado = vinculo.getApoderado();
        var usuario = apoderado.getUsuario();
        return new FichaMatriculaResponse.ApoderadoFicha(
                usuario.getNombre() + " " + usuario.getApellidoPat() + " " + usuario.getApellidoMat(),
                usuario.getDocumentoIdentidad(),
                vinculo.getParentesco() != null ? vinculo.getParentesco().getNombre() : null,
                apoderado.getCelular(), usuario.getGmail(), apoderado.getDireccion(),
                vinculo.getApoPrincipal() == 1);
    }

    private MatriculaReporteResponse aReporte(Matricula m) {
        List<PagoMatricula> pagos = pagoMatriculaRepository
                .findByMatriculaIdMatriculaAndAccesoNotOrderByFecha(m.getIdMatricula(), eliminado());
        BigDecimal pagado = pagadoNeto(m.getIdMatricula());
        Alumno alumno = m.getAlumnoApoderado().getAlumno();
        GradoSeccion gs = m.getGradoSeccion();
        MatriculaReporteResponse r = new MatriculaReporteResponse();
        r.setIdMatricula(m.getIdMatricula());
        r.setAlumno(alumno.getNombre() + " " + alumno.getApellidoPat() + " " + alumno.getApellidoMat());
        r.setCodigo(alumno.getCodigo());
        r.setDocumentoIdentidad(alumno.getDocumentoIdentidad());
        r.setAnio(gs.getAnioEscolar().getAnio());
        r.setNivel(gs.getGrado().getNivel().getNombre());
        r.setGradoSeccion(gradoYSeccion(gs));
        r.setTurno(gs.getTurno().getNombre());
        r.setFechaRegistro(m.getFechaRegistro());
        r.setFechaPago(pagos.isEmpty() ? null : pagos.get(0).getFecha().toLocalDate());
        r.setMetodoPago(pagos.stream()
                .filter(p -> p.getTipo() != TipoPago.DEVOLUCION)
                .map(p -> p.getMetodoPago().getNombre())
                .distinct()
                .collect(Collectors.joining(", ")));
        r.setMonto(m.getMonto());
        r.setMontoPago(pagado);
        r.setSaldo(m.getMonto() != null ? m.getMonto().subtract(pagado) : null);
        r.setRegistradoPor(m.getUsuario().getNombre() + " " + m.getUsuario().getApellidoPat());
        r.setEstado(m.getAcceso().getNombre());
        return r;
    }

    /** "1ro B", o solo el grado si no tiene letra. El nivel va en su propia columna. */
    public static String gradoYSeccion(GradoSeccion gs) {
        return gs.getGrado().getNombre() + (gs.getSeccion() != null ? " " + gs.getSeccion().getNombre() : "");
    }

    public void crearHistorial(Matricula matricula, GradoSeccion gradoSeccion, String motivo) {
        HistorialMatricula historial = new HistorialMatricula();
        historial.setMatricula(matricula);
        historial.setGradoSeccion(gradoSeccion);
        historial.setFechaInicio(LocalDate.now());
        historial.setMotivo(motivo);
        historial.setAcceso(accesoRepository.findById(AccesoConstants.ACTIVO).orElseThrow());
        historialMatriculaRepository.save(historial);
    }

    public void cerrarHistorialVigente(Matricula matricula) {
        historialMatriculaRepository.findByMatriculaIdMatriculaAndAccesoNot(matricula.getIdMatricula(), eliminado())
                .stream()
                .filter(h -> h.getFechaFinal() == null)
                .findFirst()
                .ifPresent(h -> {
                    h.setFechaFinal(LocalDate.now());
                    historialMatriculaRepository.save(h);
                });
    }

    private Set<Integer> idsMatriculasVisibles(Integer idUsuario, List<String> roles) {
        Alcance alcance = accesoContextoService.alcance(PERMISO, idUsuario, roles);
        if (alcance == Alcance.TODO) {
            return null;
        }
        Set<Integer> ids = new HashSet<>();
        if (alcance == Alcance.DOCENTE) {
            for (Matricula matricula : accesoContextoService.matriculasDeGradoSecciones(
                    accesoContextoService.gradoSeccionIdsDeDocente(idUsuario))) {
                ids.add(matricula.getIdMatricula());
            }
            return ids;
        }
        if (alcance == Alcance.APODERADO) {
            Apoderado apoderado = accesoContextoService.apoderadoDeUsuario(idUsuario).orElse(null);
            if (apoderado == null) {
                return ids;
            }
            alumnoApoderadoRepository.findByApoderado(apoderado).stream()
                    .flatMap(aa -> matriculaRepository.findByAlumnoApoderadoAlumnoIdAlumnoAndAccesoNot(aa.getAlumno().getIdAlumno(), eliminado()).stream())
                    .forEach(m -> ids.add(m.getIdMatricula()));
            return ids;
        }
        return ids;
    }

    private boolean esVisible(Integer idMatricula, Integer idUsuario, List<String> roles) {
        Set<Integer> visibles = idsMatriculasVisibles(idUsuario, roles);
        return visibles == null || visibles.contains(idMatricula);
    }

    public Matricula findMatricula(Integer id) {
        return matriculaRepository.findByIdMatriculaAndAccesoNot(id, eliminado())
                .orElseThrow(() -> new ResourceNotFoundException("Matrícula no encontrada con id " + id));
    }

    private GradoSeccion findGradoSeccion(Integer id) {
        return gradoSeccionRepository.findByIdGradoSeccionAndAccesoNot(id, eliminado())
                .orElseThrow(() -> new ResourceNotFoundException("Grado-sección no encontrado con id " + id));
    }

    private Acceso eliminado() {
        return accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow();
    }

    private List<MatriculaResponse> toResponses(List<Matricula> matriculas) {
        if (matriculas.isEmpty()) return List.of();
        List<Integer> ids = matriculas.stream().map(m -> m.getAlumnoApoderado().getAlumno().getIdAlumno()).distinct().toList();
        var primeros = matriculaRepository.primerosAniosPorAlumno(ids, AccesoConstants.ELIMINADO).stream()
                .collect(Collectors.toMap(MatriculaRepository.PrimerAnioAlumno::getIdAlumno, MatriculaRepository.PrimerAnioAlumno::getPrimerAnio));
        return matriculas.stream().map(m -> {
            String primero = primeros.get(m.getAlumnoApoderado().getAlumno().getIdAlumno());
            var tipo = primero != null && primero.compareTo(m.getGradoSeccion().getAnioEscolar().getAnio()) < 0
                    ? MatriculaResponse.TipoIngreso.CONTINUANTE : MatriculaResponse.TipoIngreso.NUEVO_INGRESO;
            return toResponse(m, tipo);
        }).toList();
    }

    public MatriculaResponse toResponse(Matricula matricula) {
        return toResponses(List.of(matricula)).get(0);
    }

    private MatriculaResponse toResponse(Matricula matricula, MatriculaResponse.TipoIngreso tipoIngreso) {
        AlumnoApoderado vinculo = matricula.getAlumnoApoderado();
        Alumno alumno = vinculo.getAlumno();
        GradoSeccion gs = matricula.getGradoSeccion();
        List<PagoMatricula> pagos = pagoMatriculaRepository
                .findByMatriculaIdMatriculaAndAccesoNotOrderByFecha(matricula.getIdMatricula(), eliminado());

        MatriculaResponse response = new MatriculaResponse();
        response.setTipoIngreso(tipoIngreso);
        response.setIdMatricula(matricula.getIdMatricula());
        response.setIdAlumno(alumno.getIdAlumno());
        response.setIdAlumnoApoderado(vinculo.getIdAlumnoApoderado());
        response.setCodigoAlumno(alumno.getCodigo());
        response.setDocumentoAlumno(alumno.getDocumentoIdentidad());
        response.setAlumno(alumno.getNombre() + " " + alumno.getApellidoPat() + " " + alumno.getApellidoMat());
        response.setApoderado(vinculo.getApoderado().getUsuario().getNombre() + " "
                + vinculo.getApoderado().getUsuario().getApellidoPat());
        response.setIdUsuario(matricula.getUsuario().getIdUsuario());
        response.setUsuarioRegistro(matricula.getUsuario().getNombre() + " "
                + matricula.getUsuario().getApellidoPat());
        response.setIdGradoSeccion(gs.getIdGradoSeccion());
        response.setIdNivel(gs.getGrado().getNivel().getIdNivel());
        response.setNivel(gs.getGrado().getNivel().getNombre());
        response.setIdGrado(gs.getGrado().getIdGrado());
        response.setGrado(gs.getGrado().getNombre());
        response.setSeccion(gs.getSeccion() != null ? gs.getSeccion().getNombre() : "Única");
        response.setIdTurno(gs.getTurno().getIdTurno());
        response.setTurno(gs.getTurno().getNombre());
        response.setIdAnio(gs.getAnioEscolar().getIdAnio());
        response.setAnio(gs.getAnioEscolar().getAnio());
        response.setEstadoAnio(gs.getAnioEscolar().getEstado());
        response.setMonto(matricula.getMonto());
        response.setPagadoNeto(pagos.stream()
                .map(p -> p.getTipo() == TipoPago.DEVOLUCION ? p.getMonto().negate() : p.getMonto())
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        response.setMetodoPago(pagos.stream()
                .filter(p -> p.getTipo() == TipoPago.PAGO)
                .findFirst()
                .map(p -> p.getMetodoPago().getNombre())
                .orElse(null));
        response.setFechaRegistro(matricula.getFechaRegistro());
        response.setObservaciones(matricula.getObservaciones());
        response.setAccesoId(matricula.getAcceso().getIdAcceso().longValue());
        response.setHistorial(historialMatriculaRepository
                .findByMatriculaIdMatriculaAndAccesoNot(matricula.getIdMatricula(), eliminado()).stream()
                .map(this::toHistorialResponse)
                .toList());
        return response;
    }

    private HistorialResponse toHistorialResponse(HistorialMatricula historial) {
        GradoSeccion gs = historial.getGradoSeccion();
        HistorialResponse response = new HistorialResponse();
        response.setIdHistorial(historial.getIdHistorial());
        response.setIdGradoSeccion(gs.getIdGradoSeccion());
        response.setGrado(gs.getGrado().getNombre());
        response.setSeccion(gs.getSeccion() != null ? gs.getSeccion().getNombre() : "Única");
        response.setTurno(gs.getTurno().getNombre());
        response.setAnio(gs.getAnioEscolar().getAnio());
        response.setNivel(gs.getGrado().getNivel().getNombre());
        response.setIdAnio(gs.getAnioEscolar().getIdAnio());
        response.setFechaInicio(historial.getFechaInicio());
        response.setFechaFinal(historial.getFechaFinal());
        response.setMotivo(historial.getMotivo());
        return response;
    }
}
