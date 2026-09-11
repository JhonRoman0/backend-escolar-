package com.example.Escolar.Service;

import com.example.Escolar.Dto.LoginRequest;
import com.example.Escolar.Dto.LoginResponse;
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

import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthService {

    public static final byte ACCESO_ACTIVO = 1;
    public static final byte ACCESO_ELIMINADO = 2;

    private final UsuarioRepository usuarioRepository;
    private final UsuarioRolRepository usuarioRolRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final UsuarioService usuarioService;
    private final RolPermisoService rolPermisoService;

    public LoginResponse login(LoginRequest request) {
        Usuario usuario = usuarioRepository.findByCodigoAndAccesoNot(request.getCodigo(), ACCESO_ELIMINADO)
                .orElseThrow(() -> new BadCredentialsException("Credenciales incorrectas"));
        if (usuario.getAcceso() != ACCESO_ACTIVO || !passwordEncoder.matches(request.getContraseña(), usuario.getContraseña())) {
            throw new BadCredentialsException("Credenciales incorrectas");
        }
        List<String> roles = getRoles(usuario);
        String token = jwtService.generarToken(usuario.getIdUsuario(), usuario.getCodigo(), roles);

        LoginResponse response = new LoginResponse();
        response.setToken(token);
        response.setUsuario(usuarioService.getById(usuario.getIdUsuario()));
        response.setPermisos(rolPermisoService.obtenerPermisosDelUsuario(usuario.getIdUsuario()));
        response.setEsAdmin(roles.stream().anyMatch(nombre -> "ADMIN".equalsIgnoreCase(nombre)));
        return response;
    }

    private List<String> getRoles(Usuario usuario) {
        return usuarioRolRepository.findByUsuarioIdUsuario(usuario.getIdUsuario()).stream()
                .map(UsuarioRol::getRol)
                .filter(rol -> rol.getAcceso() != ACCESO_ELIMINADO)
                .map(Rol::getNombre)
                .toList();
    }
}
