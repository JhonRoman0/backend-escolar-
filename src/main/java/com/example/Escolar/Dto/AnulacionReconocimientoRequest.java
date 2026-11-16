package com.example.Escolar.Dto;

import com.example.Escolar.Model.ReconocimientoCalificacion;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AnulacionReconocimientoRequest(
        @NotBlank(message = "Escribe el motivo")
        @Size(max = ReconocimientoCalificacion.MOTIVO_MAXIMO, message = "El motivo no puede pasar de 500 caracteres") String motivo) {
}
