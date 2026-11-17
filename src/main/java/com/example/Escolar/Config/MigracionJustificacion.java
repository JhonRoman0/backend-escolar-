package com.example.Escolar.Config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Motivos de justificación: se quitó el documento (URL) y la fecha pasó a ser
 * la de registro. Hibernate agrega fecha_registro al arrancar, pero no borra
 * ni copia columnas: esto pasa las fechas, borra las columnas viejas y deja
 * fecha_registro obligatoria. Si ya se hizo, no hace nada.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MigracionJustificacion {

    private final JdbcTemplate jdbc;

    public void migrar() {
        try {
            String tabla = nombreReal("justificacion");
            if (tabla == null) {
                return;
            }
            List<String> columnas = jdbc.queryForList(
                    "SELECT column_name FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = ?",
                    String.class, tabla).stream().map(String::toLowerCase).toList();
            if (columnas.contains("fecha_justificacion")) {
                jdbc.update("UPDATE `" + tabla + "` SET fecha_registro = fecha_justificacion WHERE fecha_registro IS NULL");
                jdbc.execute("ALTER TABLE `" + tabla + "` DROP COLUMN fecha_justificacion");
                jdbc.execute("ALTER TABLE `" + tabla + "` MODIFY fecha_registro DATE NOT NULL");
                log.info("Motivos de justificación: la fecha pasó a fecha_registro");
            }
            if (columnas.contains("documento_url")) {
                jdbc.execute("ALTER TABLE `" + tabla + "` DROP COLUMN documento_url");
                log.info("Motivos de justificación: se quitó documento_url");
            }
        } catch (RuntimeException e) {
            // Nunca impide el arranque: se reintenta la próxima vez.
            log.error("Motivos de justificación: no se pudo migrar", e);
        }
    }

    /** En Linux MySQL distingue mayúsculas en el nombre de la tabla. */
    private String nombreReal(String tabla) {
        return jdbc.queryForList("SELECT table_name FROM information_schema.tables WHERE table_schema = DATABASE()", String.class)
                .stream().filter(t -> t.equalsIgnoreCase(tabla)).findFirst().orElse(null);
    }
}
