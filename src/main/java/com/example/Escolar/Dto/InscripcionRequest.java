package com.example.Escolar.Dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/** Datos del asistente de matrícula. Para validar el paso 2 se envía sin matrícula. */
@Getter
@Setter
public class InscripcionRequest {
    @NotNull(message = "Los datos del alumno son obligatorios")
    @Valid
    private InscripcionAlumnoRequest alumno;
    @NotNull(message = "Indica al menos un apoderado")
    @Size(min = 1, max = 2, message = "El alumno debe tener uno o dos apoderados")
    @Valid
    private List<InscripcionApoderadoRequest> apoderados;
    @Valid
    private DatosMatricula matricula;

    @Getter
    @Setter
    public static class DatosMatricula {
        @NotNull(message = "La sección es obligatoria")
        private Integer idGradoSeccion;
        @NotNull(message = "El método de pago es obligatorio")
        private Integer idMetodoPago;
        @Size(max = 60, message = "El código de transacción no puede pasar de 60 caracteres")
        private String codigoTransaccion;
        @Size(max = 500, message = "Las observaciones no pueden pasar de 500 caracteres")
        private String observaciones;
    }
}
