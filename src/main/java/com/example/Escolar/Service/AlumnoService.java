package com.example.Escolar.Service;

import com.example.Escolar.Dto.AlumnoReporteResponse;
import com.example.Escolar.Dto.AlumnoEdicionRequest;
import com.example.Escolar.Dto.AlumnoResponse;
import com.example.Escolar.Dto.UploadResponse;
import com.example.Escolar.Exception.ResourceNotFoundException;
import com.example.Escolar.Model.Alumno;
import com.example.Escolar.Model.AlumnoApoderado;
import com.example.Escolar.Model.Apoderado;
import com.example.Escolar.Model.Matricula;
import com.example.Escolar.Repository.AccesoRepository;
import com.example.Escolar.Repository.AlumnoApoderadoRepository;
import com.example.Escolar.Repository.AlumnoRepository;
import com.example.Escolar.Repository.MatriculaRepository;
import com.example.Escolar.Service.AccesoContextoService.Alcance;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AlumnoService {

    private static final String PERMISO = "ALUMNOS";
    private final AlumnoRepository alumnoRepository;
    private final AlumnoApoderadoRepository alumnoApoderadoRepository;
    private final MatriculaRepository matriculaRepository;
    private final CloudinaryService cloudinaryService;
    private final AccesoContextoService accesoContextoService;
    private final AccesoRepository accesoRepository;

    public Page<AlumnoResponse> getAll(Pageable pageable, Integer idUsuario, List<String> roles,
                                        Integer idNivel, Integer idGrado, Integer idSeccion,
                                        Integer idTurno, Integer idGradoSeccion, Integer idAnio) {
        boolean filtrando = idNivel != null || idGrado != null || idSeccion != null || idTurno != null || idGradoSeccion != null || idAnio != null;
        Set<Integer> visibles = idsAlumnosVisibles(idUsuario, roles);
        boolean esGestion = visibles == null;

        if (filtrando) {
            // Filtrado vigente: requiere matrícula ACTIVA. Usa query paginada en BD (vigente) y corrige totalElements.
            if (esGestion) {
                Page<Alumno> page = alumnoRepository.findFiltrados(
                        AccesoConstants.ACTIVO, AccesoConstants.ELIMINADO,
                        idNivel, idGrado, idSeccion, idTurno, idGradoSeccion, idAnio, pageable);
                return page.map(this::toResponseEnriquecido);
            } else {
                // Docente/Apoderado: filtra en BD, luego por visibilidad y paginación en memoria
                List<Alumno> filtrados = alumnoRepository.findFiltradosList(
                        AccesoConstants.ACTIVO, AccesoConstants.ELIMINADO,
                        idNivel, idGrado, idSeccion, idTurno, idGradoSeccion, idAnio);
                List<Alumno> visiblesList = filtrados.stream()
                        .filter(a -> visibles.contains(a.getIdAlumno()))
                        .toList();
                return paginarEnMemoria(visiblesList, pageable);
            }
        }

        // Sin filtros académicos: listado clásico + enriquecimiento batch, con paginación corregida
        if (esGestion) {
            Page<Alumno> page = alumnoRepository.findByAccesoNot(accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow(), pageable);
            // Enriquecimiento batch de la página
            List<AlumnoResponse> respuestas = toResponseEnriquecidoBatch(page.getContent());
            return new PageImpl<>(respuestas, pageable, page.getTotalElements());
        } else {
            List<Alumno> todos = alumnoRepository.findByAccesoNot(accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow());
            List<Alumno> visiblesList = todos.stream()
                    .filter(a -> visibles.contains(a.getIdAlumno()))
                    .toList();
            return paginarEnMemoria(visiblesList, pageable);
        }
    }

    // Compatibilidad: firma antigua sin filtros
    public Page<AlumnoResponse> getAll(Pageable pageable, Integer idUsuario, List<String> roles) {
        return getAll(pageable, idUsuario, roles, null, null, null, null, null, null);
    }

    private Page<AlumnoResponse> paginarEnMemoria(List<Alumno> lista, Pageable pageable) {
        int total = lista.size();
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), total);
        List<Alumno> slice = start >= total ? List.of() : lista.subList(start, end);
        List<AlumnoResponse> respuestas = toResponseEnriquecidoBatch(slice);
        return new PageImpl<>(respuestas, pageable, total);
    }

    private List<AlumnoResponse> toResponseEnriquecidoBatch(List<Alumno> alumnos) {
        if (alumnos.isEmpty()) return List.of();
        List<Integer> ids = alumnos.stream().map(Alumno::getIdAlumno).toList();
        List<Matricula> matriculas = matriculaRepository.findActivasPorAlumnoIds(ids,
                accesoRepository.findById(AccesoConstants.ACTIVO).orElseThrow());
        // Puede tener la del año vigente y la del por comenzar: se muestra la actual.
        java.util.Map<Integer, Matricula> mapa = new java.util.HashMap<>();
        matriculas.stream()
                .collect(java.util.stream.Collectors.groupingBy(m -> m.getAlumnoApoderado().getAlumno().getIdAlumno()))
                .forEach((id, delAlumno) -> AccesoContextoService.elegirActual(delAlumno).ifPresent(m -> mapa.put(id, m)));
        return alumnos.stream().map(a -> {
            List<AlumnoApoderado> vinculos = alumnoApoderadoRepository.findByAlumno(a);
            Matricula vigente = mapa.get(a.getIdAlumno());
            return AlumnoResponse.fromEntity(a, vinculos, vigente);
        }).toList();
    }

    private AlumnoResponse toResponseEnriquecido(Alumno alumno) {
        List<AlumnoApoderado> vinculos = alumnoApoderadoRepository.findByAlumno(alumno);
        // Batch de 1 para reutilizar FETCH JOIN
        List<Matricula> vigente = matriculaRepository.findActivasPorAlumnoIds(
                List.of(alumno.getIdAlumno()), accesoRepository.findById(AccesoConstants.ACTIVO).orElseThrow());
        Matricula m = AccesoContextoService.elegirActual(vigente).orElse(null);
        return AlumnoResponse.fromEntity(alumno, vinculos, m);
    }

    public AlumnoResponse getById(Integer id, Integer idUsuario, List<String> roles) {
        Alumno alumno = findAlumno(id);
        if (!esVisible(alumno.getIdAlumno(), idUsuario, roles)) {
            throw new ResourceNotFoundException("Alumno no encontrado con id " + id);
        }
        return toResponse(alumno);
    }

    public AlumnoResponse getByDocumento(String documento, Integer idUsuario, List<String> roles) {
        if (documento == null || documento.isBlank()) {
            throw new IllegalArgumentException("El documento de identidad es obligatorio");
        }
        Alumno alumno = alumnoRepository.findByDocumentoIdentidadAndAccesoNot(documento, accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow())
                .orElseThrow(() -> new ResourceNotFoundException("Alumno no encontrado con documento " + documento));
        if (!esVisible(alumno.getIdAlumno(), idUsuario, roles)) {
            throw new ResourceNotFoundException("Alumno no encontrado con documento " + documento);
        }
        return toResponse(alumno);
    }

    /** Del alumno solo se corrige la discapacidad (y la foto); el resto viene de su matrícula. */
    @Transactional
    public AlumnoResponse actualizarDiscapacidad(Integer id, AlumnoEdicionRequest request) {
        Alumno alumno = findAlumno(id);
        if (request.isTieneDiscapacidad() && (request.getDiscapacidad() == null || request.getDiscapacidad().isBlank())) {
            throw new IllegalArgumentException("Describe la discapacidad del alumno");
        }
        alumno.setTieneDiscapacidad(request.isTieneDiscapacidad());
        alumno.setDiscapacidad(request.isTieneDiscapacidad() ? request.getDiscapacidad().trim() : null);
        return toResponse(alumnoRepository.save(alumno));
    }

    @Transactional
    public AlumnoResponse subirFoto(Integer id, MultipartFile file) {
        Alumno alumno = findAlumno(id);
        if (alumno.getPkUrlFoto() != null && !alumno.getPkUrlFoto().isBlank()) {
            cloudinaryService.delete(alumno.getPkUrlFoto());
        }
        UploadResponse upload = cloudinaryService.upload(file, "alumnos");
        alumno.setUrlFoto(upload.getUrl());
        alumno.setPkUrlFoto(upload.getPublicId());
        return toResponse(alumnoRepository.save(alumno));
    }

    @Transactional
    public void eliminarFoto(Integer id) {
        Alumno alumno = findAlumno(id);
        if (alumno.getPkUrlFoto() != null && !alumno.getPkUrlFoto().isBlank()) {
            cloudinaryService.delete(alumno.getPkUrlFoto());
            alumno.setUrlFoto(null);
            alumno.setPkUrlFoto(null);
            alumnoRepository.save(alumno);
        }
    }

    @Transactional(readOnly = true)
    public List<AlumnoReporteResponse> reporte(LocalDate inicio, LocalDate fin, Integer idGradoSeccion) {
        return alumnoRepository.findByFechaIngresoBetweenAndAccesoNot(inicio, fin, accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow()).stream()
                .filter(alumno -> {
                    if (idGradoSeccion == null) return true;
                    return alumnoApoderadoRepository.findByAlumno(alumno).stream()
                            .flatMap(aa -> matriculaRepository.findByAlumnoApoderadoAlumnoIdAlumnoAndAccesoNot(aa.getAlumno().getIdAlumno(), accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow()).stream())
                            .anyMatch(m -> idGradoSeccion.equals(m.getGradoSeccion().getIdGradoSeccion()));
                })
                .map(alumno -> {
                    AlumnoReporteResponse r = new AlumnoReporteResponse();
                    r.setIdAlumno(alumno.getIdAlumno());
                    r.setNombre(alumno.getNombre() + " " + alumno.getApellidoPat() + " " + alumno.getApellidoMat());
                    r.setCodigo(alumno.getCodigo());
                    r.setDocumentoIdentidad(alumno.getDocumentoIdentidad());
                    r.setFechaNacimiento(alumno.getFechaNacimiento());
                    r.setFechaIngreso(alumno.getFechaIngreso());
                    String gs = alumnoApoderadoRepository.findByAlumno(alumno).stream()
                            .flatMap(aa -> matriculaRepository.findByAlumnoApoderadoAlumnoIdAlumnoAndAccesoNot(aa.getAlumno().getIdAlumno(), accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow()).stream())
                            .map(m -> m.getGradoSeccion().getGrado().getNombre() + " "
                                    + (m.getGradoSeccion().getSeccion() != null ? m.getGradoSeccion().getSeccion().getNombre() : "Única"))
                            .findFirst().orElse("Sin matrícula");
                    r.setGradoSeccion(gs);
                    String apo = alumnoApoderadoRepository.findByAlumno(alumno).stream()
                            .filter(aa -> aa.getApoPrincipal() == 1)
                            .map(aa -> aa.getApoderado().getUsuario().getNombre() + " "
                                    + aa.getApoderado().getUsuario().getApellidoPat())
                            .findFirst().orElse("Sin apoderado");
                    r.setApoderado(apo);
                    return r;
                })
                .toList();
    }

    private Set<Integer> idsAlumnosVisibles(Integer idUsuario, List<String> roles) {
        Alcance alcance = accesoContextoService.alcance(PERMISO, idUsuario, roles);
        if (alcance == Alcance.TODO) {
            return null;
        }
        if (alcance == Alcance.DOCENTE) {
            Set<Integer> ids = new HashSet<>();
            for (Matricula matricula : accesoContextoService.matriculasDeGradoSecciones(
                    accesoContextoService.gradoSeccionIdsDeDocente(idUsuario))) {
                ids.add(matricula.getAlumnoApoderado().getAlumno().getIdAlumno());
            }
            return ids;
        }
        if (alcance == Alcance.APODERADO) {
            Set<Integer> ids = new HashSet<>();
            Apoderado apoderado = accesoContextoService.apoderadoDeUsuario(idUsuario).orElse(null);
            if (apoderado == null) {
                return ids;
            }
            alumnoApoderadoRepository.findByApoderado(apoderado)
                    .forEach(aa -> ids.add(aa.getAlumno().getIdAlumno()));
            return ids;
        }
        return new HashSet<>();
    }

    private boolean esVisible(Integer idAlumno, Integer idUsuario, List<String> roles) {
        Set<Integer> visibles = idsAlumnosVisibles(idUsuario, roles);
        return visibles == null || visibles.contains(idAlumno);
    }

    private Alumno findAlumno(Integer id) {
        return alumnoRepository.findByIdAlumnoAndAccesoNot(id, accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow())
                .orElseThrow(() -> new ResourceNotFoundException("Alumno no encontrado con id " + id));
    }

    private AlumnoResponse toResponse(Alumno alumno) {
        List<AlumnoApoderado> vinculos = alumnoApoderadoRepository.findByAlumno(alumno);
        List<Matricula> vigente = matriculaRepository.findActivasPorAlumnoIds(
                List.of(alumno.getIdAlumno()), accesoRepository.findById(AccesoConstants.ACTIVO).orElseThrow());
        Matricula m = AccesoContextoService.elegirActual(vigente).orElse(null);
        return AlumnoResponse.fromEntity(alumno, vinculos, m);
    }
}
