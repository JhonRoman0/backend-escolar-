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
import org.springframework.transaction.annotation.Transactional;

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

    public enum PropositoDesafio {
        DOS_FACTOR("2FA:"),
        SELECCION_ROL("SELROL:");

        private final String prefijo;

        PropositoDesafio(String prefijo) {
            this.prefijo = prefijo;
        }

        public String prefijo() {
            return prefijo;
        }

        public static PropositoDesafio desde(String desafio) {
            return desafio != null && desafio.startsWith(SELECCION_ROL.prefijo)
                    ? SELECCION_ROL
                    : DOS_FACTOR;
        }
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
    @Transactional
    public Desafio iniciar(Usuario usuario, Contexto contexto) {
        String correo = correoDe(usuario);
        LocalDateTime ahora = LocalDateTime.now();
        validarEnviosPorHora(usuario, ahora);
        String desafio = emitirDesafio(usuario, PropositoDesafio.DOS_FACTOR, MINUTOS_DESAFIO);
        enviarCodigo(usuario, correo, contexto, ahora, HashSeguro.sha256(desafio));
        return new Desafio(desafio, enmascarar(correo), AuthService.SEGUNDOS_REENVIO_CODIGO, MINUTOS_CODIGO);
    }

    /** Código nuevo para el mismo desafío; el anterior deja de valer. */
    @Transactional
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
        enviarCodigo(usuario, correo, contexto, ahora, HashSeguro.sha256(desafio));
        return new Desafio(desafio, enmascarar(correo), AuthService.SEGUNDOS_REENVIO_CODIGO, MINUTOS_CODIGO);
    }

    /**
     * Valida el código. Los errores suman a los intentos fallidos de la cuenta (los
     * mismos de la contraseña): al llegar al tope la cuenta se bloquea.
     */
    @Transactional(noRollbackFor = {ResourceNotFoundException.class, CuentaBloqueadaException.class})
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
            String hash = HashSeguro.sha256(desafio);
            LocalDateTime bloqueo = intentos >= AuthService.MAX_INTENTOS_FALLIDOS ? ahora : null;
            int afectadas = usuarioRepository.registrarIntentos(usuario.getIdUsuario(), hash, intentos, bloqueo);
            if (afectadas == 0) {
                throw new DosFactorVencidoException("La verificación ya no es válida. Vuelve a iniciar sesión.");
            }
            if (intentos >= AuthService.MAX_INTENTOS_FALLIDOS) {
                usuario.setFechaBloqueo(bloqueo);
                usuarioRepository.limpiarDesafioPorHash(usuario.getIdUsuario(), hash);
                limpiar(usuario);
                throw new CuentaBloqueadaException(AuthService.MENSAJE_BLOQUEO);
            }
            int restantes = AuthService.MAX_INTENTOS_FALLIDOS - intentos;
            throw new ResourceNotFoundException("El código ingresado es incorrecto. Te queda"
                    + (restantes == 1 ? "" : "n") + " " + restantes + " intento" + (restantes == 1 ? "" : "s")
                    + " antes del bloqueo.");
        }
        consumirDesafioDosFactor(usuario, desafio);
        String token = tokenDispositivo == null || tokenDispositivo.isBlank() || tokenDispositivo.length() > 100
                ? HashSeguro.tokenAleatorio() : tokenDispositivo;
        DispositivoUsuario dispositivo = registrarDispositivo(usuario, token, recordar, contexto, ahora);
        // Sus otras pestañas ven el dispositivo (nuevo o con su último uso) en Mi perfil.
        eventosService.avisarCuenta(usuario.getIdUsuario());
        return new Verificado(usuario, token, dispositivo.getIdDispositivo());
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

    private void enviarCodigo(Usuario usuario, String correo, Contexto contexto, LocalDateTime ahora,
                              String desafioHash) {
        String codigo = HashSeguro.codigo6Digitos();
        String hash = HashSeguro.sha256(codigo);
        LocalDateTime expiracion = ahora.plusMinutes(MINUTOS_CODIGO);
        LocalDateTime ventana = usuario.getDosFactorVentana();
        Integer envios = usuario.getDosFactorEnvios();
        int afectadas = usuarioRepository.guardarCodigo(usuario.getIdUsuario(), desafioHash, hash,
                expiracion, ahora, envios, ventana);
        if (afectadas == 0) {
            throw new DosFactorVencidoException("La verificación ya no es válida. Vuelve a iniciar sesión.");
        }
        usuario.setDosFactorCodigo(hash);
        usuario.setDosFactorCodigoExpiracion(expiracion);
        usuario.setDosFactorEnviado(ahora);
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

    @Transactional
    public String emitirDesafio(Usuario usuario, PropositoDesafio proposito, int minutos) {
        return emitirDesafio(usuario, proposito, minutos, null);
    }

    @Transactional
    public String emitirDesafio(Usuario usuario, PropositoDesafio proposito, int minutos, Integer idDispositivo) {
        String desafio = nuevoToken(proposito, usuario, idDispositivo);
        String hash = HashSeguro.sha256(desafio);
        LocalDateTime expiracion = LocalDateTime.now().plusMinutes(minutos);
        int afectadas = usuarioRepository.guardarDesafio(usuario.getIdUsuario(), hash, expiracion);
        if (afectadas == 0) {
            throw new DosFactorVencidoException("La verificación ya no es válida. Vuelve a iniciar sesión.");
        }
        usuario.setDosFactorDesafio(hash);
        usuario.setDosFactorDesafioExpiracion(expiracion);
        return desafio;
    }

    public Usuario validarDesafio(String desafio, PropositoDesafio proposito) {
        validarProposito(desafio, proposito);
        Usuario usuario = usuarioRepository.findByDosFactorDesafio(HashSeguro.sha256(desafio)).orElse(null);
        if (usuario == null) {
            throw new DosFactorVencidoException("La verificación ya no es válida. Vuelve a iniciar sesión.");
        }
        if (usuario.getDosFactorDesafioExpiracion() == null
                || LocalDateTime.now().isAfter(usuario.getDosFactorDesafioExpiracion())) {
            int afectadas = usuarioRepository.limpiarDesafioPorHash(usuario.getIdUsuario(), HashSeguro.sha256(desafio));
            if (afectadas > 0) {
                limpiar(usuario);
            }
            throw new DosFactorVencidoException("Se acabó el tiempo para verificar. Vuelve a iniciar sesión.");
        }
        if (usuario.getAcceso() == null
                || !AccesoConstants.ACTIVO.equals(usuario.getAcceso().getIdAcceso())) {
            throw new DosFactorVencidoException("La verificación ya no es válida. Vuelve a iniciar sesión.");
        }
        return usuario;
    }

    @Transactional
    public void consumirDesafio(Usuario usuario, String desafio, PropositoDesafio proposito) {
        validarProposito(desafio, proposito);
        int version = version(desafio, proposito, usuario);
        int afectadas = usuarioRepository.consumirDesafio(usuario.getIdUsuario(), HashSeguro.sha256(desafio),
                LocalDateTime.now(), AccesoConstants.ACTIVO, version);
        if (afectadas == 0) {
            throw new DosFactorVencidoException("La verificación ya no es válida. Vuelve a iniciar sesión.");
        }
        limpiarDesafio(usuario);
    }

    @Transactional
    public void consumirDesafioDosFactor(Usuario usuario, String desafio) {
        validarProposito(desafio, PropositoDesafio.DOS_FACTOR);
        int afectadas = usuarioRepository.consumirDesafioDosFactor(usuario.getIdUsuario(), HashSeguro.sha256(desafio),
                LocalDateTime.now(), AccesoConstants.ACTIVO, version(usuario));
        if (afectadas == 0) {
            throw new DosFactorVencidoException("La verificación ya no es válida. Vuelve a iniciar sesión.");
        }
        limpiar(usuario);
        usuario.setIntentosFallidos(0);
        usuario.setFechaBloqueo(null);
        usuario.setDosFactorEnvios(0);
        usuario.setDosFactorVentana(null);
    }

    private Usuario porDesafio(String desafio) {
        return validarDesafio(desafio, PropositoDesafio.DOS_FACTOR);
    }

    private void validarProposito(String desafio, PropositoDesafio esperado) {
        if (desafio == null || desafio.isBlank() || PropositoDesafio.desde(desafio) != esperado) {
            throw new DosFactorVencidoException("La verificación ya no es válida. Vuelve a iniciar sesión.");
        }
    }

    public Integer idDispositivo(String desafio, PropositoDesafio proposito) {
        validarProposito(desafio, proposito);
        if (proposito != PropositoDesafio.SELECCION_ROL) {
            return null;
        }
        String[] partes = desafio.substring(proposito.prefijo().length()).split(":", -1);
        if (partes.length < 3) {
            throw new DosFactorVencidoException("La verificación ya no es válida. Vuelve a iniciar sesión.");
        }
        try {
            int id = Integer.parseInt(partes[1]);
            return id == 0 ? null : id;
        } catch (NumberFormatException e) {
            throw new DosFactorVencidoException("La verificación ya no es válida. Vuelve a iniciar sesión.");
        }
    }

    private String nuevoToken(PropositoDesafio proposito, Usuario usuario, Integer idDispositivo) {
        String aleatorio = HashSeguro.tokenAleatorio();
        if (proposito != PropositoDesafio.SELECCION_ROL) {
            return proposito.prefijo() + aleatorio;
        }
        int dispositivo = idDispositivo == null ? 0 : idDispositivo;
        return proposito.prefijo() + version(usuario) + ":" + dispositivo + ":" + aleatorio;
    }

    private int version(String desafio, PropositoDesafio proposito, Usuario usuario) {
        if (proposito != PropositoDesafio.SELECCION_ROL) {
            return version(usuario);
        }
        String resto = desafio.substring(proposito.prefijo().length());
        int separador = resto.indexOf(':');
        if (separador <= 0) {
            return -1;
        }
        try {
            return Integer.parseInt(resto.substring(0, separador));
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private int version(Usuario usuario) {
        return usuario.getVersionToken() == null ? 0 : usuario.getVersionToken();
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

    // Cambia si cambian la contraseña o el correo. No depende de la versión de
    // sesión: un cambio de permisos o de roles cierra la sesión, pero el
    // navegador de confianza no vuelve a pedir el código.
    private String huella(Usuario usuario) {
        String correo = usuario.getGmail() == null ? "" : usuario.getGmail().trim().toLowerCase(Locale.ROOT);
        return HashSeguro.sha256(usuario.getContraseña() + "|" + correo);
    }

    private DispositivoUsuario dispositivoDe(Integer idUsuario, Integer idDispositivo) {
        return dispositivoRepository.findByIdDispositivoAndUsuarioIdUsuario(idDispositivo, idUsuario)
                .orElseThrow(() -> new ResourceNotFoundException("Dispositivo no encontrado"));
    }

    private void limpiar(Usuario usuario) {
        limpiarDesafio(usuario);
        usuario.setDosFactorCodigo(null);
        usuario.setDosFactorCodigoExpiracion(null);
    }

    private void limpiarDesafio(Usuario usuario) {
        usuario.setDosFactorDesafio(null);
        usuario.setDosFactorDesafioExpiracion(null);
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
