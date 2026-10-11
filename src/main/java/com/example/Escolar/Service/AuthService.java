package com.example.Escolar.Service;

import com.example.Escolar.Dto.LoginRequest;
import com.example.Escolar.Dto.LoginResponse;
import com.example.Escolar.Dto.PermisosRolResponse;
import com.example.Escolar.Exception.CodigoRecuperacionAgotadoException;
import com.example.Escolar.Exception.CuentaBloqueadaException;
import com.example.Escolar.Exception.CuentaInactivaException;
import com.example.Escolar.Exception.EsperaReenvioCodigoException;
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
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    public static final int MAX_INTENTOS_FALLIDOS = 5;
    public static final int MINUTOS_TOKEN_RESET = 15;
    public static final int MAX_INTENTOS_CODIGO = 5;
    public static final int SEGUNDOS_REENVIO_CODIGO = 45;
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
        // Inactivo: no cuenta intentos y solo revela su estado si la contraseña es correcta.
        if (esInactivo(usuario)) {
            if (passwordEncoder.matches(request.getContraseña(), usuario.getContraseña())) {
                throw new CuentaInactivaException();
            }
            throw new BadCredentialsException("Credenciales incorrectas");
        }
        verificarBloqueo(usuario);
        if (!usuario.getAcceso().getIdAcceso().equals(AccesoConstants.ACTIVO) || !passwordEncoder.matches(request.getContraseña(), usuario.getContraseña())) {
            registrarIntentoFallido(usuario);
            throw new BadCredentialsException("Credenciales incorrectas");
        }
        usuario.setIntentosFallidos(0);
        usuario.setFechaBloqueo(null);
        usuarioRepository.save(usuario);
        List<String> roles = getRoles(usuario);
        String token = jwtService.generarToken(usuario.getIdUsuario(), usuario.getCodigo(), roles,
                Boolean.TRUE.equals(usuario.getDebeCambiarContrasena()));

        LoginResponse response = new LoginResponse();
        response.setToken(token);
        response.setUsuario(usuarioService.getById(usuario.getIdUsuario()));
        response.setPermisos(rolPermisoService.obtenerPermisosDelUsuario(usuario.getIdUsuario()));
        response.setEsAdmin(roles.stream().anyMatch(nombre -> "ADMIN".equalsIgnoreCase(nombre)));
        return response;
    }

    public PermisosRolResponse permisosActuales(Integer idUsuario) {
        return rolPermisoService.obtenerPermisosDelUsuario(idUsuario);
    }

    @Transactional
    public void forgotPassword(String gmail) {
        Usuario usuario = usuarioRepository.findByGmailAndAccesoNot(gmail, accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow())
                .orElseThrow(() -> new ResourceNotFoundException("No se encontro una cuenta con ese email"));
        if (esInactivo(usuario)) {
            throw new CuentaInactivaException();
        }
        LocalDateTime ahora = LocalDateTime.now();
        if (usuario.getResetTokenEnviado() != null) {
            long segundosRestantes = SEGUNDOS_REENVIO_CODIGO
                    - Duration.between(usuario.getResetTokenEnviado(), ahora).getSeconds();
            if (segundosRestantes > 0) {
                throw new EsperaReenvioCodigoException(segundosRestantes);
            }
        }

        String codigo = generarCodigo6Digitos();
        String codigoHash = generarTokenHash(codigo);

        usuario.setResetToken(codigoHash);
        usuario.setResetTokenExpiracion(ahora.plusMinutes(MINUTOS_TOKEN_RESET));
        usuario.setResetTokenEnviado(ahora);
        usuario.setIntentosCodigoReset(0);
        usuarioRepository.save(usuario);

        emailService.enviarCodigoRecuperacion(gmail, codigo);
        log.info("Código de recuperación enviado a {}", gmail);
    }

    // noRollbackFor: el conteo de intentos y la anulación del código deben
    // persistir aunque la validación termine en excepción.
    @Transactional(noRollbackFor = {ResourceNotFoundException.class, CodigoRecuperacionAgotadoException.class})
    public void verificarCodigo(String gmail, String codigo) {
        // Valida correo + codigo sin consumirlo: el token sigue vigente hasta
        // que resetPassword lo valide de nuevo al cambiar la contrasena.
        validarCodigoReset(buscarUsuarioPorGmail(gmail), codigo);
    }

    @Transactional(noRollbackFor = {ResourceNotFoundException.class, CodigoRecuperacionAgotadoException.class})
    public void resetPassword(String gmail, String codigo, String nuevaContrasena) {
        Usuario usuario = buscarUsuarioPorGmail(gmail);
        validarCodigoReset(usuario, codigo);

        usuario.setContraseña(passwordEncoder.encode(nuevaContrasena));
        usuario.setResetToken(null);
        usuario.setResetTokenExpiracion(null);
        usuario.setIntentosCodigoReset(0);
        usuario.setIntentosFallidos(0);
        usuario.setFechaBloqueo(null);
        usuario.setDebeCambiarContrasena(false);
        usuarioRepository.save(usuario);
    }

    /**
     * Devuelve un token nuevo sin la marca de cambio pendiente, para que la
     * sesión actual quede habilitada sin volver a iniciar sesión.
     */
    @Transactional
    public String cambiarContrasena(Integer idUsuario, String contrasenaActual, String nuevaContrasena) {
        Usuario usuario = usuarioRepository.findByIdUsuarioAndAccesoNot(idUsuario,
                        accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow())
                .orElseThrow(() -> new ResourceNotFoundException("No se encontro la cuenta del usuario autenticado"));

        if (!passwordEncoder.matches(contrasenaActual, usuario.getContraseña())) {
            throw new IllegalArgumentException("La contrasena actual es incorrecta");
        }
        if (passwordEncoder.matches(nuevaContrasena, usuario.getContraseña())) {
            throw new IllegalArgumentException("La nueva contrasena debe ser diferente a la actual");
        }

        usuario.setContraseña(passwordEncoder.encode(nuevaContrasena));
        usuario.setIntentosFallidos(0);
        usuario.setFechaBloqueo(null);
        usuario.setDebeCambiarContrasena(false);
        usuarioRepository.save(usuario);
        return jwtService.generarToken(usuario.getIdUsuario(), usuario.getCodigo(), getRoles(usuario));
    }

    private Usuario buscarUsuarioPorGmail(String gmail) {
        return usuarioRepository.findByGmailAndAccesoNot(gmail, accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow())
                .orElseThrow(() -> new ResourceNotFoundException("No se encontro una cuenta con ese email"));
    }

    // Validacion comun entre verificarCodigo y resetPassword. La expiracion se
    // revisa antes que el hash para que un codigo vencido no consuma intentos;
    // al agotar los intentos el codigo se anula y hay que solicitar otro.
    private void validarCodigoReset(Usuario usuario, String codigo) {
        if (esInactivo(usuario)) {
            throw new CuentaInactivaException();
        }
        if (usuario.getResetToken() == null) {
            throw new ResourceNotFoundException("No se ha solicitado recuperación de contraseña. Solicite un código primero.");
        }

        if (usuario.getResetTokenExpiracion() == null || LocalDateTime.now().isAfter(usuario.getResetTokenExpiracion())) {
            usuario.setResetToken(null);
            usuario.setResetTokenExpiracion(null);
            usuarioRepository.save(usuario);
            throw new ResourceNotFoundException("El código ha expirado. Solicite uno nuevo.");
        }

        String codigoHash = generarTokenHash(codigo);
        if (!codigoHash.equals(usuario.getResetToken())) {
            int intentos = (usuario.getIntentosCodigoReset() == null ? 0 : usuario.getIntentosCodigoReset()) + 1;
            usuario.setIntentosCodigoReset(intentos);
            if (intentos >= MAX_INTENTOS_CODIGO) {
                usuario.setResetToken(null);
                usuario.setResetTokenExpiracion(null);
                usuarioRepository.save(usuario);
                throw new CodigoRecuperacionAgotadoException();
            }
            usuarioRepository.save(usuario);
            int restantes = MAX_INTENTOS_CODIGO - intentos;
            throw new ResourceNotFoundException("El código ingresado es incorrecto. Te queda"
                    + (restantes == 1 ? "" : "n") + " " + restantes + " intento" + (restantes == 1 ? "" : "s") + ".");
        }
    }

    private boolean esInactivo(Usuario usuario) {
        return AccesoConstants.INACTIVO.equals(usuario.getAcceso().getIdAcceso());
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
            throw new CuentaBloqueadaException("Tu cuenta está bloqueada por intentos fallidos. Recupera tu contraseña o comunícate con el administrador.");
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
