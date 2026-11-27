package com.example.Escolar.Dto;

public record MotivoCambioDocenteResponse(
        Integer idMotivoCambioDocente,
        String nombre,
        Boolean aplicaTemporal,
        Boolean aplicaDefinitivo,
        Boolean activo
) {
}
