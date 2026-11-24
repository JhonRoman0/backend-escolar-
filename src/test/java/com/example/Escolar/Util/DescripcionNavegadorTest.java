package com.example.Escolar.Util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DescripcionNavegadorTest {

    @Test
    void reconoceLosNavegadoresComunes() {
        assertEquals("Chrome en Windows", DescripcionNavegador.de(
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0 Safari/537.36"));
        assertEquals("Edge en Windows", DescripcionNavegador.de(
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0 Safari/537.36 Edg/131.0"));
        assertEquals("Safari en iPhone", DescripcionNavegador.de(
                "Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.0 Mobile/15E148 Safari/604.1"));
        assertEquals("Chrome en Android", DescripcionNavegador.de(
                "Mozilla/5.0 (Linux; Android 14; SM-A145M) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0 Mobile Safari/537.36"));
        assertEquals("Firefox en Mac", DescripcionNavegador.de(
                "Mozilla/5.0 (Macintosh; Intel Mac OS X 14.0; rv:132.0) Gecko/20100101 Firefox/132.0"));
        assertEquals("Navegador desconocido", DescripcionNavegador.de(null));
    }
}
