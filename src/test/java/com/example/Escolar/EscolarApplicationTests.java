package com.example.Escolar;

import com.example.Escolar.Config.DataSeeder;
import com.example.Escolar.Model.Acceso;
import com.example.Escolar.Model.Usuario;
import com.example.Escolar.Repository.AccesoRepository;
import com.example.Escolar.Repository.UsuarioRepository;
import com.example.Escolar.Security.ConcesionesService;
import com.example.Escolar.Service.AccesoConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDate;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.boot.test.context.SpringBootTest;

@EnabledIfEnvironmentVariable(named = "ESCOLAR_TEST_MYSQL_URL",
        matches = "jdbc:mysql://127\\.0\\.0\\.1:[0-9]+/[a-zA-Z0-9_]+_test(?:\\?.*)?")
@SpringBootTest(properties = {
        "spring.datasource.url=${ESCOLAR_TEST_MYSQL_URL}",
        "spring.datasource.username=${ESCOLAR_TEST_MYSQL_USER}",
        "spring.datasource.password=${ESCOLAR_TEST_MYSQL_PASSWORD:}",
        "spring.jpa.hibernate.ddl-auto=create"
})
class EscolarApplicationTests {

	@MockitoBean
	private DataSeeder dataSeeder;

	@Test
	void contextLoads() {
	}

    @Autowired private AccesoRepository accesos;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private UsuarioRepository usuarios;
    @Autowired private ConcesionesService concesiones;
    @Autowired private TransactionTemplate transacciones;

    @Test
    void versionSoloCambiaDespuesDelCommitYParaLosAfectados() {
        jdbc.update("insert into acceso (id_acceso, nombre) values (?, ?)", AccesoConstants.ACTIVO, "Activo");
        Acceso activo = accesos.findById(AccesoConstants.ACTIVO).orElseThrow();
        Integer afectado = usuario("TEST-RBAC1", activo);
        Integer ajeno = usuario("TEST-RBAC2", activo);

        transacciones.executeWithoutResult(tx -> {
            concesiones.invalidarVinculoDespuesDeCommit(Set.of(afectado));
            assertEquals(0, version(afectado));
            tx.setRollbackOnly();
        });
        assertEquals(0, version(afectado));

        transacciones.executeWithoutResult(tx -> {
            concesiones.invalidarVinculoDespuesDeCommit(Set.of(afectado));
            concesiones.invalidarVinculoDespuesDeCommit(Set.of(afectado));
            assertEquals(0, version(afectado));
        });
        assertEquals(1, version(afectado));
        assertEquals(0, version(ajeno));
    }

    private Integer usuario(String codigo, Acceso activo) {
        Usuario usuario = new Usuario();
        usuario.setCodigo(codigo);
        usuario.setNombre("Prueba");
        usuario.setApellidoPat("RBAC");
        usuario.setApellidoMat("RBAC");
        usuario.setAcceso(activo);
        usuario.setContraseña("sin acceso interactivo");
        usuario.setFechaNaci(LocalDate.of(1990, 1, 1));
        usuario.setFechaCreacion(LocalDate.now());
        usuario.setFechaInicioVigencia(LocalDate.now());
        return usuarios.save(usuario).getIdUsuario();
    }

    private int version(Integer id) {
        return usuarios.findById(id).orElseThrow().getVersionToken();
    }
}
