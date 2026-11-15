package com.example.Escolar.Dto;
import jakarta.validation.constraints.*;
public record AnulacionReconocimientoRequest(@NotBlank @Size(max = com.example.Escolar.Model.ReconocimientoCalificacion.MOTIVO_MAXIMO) String motivo) {}
