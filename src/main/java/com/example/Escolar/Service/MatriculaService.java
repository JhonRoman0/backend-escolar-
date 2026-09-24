package com.example.Escolar.Service;

import com.example.Escolar.Dto.HistorialResponse;
import com.example.Escolar.Dto.MatriculaReporteResponse;
import com.example.Escolar.Dto.MatriculaRequest;
import com.example.Escolar.Dto.MatriculaResponse;
import com.example.Escolar.Exception.ResourceNotFoundException;
import com.example.Escolar.Model.Alumno;
import com.example.Escolar.Model.AlumnoApoderado;
import com.example.Escolar.Model.Apoderado;
import com.example.Escolar.Model.GradoSeccion;
import com.example.Escolar.Model.HistorialMatricula;
import com.example.Escolar.Model.Matricula;
import com.example.Escolar.Model.Usuario;
import com.example.Escolar.Repository.AccesoRepository;
import com.example.Escolar.Repository.AlumnoApoderadoRepository;
import com.example.Escolar.Repository.AlumnoRepository;
import com.example.Escolar.Repository.GradoSeccionRepository;
import com.example.Escolar.Repository.HistorialMatriculaRepository;
import com.example.Escolar.Repository.MatriculaRepository;
import com.example.Escolar.Repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class MatriculaService {

    public static final byte PRINCIPAL = 1;
    public static final byte ESTADO_CERRADO = 2;

    private final MatriculaRepository matriculaRepository;
    private final HistorialMatriculaRepository historialMatriculaRepository;
    private final AlumnoRepository alumnoRepository;
    private final AlumnoApoderadoRepository alumnoApoderadoRepository;
    private final GradoSeccionRepository gradoSeccionRepository;
    private final UsuarioRepository usuarioRepository;
    private final AccesoRepository accesoRepository;
    private final AccesoContextoService accesoContextoService;

    public Page<MatriculaResponse> getAll(Pageable pageable, Integer idUsuario, List<String> roles,
                                         Integer idNivel, Integer idGrado, Integer idSeccion,
                                         Integer idTurno, Integer idGradoSeccion, Integer idAnio) {
        boolean filtrando = idNivel != null || idGrado != null || idSeccion != null || idTurno != null || idGradoSeccion != null || idAnio != null;
        Set<Integer> visibles = idsMatriculasVisibles(idUsuario, roles);
        boolean esGestion = visibles == null;

        if (filtrando) {
            if (esGestion) {
                Page<Matricula> page = matriculaRepository.findFiltradas(
                        AccesoConstants.ELIMINADO, idNivel, idGrado, idSeccion, idTurno, idGradoSeccion, idAnio, pageable);
                return page.map(this::toResponse);
            } else {
                List<Matricula> filtradas = matriculaRepository.findFiltradasList(
                        AccesoConstants.ELIMINADO, idNivel, idGrado, idSeccion, idTurno, idGradoSeccion, idAnio);
                List<Matricula> visiblesList = filtradas.stream()
                        .filter(m -> visibles.contains(m.getIdMatricula()))
                        .toList();
                return paginarMatriculasEnMemoria(visiblesList, pageable);
            }
        }

        if (esGestion) {
            Page<Matricula> page = matriculaRepository.findByAccesoNot(accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow(), pageable);
            return page.map(this::toResponse);
        } else {
            List<Matricula> todas = matriculaRepository.findByAccesoNot(accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow());
            List<Matricula> visiblesList = todas.stream()
                    .filter(m -> visibles.contains(m.getIdMatricula()))
                    .toList();
            return paginarMatriculasEnMemoria(visiblesList, pageable);
        }
    }

    public Page<MatriculaResponse> getAll(Pageable pageable, Integer idUsuario, List<String> roles) {
        return getAll(pageable, idUsuario, roles, null, null, null, null, null, null);
    }

    private Page<MatriculaResponse> paginarMatriculasEnMemoria(List<Matricula> lista, Pageable pageable) {
        int total = lista.size();
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), total);
        List<Matricula> slice = start >= total ? List.of() : lista.subList(start, end);
        List<MatriculaResponse> respuestas = slice.stream().map(this::toResponse).toList();
        return new PageImpl<>(respuestas, pageable, total);
    }

    public MatriculaResponse getById(Integer id, Integer idUsuario, List<String> roles) {
        Matricula matricula = findMatricula(id);
        if (!esVisible(matricula.getIdMatricula(), idUsuario, roles)) {
            throw new ResourceNotFoundException("Matrícula no encontrada con id " + id);
        }
        return toResponse(matricula);
    }

    @Transactional
    public MatriculaResponse create(MatriculaRequest request, Integer idUsuarioRegistro) {
        Alumno alumno = findAlumno(request.getIdAlumno());
        AlumnoApoderado alumnoApoderado = resolverVinculoPrincipal(alumno);
        GradoSeccion gradoSeccion = findGradoSeccion(request.getIdGradoSeccion());
        Usuario usuario = findUsuario(idUsuarioRegistro);

        cerrarMatriculaAnteriorSiAplica(alumnoApoderado, gradoSeccion);

        Matricula matricula = new Matricula();
        matricula.setAlumnoApoderado(alumnoApoderado);
        matricula.setGradoSeccion(gradoSeccion);
        matricula.setUsuario(usuario);
        matricula.setSolicitudMatricula(request.getSolicitudMatricula());
        matricula.setFechaPago(request.getFechaPago());
        matricula.setMontoPago(request.getMontoPago());
        matricula.setObservaciones(request.getObservaciones());
        matricula.setAcceso(accesoRepository.findById(request.getAccesoId() == null ? AccesoConstants.ACTIVO : request.getAccesoId()).orElseThrow());
        matricula.setFechaRegistro(LocalDate.now());
        Matricula saved = matriculaRepository.save(matricula);

        crearHistorial(saved, gradoSeccion, LocalDate.now(), null, "Matrícula inicial");
        return toResponse(saved);
    }

    private void cerrarMatriculaAnteriorSiAplica(AlumnoApoderado alumnoApoderado, GradoSeccion nuevoGradoSeccion) {
        Optional<Matricula> activa = matriculaRepository
                .findByAlumnoApoderadoAndAcceso(alumnoApoderado, accesoRepository.findById(AccesoConstants.ACTIVO).orElseThrow());
        if (activa.isEmpty()) {
            return;
        }
        Matricula anterior = activa.get();
        Integer idAnioAnterior = anterior.getGradoSeccion().getAnioEscolar().getIdAnio();
        Integer idAnioNuevo = nuevoGradoSeccion.getAnioEscolar().getIdAnio();
        if (idAnioAnterior.equals(idAnioNuevo)) {
            throw new IllegalArgumentException("El alumno ya tiene una matrícula activa en el año " + nuevoGradoSeccion.getAnioEscolar().getAnio());
        }
        if (anterior.getGradoSeccion().getAnioEscolar().getEstado() != ESTADO_CERRADO) {
            throw new IllegalArgumentException("El año " + anterior.getGradoSeccion().getAnioEscolar().getAnio()
                    + " aún no está cerrado. Cierre el año escolar antes de matricular en el nuevo año.");
        }
        cerrarHistorialVigente(anterior);
        anterior.setAcceso(accesoRepository.findById(AccesoConstants.INACTIVO).orElseThrow());
        matriculaRepository.save(anterior);
    }

    @Transactional
    public MatriculaResponse cambioSeccion(Integer idMatricula, Integer idGradoSeccion, String motivo) {
        Matricula matricula = findMatricula(idMatricula);
        GradoSeccion nuevoGradoSeccion = findGradoSeccion(idGradoSeccion);

        cerrarHistorialVigente(matricula);
        matricula.setGradoSeccion(nuevoGradoSeccion);
        matricula = matriculaRepository.save(matricula);

        crearHistorial(matricula, nuevoGradoSeccion, LocalDate.now(), null, motivo);
        return toResponse(matricula);
    }

    @Transactional
    public MatriculaResponse update(Integer id, MatriculaRequest request) {
        Matricula matricula = findMatricula(id);
        if (request.getIdGradoSeccion() != null
                && !request.getIdGradoSeccion().equals(matricula.getGradoSeccion().getIdGradoSeccion())) {
            throw new IllegalArgumentException("No se puede cambiar el grado-sección por este medio; use cambio de sección");
        }
        if (request.getSolicitudMatricula() != null) {
            matricula.setSolicitudMatricula(request.getSolicitudMatricula());
        }
        if (request.getFechaPago() != null) {
            matricula.setFechaPago(request.getFechaPago());
        }
        if (request.getMontoPago() != null) {
            matricula.setMontoPago(request.getMontoPago());
        }
        if (request.getObservaciones() != null) {
            matricula.setObservaciones(request.getObservaciones());
        }
        if (request.getAccesoId() != null) {
            matricula.setAcceso(accesoRepository.findById(request.getAccesoId()).orElseThrow());
        }
        return toResponse(matriculaRepository.save(matricula));
    }

    @Transactional
    public MatriculaResponse aprobar(Integer id, String observaciones) {
        Matricula matricula = findMatricula(id);
        matricula.setSolicitudMatricula((byte) 2);
        matricula.setObservaciones(observaciones);
        return toResponse(matriculaRepository.save(matricula));
    }

    @Transactional
    public MatriculaResponse rechazar(Integer id, String observaciones) {
        Matricula matricula = findMatricula(id);
        matricula.setSolicitudMatricula((byte) 3);
        matricula.setObservaciones(observaciones);
        return toResponse(matriculaRepository.save(matricula));
    }

    @Transactional
    public MatriculaResponse matricular(Integer id, LocalDate fechaPago, BigDecimal montoPago) {
        Matricula matricula = findMatricula(id);
        matricula.setFechaPago(fechaPago);
        matricula.setMontoPago(montoPago);
        matricula.setSolicitudMatricula((byte) 2);
        return toResponse(matriculaRepository.save(matricula));
    }

    @Transactional
    public void delete(Integer id) {
        Matricula matricula = findMatricula(id);
        matricula.setAcceso(accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow());
        historialMatriculaRepository.findByMatriculaIdMatriculaAndAccesoNot(id, accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow())
                .forEach(h -> h.setAcceso(accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow()));
        matriculaRepository.save(matricula);
    }

    @Transactional(readOnly = true)
    public List<MatriculaReporteResponse> reporte(LocalDate inicio, LocalDate fin, Integer idAnio, Integer idGradoSeccion) {
        return matriculaRepository.findByFechaRegistroBetweenAndAccesoNot(inicio, fin, accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow()).stream()
                .filter(m -> {
                    if (idAnio != null) {
                        if (!idAnio.equals(m.getGradoSeccion().getAnioEscolar().getIdAnio())) return false;
                    }
                    if (idGradoSeccion != null) {
                        if (!idGradoSeccion.equals(m.getGradoSeccion().getIdGradoSeccion())) return false;
                    }
                    return true;
                })
                .map(m -> {
                    MatriculaReporteResponse r = new MatriculaReporteResponse();
                    r.setIdMatricula(m.getIdMatricula());
                    r.setAlumno(m.getAlumnoApoderado().getAlumno().getNombre() + " "
                            + m.getAlumnoApoderado().getAlumno().getApellidoPat());
                    r.setCodigo(m.getAlumnoApoderado().getAlumno().getCodigo());
                    r.setGradoSeccion(m.getGradoSeccion().getGrado().getNombre() + " "
                            + (m.getGradoSeccion().getSeccion() != null ? m.getGradoSeccion().getSeccion().getNombre() : "Única"));
                    r.setFechaRegistro(m.getFechaRegistro());
                    r.setFechaPago(m.getFechaPago());
                    r.setMontoPago(m.getMontoPago());
                    r.setRegistradoPor(m.getUsuario().getNombre() + " " + m.getUsuario().getApellidoPat());
                    return r;
                })
                .toList();
    }

    private void crearHistorial(Matricula matricula, GradoSeccion gradoSeccion,
                                LocalDate fechaInicio, LocalDate fechaFinal, String motivo) {
        HistorialMatricula historial = new HistorialMatricula();
        historial.setMatricula(matricula);
        historial.setGradoSeccion(gradoSeccion);
        historial.setFechaInicio(fechaInicio);
        historial.setFechaFinal(fechaFinal);
        historial.setMotivo(motivo);
        historial.setAcceso(accesoRepository.findById(AccesoConstants.ACTIVO).orElseThrow());
        historialMatriculaRepository.save(historial);
    }

    private void cerrarHistorialVigente(Matricula matricula) {
        historialMatriculaRepository.findByMatriculaIdMatriculaAndAccesoNot(matricula.getIdMatricula(), accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow())
                .stream()
                .filter(h -> h.getFechaFinal() == null)
                .findFirst()
                .ifPresent(h -> {
                    h.setFechaFinal(LocalDate.now());
                    historialMatriculaRepository.save(h);
                });
    }

    private AlumnoApoderado resolverVinculoPrincipal(Alumno alumno) {
        return alumnoApoderadoRepository.findByAlumno(alumno).stream()
                .filter(v -> v.getApoPrincipal() == PRINCIPAL)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("El alumno no tiene un apoderado principal registrado"));
    }

    private Set<Integer> idsMatriculasVisibles(Integer idUsuario, List<String> roles) {
        if (accesoContextoService.esGestion(roles)) {
            return null;
        }
        Set<Integer> ids = new HashSet<>();
        if (accesoContextoService.esPersonalDocente(roles)) {
            for (Matricula matricula : accesoContextoService.matriculasDeGradoSecciones(
                    accesoContextoService.gradoSeccionIdsDeDocente(idUsuario))) {
                ids.add(matricula.getIdMatricula());
            }
            return ids;
        }
        if (accesoContextoService.esApoderado(roles)) {
            Apoderado apoderado = accesoContextoService.apoderadoDeUsuario(idUsuario).orElse(null);
            if (apoderado == null) {
                return ids;
            }
            alumnoApoderadoRepository.findByApoderado(apoderado).stream()
                    .flatMap(aa -> matriculaRepository.findByAlumnoApoderadoAndAccesoNot(aa, accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow()).stream())
                    .forEach(m -> ids.add(m.getIdMatricula()));
            return ids;
        }
        return ids;
    }

    private boolean esVisible(Integer idMatricula, Integer idUsuario, List<String> roles) {
        Set<Integer> visibles = idsMatriculasVisibles(idUsuario, roles);
        return visibles == null || visibles.contains(idMatricula);
    }

    private Matricula findMatricula(Integer id) {
        return matriculaRepository.findByIdMatriculaAndAccesoNot(id, accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow())
                .orElseThrow(() -> new ResourceNotFoundException("Matrícula no encontrada con id " + id));
    }

    private Alumno findAlumno(Integer id) {
        return alumnoRepository.findByIdAlumnoAndAccesoNot(id, accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow())
                .orElseThrow(() -> new ResourceNotFoundException("Alumno no encontrado con id " + id));
    }

    private GradoSeccion findGradoSeccion(Integer id) {
        return gradoSeccionRepository.findByIdGradoSeccionAndAccesoNot(id, accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow())
                .orElseThrow(() -> new ResourceNotFoundException("Grado-sección no encontrado con id " + id));
    }

    private Usuario findUsuario(Integer id) {
        if (id == null) {
            throw new IllegalArgumentException("No se pudo identificar al usuario que realiza la matrícula");
        }
        return usuarioRepository.findByIdUsuarioAndAccesoNot(id, accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con id " + id));
    }

    private MatriculaResponse toResponse(Matricula matricula) {
        AlumnoApoderado vinculo = matricula.getAlumnoApoderado();
        Alumno alumno = vinculo.getAlumno();
        GradoSeccion gs = matricula.getGradoSeccion();

        MatriculaResponse response = new MatriculaResponse();
        response.setIdMatricula(matricula.getIdMatricula());
        response.setIdAlumno(alumno.getIdAlumno());
        response.setIdAlumnoApoderado(vinculo.getIdAlumnoApoderado());
        response.setCodigoAlumno(alumno.getCodigo());
        response.setAlumno(alumno.getNombre() + " " + alumno.getApellidoPat() + " " + alumno.getApellidoMat());
        response.setApoderado(vinculo.getApoderado().getUsuario().getNombre() + " "
                + vinculo.getApoderado().getUsuario().getApellidoPat());
        response.setIdUsuario(matricula.getUsuario().getIdUsuario());
        response.setUsuarioRegistro(matricula.getUsuario().getNombre() + " "
                + matricula.getUsuario().getApellidoPat());
        response.setIdGradoSeccion(gs.getIdGradoSeccion());
        response.setGrado(gs.getGrado().getNombre());
        response.setSeccion(gs.getSeccion() != null ? gs.getSeccion().getNombre() : "Única");
        response.setTurno(gs.getTurno().getNombre());
        response.setIdAnio(gs.getAnioEscolar().getIdAnio());
        response.setAnio(gs.getAnioEscolar().getAnio());
        response.setSolicitudMatricula(matricula.getSolicitudMatricula());
        response.setFechaPago(matricula.getFechaPago());
        response.setMontoPago(matricula.getMontoPago());
        response.setObservaciones(matricula.getObservaciones());
        response.setAccesoId(matricula.getAcceso().getIdAcceso().longValue());
        response.setHistorial(historialMatriculaRepository
                .findByMatriculaIdMatriculaAndAccesoNot(matricula.getIdMatricula(), accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow()).stream()
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
        response.setIdAnio(gs.getAnioEscolar().getIdAnio());
        response.setFechaInicio(historial.getFechaInicio());
        response.setFechaFinal(historial.getFechaFinal());
        response.setMotivo(historial.getMotivo());
        return response;
    }
}
