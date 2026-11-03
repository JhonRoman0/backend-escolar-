package com.example.Escolar.Config;

import com.example.Escolar.Model.*;
import com.example.Escolar.Repository.*;
import com.example.Escolar.Service.AccesoConstants;
import com.example.Escolar.Service.ExclusividadDniService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DataSeederExclusividadDniTest {
    @Mock private ExclusividadDniService exclusividadDni;
    @Mock private PlatformTransactionManager transactionManager;
    @Mock private UsuarioRepository usuarios;
    @Mock private UsuarioRolRepository rolesUsuario;
    @Mock private RolRepository roles;
    @Mock private AccesoRepository accesos;
    @Mock private PasswordEncoder passwordEncoder;
    @InjectMocks private DataSeeder seeder;
    private Usuario usuario;

    private Acceso acceso(Long id) {
        Acceso acceso = new Acceso();
        acceso.setIdAcceso(id);
        return acceso;
    }

    @BeforeEach
    void prepararCuenta() {
        when(transactionManager.getTransaction(any())).thenReturn(mock(TransactionStatus.class));
        ReflectionTestUtils.setField(seeder, "adminRol", "ADMIN");
        ReflectionTestUtils.setField(seeder, "adminCodigo", "ADMIN");
        ReflectionTestUtils.setField(seeder, "adminPassword", "temporal");
        Rol rol = new Rol();
        rol.setIdRol(1);
        when(roles.findByNombre("ADMIN")).thenReturn(Optional.of(rol));
        when(accesos.findById(AccesoConstants.ELIMINADO)).thenReturn(Optional.of(acceso(AccesoConstants.ELIMINADO)));
        usuario = new Usuario();
        usuario.setIdUsuario(1);
        usuario.setAcceso(acceso(AccesoConstants.ELIMINADO));
        usuario.setDocumentoIdentidad("12345678");
        usuario.setContraseña("anterior");
    }

    @Test
    void unConflictoNoReactivaLaCuentaNiImpideElArranque() {
        when(usuarios.findByCodigo("ADMIN")).thenReturn(Optional.of(usuario));
        doThrow(new IllegalArgumentException("DNI de alumno")).when(exclusividadDni).validarUsuario("12345678");
        assertDoesNotThrow(() -> seeder.seedAdmin());
        assertEquals(AccesoConstants.ELIMINADO, usuario.getAcceso().getIdAcceso());
        assertEquals("anterior", usuario.getContraseña());
        verify(usuarios, never()).save(any());
        verifyNoInteractions(passwordEncoder, rolesUsuario);
    }

    private void permitirReactivacion() {
        when(usuarios.findByCodigo("ADMIN")).thenReturn(Optional.of(usuario));
        when(accesos.findById(AccesoConstants.ACTIVO)).thenReturn(Optional.of(acceso(AccesoConstants.ACTIVO)));
        when(passwordEncoder.encode("temporal")).thenReturn("nueva");
    }

    @Test
    void unaCuentaConDniCompatibleConservaSuReactivacion() {
        permitirReactivacion();
        seeder.seedAdmin();
        verify(exclusividadDni).validarUsuario("12345678");
        assertEquals(AccesoConstants.ACTIVO, usuario.getAcceso().getIdAcceso());
        verify(usuarios).save(usuario);
        verify(transactionManager).getTransaction(argThat(definicion ->
                definicion.getIsolationLevel() == TransactionDefinition.ISOLATION_SERIALIZABLE
                        && definicion.getPropagationBehavior() == TransactionDefinition.PROPAGATION_REQUIRES_NEW));
    }

    @Test
    void unaCuentaInicialSinDniConservaSuFuncionamiento() {
        usuario.setDocumentoIdentidad(null);
        permitirReactivacion();
        seeder.seedAdmin();
        verifyNoInteractions(exclusividadDni);
        assertEquals(AccesoConstants.ACTIVO, usuario.getAcceso().getIdAcceso());
    }

    @Test
    void unaCuentaInicialNuevaSigueCreandoseSinDni() {
        when(accesos.findById(AccesoConstants.ACTIVO)).thenReturn(Optional.of(acceso(AccesoConstants.ACTIVO)));
        when(passwordEncoder.encode("temporal")).thenReturn("nueva");
        seeder.seedAdmin();
        var creada = org.mockito.ArgumentCaptor.forClass(Usuario.class);
        verify(usuarios).save(creada.capture());
        assertNull(creada.getValue().getDocumentoIdentidad());
        assertEquals("ADMIN", creada.getValue().getCodigo());
        verifyNoInteractions(exclusividadDni);
    }

    @Test
    void unAdministradorExistenteNoSeReactivaNiDebeResolverConflictosPrevios() {
        usuario.setAcceso(acceso(AccesoConstants.ACTIVO));
        when(usuarios.findByCodigoAndAccesoNot(eq("ADMIN"), any())).thenReturn(Optional.of(usuario));
        seeder.seedAdmin();
        verifyNoInteractions(exclusividadDni, passwordEncoder);
        verify(usuarios, never()).save(any());
    }
}
