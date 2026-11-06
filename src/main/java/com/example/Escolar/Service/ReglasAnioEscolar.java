package com.example.Escolar.Service;

import com.example.Escolar.Model.AnioEscolar;

/**
 * Lo que se puede hacer con la configuracion de un año (recreos, asignaciones)
 * segun su estado. Un por comenzar se configura libremente; un vigente solo
 * admite la configuracion inicial (lo que todavia no existe) y ya no se edita
 * ni se borra; un cerrado es de solo lectura.
 */
final class ReglasAnioEscolar {

    private ReglasAnioEscolar() {
    }

    /** @param que lo que se configura, en plural: "recreos", "asignaciones". */
    static void validarCreable(AnioEscolar anio, boolean esConfiguracionInicial, String que) {
        if (anio.getEstado() == AnioEscolarService.ESTADO_POR_COMENZAR) {
            return;
        }
        if (anio.getEstado() == AnioEscolarService.ESTADO_VIGENTE && esConfiguracionInicial) {
            return;
        }
        throw new IllegalArgumentException("El año " + anio.getAnio() + " está " + estado(anio)
                + ": ya no se pueden crear " + que);
    }

    static void validarEditable(AnioEscolar anio, String que) {
        if (anio.getEstado() != AnioEscolarService.ESTADO_POR_COMENZAR) {
            throw new IllegalArgumentException("El año " + anio.getAnio() + " está " + estado(anio)
                    + ": sus " + que + " ya no se pueden editar ni eliminar");
        }
    }

    private static String estado(AnioEscolar anio) {
        return anio.getEstado() == AnioEscolarService.ESTADO_VIGENTE ? "vigente" : "cerrado";
    }
}
