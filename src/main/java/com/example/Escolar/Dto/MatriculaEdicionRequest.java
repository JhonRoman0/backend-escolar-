package com.example.Escolar.Dto;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Edición de una matrícula. En un año por comenzar puede cambiar la sección (y
 * el grado); si cambia de nivel, el método y el código son los de la diferencia
 * o la devolución. En un año vigente solo cambian las observaciones.
 */
@Getter
@Setter
public class MatriculaEdicionRequest {
    private Integer idGradoSeccion;
    private Integer idMetodoPago;
    @Size(max = 60, message = "El código de transacción no puede pasar de 60 caracteres")
    private String codigoTransaccion;
    @Size(max = 500, message = "Las observaciones no pueden pasar de 500 caracteres")
    private String observaciones;
}
