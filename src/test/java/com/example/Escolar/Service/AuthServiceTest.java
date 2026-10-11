package com.example.Escolar.Service;

import com.example.Escolar.Dto.LoginRequest;
import com.example.Escolar.Dto.LoginResponse;
import com.example.Escolar.Exception.CodigoRecuperacionAgotadoException;
import com.example.Escolar.Exception.CuentaBloqueadaException;
import com.example.Escolar.Exception.CuentaInactivaException;
import com.example.Escolar.Exception.EsperaReenvioCodigoException;
import com.example.Escolar.Exception.ResourceNotFoundException;
import com.example.Escolar.Model.Acceso;
import com.example.Escolar.Model.Usuario;
import com.example.Escolar.Repository.AccesoRepository;
import com.example.Escolar.Repository.UsuarioRepository;
import com.example.Escolar.Repository.UsuarioRolRepository;
import com.example.Escolar.Security.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuthServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private UsuarioRolRepository usuarioRolRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;
    @Mock
    private UsuarioService usuarioService;
    @Mock
    private RolPermisoService rolPermisoService;
    @Mock
    private AccesoRepository accesoRepository;
    @Mock
    private EmailService emailService;

    @InjectMocks
    private AuthService authService;

    private Acceso mockAcceso(Long id) {
        Acceso acceso = new Acceso();
        acceso.setIdAcceso(id);
        acceso.setNombre(id.equals(1L) ? "Activo" : "Eliminado");
        return acceso;
    }

    private void stubAccesoRepository() {
        when(accesoRepository.findById(AccesoConstants.ELIMINADO)).thenReturn(Optional.of(mockAcceso(AccesoConstants.ELIMINADO)));
        when(accesoRepository.findById(AccesoConstants.ACTIVO)).thenReturn(Optional.of(mockAcceso(AccesoConstants.ACTIVO)));
    }

    private Usuario usuario(int intentosFallidos) {
        Usuario usuario = new Usuario();
        usuario.setIdUsuario(1);
        usuario.setCodigo("ADM20240001");
        usuario.setContraseña("$2a$10$hash");
        usuario.setAcceso(mockAcceso(AccesoConstants.ACTIVO));
        usuario.setIntentosFallidos(intentosFallidos);
        return usuario;
    }

    private LoginRequest request() {
        LoginRequest request = new LoginRequest();
        request.setCodigo("ADM20240001");
        request.setContraseña("clave");
        return request;
    }

    @Test
    void loginExitosoReseteaIntentosFallidos() {
        stubAccesoRepository();
        Usuario usuario = usuario(3);
        usuario.setFechaBloqueo(LocalDateTime.now().minusMinutes(31));
        when(usuarioRepository.findByCodigoAndAccesoNot(anyString(), any(Acceso.class))).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("clave", "$2a$10$hash")).thenReturn(true);
        when(usuarioRolRepository.findByUsuarioIdUsuario(1)).thenReturn(List.of());
        when(jwtService.generarToken(1, "ADM20240001", List.of())).thenReturn("token");

        authService.login(request());

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(captor.capture());
        assertEquals(0, captor.getValue().getIntentosFallidos());
        assertNull(captor.getValue().getFechaBloqueo());
    }

    @Test
    void contrasenaIncorrectaIncrementaIntentos() {
        stubAccesoRepository();
        Usuario usuario = usuario(0);
        when(usuarioRepository.findByCodigoAndAccesoNot(anyString(), any(Acceso.class))).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("clave", "$2a$10$hash")).thenReturn(false);

        assertThrows(BadCredentialsException.class, () -> authService.login(request()));

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(captor.capture());
        assertEquals(1, captor.getValue().getIntentosFallidos());
        assertNull(captor.getValue().getFechaBloqueo());
    }

    @Test
    void quintoIntentoFallidoBloqueaCuenta() {
        stubAccesoRepository();
        Usuario usuario = usuario(4);
        when(usuarioRepository.findByCodigoAndAccesoNot(anyString(), any(Acceso.class))).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("clave", "$2a$10$hash")).thenReturn(false);

        assertThrows(BadCredentialsException.class, () -> authService.login(request()));

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(captor.capture());
        assertEquals(AuthService.MAX_INTENTOS_FALLIDOS, captor.getValue().getIntentosFallidos());
        assertNotNull(captor.getValue().getFechaBloqueo());
    }

    @Test
    void cuentaBloqueadaLanzaCuentaBloqueadaException() {
        stubAccesoRepository();
        Usuario usuario = usuario(5);
        usuario.setFechaBloqueo(LocalDateTime.now());
        when(usuarioRepository.findByCodigoAndAccesoNot(anyString(), any(Acceso.class))).thenReturn(Optional.of(usuario));

        assertThrows(CuentaBloqueadaException.class, () -> authService.login(request()));

        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void bloqueoEsPermanenteAunqueHayaPasadoElTiempo() {
        stubAccesoRepository();
        Usuario usuario = usuario(5);
        usuario.setFechaBloqueo(LocalDateTime.now().minusDays(1));
        when(usuarioRepository.findByCodigoAndAccesoNot(anyString(), any(Acceso.class))).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("clave", "$2a$10$hash")).thenReturn(true);

        CuentaBloqueadaException ex = assertThrows(CuentaBloqueadaException.class, () -> authService.login(request()));

        assertEquals("Tu cuenta está bloqueada por intentos fallidos. Recupera tu contraseña o comunícate con el administrador.",
                ex.getMessage());
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void inactivoConContrasenaCorrectaRecibeMensajeSinContarIntentos() {
        stubAccesoRepository();
        Usuario usuario = usuario(2);
        usuario.setAcceso(mockAcceso(AccesoConstants.INACTIVO));
        when(usuarioRepository.findByCodigoAndAccesoNot(anyString(), any(Acceso.class))).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("clave", "$2a$10$hash")).thenReturn(true);

        assertThrows(CuentaInactivaException.class, () -> authService.login(request()));

        assertEquals(2, usuario.getIntentosFallidos());
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void inactivoConContrasenaIncorrectaRecibeCredencialesIncorrectasSinContar() {
        stubAccesoRepository();
        Usuario usuario = usuario(4);
        usuario.setAcceso(mockAcceso(AccesoConstants.INACTIVO));
        when(usuarioRepository.findByCodigoAndAccesoNot(anyString(), any(Acceso.class))).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("clave", "$2a$10$hash")).thenReturn(false);

        assertThrows(BadCredentialsException.class, () -> authService.login(request()));

        assertEquals(4, usuario.getIntentosFallidos());
        assertNull(usuario.getFechaBloqueo());
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void inactivoNoPuedeSolicitarCodigoDeRecuperacion() {
        stubAccesoRepository();
        Usuario usuario = usuario(0);
        usuario.setAcceso(mockAcceso(AccesoConstants.INACTIVO));
        when(usuarioRepository.findByGmailAndAccesoNot(anyString(), any(Acceso.class))).thenReturn(Optional.of(usuario));

        assertThrows(CuentaInactivaException.class, () -> authService.forgotPassword("correo@gmail.com"));

        verify(emailService, never()).enviarCodigoRecuperacion(anyString(), anyString());
        assertNull(usuario.getResetToken());
    }

    @Test
    void inactivoConCodigoPrevioNoPuedeVerificarlo() {
        stubAccesoRepository();
        Usuario usuario = usuarioConCodigo("123456");
        usuario.setAcceso(mockAcceso(AccesoConstants.INACTIVO));
        when(usuarioRepository.findByGmailAndAccesoNot(anyString(), any(Acceso.class))).thenReturn(Optional.of(usuario));

        assertThrows(CuentaInactivaException.class, () -> authService.verificarCodigo("correo@gmail.com", "123456"));
        assertThrows(CuentaInactivaException.class,
                () -> authService.resetPassword("correo@gmail.com", "123456", "ClaveNueva1@"));
    }

    @Test
    void solicitarCodigoReiniciaIntentosYEnviaCorreo() {
        stubAccesoRepository();
        Usuario usuario = usuario(0);
        usuario.setIntentosCodigoReset(3);
        when(usuarioRepository.findByGmailAndAccesoNot(anyString(), any(Acceso.class))).thenReturn(Optional.of(usuario));

        authService.forgotPassword("correo@gmail.com");

        assertEquals(0, usuario.getIntentosCodigoReset());
        assertNotNull(usuario.getResetToken());
        assertNotNull(usuario.getResetTokenEnviado());
        verify(emailService).enviarCodigoRecuperacion(anyString(), anyString());
    }

    @Test
    void solicitarCodigoAntesDe45SegundosResponde429() {
        stubAccesoRepository();
        Usuario usuario = usuario(0);
        usuario.setResetTokenEnviado(LocalDateTime.now().minusSeconds(10));
        when(usuarioRepository.findByGmailAndAccesoNot(anyString(), any(Acceso.class))).thenReturn(Optional.of(usuario));

        EsperaReenvioCodigoException ex = assertThrows(EsperaReenvioCodigoException.class,
                () -> authService.forgotPassword("correo@gmail.com"));

        assertTrue(ex.getSegundosRestantes() > 0 && ex.getSegundosRestantes() <= 35);
        verify(emailService, never()).enviarCodigoRecuperacion(anyString(), anyString());
    }

    @Test
    void solicitarCodigoDespuesDe45SegundosPermiteReenviar() {
        stubAccesoRepository();
        Usuario usuario = usuario(0);
        usuario.setResetTokenEnviado(LocalDateTime.now().minusSeconds(46));
        when(usuarioRepository.findByGmailAndAccesoNot(anyString(), any(Acceso.class))).thenReturn(Optional.of(usuario));

        authService.forgotPassword("correo@gmail.com");

        verify(emailService).enviarCodigoRecuperacion(anyString(), anyString());
    }

    @Test
    void codigoIncorrectoDescuentaIntentosRestantes() {
        stubAccesoRepository();
        Usuario usuario = usuarioConCodigo("123456");
        when(usuarioRepository.findByGmailAndAccesoNot(anyString(), any(Acceso.class))).thenReturn(Optional.of(usuario));

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> authService.verificarCodigo("correo@gmail.com", "000000"));

        assertEquals("El código ingresado es incorrecto. Te quedan 4 intentos.", ex.getMessage());
        assertEquals(1, usuario.getIntentosCodigoReset());
        assertNotNull(usuario.getResetToken());
    }

    @Test
    void quintoCodigoIncorrectoAnulaElCodigo() {
        stubAccesoRepository();
        Usuario usuario = usuarioConCodigo("123456");
        usuario.setIntentosCodigoReset(4);
        when(usuarioRepository.findByGmailAndAccesoNot(anyString(), any(Acceso.class))).thenReturn(Optional.of(usuario));

        assertThrows(CodigoRecuperacionAgotadoException.class,
                () -> authService.verificarCodigo("correo@gmail.com", "000000"));

        assertNull(usuario.getResetToken());
        // Ya anulado, ni el codigo correcto sirve.
        assertThrows(ResourceNotFoundException.class,
                () -> authService.verificarCodigo("correo@gmail.com", "123456"));
    }

    @Test
    void codigoExpiradoNoConsumeIntentos() {
        stubAccesoRepository();
        Usuario usuario = usuarioConCodigo("123456");
        usuario.setResetTokenExpiracion(LocalDateTime.now().minusMinutes(1));
        when(usuarioRepository.findByGmailAndAccesoNot(anyString(), any(Acceso.class))).thenReturn(Optional.of(usuario));

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> authService.verificarCodigo("correo@gmail.com", "000000"));

        assertEquals("El código ha expirado. Solicite uno nuevo.", ex.getMessage());
        assertEquals(0, usuario.getIntentosCodigoReset());
    }

    @Test
    void bloqueadoPuedeRecuperarContrasenaYQuedaDesbloqueado() {
        stubAccesoRepository();
        Usuario usuario = usuarioConCodigo("123456");
        usuario.setIntentosFallidos(5);
        usuario.setFechaBloqueo(LocalDateTime.now());
        usuario.setIntentosCodigoReset(2);
        when(usuarioRepository.findByGmailAndAccesoNot(anyString(), any(Acceso.class))).thenReturn(Optional.of(usuario));
        when(passwordEncoder.encode("ClaveNueva1@")).thenReturn("$2a$10$nuevo");

        authService.resetPassword("correo@gmail.com", "123456", "ClaveNueva1@");

        assertEquals(0, usuario.getIntentosFallidos());
        assertNull(usuario.getFechaBloqueo());
        assertEquals(0, usuario.getIntentosCodigoReset());
    }

    private Usuario usuarioConCodigo(String codigo) {
        Usuario usuario = usuario(0);
        usuario.setResetToken(sha256(codigo));
        usuario.setResetTokenExpiracion(LocalDateTime.now().plusMinutes(10));
        usuario.setIntentosCodigoReset(0);
        return usuario;
    }

    @Test
    void cambiarContrasenaConContrasenaActualCorrectaGuardaNueva() {
        stubAccesoRepository();
        Usuario usuario = usuario(0);
        usuario.setIntentosFallidos(3);
        usuario.setFechaBloqueo(LocalDateTime.now().minusMinutes(5));
        when(usuarioRepository.findByIdUsuarioAndAccesoNot(any(), any(Acceso.class))).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("claveActual", "$2a$10$hash")).thenReturn(true);
        when(passwordEncoder.encode("ClaveNueva1@")).thenReturn("$2a$10$nuevo");

        authService.cambiarContrasena(1, "claveActual", "ClaveNueva1@");

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(captor.capture());
        assertEquals("$2a$10$nuevo", captor.getValue().getContraseña());
        assertEquals(0, captor.getValue().getIntentosFallidos());
        assertNull(captor.getValue().getFechaBloqueo());
    }

    @Test
    void cambiarContrasenaConContrasenaActualIncorrectaLanzaIllegalArgument() {
        stubAccesoRepository();
        Usuario usuario = usuario(0);
        when(usuarioRepository.findByIdUsuarioAndAccesoNot(any(), any(Acceso.class))).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("claveIncorrecta", "$2a$10$hash")).thenReturn(false);

        assertThrows(IllegalArgumentException.class,
                () -> authService.cambiarContrasena(1, "claveIncorrecta", "ClaveNueva1@"));

        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void loginConCambioPendienteGeneraTokenConMarca() {
        stubAccesoRepository();
        Usuario usuario = usuario(0);
        usuario.setDebeCambiarContrasena(true);
        when(usuarioRepository.findByCodigoAndAccesoNot(anyString(), any(Acceso.class))).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("clave", "$2a$10$hash")).thenReturn(true);
        when(usuarioRolRepository.findByUsuarioIdUsuario(1)).thenReturn(List.of());
        when(jwtService.generarToken(1, "ADM20240001", List.of(), true)).thenReturn("tokenPendiente");

        LoginResponse response = authService.login(request());

        assertEquals("tokenPendiente", response.getToken());
    }

    @Test
    void cambiarContrasenaQuitaMarcaYDevuelveTokenNuevo() {
        stubAccesoRepository();
        Usuario usuario = usuario(0);
        usuario.setDebeCambiarContrasena(true);
        when(usuarioRepository.findByIdUsuarioAndAccesoNot(any(), any(Acceso.class))).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("claveTemporal", "$2a$10$hash")).thenReturn(true);
        when(passwordEncoder.encode("ClaveNueva1@")).thenReturn("$2a$10$nuevo");
        when(usuarioRolRepository.findByUsuarioIdUsuario(1)).thenReturn(List.of());
        when(jwtService.generarToken(1, "ADM20240001", List.of())).thenReturn("tokenLimpio");

        String token = authService.cambiarContrasena(1, "claveTemporal", "ClaveNueva1@");

        assertEquals("tokenLimpio", token);
        assertEquals(Boolean.FALSE, usuario.getDebeCambiarContrasena());
    }

    @Test
    void cambiarContrasenaIgualALaActualLanzaIllegalArgument() {
        stubAccesoRepository();
        Usuario usuario = usuario(0);
        when(usuarioRepository.findByIdUsuarioAndAccesoNot(any(), any(Acceso.class))).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("Clave1@x", "$2a$10$hash")).thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> authService.cambiarContrasena(1, "Clave1@x", "Clave1@x"));

        assertEquals("La nueva contrasena debe ser diferente a la actual", ex.getMessage());
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void resetPasswordQuitaMarcaDeCambioPendiente() {
        stubAccesoRepository();
        Usuario usuario = usuario(0);
        usuario.setDebeCambiarContrasena(true);
        usuario.setResetToken(sha256("123456"));
        usuario.setResetTokenExpiracion(LocalDateTime.now().plusMinutes(10));
        when(usuarioRepository.findByGmailAndAccesoNot(anyString(), any(Acceso.class))).thenReturn(Optional.of(usuario));
        when(passwordEncoder.encode("ClaveNueva1@")).thenReturn("$2a$10$nuevo");

        authService.resetPassword("correo@gmail.com", "123456", "ClaveNueva1@");

        assertEquals(Boolean.FALSE, usuario.getDebeCambiarContrasena());
        assertEquals("$2a$10$nuevo", usuario.getContraseña());
    }

    private String sha256(String valor) {
        try {
            byte[] hash = java.security.MessageDigest.getInstance("SHA-256")
                    .digest(valor.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    @Test
    void cambiarContrasenaDeUsuarioInexistenteLanzaNoEncontrado() {
        stubAccesoRepository();
        when(usuarioRepository.findByIdUsuarioAndAccesoNot(any(), any(Acceso.class))).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> authService.cambiarContrasena(999, "claveActual", "ClaveNueva1@"));

        verify(usuarioRepository, never()).save(any());
    }
}
