package com.example.Escolar.Service;

import com.example.Escolar.Dto.LoginRequest;
import com.example.Escolar.Dto.LoginResponse;
import com.example.Escolar.Dto.PermisosRolResponse;
import com.example.Escolar.Dto.RolAsignadoSesionResponse;
import com.example.Escolar.Dto.RolAsignadoSesionResponse.EstadoRol;
import com.example.Escolar.Exception.AccesoDenegadoException;
import com.example.Escolar.Exception.CodigoRecuperacionAgotadoException;
import com.example.Escolar.Exception.CuentaBloqueadaException;
import com.example.Escolar.Exception.CuentaInactivaException;
import com.example.Escolar.Exception.EsperaReenvioCodigoException;
import com.example.Escolar.Exception.ResourceNotFoundException;
import com.example.Escolar.Exception.VigenciaNoValidaException;
import com.example.Escolar.Model.Acceso;
import com.example.Escolar.Model.Usuario;
import com.example.Escolar.Repository.AccesoRepository;
import com.example.Escolar.Repository.UsuarioRepository;
import com.example.Escolar.Repository.UsuarioRolRepository;
import com.example.Escolar.Security.JwtService;
import com.example.Escolar.Service.VigenciaService.EstadoVigencia;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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
    @Mock
    private AccesoContextoService accesoContextoService;
    @Mock
    private VigenciaService vigenciaService;
    @Mock
    private SeleccionRolDesafioService seleccionRolDesafioService;
    @Mock
    private RolAsignadoResolver rolAsignadoResolver;

    @InjectMocks
    private AuthService authService;

    @BeforeEach
    void vigenciaValidaPorDefecto() {
        when(vigenciaService.esValida(org.mockito.ArgumentMatchers.<LocalDate>any(),
                org.mockito.ArgumentMatchers.<LocalDate>any())).thenReturn(true);
    }

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
        usuario.setFechaInicioVigencia(LocalDate.of(2026, 1, 1));
        usuario.setVersionToken(0);
        return usuario;
    }

    private LoginRequest request() {
        LoginRequest request = new LoginRequest();
        request.setCodigo("ADM20240001");
        request.setContraseña("clave");
        return request;
    }

    private RolAsignadoSesionResponse rolActivo() {
        return new RolAsignadoSesionResponse(2, "Docente", EstadoRol.ACTIVO);
    }

    private RolAsignadoSesionResponse rolAsignado(int idRol, String nombre, EstadoRol estado) {
        return new RolAsignadoSesionResponse(idRol, nombre, estado);
    }

    private PermisosRolResponse stubPermisos(int idRol) {
        PermisosRolResponse permisos = new PermisosRolResponse();
        permisos.setIdRol(idRol);
        permisos.setNombreRol(idRol == 3 ? "ADMIN" : "Docente");
        when(rolPermisoService.obtenerPermisosDelUsuario(1, idRol)).thenReturn(permisos);
        when(accesoContextoService.vinculoEfectivo(eq(1), eq(idRol)))
                .thenReturn(AccesoContextoService.Alcance.TODO);
        return permisos;
    }

    private void stubToken(String token, Usuario usuario, Integer idRol, List<String> roles,
                           Integer versionToken, Integer idDispositivo) {
        when(jwtService.generarToken(eq(usuario.getIdUsuario()), eq(usuario.getCodigo()), eq(idRol), eq(roles),
                eq(false), eq(usuario.getFechaFinVigencia()), eq(versionToken), eq(idDispositivo)))
                .thenReturn(token);
    }

    private void verificarToken(Usuario usuario, Integer idRol, List<String> roles,
                                Integer versionToken, Integer idDispositivo) {
        verify(jwtService).generarToken(eq(usuario.getIdUsuario()), eq(usuario.getCodigo()), eq(idRol), eq(roles),
                eq(false), eq(usuario.getFechaFinVigencia()), eq(versionToken), eq(idDispositivo));
    }

    private void stubIngresoDirecto() {
        when(seleccionRolDesafioService.evaluar(1)).thenReturn(new SeleccionRolDesafioService.Evaluacion(
                SeleccionRolDesafioService.Resultado.INGRESO_DIRECTO, List.of(rolActivo())));
    }

    private void stubPermisos() {
        when(rolPermisoService.obtenerPermisosDelUsuario(1, 2)).thenReturn(new PermisosRolResponse());
        when(accesoContextoService.vinculoEfectivo(eq(1), eq(2))).thenReturn(AccesoContextoService.Alcance.TODO);
    }

    @Test
    void loginExitosoReseteaIntentosFallidos() {
        stubAccesoRepository();
        Usuario usuario = usuario(3);
        usuario.setFechaBloqueo(LocalDateTime.now().minusMinutes(31));
        when(usuarioRepository.findByCodigoAndAccesoNot(anyString(), any(Acceso.class))).thenReturn(Optional.of(usuario));
        when(usuarioRepository.findByIdUsuarioAndAccesoNot(eq(1), any(Acceso.class))).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("clave", "$2a$10$hash")).thenReturn(true);
        stubIngresoDirecto();
        when(rolAsignadoResolver.resolver(1)).thenReturn(List.of(rolActivo()));
        when(jwtService.generarToken(eq(1), eq("ADM20240001"), eq(2), eq(List.of("Docente")),
                eq(false), org.mockito.ArgumentMatchers.isNull(), eq(0), org.mockito.ArgumentMatchers.isNull()))
                .thenReturn("token");
        stubPermisos();

        authService.login(request());

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(captor.capture());
        assertEquals(0, captor.getValue().getIntentosFallidos());
        assertNull(captor.getValue().getFechaBloqueo());
        verify(rolPermisoService).obtenerPermisosDelUsuario(1, 2);
    }

    @Test
    void emitirSesionCalculaVinculoConRolActivoSinContextoAutenticado() {
        stubAccesoRepository();
        SecurityContextHolder.clearContext();
        Usuario usuario = usuario(0);
        when(usuarioRepository.findByIdUsuarioAndAccesoNot(eq(1), any(Acceso.class))).thenReturn(Optional.of(usuario));
        when(rolAsignadoResolver.resolver(1)).thenReturn(List.of(rolActivo()));
        when(jwtService.generarToken(eq(1), eq("ADM20240001"), eq(2), eq(List.of("Docente")),
                eq(false), org.mockito.ArgumentMatchers.isNull(), eq(0), org.mockito.ArgumentMatchers.isNull()))
                .thenReturn("token");
        when(rolPermisoService.obtenerPermisosDelUsuario(1, 2)).thenReturn(new PermisosRolResponse());
        when(accesoContextoService.vinculoEfectivo(1, 2)).thenReturn(AccesoContextoService.Alcance.DOCENTE);

        LoginResponse response = authService.emitirSesion(usuario, null, rolActivo());

        verify(accesoContextoService).vinculoEfectivo(1, 2);
        assertEquals("DOCENTE", response.getPermisos().getVinculo());
    }

    @Test
    void emitirSesionConservaIdentidadDelRolSeleccionadoSinPermisos() {
        stubAccesoRepository();
        SecurityContextHolder.clearContext();
        Usuario usuario = usuario(0);
        when(usuarioRepository.findByIdUsuarioAndAccesoNot(eq(1), any(Acceso.class))).thenReturn(Optional.of(usuario));
        when(rolAsignadoResolver.resolver(1)).thenReturn(List.of(rolActivo()));
        when(jwtService.generarToken(eq(1), eq("ADM20240001"), eq(2), eq(List.of("Docente")),
                eq(false), org.mockito.ArgumentMatchers.isNull(), eq(0), org.mockito.ArgumentMatchers.isNull()))
                .thenReturn("token");
        PermisosRolResponse permisos = new PermisosRolResponse();
        permisos.setIdRol(2);
        permisos.setNombreRol("Docente");
        permisos.setModulos(List.of());
        when(rolPermisoService.obtenerPermisosDelUsuario(1, 2)).thenReturn(permisos);
        when(accesoContextoService.vinculoEfectivo(1, 2)).thenReturn(AccesoContextoService.Alcance.TODO);

        LoginResponse response = authService.emitirSesion(usuario, null, rolActivo());

        assertEquals(2, response.getPermisos().getIdRol());
        assertEquals("Docente", response.getPermisos().getNombreRol());
        assertTrue(response.getPermisos().getModulos().isEmpty());
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
    void loginConVigenciaVencidaNoCuentaIntentosNiGeneraToken() {
        stubAccesoRepository();
        Usuario usuario = usuario(2);
        usuario.setFechaInicioVigencia(LocalDate.of(2026, 1, 1));
        usuario.setFechaFinVigencia(LocalDate.of(2026, 1, 31));
        when(usuarioRepository.findByCodigoAndAccesoNot(anyString(), any(Acceso.class))).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("clave", "$2a$10$hash")).thenReturn(true);
        when(vigenciaService.esValida(any(), any())).thenReturn(false);
        when(vigenciaService.evaluar(any(), any())).thenReturn(VigenciaService.EstadoVigencia.VENCIDA);

        VigenciaNoValidaException ex = assertThrows(VigenciaNoValidaException.class, () -> authService.login(request()));

        assertEquals(VigenciaNoValidaException.CODIGO_VIGENCIA_VENCIDA, ex.getCodigo());
        assertEquals("El período de vigencia de su cuenta ha finalizado. Comuníquese con el administrador.",
                ex.getMessage());
        assertEquals(2, usuario.getIntentosFallidos());
        verify(usuarioRepository, never()).save(any());
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
        when(rolAsignadoResolver.resolver(1)).thenReturn(List.of(rolActivo()));

        authService.cambiarContrasena(1, "claveActual", "ClaveNueva1@");

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(captor.capture());
        assertEquals("$2a$10$nuevo", captor.getValue().getContraseña());
        assertEquals(0, captor.getValue().getIntentosFallidos());
        assertNull(captor.getValue().getFechaBloqueo());
    }

    @Test
    void cambiarContrasenaConservaElDispositivoDeLaSesion() {
        stubAccesoRepository();
        Usuario usuario = usuario(0);
        when(usuarioRepository.findByIdUsuarioAndAccesoNot(any(), any(Acceso.class))).thenReturn(Optional.of(usuario));
        when(usuarioRepository.tieneDispositivo(1, 40)).thenReturn(true);
        when(passwordEncoder.matches("claveActual", "$2a$10$hash")).thenReturn(true);
        when(passwordEncoder.encode("ClaveNueva1@")).thenReturn("$2a$10$nuevo");
        when(rolAsignadoResolver.resolver(1)).thenReturn(List.of(rolActivo()));

        authService.cambiarContrasena(1, "claveActual", "ClaveNueva1@", 40, 2);

        verify(jwtService).generarToken(org.mockito.ArgumentMatchers.eq(1), anyString(),
                org.mockito.ArgumentMatchers.eq(2), eq(List.of("Docente")), org.mockito.ArgumentMatchers.eq(false),
                any(), org.mockito.ArgumentMatchers.eq(0), org.mockito.ArgumentMatchers.eq(40));
    }

    @Test
    void cambiarContrasenaConservaElRolActivoDeLaSesionConMultiplesRoles() {
        stubAccesoRepository();
        Usuario usuario = usuario(0);
        when(usuarioRepository.findByIdUsuarioAndAccesoNot(any(), any(Acceso.class))).thenReturn(Optional.of(usuario));
        when(usuarioRepository.tieneDispositivo(1, 40)).thenReturn(true);
        when(passwordEncoder.matches("claveActual", "$2a$10$hash")).thenReturn(true);
        when(passwordEncoder.encode("ClaveNueva1@")).thenReturn("$2a$10$nuevo");
        when(rolAsignadoResolver.resolver(1)).thenReturn(List.of(
                rolActivo(), rolAsignado(3, "ADMIN", EstadoRol.ACTIVO)));
        when(jwtService.generarToken(eq(1), eq("ADM20240001"), eq(2), eq(List.of("Docente")),
                eq(false), org.mockito.ArgumentMatchers.isNull(), eq(0), eq(40)))
                .thenReturn("jwt-mismo-rol");

        String token = authService.cambiarContrasena(1, "claveActual", "ClaveNueva1@", 40, 2);

        assertEquals("jwt-mismo-rol", token);
        verificarToken(usuario, 2, List.of("Docente"), 0, 40);
    }

    @Test
    void cambiarContrasenaConVigenciaVencidaNoGuardaNiRenuevaLaVigencia() {
        stubAccesoRepository();
        Usuario usuario = usuario(0);
        LocalDate inicio = usuario.getFechaInicioVigencia();
        when(usuarioRepository.findByIdUsuarioAndAccesoNot(any(), any(Acceso.class))).thenReturn(Optional.of(usuario));
        when(vigenciaService.datosNoDisponibles(inicio, null)).thenReturn(false);
        when(vigenciaService.esValida(inicio, null)).thenReturn(false);
        when(vigenciaService.evaluar(inicio, null)).thenReturn(EstadoVigencia.VENCIDA);

        VigenciaNoValidaException ex = assertThrows(VigenciaNoValidaException.class,
                () -> authService.cambiarContrasena(1, "claveActual", "ClaveNueva1@"));

        assertEquals(VigenciaNoValidaException.CODIGO_VIGENCIA_VENCIDA, ex.getCodigo());
        assertEquals(inicio, usuario.getFechaInicioVigencia());
        assertEquals(0, usuario.getVersionToken());
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void cambiarContrasenaSinDatosDeVigenciaUsaCodigoNoDisponible() {
        stubAccesoRepository();
        Usuario usuario = usuario(0);
        LocalDate inicio = usuario.getFechaInicioVigencia();
        when(usuarioRepository.findByIdUsuarioAndAccesoNot(any(), any(Acceso.class))).thenReturn(Optional.of(usuario));
        when(vigenciaService.datosNoDisponibles(inicio, null)).thenReturn(true);

        VigenciaNoValidaException ex = assertThrows(VigenciaNoValidaException.class,
                () -> authService.cambiarContrasena(1, "claveActual", "ClaveNueva1@"));

        assertEquals(VigenciaNoValidaException.CODIGO_NO_DISPONIBLE, ex.getCodigo());
        assertEquals(VigenciaNoValidaException.MENSAJE_NO_DISPONIBLE, ex.getMessage());
        verify(usuarioRepository, never()).save(any());
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
        when(usuarioRepository.findByIdUsuarioAndAccesoNot(eq(1), any(Acceso.class))).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("clave", "$2a$10$hash")).thenReturn(true);
        stubIngresoDirecto();
        when(rolAsignadoResolver.resolver(1)).thenReturn(List.of(rolActivo()));
        when(jwtService.generarToken(eq(1), eq("ADM20240001"), eq(2), eq(List.of("Docente")),
                eq(true), org.mockito.ArgumentMatchers.isNull(), eq(0), org.mockito.ArgumentMatchers.isNull()))
                .thenReturn("tokenPendiente");
        stubPermisos();

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
        when(rolAsignadoResolver.resolver(1)).thenReturn(List.of(rolActivo()));
        when(jwtService.generarToken(eq(1), eq("ADM20240001"), eq(2), eq(List.of("Docente")),
                eq(false), org.mockito.ArgumentMatchers.isNull(), eq(0), org.mockito.ArgumentMatchers.isNull()))
                .thenReturn("tokenLimpio");

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

    @Test
    void loginConRolActivoEInactivoRequiereSeleccion() {
        stubAccesoRepository();
        Usuario usuario = usuario(0);
        when(usuarioRepository.findByCodigoAndAccesoNot(anyString(), any(Acceso.class)))
                .thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("clave", "$2a$10$hash")).thenReturn(true);
        RolAsignadoSesionResponse activo = rolActivo();
        RolAsignadoSesionResponse inactivo = new RolAsignadoSesionResponse(3, "Administrador", EstadoRol.INACTIVO);
        when(seleccionRolDesafioService.evaluar(1)).thenReturn(new SeleccionRolDesafioService.Evaluacion(
                SeleccionRolDesafioService.Resultado.SELECCION_REQUERIDA, List.of(activo, inactivo)));
        when(seleccionRolDesafioService.emitir(usuario, null)).thenReturn("SELROL:0:0:abc");

        LoginResponse response = authService.login(request());

        assertNull(response.getToken());
        assertTrue(response.isRequiereSeleccionRol());
        assertEquals("SELROL:0:0:abc", response.getDesafioSeleccionRol());
        assertEquals(List.of(activo, inactivo), response.getRoles());
        verifyNoInteractions(jwtService);
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void iniciarSesionConSeleccionDeRolNoGeneraTokenNiReiniciaIntentos() {
        Usuario usuario = usuario(3);
        RolAsignadoSesionResponse suspendido = new RolAsignadoSesionResponse(1, "Portero", EstadoRol.SUSPENDIDO);
        RolAsignadoSesionResponse activo = rolActivo();
        when(seleccionRolDesafioService.evaluar(1)).thenReturn(new SeleccionRolDesafioService.Evaluacion(
                SeleccionRolDesafioService.Resultado.SELECCION_REQUERIDA, List.of(suspendido, activo)));
        when(seleccionRolDesafioService.emitir(usuario, 40)).thenReturn("SELROL:0:40:abc");

        LoginResponse response = authService.iniciarSesion(usuario, 40);

        assertNull(response.getToken());
        assertTrue(response.isRequiereSeleccionRol());
        assertEquals("SELROL:0:40:abc", response.getDesafioSeleccionRol());
        assertEquals(List.of(suspendido, activo), response.getRoles());
        assertEquals(3, usuario.getIntentosFallidos());
        verifyNoInteractions(jwtService);
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void iniciarSesionSinRolesActivosNiegaElIngreso() {
        Usuario usuario = usuario(0);
        when(seleccionRolDesafioService.evaluar(1)).thenReturn(new SeleccionRolDesafioService.Evaluacion(
                SeleccionRolDesafioService.Resultado.SIN_ROLES_DISPONIBLES, List.of()));

        assertThrows(AccesoDenegadoException.class, () -> authService.iniciarSesion(usuario, null));
        verifyNoInteractions(jwtService);
    }

    @Test
    void cambiarContrasenaConRolInactivoNoRenuevaLaSesion() {
        stubAccesoRepository();
        Usuario usuario = usuario(0);
        when(usuarioRepository.findByIdUsuarioAndAccesoNot(any(), any(Acceso.class))).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("claveActual", "$2a$10$hash")).thenReturn(true);
        when(rolAsignadoResolver.resolver(1)).thenReturn(List.of(
                new RolAsignadoSesionResponse(2, "Docente", EstadoRol.INACTIVO)));

        assertThrows(AccesoDenegadoException.class,
                () -> authService.cambiarContrasena(1, "claveActual", "ClaveNueva1@", null, 2));
        verify(usuarioRepository, never()).save(any());
        verifyNoInteractions(jwtService);
    }

    @Test
    void emitirSesionRechazaCuentaInactivaDelContexto() {
        stubAccesoRepository();
        Usuario contexto = usuario(0);
        Usuario actual = usuario(0);
        actual.setAcceso(mockAcceso(AccesoConstants.INACTIVO));
        when(usuarioRepository.findByIdUsuarioAndAccesoNot(eq(1), any(Acceso.class))).thenReturn(Optional.of(actual));

        VigenciaNoValidaException ex = assertThrows(VigenciaNoValidaException.class,
                () -> authService.emitirSesion(contexto, null, rolActivo()));

        assertEquals(VigenciaNoValidaException.CODIGO_CUENTA_INACTIVA, ex.getCodigo());
        verifyNoInteractions(jwtService);
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void emitirSesionRechazaVigenciaVencidaDelContextoActual() {
        stubAccesoRepository();
        Usuario contexto = usuario(0);
        Usuario actual = usuario(0);
        LocalDate inicio = actual.getFechaInicioVigencia();
        when(vigenciaService.datosNoDisponibles(inicio, null)).thenReturn(false);
        when(vigenciaService.esValida(inicio, null)).thenReturn(false);
        when(vigenciaService.evaluar(inicio, null)).thenReturn(EstadoVigencia.VENCIDA);
        when(usuarioRepository.findByIdUsuarioAndAccesoNot(eq(1), any(Acceso.class))).thenReturn(Optional.of(actual));

        VigenciaNoValidaException ex = assertThrows(VigenciaNoValidaException.class,
                () -> authService.emitirSesion(contexto, null, rolActivo()));

        assertEquals(VigenciaNoValidaException.CODIGO_VIGENCIA_VENCIDA, ex.getCodigo());
        verifyNoInteractions(jwtService);
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void emitirSesionRechazaVersionTokenDistintaDelContextoOriginal() {
        stubAccesoRepository();
        Usuario contexto = usuario(0);
        Usuario actual = usuario(0);
        actual.setVersionToken(1);
        when(usuarioRepository.findByIdUsuarioAndAccesoNot(eq(1), any(Acceso.class))).thenReturn(Optional.of(actual));

        VigenciaNoValidaException ex = assertThrows(VigenciaNoValidaException.class,
                () -> authService.emitirSesion(contexto, null, rolActivo()));

        assertEquals(VigenciaNoValidaException.CODIGO_TOKEN_OBSOLETO, ex.getCodigo());
        verifyNoInteractions(jwtService);
        verify(usuarioRepository, never()).save(any());
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

    @Test
    void cambiarRolEmiteSesionConRolSolicitadoYConservaDispositivo() {
        stubAccesoRepository();
        Usuario original = usuario(0);
        original.setVersionToken(1);
        Usuario actualizado = usuario(0);
        actualizado.setVersionToken(2);
        when(usuarioRepository.findByIdUsuarioAndAccesoNot(eq(1), any(Acceso.class)))
                .thenReturn(Optional.of(original), Optional.of(actualizado));
        when(rolAsignadoResolver.resolver(1)).thenReturn(List.of(
                rolActivo(), rolAsignado(3, "ADMIN", EstadoRol.INACTIVO)));
        when(usuarioRepository.incrementarVersionTokenSiCoincide(1, 1, AccesoConstants.ELIMINADO)).thenReturn(1);
        when(usuarioRepository.tieneDispositivo(1, 40)).thenReturn(true);
        stubToken("jwt-rol-2", actualizado, 2, List.of("Docente"), 2, 40);
        stubPermisos(2);

        LoginResponse response = authService.cambiarRol(1, 2, 1, 40, 1);

        assertEquals("jwt-rol-2", response.getToken());
        assertEquals(2, response.getPermisos().getIdRol());
        assertEquals("Docente", response.getPermisos().getNombreRol());
        assertTrue(!response.isEsAdmin());
        verify(usuarioRepository).incrementarVersionTokenSiCoincide(1, 1, AccesoConstants.ELIMINADO);
        verificarToken(actualizado, 2, List.of("Docente"), 2, 40);
    }

    @Test
    void cambiarRolConVersionTokenNuloUsaCeroComoVersionEsperada() {
        stubAccesoRepository();
        Usuario original = usuario(0);
        Usuario actualizado = usuario(0);
        actualizado.setVersionToken(1);
        when(usuarioRepository.findByIdUsuarioAndAccesoNot(eq(1), any(Acceso.class)))
                .thenReturn(Optional.of(original), Optional.of(actualizado));
        when(rolAsignadoResolver.resolver(1)).thenReturn(List.of(rolActivo()));
        when(usuarioRepository.incrementarVersionTokenSiCoincide(1, 0, AccesoConstants.ELIMINADO)).thenReturn(1);
        when(usuarioRepository.tieneDispositivo(1, 40)).thenReturn(true);
        stubToken("jwt-version-nula", actualizado, 2, List.of("Docente"), 1, 40);
        stubPermisos(2);

        LoginResponse response = authService.cambiarRol(1, 2, 1, 40, null);

        assertEquals("jwt-version-nula", response.getToken());
        verify(usuarioRepository).incrementarVersionTokenSiCoincide(1, 0, AccesoConstants.ELIMINADO);
    }

    @Test
    void cambiarRolRechazaRolYaActivoSinModificarSesion() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> authService.cambiarRol(1, 2, 2, 40, 1));

        assertEquals("El rol ya está activo en la sesión.", ex.getMessage());
        verifyNoInteractions(usuarioRepository, jwtService);
    }

    @Test
    void cambiarRolRechazaRolNoDisponibleParaLaCuenta() {
        stubAccesoRepository();
        when(usuarioRepository.findByIdUsuarioAndAccesoNot(eq(1), any(Acceso.class)))
                .thenReturn(Optional.of(usuario(0)));
        when(rolAsignadoResolver.resolver(1)).thenReturn(List.of(
                rolActivo(), rolAsignado(3, "ADMIN", EstadoRol.INACTIVO)));

        AccesoDenegadoException ex = assertThrows(AccesoDenegadoException.class,
                () -> authService.cambiarRol(1, 3, 1, 40, 1));

        assertEquals("El rol solicitado no está disponible para esta cuenta.", ex.getMessage());
        verify(usuarioRepository, never()).incrementarVersionTokenSiCoincide(
                eq(1), eq(1), eq(AccesoConstants.ELIMINADO));
        verifyNoInteractions(jwtService);
    }

    @Test
    void cambiarRolConVersionTokenObsoletoNoEmiteNuevaSesion() {
        stubAccesoRepository();
        when(usuarioRepository.findByIdUsuarioAndAccesoNot(eq(1), any(Acceso.class)))
                .thenReturn(Optional.of(usuario(0)));
        when(rolAsignadoResolver.resolver(1)).thenReturn(List.of(rolActivo()));
        when(usuarioRepository.incrementarVersionTokenSiCoincide(1, 0, AccesoConstants.ELIMINADO)).thenReturn(0);

        VigenciaNoValidaException ex = assertThrows(VigenciaNoValidaException.class,
                () -> authService.cambiarRol(1, 2, 1, 40, 0));

        assertEquals(VigenciaNoValidaException.CODIGO_TOKEN_OBSOLETO, ex.getCodigo());
        verifyNoInteractions(jwtService);
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void cambiarRolPropagaFalloDeEmisionTrasIncrementarVersion() {
        stubAccesoRepository();
        Usuario original = usuario(0);
        original.setVersionToken(1);
        Usuario actualizado = usuario(0);
        actualizado.setVersionToken(2);
        when(usuarioRepository.findByIdUsuarioAndAccesoNot(eq(1), any(Acceso.class)))
                .thenReturn(Optional.of(original), Optional.of(actualizado));
        when(rolAsignadoResolver.resolver(1)).thenReturn(List.of(rolActivo()));
        when(usuarioRepository.incrementarVersionTokenSiCoincide(1, 1, AccesoConstants.ELIMINADO)).thenReturn(1);
        when(usuarioRepository.tieneDispositivo(1, 40)).thenReturn(true);
        RuntimeException fallo = new RuntimeException("fallo al emitir");
        doThrow(fallo).when(jwtService)
                .generarToken(eq(1), eq(actualizado.getCodigo()), eq(2), eq(List.of("Docente")), eq(false),
                        eq(actualizado.getFechaFinVigencia()), eq(2), eq(40));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> authService.cambiarRol(1, 2, 1, 40, 1));

        assertSame(fallo, ex);
        verify(usuarioRepository).incrementarVersionTokenSiCoincide(1, 1, AccesoConstants.ELIMINADO);
        verify(usuarioRepository).save(any());
    }

    @Test
    void cambiarRolRechazaDispositivoQueYaNoPerteneceALaSesion() {
        stubAccesoRepository();
        Usuario original = usuario(0);
        original.setVersionToken(1);
        Usuario actualizado = usuario(0);
        actualizado.setVersionToken(2);
        when(usuarioRepository.findByIdUsuarioAndAccesoNot(eq(1), any(Acceso.class)))
                .thenReturn(Optional.of(original), Optional.of(actualizado));
        when(rolAsignadoResolver.resolver(1)).thenReturn(List.of(rolActivo()));
        when(usuarioRepository.incrementarVersionTokenSiCoincide(1, 1, AccesoConstants.ELIMINADO)).thenReturn(1);
        when(usuarioRepository.tieneDispositivo(1, 40)).thenReturn(false);

        assertThrows(AccesoDenegadoException.class,
                () -> authService.cambiarRol(1, 2, 1, 40, 1));

        verifyNoInteractions(jwtService);
    }

    @Test
    void cambiarRolHaciaAdminRespetaRolActivoYConservaDispositivo() {
        stubAccesoRepository();
        Usuario original = usuario(0);
        original.setVersionToken(1);
        Usuario actualizado = usuario(0);
        actualizado.setVersionToken(2);
        when(usuarioRepository.findByIdUsuarioAndAccesoNot(eq(1), any(Acceso.class)))
                .thenReturn(Optional.of(original), Optional.of(actualizado));
        when(rolAsignadoResolver.resolver(1)).thenReturn(List.of(
                rolActivo(), rolAsignado(3, "ADMIN", EstadoRol.ACTIVO)));
        when(usuarioRepository.incrementarVersionTokenSiCoincide(1, 1, AccesoConstants.ELIMINADO)).thenReturn(1);
        when(usuarioRepository.tieneDispositivo(1, 40)).thenReturn(true);
        stubToken("jwt-admin", actualizado, 3, List.of("ADMIN"), 2, 40);
        stubPermisos(3);

        LoginResponse response = authService.cambiarRol(1, 3, 1, 40, 1);

        assertTrue(response.isEsAdmin());
        assertEquals(3, response.getPermisos().getIdRol());
        assertEquals("ADMIN", response.getPermisos().getNombreRol());
        verificarToken(actualizado, 3, List.of("ADMIN"), 2, 40);
    }
}
