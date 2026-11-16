package com.example.Escolar.Service;

import com.example.Escolar.Dto.NotaReporteResponse;
import com.example.Escolar.Dto.NotaResponse;
import com.example.Escolar.Exception.ResourceNotFoundException;
import com.example.Escolar.Model.Competencia;
import com.example.Escolar.Model.Curso;
import com.example.Escolar.Model.Docente;
import com.example.Escolar.Model.Matricula;
import com.example.Escolar.Model.Nota;
import com.example.Escolar.Model.Usuario;
import com.example.Escolar.Repository.AccesoRepository;
import com.example.Escolar.Repository.CompetenciaRepository;
import com.example.Escolar.Repository.MatriculaRepository;
import com.example.Escolar.Repository.NotaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class NotaService {

    private static final String PERMISO = "NOTAS";

    private final NotaRepository notaRepository;
    private final CompetenciaRepository competenciaRepository;
    private final MatriculaRepository matriculaRepository;
    private final AccesoContextoService accesoContextoService;
    private final AccesoRepository accesoRepository;

    public List<NotaResponse> getPorCompetencia(Integer idCompetencia, Byte bimestre, Integer idUsuario, List<String> roles) {
        Competencia competencia = findCompetencia(idCompetencia);
        List<Nota> notas;
        if (bimestre != null) {
            notas = notaRepository.findByCompetenciaIdCompetenciaAndBimestreAndAccesoNot(idCompetencia, bimestre, accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow());
        } else {
            notas = notaRepository.findByCompetenciaIdCompetenciaAndBimestreAndAccesoNot(idCompetencia, (byte) 0, accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow());
            if (notas.isEmpty()) {
                notas = notaRepository.findByCompetenciaIdCompetenciaAndBimestreAndAccesoNot(idCompetencia, (byte) 1, accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow());
            }
        }
        return notas.stream().map(this::toResponse).toList();
    }

    public List<NotaResponse> getPorMatricula(Integer idMatricula, Integer idUsuario, List<String> roles) {
        Matricula matricula = findMatricula(idMatricula);
        if (!esVisibleMatricula(matricula, idUsuario, roles)) {
            throw new ResourceNotFoundException("Matrícula no encontrada con id " + idMatricula);
        }
        return notaRepository.findByMatriculaIdMatriculaAndAccesoNot(idMatricula, accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow()).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<NotaReporteResponse> reporte(LocalDate inicio, LocalDate fin, Integer idGradoSeccion, Integer bimestre) {
        return notaRepository.findByFechaRegistroBetweenAndAccesoNot(inicio, fin, accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow()).stream()
                .filter(nota -> {
                    if (idGradoSeccion != null) {
                        Integer gsId = nota.getMatricula().getGradoSeccion().getIdGradoSeccion();
                        if (!idGradoSeccion.equals(gsId)) return false;
                    }
                    if (bimestre != null) {
                        if (bimestre != (int) nota.getBimestre()) return false;
                    }
                    return true;
                })
                .map(nota -> {
                    NotaReporteResponse r = new NotaReporteResponse();
                    r.setFecha(nota.getFechaRegistro());
                    r.setAlumno(nota.getMatricula().getAlumnoApoderado().getAlumno().getNombre() + " "
                            + nota.getMatricula().getAlumnoApoderado().getAlumno().getApellidoPat());
                    r.setCodigo(nota.getMatricula().getAlumnoApoderado().getAlumno().getCodigo());
                    r.setCurso(nota.getCompetencia().getCurso().getNombre());
                    r.setCompetencia(nota.getCompetencia().getNombre());
                    r.setBimestre((int) nota.getBimestre());
                    r.setCalificacion(nota.getCalificacion());
                    r.setDocente(nombreUsuario(nota.getUsuarioRegistro()));
                    return r;
                })
                .toList();
    }

    private String nombreUsuario(Usuario usuario) {
        return usuario.getNombre() + " " + usuario.getApellidoPat();
    }

    private boolean esVisibleMatricula(Matricula matricula, Integer idUsuario, List<String> roles) {
        return switch (accesoContextoService.alcance(PERMISO, idUsuario, roles)) {
            case TODO -> true;
            // Solo las matrículas de las secciones donde enseña.
            case DOCENTE -> accesoContextoService.gradoSeccionIdsDeDocente(idUsuario)
                    .contains(matricula.getGradoSeccion().getIdGradoSeccion());
            case APODERADO -> accesoContextoService.esApoderadoDe(idUsuario, matricula);
        };
    }

    private Matricula findMatricula(Integer id) {
        return matriculaRepository.findByIdMatriculaAndAccesoNot(id, accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow())
                .orElseThrow(() -> new ResourceNotFoundException("Matrícula no encontrada con id " + id));
    }

    private Competencia findCompetencia(Integer id) {
        return competenciaRepository.findByIdCompetenciaAndAccesoNot(id, accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow())
                .orElseThrow(() -> new ResourceNotFoundException("Competencia no encontrada con id " + id));
    }

    private NotaResponse toResponse(Nota nota) {
        Matricula matricula = nota.getMatricula();
        Competencia competencia = nota.getCompetencia();
        NotaResponse response = new NotaResponse();
        response.setIdNota(nota.getIdNota());
        response.setIdMatricula(matricula.getIdMatricula());
        response.setIdAlumno(matricula.getAlumnoApoderado().getAlumno().getIdAlumno());
        response.setCodigoAlumno(matricula.getAlumnoApoderado().getAlumno().getCodigo());
        response.setAlumno(matricula.getAlumnoApoderado().getAlumno().getNombre() + " "
                + matricula.getAlumnoApoderado().getAlumno().getApellidoPat());
        response.setIdCompetencia(competencia.getIdCompetencia());
        response.setCompetencia(competencia.getNombre());
        response.setIdCurso(competencia.getCurso().getIdCurso());
        response.setCurso(competencia.getCurso().getNombre());
        response.setBimestre(nota.getBimestre());
        response.setCalificacion(nota.getCalificacion());
        response.setConclusionDescriptiva(nota.getConclusionDescriptiva());
        response.setFechaRegistro(nota.getFechaRegistro());
        response.setDocenteRegistro(nombreUsuario(nota.getUsuarioRegistro()));
        response.setAccesoId(nota.getAcceso().getIdAcceso().longValue());
        return response;
    }
}
