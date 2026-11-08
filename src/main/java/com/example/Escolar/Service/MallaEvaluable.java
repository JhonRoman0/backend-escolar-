package com.example.Escolar.Service;

import com.example.Escolar.Model.*;
import com.example.Escolar.Repository.AccesoRepository;
import com.example.Escolar.Repository.CompetenciaRepository;
import com.example.Escolar.Repository.CursoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

/**
 * Áreas y transversales que se califican en un nivel y año, con sus competencias
 * activas, en el orden del SIAGIE: primero las áreas por código, al final las
 * transversales. La usan la exportación al SIAGIE y la boleta.
 */
@Component
@RequiredArgsConstructor
public class MallaEvaluable {

    private final CursoRepository cursoRepository;
    private final CompetenciaRepository competenciaRepository;
    private final AccesoRepository accesoRepository;

    public record Area(Curso curso, List<Competencia> competencias) {
    }

    public List<Area> areas(AnioEscolar anio, Nivel nivel) {
        Acceso eliminado = accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow();
        return cursoRepository.findByAnioEscolarIdAnioAndNivelIdNivelAndAccesoNot(anio.getIdAnio(), nivel.getIdNivel(), eliminado)
                .stream()
                .filter(c -> c.getTipo() != TipoCurso.SUBCURSO && AccesoConstants.ACTIVO.equals(c.getAcceso().getIdAcceso()))
                .sorted(Comparator.comparing((Curso c) -> c.getTipo() == TipoCurso.TRANSVERSAL)
                        .thenComparing(c -> c.getCodigo() == null ? "~" + c.getNombre() : c.getCodigo()))
                .map(c -> new Area(c, competenciaRepository.findByCursoIdCursoAndAccesoNot(c.getIdCurso(), eliminado).stream()
                        .filter(x -> AccesoConstants.ACTIVO.equals(x.getAcceso().getIdAcceso()))
                        .sorted(Comparator.comparing(Competencia::getOrden))
                        .toList()))
                .filter(a -> !a.competencias().isEmpty())
                .toList();
    }
}
