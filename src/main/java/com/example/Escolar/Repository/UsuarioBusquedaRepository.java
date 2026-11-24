package com.example.Escolar.Repository;

import com.example.Escolar.Model.Usuario;
import com.example.Escolar.Service.VigenciaService.EstadoVigencia;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

public interface UsuarioBusquedaRepository {
    Page<Usuario> buscar(
            Long eliminadoId,
            LocalDate inicio,
            LocalDate fin,
            List<String> tokens,
            Integer idRol,
            Long idAcceso,
            EstadoVigencia estadoVigencia,
            LocalDate hoy,
            Pageable pageable);
}
