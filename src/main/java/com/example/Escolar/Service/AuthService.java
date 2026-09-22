package com.example.Escolar.Service;

import com.example.Escolar.Dto.LoginRequest;
import com.example.Escolar.Dto.LoginResponse;
import com.example.Escolar.Exception.CuentaBloqueadaException;
import com.example.Escolar.Exception.ResourceNotFoundException;
import com.example.Escolar.Model.Rol;
import com.example.Escolar.Model.Usuario;
import com.example.Escolar.Model.UsuarioRol;
import com.example.Escolar.Repository.AccesoRepository;
import com.example.Escolar.Repository.UsuarioRepository;
import com.example.Escolar.Repository.UsuarioRolRepository;
import com.example.Escolar.Security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    public static final int MAX_INTENTOS_FALLIDOS = 5;
    public static final int MINUTOS_TOKEN_RESET = 15;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UsuarioRepository usuarioRepository;
    private final UsuarioRolRepository usuarioRolRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final UsuarioService usuarioService;
    private final RolPermisoService rolPermisoService;
    private final AccesoRepository accesoRepository;
    private final EmailService emailService;

    public LoginResponse login(LoginRequest request) {
        Usuario usuario = usuarioRepository.findByCodigoAndAccesoNot(request.getCodigo(), accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow())
                .orElseThrow(() -> new BadCredentialsException("Credenciales incorrectas"));
        verificarBloqueo(usuario);
        if (!usuario.getAcceso().getIdAcceso().equals(AccesoConstants.ACTIVO) || !passwordEncoder.matches(request.getContraseña(), usuario.getContraseña())) {
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

    @Transactional
    public void forgotPassword(String gmail) {
        Usuario usuario = usuarioRepository.findByGmailAndAccesoNot(gmail, accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow())
                .orElseThrow(() -> new ResourceNotFoundException("No se encontro una cuenta con ese email"));

        String codigo = generarCodigo6Digitos();
        String codigoHash = generarTokenHash(codigo);

        usuario.setResetToken(codigoHash);
        usuario.setResetTokenExpiracion(LocalDateTime.now().plusMinutes(MINUTOS_TOKEN_RESET));
        usuarioRepository.save(usuario);

        emailService.enviarCodigoRecuperacion(gmail, codigo);
        log.info("Código de recuperación enviado a {}", gmail);
    }

    @Transactional
    public void resetPassword(String gmail, String codigo, String nuevaContrasena) {
        Usuario usuario = usuarioRepository.findByGmailAndAccesoNot(gmail, accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow())
                .orElseThrow(() -> new ResourceNotFoundException("No se encontro una cuenta con ese email"));

        if (usuario.getResetToken() == null) {
            throw new ResourceNotFoundException("No se ha solicitado recuperación de contraseña. Solicite un código primero.");
        }

        String codigoHash = generarTokenHash(codigo);
        if (!codigoHash.equals(usuario.getResetToken())) {
            throw new ResourceNotFoundException("El código ingresado es incorrecto");
        }

        if (usuario.getResetTokenExpiracion() == null || LocalDateTime.now().isAfter(usuario.getResetTokenExpiracion())) {
            usuario.setResetToken(null);
            usuario.setResetTokenExpiracion(null);
            usuarioRepository.save(usuario);
            throw new ResourceNotFoundException("El código ha expirado. Solicite uno nuevo.");
        }

        usuario.setContraseña(passwordEncoder.encode(nuevaContrasena));
        usuario.setResetToken(null);
        usuario.setResetTokenExpiracion(null);
        usuario.setIntentosFallidos(0);
        usuario.setFechaBloqueo(null);
        usuarioRepository.save(usuario);
    }

    private String generarCodigo6Digitos() {
        int codigo = 100000 + SECURE_RANDOM.nextInt(900000);
        return String.valueOf(codigo);
    }

    private void verificarBloqueo(Usuario usuario) {
        if (usuario.getFechaBloqueo() == null) {
            return;
        }
        if (usuario.getIntentosFallidos() != null && usuario.getIntentosFallidos() >= MAX_INTENTOS_FALLIDOS) {
            throw new CuentaBloqueadaException("Cuenta bloqueada permanentemente por intentos fallidos. Contacte al administrador.");
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

    private String generarTokenHash(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalArgumentException("No se pudo generar el hash del token");
        }
    }

    private List<String> getRoles(Usuario usuario) {
        return usuarioRolRepository.findByUsuarioIdUsuario(usuario.getIdUsuario()).stream()
                .map(UsuarioRol::getRol)
                .filter(rol -> !rol.getAcceso().getIdAcceso().equals(AccesoConstants.ELIMINADO))
                .map(Rol::getNombre)
                .toList();
    }
}
