package com.example.Escolar.Dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReniecResponse {
    private String dni;
    private String nombres;
    @JsonAlias({"apellido_paterno", "apellidoPaterno"})
    private String apellidoPaterno;
    @JsonAlias({"apellido_materno", "apellidoMaterno"})
    private String apellidoMaterno;
    private String codVerifica;
    private String origen; // LOCAL, CACHE, RENIEC

    @JsonIgnore
    private String nombreCompleto;

    @JsonIgnore
    public String getNombreCompleto() {
        if (nombreCompleto != null) return nombreCompleto;
        StringBuilder sb = new StringBuilder();
        if (nombres != null) sb.append(nombres);
        if (apellidoPaterno != null) sb.append(" ").append(apellidoPaterno);
        if (apellidoMaterno != null) sb.append(" ").append(apellidoMaterno);
        return sb.toString().trim();
    }
}
