package com.example.Escolar.Dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AjusteResponse {
    private Integer idAjuste;
    private String clave;
    private String valor;
    private Long accesoId;
}