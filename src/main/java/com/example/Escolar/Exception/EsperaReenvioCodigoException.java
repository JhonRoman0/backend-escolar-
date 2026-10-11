package com.example.Escolar.Exception;

import lombok.Getter;

@Getter
public class EsperaReenvioCodigoException extends RuntimeException {
    private final long segundosRestantes;

    public EsperaReenvioCodigoException(long segundosRestantes) {
        super("Espera " + segundosRestantes + " segundos para solicitar un nuevo código.");
        this.segundosRestantes = segundosRestantes;
    }
}
