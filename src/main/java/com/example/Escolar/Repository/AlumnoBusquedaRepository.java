package com.example.Escolar.Repository;

import com.example.Escolar.Model.Alumno;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Collection;
import java.util.List;

public interface AlumnoBusquedaRepository {
    Page<Alumno> buscar(
            List<String> tokens,
            Long accesoActivoId,
            Long eliminadoId,
            Integer idNivel,
            Integer idGrado,
            Integer idSeccion,
            Integer idTurno,
            Integer idGradoSeccion,
            Integer idAnio,
            Collection<Integer> idsVisibles,
            Pageable pageable);
}
