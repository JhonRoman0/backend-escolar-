package com.example.Escolar.Util;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TextoBusquedaTest {

    @Test
    void tokensNormalizaMayusculasTildesYEspacios() {
        assertEquals(List.of("lucia", "brav"), TextoBusqueda.tokens("  LUCÍA   Brav  "));
    }

    @Test
    void tokensDeTextoVacioSonVacios() {
        assertTrue(TextoBusqueda.tokens(null).isEmpty());
        assertTrue(TextoBusqueda.tokens("   ").isEmpty());
    }

    @Test
    void patronLikeEscapaComodinesYCaracterDeEscape() {
        assertEquals("%50!%a!_b!!%", TextoBusqueda.patronLike("50%a_b!"));
    }
}
