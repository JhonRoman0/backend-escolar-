package com.example.Escolar.Service;

import com.example.Escolar.Dto.LoginRequest;
import com.example.Escolar.Exception.CuentaBloqueadaException;
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
    void bloqueoVencidoPermiteReintentar() {
        stubAccesoRepository();
        Usuario usuario = usuario(5);
        usuario.setFechaBloqueo(LocalDateTime.now().minusMinutes(31));
        when(usuarioRepository.findByCodigoAndAccesoNot(anyString(), any(Acceso.class))).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("clave", "$2a$10$hash")).thenReturn(false);

        assertThrows(BadCredentialsException.class, () -> authService.login(request()));

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(captor.capture());
        assertEquals(1, captor.getValue().getIntentosFallidos());
        assertNull(captor.getValue().getFechaBloqueo());
    }
}
