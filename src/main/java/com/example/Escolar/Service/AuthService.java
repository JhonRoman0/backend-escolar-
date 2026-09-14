package com.example.Escolar.Service;

import com.example.Escolar.Dto.LoginRequest;
import com.example.Escolar.Dto.LoginResponse;
import com.example.Escolar.Exception.CuentaBloqueadaException;
import com.example.Escolar.Model.Rol;
import com.example.Escolar.Model.Usuario;
import com.example.Escolar.Model.UsuarioRol;
import com.example.Escolar.Repository.UsuarioRepository;
import com.example.Escolar.Repository.UsuarioRolRepository;
import com.example.Escolar.Security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthService {

    public static final byte ACCESO_ACTIVO = 1;
    public static final byte ACCESO_ELIMINADO = 2;
    public static final int MAX_INTENTOS_FALLIDOS = 5;
    public static final long MINUTOS_BLOQUEO = 30;

    private final UsuarioRepository usuarioRepository;
    private final UsuarioRolRepository usuarioRolRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final UsuarioService usuarioService;
    private final RolPermisoService rolPermisoService;

    public LoginResponse login(LoginRequest request) {
        Usuario usuario = usuarioRepository.findByCodigoAndAccesoNot(request.getCodigo(), ACCESO_ELIMINADO)
                .orElseThrow(() -> new BadCredentialsException("Credenciales incorrectas"));
        verificarBloqueo(usuario);
        if (usuario.getAcceso() != ACCESO_ACTIVO || !passwordEncoder.matches(request.getContraseña(), usuario.getContraseña())) {
            registrarIntentoFallido(usuario);
            throw new BadCredentialsException("Credenciales incorrectas");
        }
        usuario.setIntentosFallidos(0);
        usuario.setFechaBloqueo(null);
        usuarioRepository.save(usuario);
        List<String> roles = getRoles(usuario);
        String token = jwtService.generarToken(usuario.getIdUsuario(), usuario.getCodigo(), roles);

        LoginResponse response = new LoginResponse();
        response.setToken(token);
        response.setUsuario(usuarioService.getById(usuario.getIdUsuario()));
        response.setPermisos(rolPermisoService.obtenerPermisosDelUsuario(usuario.getIdUsuario()));
        response.setEsAdmin(roles.stream().anyMatch(nombre -> "ADMIN".equalsIgnoreCase(nombre)));
        return response;
    }

    private void verificarBloqueo(Usuario usuario) {
        if (usuario.getFechaBloqueo() == null) {
            return;
        }
        LocalDateTime finBloqueo = usuario.getFechaBloqueo().plusMinutes(MINUTOS_BLOQUEO);
        if (LocalDateTime.now().isBefore(finBloqueo)) {
            throw new CuentaBloqueadaException("Cuenta bloqueada temporalmente por intentos fallidos. Intente de nuevo más tarde.");
        }
        usuario.setFechaBloqueo(null);
        usuario.setIntentosFallidos(0);
    }

    private void registrarIntentoFallido(Usuario usuario) {
        int intentos = (usuario.getIntentosFallidos() == null ? 0 : usuario.getIntentosFallidos()) + 1;
        usuario.setIntentosFallidos(intentos);
        if (intentos >= MAX_INTENTOS_FALLIDOS) {
            usuario.setFechaBloqueo(LocalDateTime.now());
        }
        usuarioRepository.save(usuario);
    }

    private List<String> getRoles(Usuario usuario) {
        return usuarioRolRepository.findByUsuarioIdUsuario(usuario.getIdUsuario()).stream()
                .map(UsuarioRol::getRol)
                .filter(rol -> rol.getAcceso() != ACCESO_ELIMINADO)
                .map(Rol::getNombre)
                .toList();
    }
}
