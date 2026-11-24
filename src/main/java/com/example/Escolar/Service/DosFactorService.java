package com.example.Escolar.Service;

import com.example.Escolar.Dto.DispositivoResponse;
import com.example.Escolar.Exception.CuentaBloqueadaException;
import com.example.Escolar.Exception.DosFactorVencidoException;
import com.example.Escolar.Exception.EnvioCodigoFallidoException;
import com.example.Escolar.Exception.EsperaReenvioCodigoException;
import com.example.Escolar.Exception.LimiteEnviosCodigoException;
import com.example.Escolar.Exception.ResourceNotFoundException;
import com.example.Escolar.Model.DispositivoUsuario;
import com.example.Escolar.Model.Usuario;
import com.example.Escolar.Repository.DispositivoUsuarioRepository;
import com.example.Escolar.Repository.UsuarioRepository;
import com.example.Escolar.Util.FechaColegio;
import com.example.Escolar.Util.HashSeguro;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/**
 * Doble factor por correo: tras la contraseña se envía un código de 6 dígitos
 * que completa el inicio de sesión. Sigue el mismo esquema que el código de
 * recuperación (hash, vencimiento, espera para reenviar) y además recuerda los
 * navegadores del usuario para avisar de uno nuevo y, si lo pide, no volver a
 * pedir el código durante unos días.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DosFactorService {

    public static final int MINUTOS_CODIGO = 5;
    /** Tiempo para terminar el inicio de sesión (permite reenviar el código). */
    public static final int MINUTOS_DESAFIO = 15;
    public static final int MAX_ENVIOS_POR_HORA = 5;
    public static final int DIAS_CONFIANZA = 7;
    public static final String COOKIE_DISPOSITIVO = "escolar_dispositivo";
    public static final long SEGUNDOS_COOKIE_DISPOSITIVO = Duration.ofDays(365).toSeconds();

    private static final DateTimeFormatter FORMATO_FECHA =
            DateTimeFormatter.ofPattern("dd/MM/yyyy 'a las' HH:mm", Locale.forLanguageTag("es-PE"));

    private final UsuarioRepository usuarioRepository;
    private final DispositivoUsuarioRepository dispositivoRepository;
    private final EmailService emailService;
    private final EventosService eventosService;

    @Value("${app.dos-factor.activo:false}")
    private boolean activo;

    /** Quién y desde dónde inicia sesión. */
    public record Contexto(String navegador, String ip) {
    }

    /** Inicio de sesión a medias: lo que el navegador necesita para el paso del código. */
    public record Desafio(String desafio, String correoEnmascarado, long segundosReenvio, int minutosCodigo) {
    }

    /** Código correcto: el usuario, el valor de la cookie de su navegador y su registro. */
    public record Verificado(Usuario usuario, String tokenDispositivo, Integer idDispositivo) {
    }

    public boolean activo() {
        return activo;
    }

    /**
     * El backend no arranca si el doble factor está activo y algún administrador no
     * tiene correo: no podría volver a entrar.
     */
    public void verificarAdministradores() {
        if (!activo) {
            return;
        }
        List<String> sinCorreo = usuarioRepository.administradoresSinCorreo(AccesoConstants.ACTIVO, AccesoConstants.ELIMINADO);
        if (!sinCorreo.isEmpty()) {
            throw new IllegalStateException("El doble factor está activo y estos administradores no tienen correo: "
                    + String.join(", ", sinCorreo) + ". Registra su correo (o define JWT_ADMIN_GMAIL) antes de arrancar.");
        }
    }

    public boolean esDeConfianza(Usuario usuario, String tokenDispositivo) {
        return dispositivoDeConfianza(usuario, tokenDispositivo).isPresent();
    }

    /** Si este navegador es de confianza para el usuario, anota el uso y devuelve su id. */
    public Optional<Integer> dispositivoDeConfianza(Usuario usuario, String tokenDispositivo) {
        if (tokenDispositivo == null || tokenDispositivo.isBlank()) {
            return Optional.empty();
        }
        LocalDateTime ahora = LocalDateTime.now();
        return dispositivoRepository.findByUsuarioIdUsuarioAndTokenHash(usuario.getIdUsuario(), HashSeguro.sha256(tokenDispositivo))
                .filter(d -> confiable(d, usuario, ahora))
                .map(d -> {
                    d.setUltimoUso(ahora);
                    dispositivoRepository.save(d);
                    eventosService.avisarCuenta(usuario.getIdUsuario());
                    return d.getIdDispositivo();
                });
    }

    /** Contraseña correcta: crea el desafío y envía el primer código. */
    public Desafio iniciar(Usuario usuario, Contexto contexto) {
        String correo = correoDe(usuario);
        LocalDateTime ahora = LocalDateTime.now();
        validarEnviosPorHora(usuario, ahora);
        String desafio = HashSeguro.tokenAleatorio();
        usuario.setDosFactorDesafio(HashSeguro.sha256(desafio));
        usuario.setDosFactorDesafioExpiracion(ahora.plusMinutes(MINUTOS_DESAFIO));
        enviarCodigo(usuario, correo, contexto, ahora);
        return new Desafio(desafio, enmascarar(correo), AuthService.SEGUNDOS_REENVIO_CODIGO, MINUTOS_CODIGO);
    }

    /** Código nuevo para el mismo desafío; el anterior deja de valer. */
    public Desafio reenviar(String desafio, Contexto contexto) {
        Usuario usuario = porDesafio(desafio);
        String correo = correoDe(usuario);
        LocalDateTime ahora = LocalDateTime.now();
        if (usuario.getDosFactorEnviado() != null) {
            long faltan = AuthService.SEGUNDOS_REENVIO_CODIGO
                    - Duration.between(usuario.getDosFactorEnviado(), ahora).getSeconds();
            if (faltan > 0) {
                throw new EsperaReenvioCodigoException(faltan);
            }
        }
        validarEnviosPorHora(usuario, ahora);
        enviarCodigo(usuario, correo, contexto, ahora);
        return new Desafio(desafio, enmascarar(correo), AuthService.SEGUNDOS_REENVIO_CODIGO, MINUTOS_CODIGO);
    }

    /**
     * Valida el código. Los errores suman a los intentos fallidos de la cuenta (los
     * mismos de la contraseña): al llegar al tope la cuenta se bloquea.
     */
    public Verificado verificar(String desafio, String codigo, boolean recordar, String tokenDispositivo,
                                Contexto contexto) {
        Usuario usuario = porDesafio(desafio);
        int intentos = usuario.getIntentosFallidos() == null ? 0 : usuario.getIntentosFallidos();
        if (usuario.getFechaBloqueo() != null && intentos >= AuthService.MAX_INTENTOS_FALLIDOS) {
            throw new CuentaBloqueadaException(AuthService.MENSAJE_BLOQUEO);
        }
        LocalDateTime ahora = LocalDateTime.now();
        // El vencimiento se revisa antes que el código para que uno vencido no gaste intentos.
        if (usuario.getDosFactorCodigo() == null || usuario.getDosFactorCodigoExpiracion() == null
                || ahora.isAfter(usuario.getDosFactorCodigoExpiracion())) {
            throw new ResourceNotFoundException("El código ha expirado. Solicita uno nuevo.");
        }
        if (!HashSeguro.sha256(codigo).equals(usuario.getDosFactorCodigo())) {
            intentos++;
            usuario.setIntentosFallidos(intentos);
            if (intentos >= AuthService.MAX_INTENTOS_FALLIDOS) {
                usuario.setFechaBloqueo(ahora);
                limpiar(usuario);
                usuarioRepository.save(usuario);
                throw new CuentaBloqueadaException(AuthService.MENSAJE_BLOQUEO);
            }
            usuarioRepository.save(usuario);
            int restantes = AuthService.MAX_INTENTOS_FALLIDOS - intentos;
            throw new ResourceNotFoundException("El código ingresado es incorrecto. Te queda"
                    + (restantes == 1 ? "" : "n") + " " + restantes + " intento" + (restantes == 1 ? "" : "s")
                    + " antes del bloqueo.");
        }
        limpiar(usuario);
        // El tope por hora frena códigos que nadie usa; uno bien usado lo reinicia
        // (entrar y salir varias veces no debe bloquear al dueño del correo).
        usuario.setDosFactorEnvios(0);
        usuario.setDosFactorVentana(null);
        Usuario guardado = usuarioRepository.save(usuario);
        String token = tokenDispositivo == null || tokenDispositivo.isBlank() || tokenDispositivo.length() > 100
                ? HashSeguro.tokenAleatorio() : tokenDispositivo;
        DispositivoUsuario dispositivo = registrarDispositivo(guardado, token, recordar, contexto, ahora);
        // Sus otras pestañas ven el dispositivo (nuevo o con su último uso) en Mi perfil.
        eventosService.avisarCuenta(guardado.getIdUsuario());
        return new Verificado(guardado, token, dispositivo.getIdDispositivo());
    }

    // ───────────── Dispositivos del usuario (Mi perfil) ─────────────

    public List<DispositivoResponse> dispositivos(Integer idUsuario, String tokenActual) {
        Usuario usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
        String hashActual = tokenActual == null || tokenActual.isBlank() ? null : HashSeguro.sha256(tokenActual);
        LocalDateTime ahora = LocalDateTime.now();
        return dispositivoRepository.findByUsuarioIdUsuarioOrderByUltimoUsoDesc(idUsuario).stream()
                .map(d -> new DispositivoResponse(d.getIdDispositivo(), d.getNavegador(), d.getIp(),
                        d.getPrimerUso(), d.getUltimoUso(),
                        confiable(d, usuario, ahora) ? d.getConfiableHasta() : null,
                        d.getTokenHash().equals(hashActual)))
                .toList();
    }

    /**
     * Quita el dispositivo: su sesión deja de valer (el filtro la rechaza), pierde la
     * confianza y, si vuelve a entrar, se le pide el código y se avisa como nuevo.
     */
    public void olvidar(Integer idUsuario, Integer idDispositivo) {
        dispositivoRepository.delete(dispositivoDe(idUsuario, idDispositivo));
        // Las pestañas abiertas del usuario revisan su sesión: la de ese dispositivo se cierra ya.
        eventosService.avisarCuenta(idUsuario);
    }

    // ───────────── Internos ─────────────

    private void enviarCodigo(Usuario usuario, String correo, Contexto contexto, LocalDateTime ahora) {
        String codigo = HashSeguro.codigo6Digitos();
        usuario.setDosFactorCodigo(HashSeguro.sha256(codigo));
        usuario.setDosFactorCodigoExpiracion(ahora.plusMinutes(MINUTOS_CODIGO));
        usuario.setDosFactorEnviado(ahora);
        usuarioRepository.save(usuario);
        boolean enviado = emailService.enviarCodigoDosFactor(correo, usuario.getNombre(), codigo, MINUTOS_CODIGO,
                contexto.navegador(), FORMATO_FECHA.format(LocalDateTime.now(FechaColegio.ZONA)));
        if (!enviado) {
            throw new EnvioCodigoFallidoException();
        }
    }

    // Tope por hora: evita que "reenviar" o los reintentos agoten el cupo del correo.
    private void validarEnviosPorHora(Usuario usuario, LocalDateTime ahora) {
        LocalDateTime ventana = usuario.getDosFactorVentana();
        if (ventana == null || !ahora.isBefore(ventana.plusHours(1))) {
            usuario.setDosFactorVentana(ahora);
            usuario.setDosFactorEnvios(0);
            ventana = ahora;
        }
        int envios = usuario.getDosFactorEnvios() == null ? 0 : usuario.getDosFactorEnvios();
        if (envios >= MAX_ENVIOS_POR_HORA) {
            long minutos = Math.max(1, Duration.between(ahora, ventana.plusHours(1)).toMinutes() + 1);
            throw new LimiteEnviosCodigoException(minutos);
        }
        usuario.setDosFactorEnvios(envios + 1);
    }

    private Usuario porDesafio(String desafio) {
        Usuario usuario = desafio == null || desafio.isBlank() ? null
                : usuarioRepository.findByDosFactorDesafio(HashSeguro.sha256(desafio)).orElse(null);
        if (usuario == null) {
            throw new DosFactorVencidoException("La verificación ya no es válida. Vuelve a iniciar sesión.");
        }
        if (usuario.getDosFactorDesafioExpiracion() == null
                || LocalDateTime.now().isAfter(usuario.getDosFactorDesafioExpiracion())) {
            limpiar(usuario);
            usuarioRepository.save(usuario);
            throw new DosFactorVencidoException("Se acabó el tiempo para verificar. Vuelve a iniciar sesión.");
        }
        if (!AccesoConstants.ACTIVO.equals(usuario.getAcceso().getIdAcceso())) {
            throw new DosFactorVencidoException("La verificación ya no es válida. Vuelve a iniciar sesión.");
        }
        return usuario;
    }

    // La cookie identifica al navegador; si varias personas lo comparten, cada
    // una tiene su propio registro con el mismo valor.
    private DispositivoUsuario registrarDispositivo(Usuario usuario, String token, boolean recordar, Contexto contexto,
                                                    LocalDateTime ahora) {
        String hash = HashSeguro.sha256(token);
        DispositivoUsuario d = dispositivoRepository.findByUsuarioIdUsuarioAndTokenHash(usuario.getIdUsuario(), hash)
                .orElse(null);
        if (d == null) {
            // El primer navegador de la cuenta no se avisa: no hay otro con qué compararlo.
            boolean tieneOtros = dispositivoRepository.existsByUsuarioIdUsuario(usuario.getIdUsuario());
            d = new DispositivoUsuario();
            d.setUsuario(usuario);
            d.setTokenHash(hash);
            d.setPrimerUso(ahora);
            if (tieneOtros) {
                emailService.enviarAvisoNuevoDispositivo(correoDe(usuario), usuario.getNombre(), contexto.navegador(),
                        contexto.ip(), FORMATO_FECHA.format(LocalDateTime.now(FechaColegio.ZONA)));
            }
        }
        d.setNavegador(recortar(contexto.navegador(), 80));
        d.setIp(recortar(contexto.ip(), 45));
        d.setUltimoUso(ahora);
        if (recordar) {
            d.setConfiableHasta(ahora.plusDays(DIAS_CONFIANZA));
            d.setHuellaConfianza(huella(usuario));
        }
        return dispositivoRepository.save(d);
    }

    private boolean confiable(DispositivoUsuario d, Usuario usuario, LocalDateTime ahora) {
        return d.getConfiableHasta() != null && ahora.isBefore(d.getConfiableHasta())
                && Objects.equals(d.getHuellaConfianza(), huella(usuario));
    }

    // Cambia si cambian la contraseña, el correo o la versión de sesión.
    private String huella(Usuario usuario) {
        String correo = usuario.getGmail() == null ? "" : usuario.getGmail().trim().toLowerCase(Locale.ROOT);
        int version = usuario.getVersionToken() == null ? 0 : usuario.getVersionToken();
        return HashSeguro.sha256(usuario.getContraseña() + "|" + correo + "|" + version);
    }

    private DispositivoUsuario dispositivoDe(Integer idUsuario, Integer idDispositivo) {
        return dispositivoRepository.findByIdDispositivoAndUsuarioIdUsuario(idDispositivo, idUsuario)
                .orElseThrow(() -> new ResourceNotFoundException("Dispositivo no encontrado"));
    }

    private void limpiar(Usuario usuario) {
        usuario.setDosFactorDesafio(null);
        usuario.setDosFactorDesafioExpiracion(null);
        usuario.setDosFactorCodigo(null);
        usuario.setDosFactorCodigoExpiracion(null);
    }

    private String correoDe(Usuario usuario) {
        if (usuario.getGmail() == null || usuario.getGmail().isBlank()) {
            throw new IllegalArgumentException("Tu cuenta no tiene un correo registrado. Comunícate con el administrador.");
        }
        return usuario.getGmail().trim();
    }

    /** "jairo@gmail.com" → "ja***@gmail.com". */
    static String enmascarar(String correo) {
        int arroba = correo.indexOf('@');
        if (arroba <= 0) {
            return correo;
        }
        String visible = correo.substring(0, Math.min(2, arroba));
        return visible + "***" + correo.substring(arroba);
    }

    private static String recortar(String valor, int largo) {
        return valor == null || valor.length() <= largo ? valor : valor.substring(0, largo);
    }
}
