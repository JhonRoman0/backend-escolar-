package com.example.Escolar.Dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CursoImportResponse {
    private int totalDetectados;
    private int creados;
    private int actualizados;
    private List<CursoResponse> cursos;
}
