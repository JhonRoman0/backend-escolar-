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
import java.util.Comparator;
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
            // La matrícula activa que enriquece la respuesta debe cumplir el mismo contexto que la consulta.
            java.util.function.Predicate<Matricula> contexto = m -> {
                var gs = m.getGradoSeccion();
                // No depende del apoderado: una matrícula antigua sigue apuntando a quien la hizo.
                return !AccesoConstants.ELIMINADO.equals(gs.getAcceso().getIdAcceso())
                        && (idAnio == null || idAnio.equals(gs.getAnioEscolar().getIdAnio()))
                        && (idNivel == null || idNivel.equals(gs.getGrado().getNivel().getIdNivel()))
                        && (idGrado == null || idGrado.equals(gs.getGrado().getIdGrado()))
                        && (idSeccion == null || (gs.getSeccion() != null && idSeccion.equals(gs.getSeccion().getIdSeccion())))
                        && (idTurno == null || idTurno.equals(gs.getTurno().getIdTurno()))
                        && (idGradoSeccion == null || idGradoSeccion.equals(gs.getIdGradoSeccion()));
            };
            if (esGestion) {
                Page<Alumno> page = alumnoRepository.findFiltrados(
                        AccesoConstants.ACTIVO, AccesoConstants.ELIMINADO,
                        idNivel, idGrado, idSeccion, idTurno, idGradoSeccion, idAnio, pageable);
                return new PageImpl<>(toResponseEnriquecidoBatch(page.getContent(), contexto), pageable, page.getTotalElements());
            } else {
                // Docente/Apoderado: filtra en BD, luego por visibilidad y paginación en memoria
                List<Alumno> filtrados = alumnoRepository.findFiltradosList(
                        AccesoConstants.ACTIVO, AccesoConstants.ELIMINADO,
                        idNivel, idGrado, idSeccion, idTurno, idGradoSeccion, idAnio);
                List<Alumno> visiblesList = filtrados.stream()
                        .filter(a -> visibles.contains(a.getIdAlumno()))
                        .toList();
                int start = (int) Math.min(pageable.getOffset(), visiblesList.size());
                int end = Math.min(start + pageable.getPageSize(), visiblesList.size());
                return new PageImpl<>(toResponseEnriquecidoBatch(visiblesList.subList(start, end), contexto), pageable, visiblesList.size());
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
        return toResponseEnriquecidoBatch(alumnos, m -> true);
    }

    private List<AlumnoResponse> toResponseEnriquecidoBatch(List<Alumno> alumnos, java.util.function.Predicate<Matricula> contexto) {
        if (alumnos.isEmpty()) return List.of();
        List<Integer> ids = alumnos.stream().map(Alumno::getIdAlumno).toList();
        List<Matricula> matriculas = matriculaRepository.findActivasPorAlumnoIds(ids,
                accesoRepository.findById(AccesoConstants.ACTIVO).orElseThrow());
        // Con filtros se muestra la matrícula del contexto consultado; sin filtros, la actual.
        java.util.Map<Integer, Matricula> mapa = new java.util.HashMap<>();
        matriculas.stream()
                .filter(contexto)
                .collect(java.util.stream.Collectors.groupingBy(m -> m.getAlumnoApoderado().getAlumno().getIdAlumno()))
                .forEach((id, delAlumno) -> AccesoContextoService.elegirActual(delAlumno).ifPresent(m -> mapa.put(id, m)));
        boolean verApoderados = puedeVerApoderados();
        return alumnos.stream().map(a -> {
            List<AlumnoApoderado> vinculos = verApoderados ? alumnoApoderadoRepository.findByAlumno(a) : List.of();
            Matricula vigente = mapa.get(a.getIdAlumno());
            return AlumnoResponse.fromEntity(a, vinculos, vigente);
        }).toList();
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
    /** Padrón de un año: una fila por matrícula activa de ese año. */
    public List<AlumnoReporteResponse> reporte(Integer idAnio, Integer idNivel, Integer idGrado,
            Integer idGradoSeccion, Integer idUsuario, List<String> roles) {
        Set<Integer> visibles = idsAlumnosVisibles(idUsuario, roles);
        boolean verApoderados = puedeVerApoderados();
        return matriculaRepository.findFiltradasList(AccesoConstants.ELIMINADO, idNivel, idGrado, null, null,
                        idGradoSeccion, idAnio, AccesoConstants.ACTIVO).stream()
                .filter(m -> !AccesoConstants.ELIMINADO.equals(
                        m.getAlumnoApoderado().getAlumno().getAcceso().getIdAcceso()))
                .filter(m -> visibles == null || visibles.contains(m.getAlumnoApoderado().getAlumno().getIdAlumno()))
                .sorted(MatriculaService.ORDEN_REPORTE)
                .map(m -> aReporte(m, verApoderados))
                .toList();
    }

    private AlumnoReporteResponse aReporte(Matricula m, boolean verApoderados) {
        Alumno alumno = m.getAlumnoApoderado().getAlumno();
        AlumnoReporteResponse r = new AlumnoReporteResponse();
        r.setIdAlumno(alumno.getIdAlumno());
        r.setNombre(alumno.getNombre() + " " + alumno.getApellidoPat() + " " + alumno.getApellidoMat());
        r.setCodigo(alumno.getCodigo());
        r.setDocumentoIdentidad(alumno.getDocumentoIdentidad());
        r.setFechaNacimiento(alumno.getFechaNacimiento());
        r.setFechaIngreso(alumno.getFechaIngreso());
        r.setAnio(m.getGradoSeccion().getAnioEscolar().getAnio());
        r.setNivel(m.getGradoSeccion().getGrado().getNivel().getNombre());
        r.setGradoSeccion(MatriculaService.gradoYSeccion(m.getGradoSeccion()));
        r.setTurno(m.getGradoSeccion().getTurno().getNombre());
        if (verApoderados) {
            r.setApoderados(alumnoApoderadoRepository.findByAlumno(alumno).stream()
                    .sorted(Comparator.comparing(AlumnoApoderado::getApoPrincipal).reversed())
                    .map(AlumnoService::textoApoderado)
                    .toList());
        }
        return r;
    }

    /** "BETTY CASTILLO RODRIGUEZ (Madre) 987654321" */
    private static String textoApoderado(AlumnoApoderado vinculo) {
        var usuario = vinculo.getApoderado().getUsuario();
        String nombre = usuario.getNombre() + " " + usuario.getApellidoPat() + " " + usuario.getApellidoMat();
        if (vinculo.getParentesco() != null) {
            nombre += " (" + vinculo.getParentesco().getNombre() + ")";
        }
        String celular = vinculo.getApoderado().getCelular();
        return celular != null && !celular.isBlank() ? nombre + " " + celular : nombre;
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

    // Son datos personales: solo llegan a quien tiene el permiso de Apoderados.
    private boolean puedeVerApoderados() {
        return accesoContextoService.tienePermiso("APODERADOS",
                accesoContextoService.idUsuarioAutenticado(), accesoContextoService.rolesAutenticados());
    }

    private AlumnoResponse toResponse(Alumno alumno) {
        List<AlumnoApoderado> vinculos = puedeVerApoderados() ? alumnoApoderadoRepository.findByAlumno(alumno) : List.of();
        List<Matricula> vigente = matriculaRepository.findActivasPorAlumnoIds(
                List.of(alumno.getIdAlumno()), accesoRepository.findById(AccesoConstants.ACTIVO).orElseThrow());
        Matricula m = AccesoContextoService.elegirActual(vigente).orElse(null);
        return AlumnoResponse.fromEntity(alumno, vinculos, m);
    }
}
