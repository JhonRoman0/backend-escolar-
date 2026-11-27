package com.example.Escolar.Model;

import java.util.List;

public enum EstadoCambioDocente {
    PROGRAMADO,
    VIGENTE,
    FINALIZADO,
    APLICADO,
    CANCELADO;

    public static final List<EstadoCambioDocente> ACTIVOS =
            List.of(PROGRAMADO, VIGENTE);
}
