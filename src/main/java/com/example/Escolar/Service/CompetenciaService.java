package com.example.Escolar.Service;

import com.example.Escolar.Dto.CompetenciaResponse;
import com.example.Escolar.Exception.ResourceNotFoundException;
import com.example.Escolar.Model.Acceso;
import com.example.Escolar.Model.Competencia;
import com.example.Escolar.Model.Curso;
import com.example.Escolar.Model.TipoCurso;
import com.example.Escolar.Repository.AccesoRepository;
import com.example.Escolar.Repository.CompetenciaRepository;
import com.example.Escolar.Repository.CursoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;

/**
 * Las competencias vienen del Excel del SIAGIE: aqui solo se consultan y se
 * activan o desactivan, con el mismo bloqueo por año que los cursos.
 */
@Service
@RequiredArgsConstructor
public class CompetenciaService {

    private final CompetenciaRepository competenciaRepository;
    private final CursoRepository cursoRepository;
    private final AccesoRepository accesoRepository;
    private final CursoService cursoService;

    /** De un subcurso devuelve las competencias del area que evalua. */
    @Transactional(readOnly = true)
    public List<CompetenciaResponse> listar(Integer idCurso) {
        Acceso eliminado = accesoEliminado();
        if (idCurso == null) {
            return ordenar(competenciaRepository.findByAccesoNot(eliminado));
        }
        Curso curso = cursoRepository.findByIdCursoAndAccesoNot(idCurso, eliminado)
                .orElseThrow(() -> new ResourceNotFoundException("Curso no encontrado con id " + idCurso));
        if (curso.getTipo() == TipoCurso.SUBCURSO) {
            return ordenar(curso.getCompetencias().stream()
                    .filter(c -> !AccesoConstants.ELIMINADO.equals(c.getAcceso().getIdAcceso()))
                    .toList());
        }
        return ordenar(competenciaRepository.findByCursoIdCursoAndAccesoNot(idCurso, eliminado));
    }

    @Transactional
    public CompetenciaResponse cambiarEstado(Integer id, Long nuevoAccesoId) {
        Competencia competencia = competenciaRepository.findByIdCompetenciaAndAccesoNot(id, accesoEliminado())
                .orElseThrow(() -> new ResourceNotFoundException("Competencia no encontrada con id " + id));
        cursoService.validarConfigurable(competencia.getCurso().getAnioEscolar());
        if (!AccesoConstants.ACTIVO.equals(nuevoAccesoId) && !AccesoConstants.INACTIVO.equals(nuevoAccesoId)) {
            throw new IllegalArgumentException("El estado debe ser activo o inactivo");
        }
        competencia.setAcceso(accesoRepository.findById(nuevoAccesoId).orElseThrow());
        return toResponse(competenciaRepository.save(competencia));
    }

    private List<CompetenciaResponse> ordenar(Collection<Competencia> competencias) {
        return competencias.stream()
                .sorted(Comparator.comparing((Competencia c) -> c.getCurso().getIdCurso())
                        .thenComparing(Competencia::getOrden))
                .map(this::toResponse)
                .toList();
    }

    private Acceso accesoEliminado() {
        return accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow();
    }

    private CompetenciaResponse toResponse(Competencia competencia) {
        CompetenciaResponse response = new CompetenciaResponse();
        response.setIdCompetencia(competencia.getIdCompetencia());
        response.setCodigo(competencia.getCodigo());
        response.setCompetencia(competencia.getNombre());
        response.setIdCurso(competencia.getCurso().getIdCurso());
        response.setCurso(competencia.getCurso().getNombre());
        response.setOrden(competencia.getOrden());
        response.setAccesoId(competencia.getAcceso().getIdAcceso());
        return response;
    }
}
