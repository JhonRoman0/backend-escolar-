package com.example.Escolar.Service;

import org.junit.jupiter.api.Test;
import java.sql.*;
import java.util.UUID;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/** Opt-in: solo una base aislada cuyo nombre termine en _test. No usa la configuración de la aplicación. */
class ExclusividadDniMySqlTest {
    @Test
    void dosAltasCruzadasNoPuedenConfirmarElMismoDni() throws Exception {
        String url = System.getenv("DNI_TEST_MYSQL_URL");
        assumeTrue(url != null && url.startsWith("jdbc:mysql:"), "Falta DNI_TEST_MYSQL_URL: concurrencia MySQL no verificada");
        String usuario = System.getenv("DNI_TEST_MYSQL_USER");
        String clave = System.getenv("DNI_TEST_MYSQL_PASSWORD");
        String sufijo = UUID.randomUUID().toString().replace("-", "");
        String alumnos = "dni_alumno_" + sufijo;
        String usuarios = "dni_usuario_" + sufijo;
        try (Connection conexion = DriverManager.getConnection(url, usuario, clave)) {
            assertTrue(conexion.getCatalog() != null && conexion.getCatalog().endsWith("_test"),
                    "La base aislada debe terminar en _test; no se crearon tablas");
            try (Statement statement = conexion.createStatement()) {
                statement.execute("CREATE TABLE " + alumnos + " (dni VARCHAR(8) PRIMARY KEY) ENGINE=InnoDB");
                try {
                    statement.execute("CREATE TABLE " + usuarios + " (dni VARCHAR(8) PRIMARY KEY) ENGINE=InnoDB");
                    try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
                        CyclicBarrier barrera = new CyclicBarrier(2);
                        Future<Boolean> alumno = executor.submit(() -> insertar(url, usuario, clave, alumnos, usuarios, barrera));
                        Future<Boolean> cuenta = executor.submit(() -> insertar(url, usuario, clave, usuarios, alumnos, barrera));
                        assertNotEquals(alumno.get(30, TimeUnit.SECONDS), cuenta.get(30, TimeUnit.SECONDS));
                        try (ResultSet resultado = statement.executeQuery("SELECT (SELECT COUNT(*) FROM " + alumnos
                                + ") + (SELECT COUNT(*) FROM " + usuarios + ")")) {
                            assertTrue(resultado.next());
                            assertEquals(1, resultado.getInt(1));
                        }
                    } finally {
                        statement.execute("DROP TABLE " + usuarios);
                    }
                } finally {
                    statement.execute("DROP TABLE " + alumnos);
                }
            }
        }
    }

    private boolean insertar(String url, String usuario, String clave, String destino, String opuesto,
                             CyclicBarrier barrera) throws Exception {
        try (Connection conexion = DriverManager.getConnection(url, usuario, clave)) {
            conexion.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
            conexion.setAutoCommit(false);
            try (Statement statement = conexion.createStatement()) {
                statement.execute("SET SESSION innodb_lock_wait_timeout=5");
                try (ResultSet resultado = statement.executeQuery("SELECT dni FROM " + opuesto + " WHERE dni='12345678'")) {
                    assertFalse(resultado.next());
                }
                barrera.await(10, TimeUnit.SECONDS);
                statement.executeUpdate("INSERT INTO " + destino + " (dni) VALUES ('12345678')");
                conexion.commit();
                return true;
            } catch (SQLException ex) {
                conexion.rollback();
                if ("40001".equals(ex.getSQLState()) || ex.getErrorCode() == 1213 || ex.getErrorCode() == 1205) return false;
                throw ex;
            }
        }
    }
}
