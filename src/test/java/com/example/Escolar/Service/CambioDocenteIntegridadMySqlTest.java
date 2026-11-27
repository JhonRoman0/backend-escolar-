package com.example.Escolar.Service;

import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class CambioDocenteIntegridadMySqlTest {
    @Test
    void reproduceErrorLegacyYVerificaCreacionConDosAsignaciones() throws Exception {
        String url = System.getenv("CAMBIO_TEST_MYSQL_URL");
        assumeTrue(url != null && url.startsWith("jdbc:mysql:"),
                "Falta CAMBIO_TEST_MYSQL_URL: integridad MySQL no verificada");
        String usuario = System.getenv("CAMBIO_TEST_MYSQL_USER");
        String clave = System.getenv("CAMBIO_TEST_MYSQL_PASSWORD");
        String sufijo = UUID.randomUUID().toString().replace("-", "");
        String asignaciones = "cambio_asignacion_" + sufijo;
        String cambios = "cambio_docente_" + sufijo;
        String detalles = "cambio_detalle_" + sufijo;
        String fkAsignacion = "fk_cambio_asignacion_" + sufijo;
        String fkCambio = "fk_detalle_cambio_" + sufijo;
        String ukDetalle = "uk_cambio_asignacion_" + sufijo;

        try (Connection conexion = DriverManager.getConnection(url, usuario, clave)) {
            String catalogo = conexion.getCatalog();
            assumeTrue(catalogo != null && catalogo.endsWith("_test"),
                    "La base aislada debe terminar en _test; no se crearon tablas");
            try (Statement statement = conexion.createStatement()) {
                statement.execute("CREATE TABLE " + asignaciones + " ("
                        + "id_asignacion INT NOT NULL PRIMARY KEY, id_docente INT NOT NULL) ENGINE=InnoDB");
                try {
                    statement.execute("INSERT INTO " + asignaciones + " (id_asignacion, id_docente) VALUES "
                            + "(100, 1), (200, 1)");
                    statement.execute("CREATE TABLE " + cambios + " ("
                            + "id_cambio INT NOT NULL AUTO_INCREMENT, "
                            + "id_asignacion INT NOT NULL, "
                            + "fecha_inicio DATE NOT NULL, "
                            + "tipo VARCHAR(15) NOT NULL, "
                            + "PRIMARY KEY (id_cambio), "
                            + "CONSTRAINT " + fkAsignacion + " FOREIGN KEY (id_asignacion) "
                            + "REFERENCES " + asignaciones + " (id_asignacion)) ENGINE=InnoDB");
                    try {
                        statement.execute("CREATE TABLE " + detalles + " ("
                                + "id_detalle INT NOT NULL AUTO_INCREMENT, "
                                + "id_cambio INT NOT NULL, "
                                + "id_asignacion INT NOT NULL, "
                                + "PRIMARY KEY (id_detalle), "
                                + "CONSTRAINT " + ukDetalle + " UNIQUE (id_cambio, id_asignacion), "
                                + "CONSTRAINT " + fkCambio + " FOREIGN KEY (id_cambio) "
                                + "REFERENCES " + cambios + " (id_cambio), "
                                + "FOREIGN KEY (id_asignacion) REFERENCES " + asignaciones
                                + " (id_asignacion)) ENGINE=InnoDB");
                        try {
                            SQLException ex = null;
                            try (PreparedStatement insert = conexion.prepareStatement(
                                    "INSERT INTO " + cambios + " (fecha_inicio, tipo) VALUES (?, ?)")) {
                                insert.setDate(1, java.sql.Date.valueOf("2026-11-26"));
                                insert.setString(2, "TEMPORAL");
                                insert.executeUpdate();
                            } catch (SQLException error) {
                                ex = error;
                            }
                            assertNotNull(ex, "El esquema legacy debía rechazar el registro");
                            assertEquals("HY000", ex.getSQLState());
                            assertEquals(1364, ex.getErrorCode());
                            assertTrue(ex.getMessage().contains("id_asignacion"));

                            statement.execute("ALTER TABLE " + cambios + " DROP FOREIGN KEY " + fkAsignacion);
                            statement.execute("ALTER TABLE " + cambios + " DROP COLUMN id_asignacion");

                            Integer idCambio;
                            try (PreparedStatement insert = conexion.prepareStatement(
                                    "INSERT INTO " + cambios + " (fecha_inicio, tipo) VALUES (?, ?)",
                                    Statement.RETURN_GENERATED_KEYS)) {
                                insert.setDate(1, java.sql.Date.valueOf("2026-11-26"));
                                insert.setString(2, "TEMPORAL");
                                insert.executeUpdate();
                                try (ResultSet claves = insert.getGeneratedKeys()) {
                                    assertTrue(claves.next());
                                    idCambio = claves.getInt(1);
                                }
                            }
                            assertNotNull(idCambio);

                            for (Integer idAsignacion : new Integer[]{100, 200}) {
                                try (PreparedStatement insert = conexion.prepareStatement(
                                        "INSERT INTO " + detalles + " (id_cambio, id_asignacion) VALUES (?, ?)")) {
                                    insert.setInt(1, idCambio);
                                    insert.setInt(2, idAsignacion);
                                    insert.executeUpdate();
                                }
                            }
                            try (PreparedStatement update = conexion.prepareStatement(
                                    "UPDATE " + asignaciones + " SET id_docente = 5 WHERE id_docente = 1")) {
                                assertEquals(2, update.executeUpdate());
                            }
                            try (ResultSet resultado = statement.executeQuery(
                                    "SELECT COUNT(*) FROM " + detalles + " WHERE id_cambio = " + idCambio)) {
                                assertTrue(resultado.next());
                                assertEquals(2, resultado.getInt(1));
                            }
                        } finally {
                            statement.execute("DROP TABLE " + detalles);
                        }
                    } finally {
                        statement.execute("DROP TABLE " + cambios);
                    }
                } finally {
                    statement.execute("DROP TABLE " + asignaciones);
                }
            }
        }
    }
}
