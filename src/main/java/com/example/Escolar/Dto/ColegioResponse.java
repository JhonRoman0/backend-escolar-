package com.example.Escolar.Dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ColegioResponse {
    private Integer idColegio;
    private String nombre;
    private String celular;
    private String telefono;
    private String direccion;
    private String codigoColegio;
    private String urlFoto;
    private String urlPortal;
    private Byte acceso;
}