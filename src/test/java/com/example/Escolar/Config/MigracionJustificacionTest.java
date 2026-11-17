package com.example.Escolar.Config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MigracionJustificacionTest {
    @Mock JdbcTemplate jdbc;
    @InjectMocks MigracionJustificacion migracion;

    private void conColumnas(String... columnas) {
        when(jdbc.queryForList(startsWith("SELECT table_name"), eq(String.class))).thenReturn(List.of("justificacion", "alumno"));
        when(jdbc.queryForList(startsWith("SELECT column_name"), eq(String.class), eq("justificacion"))).thenReturn(List.of(columnas));
    }

    @Test
    void copiaLaFechaYBorraLasColumnasViejas() {
        conColumnas("id_justificacion", "motivo", "documento_url", "fecha_justificacion", "fecha_registro", "id_acceso");

        migracion.migrar();

        // Primero copia, luego borra: no se pierde ninguna fecha.
        var orden = inOrder(jdbc);
        orden.verify(jdbc).update(contains("SET fecha_registro = fecha_justificacion"));
        orden.verify(jdbc).execute(contains("DROP COLUMN fecha_justificacion"));
        orden.verify(jdbc).execute(contains("MODIFY fecha_registro DATE NOT NULL"));
        verify(jdbc).execute(contains("DROP COLUMN documento_url"));
    }

    @Test
    void siYaSeMigroNoHaceNada() {
        conColumnas("id_justificacion", "motivo", "fecha_registro", "id_acceso");

        migracion.migrar();

        verify(jdbc, never()).update(anyString());
        verify(jdbc, never()).execute(anyString());
    }

    @Test
    void unErrorNoImpideElArranque() {
        when(jdbc.queryForList(anyString(), eq(String.class))).thenThrow(new RuntimeException("sin base"));
        assertDoesNotThrow(() -> migracion.migrar());
    }
}
