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
import com.example.Escolar.Model.Usuario;
import com.example.Escolar.Repository.AccesoRepository;
import com.example.Escolar.Repository.UsuarioRepository;
import com.example.Escolar.Security.JwtService;
import com.example.Escolar.Service.AccesoContextoService.Alcance;
import com.example.Escolar.Util.HashSeguro;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    public static final int MAX_INTENTOS_FALLIDOS = 5;
    public static final int MINUTOS_TOKEN_RESET = 15;
    public static final int MAX_INTENTOS_CODIGO = 5;
    public static final int SEGUNDOS_REENVIO_CODIGO = 45;
    public static final String MENSAJE_BLOQUEO =
            "Tu cuenta está bloqueada por intentos fallidos. Recupera tu contraseña o comunícate con el administrador.";

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final UsuarioService usuarioService;
    private final RolPermisoService rolPermisoService;
    private final AccesoRepository accesoRepository;
    private final EmailService emailService;
    private final AccesoContextoService accesoContextoService;
    private final VigenciaService vigenciaService;
    private final SeleccionRolDesafioService seleccionRolDesafioService;
    private final RolAsignadoResolver rolAsignadoResolver;

    /** Contraseña y sesión en un paso (sin doble factor). */
    public LoginResponse login(LoginRequest request) {
        return iniciarSesion(validarCredenciales(request));
    }

    /**
     * Valida código y contraseña sin abrir sesión. Los intentos fallidos no se
     * reinician aquí: con doble factor, los errores del código suman al mismo
     * contador hasta que el ingreso se completa.
     */
    public Usuario validarCredenciales(LoginRequest request) {
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
        validarVigencia(usuario);
        return usuario;
    }

    public LoginResponse iniciarSesion(Usuario usuario) {
        return iniciarSesion(usuario, null);
    }

    public LoginResponse iniciarSesion(Usuario usuario, Integer idDispositivo) {
        validarVigencia(usuario);
        SeleccionRolDesafioService.Evaluacion evaluacion = seleccionRolDesafioService.evaluar(usuario.getIdUsuario());
        if (evaluacion.resultado() == SeleccionRolDesafioService.Resultado.SIN_ROLES_DISPONIBLES) {
            throw new AccesoDenegadoException("La cuenta no tiene un rol activo disponible para iniciar sesión.");
        }
        if (evaluacion.resultado() == SeleccionRolDesafioService.Resultado.SELECCION_REQUERIDA) {
            LoginResponse response = new LoginResponse();
            response.setUsuario(usuarioService.getById(usuario.getIdUsuario()));
            response.setRequiereSeleccionRol(true);
            response.setDesafioSeleccionRol(seleccionRolDesafioService.emitir(usuario, idDispositivo));
            response.setRoles(evaluacion.roles());
            return response;
        }
        return emitirSesion(usuario, idDispositivo, evaluacion.rolesSeleccionables().get(0));
    }

    @Transactional
    public LoginResponse cambiarRol(Integer idUsuario, Integer idRolSolicitado, Integer idRolActual,
                                    Integer idDispositivo, Integer versionToken) {
        if (Objects.equals(idRolSolicitado, idRolActual)) {
            throw new IllegalArgumentException("El rol ya está activo en la sesión.");
        }

        Usuario usuario = usuarioRepository.findByIdUsuarioAndAccesoNot(idUsuario,
                        accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow())
                .orElseThrow(() -> new AccesoDenegadoException("La cuenta ya no está disponible."));
        validarAccesoActivo(usuario);
        validarVigencia(usuario);

        RolAsignadoSesionResponse rolSolicitado = rolAsignadoResolver.resolver(idUsuario).stream()
                .filter(rol -> Objects.equals(idRolSolicitado, rol.idRol()))
                .filter(rol -> rol.estado() == EstadoRol.ACTIVO)
                .findFirst()
                .orElseThrow(() -> new AccesoDenegadoException(
                        "El rol solicitado no está disponible para esta cuenta."));

        int versionEsperada = versionToken == null ? 0 : versionToken;
        int actualizados = usuarioRepository.incrementarVersionTokenSiCoincide(
                idUsuario, versionEsperada, AccesoConstants.ELIMINADO);
        if (actualizados != 1) {
            throw new VigenciaNoValidaException(VigenciaNoValidaException.CODIGO_TOKEN_OBSOLETO,
                    "Su sesión quedó obsoleta. Vuelva a iniciar sesión.");
        }

        Usuario actualizado = usuarioRepository.findByIdUsuarioAndAccesoNot(idUsuario,
                        accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow())
                .orElseThrow(() -> new AccesoDenegadoException("La cuenta ya no está disponible."));
        return emitirSesion(actualizado, idDispositivo, rolSolicitado);
    }

    public LoginResponse emitirSesion(Usuario usuario, Integer idDispositivo, RolAsignadoSesionResponse rol) {
        Usuario actual = usuarioRepository.findByIdUsuarioAndAccesoNot(usuario.getIdUsuario(),
                        accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow())
                .orElseThrow(() -> new AccesoDenegadoException("La cuenta ya no está disponible."));
        validarContextoEmision(usuario, actual);
        RolAsignadoSesionResponse rolActivo = rolActivo(actual.getIdUsuario(), rol == null ? null : rol.idRol());
        if (idDispositivo != null
                && !usuarioRepository.tieneDispositivo(actual.getIdUsuario(), idDispositivo)) {
            throw new AccesoDenegadoException("El dispositivo de la sesión ya no está disponible.");
        }

        actual.setIntentosFallidos(0);
        actual.setFechaBloqueo(null);
        usuarioRepository.save(actual);
        List<String> roles = List.of(rolActivo.nombre());
        String token = generarToken(actual, rolActivo.idRol(), roles,
                Boolean.TRUE.equals(actual.getDebeCambiarContrasena()), idDispositivo);

        LoginResponse response = new LoginResponse();
        response.setToken(token);
        response.setUsuario(usuarioService.getById(actual.getIdUsuario()));
        response.setPermisos(permisosActuales(actual.getIdUsuario(), rolActivo.idRol()));
        response.setEsAdmin("ADMIN".equalsIgnoreCase(rolActivo.nombre()));
        return response;
    }

    public PermisosRolResponse permisosActuales(Integer idUsuario, Integer idRolActivo) {
        PermisosRolResponse permisos = rolPermisoService.obtenerPermisosDelUsuario(idUsuario, idRolActivo);
        Alcance vinculo = accesoContextoService.vinculoEfectivo(idUsuario, idRolActivo);
        permisos.setVinculo(vinculo == Alcance.TODO || vinculo == Alcance.NINGUNO ? null : vinculo.name());
        return permisos;
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
    public String cambiarContrasena(Integer idUsuario, String contrasenaActual, String nuevaContrasena) {
        return cambiarContrasena(idUsuario, contrasenaActual, nuevaContrasena, null, null);
    }

    public String cambiarContrasena(Integer idUsuario, String contrasenaActual, String nuevaContrasena,
                                    Integer idDispositivo) {
        return cambiarContrasena(idUsuario, contrasenaActual, nuevaContrasena, idDispositivo, null);
    }

    @Transactional
    public String cambiarContrasena(Integer idUsuario, String contrasenaActual, String nuevaContrasena,
                                    Integer idDispositivo, Integer idRolActivo) {
        Usuario usuario = usuarioRepository.findByIdUsuarioAndAccesoNot(idUsuario,
                        accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow())
                .orElseThrow(() -> new ResourceNotFoundException("No se encontro la cuenta del usuario autenticado"));
        validarVigencia(usuario);

        if (!passwordEncoder.matches(contrasenaActual, usuario.getContraseña())) {
            throw new IllegalArgumentException("La contrasena actual es incorrecta");
        }
        if (passwordEncoder.matches(nuevaContrasena, usuario.getContraseña())) {
            throw new IllegalArgumentException("La nueva contrasena debe ser diferente a la actual");
        }
        RolAsignadoSesionResponse rolActivo = rolActivo(idUsuario, idRolActivo);
        if (idDispositivo != null && !usuarioRepository.tieneDispositivo(idUsuario, idDispositivo)) {
            throw new AccesoDenegadoException("El dispositivo de la sesión ya no está disponible.");
        }

        usuario.setContraseña(passwordEncoder.encode(nuevaContrasena));
        usuario.setIntentosFallidos(0);
        usuario.setFechaBloqueo(null);
        usuario.setDebeCambiarContrasena(false);
        usuarioRepository.save(usuario);
        return generarToken(usuario, rolActivo.idRol(), List.of(rolActivo.nombre()), false, idDispositivo);
    }

    private void validarVigencia(Usuario usuario) {
        if (vigenciaService.datosNoDisponibles(usuario.getFechaInicioVigencia(),
                usuario.getFechaFinVigencia())) {
            throw new VigenciaNoValidaException(VigenciaNoValidaException.CODIGO_NO_DISPONIBLE,
                    VigenciaNoValidaException.MENSAJE_NO_DISPONIBLE);
        }
        if (!vigenciaService.esValida(usuario.getFechaInicioVigencia(), usuario.getFechaFinVigencia())) {
            throw new VigenciaNoValidaException(
                    vigenciaService.evaluar(usuario.getFechaInicioVigencia(), usuario.getFechaFinVigencia()),
                    usuario.getFechaInicioVigencia(),
                    usuario.getFechaFinVigencia());
        }
    }

    private void validarContextoEmision(Usuario contexto, Usuario actual) {
        validarAccesoActivo(contexto);
        validarVigencia(contexto);
        validarAccesoActivo(actual);
        validarVigencia(actual);
        Integer versionContexto = contexto.getVersionToken() == null ? 0 : contexto.getVersionToken();
        Integer versionActual = actual.getVersionToken() == null ? 0 : actual.getVersionToken();
        if (!Objects.equals(versionContexto, versionActual)) {
            throw new VigenciaNoValidaException(VigenciaNoValidaException.CODIGO_TOKEN_OBSOLETO,
                    "Su sesión quedó obsoleta. Vuelva a iniciar sesión.");
        }
    }

    private void validarAccesoActivo(Usuario usuario) {
        if (usuario == null || usuario.getAcceso() == null
                || !AccesoConstants.ACTIVO.equals(usuario.getAcceso().getIdAcceso())) {
            throw new VigenciaNoValidaException(VigenciaNoValidaException.CODIGO_CUENTA_INACTIVA,
                    VigenciaNoValidaException.MENSAJE_CUENTA_INACTIVA);
        }
    }

    private RolAsignadoSesionResponse rolActivo(Integer idUsuario, Integer idRolActivo) {
        List<RolAsignadoSesionResponse> activos = rolAsignadoResolver.resolver(idUsuario).stream()
                .filter(rol -> rol.estado() == EstadoRol.ACTIVO)
                .filter(rol -> idRolActivo == null || Objects.equals(idRolActivo, rol.idRol()))
                .toList();
        if (activos.size() != 1) {
            throw new AccesoDenegadoException("El rol activo de la sesión ya no está disponible.");
        }
        return activos.get(0);
    }

    private String generarToken(Usuario usuario, Integer idRolActivo, List<String> roles,
                                boolean cambioContrasenaPendiente, Integer idDispositivo) {
        Integer versionToken = usuario.getVersionToken() == null ? 0 : usuario.getVersionToken();
        return jwtService.generarToken(usuario.getIdUsuario(), usuario.getCodigo(), idRolActivo, roles,
                cambioContrasenaPendiente, usuario.getFechaFinVigencia(), versionToken, idDispositivo);
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
        return HashSeguro.codigo6Digitos();
    }

    private void verificarBloqueo(Usuario usuario) {
        if (usuario.getFechaBloqueo() == null) {
            return;
        }
        if (usuario.getIntentosFallidos() != null && usuario.getIntentosFallidos() >= MAX_INTENTOS_FALLIDOS) {
            throw new CuentaBloqueadaException(MENSAJE_BLOQUEO);
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
        return HashSeguro.sha256(token);
    }

}
