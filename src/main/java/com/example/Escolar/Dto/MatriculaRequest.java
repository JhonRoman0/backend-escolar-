package com.example.Escolar.Dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class MatriculaRequest {
    @NotNull(message = "El alumno es obligatorio")
    private Integer idAlumno;
    @NotNull(message = "El grado-sección es obligatorio")
    private Integer idGradoSeccion;
    @NotNull(message = "El estado de la solicitud es obligatorio")
    @Min(value = 1, message = "La solicitud debe ser 1 (pendiente), 2 (aprobada) o 3 (rechazada)")
    @Max(value = 3, message = "La solicitud debe ser 1 (pendiente), 2 (aprobada) o 3 (rechazada)")
    private Byte solicitudMatricula;
    private LocalDate fechaPago;
    @DecimalMin(value = "0.0", message = "El monto de pago no puede ser negativo")
    private BigDecimal montoPago;
    @Min(value = 0, message = "El acceso debe ser 0 (inactivo), 1 (activo) o 2 (eliminado)")
    @Max(value = 2, message = "El acceso debe ser 0 (inactivo), 1 (activo) o 2 (eliminado)")
    private Byte acceso;
}