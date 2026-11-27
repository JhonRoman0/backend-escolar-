package com.example.Escolar.Controller;

import com.example.Escolar.Dto.CambiarContrasenaRequest;
import com.example.Escolar.Dto.DispositivoResponse;
import com.example.Escolar.Dto.DosFactorReenviarRequest;
import com.example.Escolar.Dto.DosFactorVerificarRequest;
import com.example.Escolar.Model.Usuario;
import com.example.Escolar.Service.DosFactorService;
import com.example.Escolar.Util.DescripcionNavegador;
import jakarta.servlet.http.Cookie;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import com.example.Escolar.Dto.ForgotPasswordRequest;
import com.example.Escolar.Dto.LoginRequest;
import com.example.Escolar.Dto.LoginResponse;
import com.example.Escolar.Dto.PermisosRolResponse;
import com.example.Escolar.Dto.ResetPasswordRequest;
import com.example.Escolar.Dto.UsuarioResponse;
import com.example.Escolar.Dto.VerificarCodigoRequest;
import com.example.Escolar.Security.JwtService;
import com.example.Escolar.Security.UsuarioAutenticado;
import com.example.Escolar.Service.AuthService;
import com.example.Escolar.Service.UsuarioService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    public static final String COOKIE_NOMBRE = "escolar_token";

    private final AuthService authService;
    private final UsuarioService usuarioService;
    private final JwtService jwtService;
    private final DosFactorService dosFactorService;

    @Value("${jwt.expiration}")
    private long jwtExpirationMs;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request,
                                               HttpServletRequest httpRequest,
                                               HttpServletResponse httpResponse) {
        Usuario usuario = authService.validarCredenciales(request);
        // Con doble factor, la sesión espera al código del correo (salvo navegador de confianza).
        Integer idDispositivo = null;
        if (dosFactorService.activo()) {
            idDispositivo = dosFactorService.dispositivoDeConfianza(usuario,
                    leerCookie(httpRequest, DosFactorService.COOKIE_DISPOSITIVO)).orElse(null);
            if (idDispositivo == null) {
                DosFactorService.Desafio desafio = dosFactorService.iniciar(usuario, contexto(httpRequest));
                return ResponseEntity.ok(respuestaDesafio(desafio));
            }
        }
        LoginResponse response = authService.iniciarSesion(usuario, idDispositivo);
        httpResponse.addHeader(HttpHeaders.SET_COOKIE,
                cookieSesion(response.getToken(), jwtExpirationMs / 1000, httpRequest));
        return ResponseEntity.ok(response);
    }

    @PostMapping("/2fa/verificar")
    public ResponseEntity<LoginResponse> verificarDosFactor(@Valid @RequestBody DosFactorVerificarRequest request,
                                                            HttpServletRequest httpRequest,
                                                            HttpServletResponse httpResponse) {
        DosFactorService.Verificado verificado = dosFactorService.verificar(request.getDesafio(), request.getCodigo(),
                Boolean.TRUE.equals(request.getRecordar()),
                leerCookie(httpRequest, DosFactorService.COOKIE_DISPOSITIVO), contexto(httpRequest));
        LoginResponse response = authService.iniciarSesion(verificado.usuario(), verificado.idDispositivo());
        httpResponse.addHeader(HttpHeaders.SET_COOKIE,
                cookieSesion(response.getToken(), jwtExpirationMs / 1000, httpRequest));
        httpResponse.addHeader(HttpHeaders.SET_COOKIE, cookie(DosFactorService.COOKIE_DISPOSITIVO,
                verificado.tokenDispositivo(), DosFactorService.SEGUNDOS_COOKIE_DISPOSITIVO, httpRequest));
        return ResponseEntity.ok(response);
    }

    @PostMapping("/2fa/reenviar")
    public ResponseEntity<LoginResponse> reenviarDosFactor(@Valid @RequestBody DosFactorReenviarRequest request,
                                                           HttpServletRequest httpRequest) {
        return ResponseEntity.ok(respuestaDesafio(dosFactorService.reenviar(request.getDesafio(), contexto(httpRequest))));
    }

    @GetMapping("/dispositivos")
    public List<DispositivoResponse> dispositivos(HttpServletRequest httpRequest) {
        return dosFactorService.dispositivos(idAutenticado(),
                leerCookie(httpRequest, DosFactorService.COOKIE_DISPOSITIVO));
    }

    @DeleteMapping("/dispositivos/{id}")
    public ResponseEntity<Void> olvidarDispositivo(@PathVariable Integer id) {
        dosFactorService.olvidar(idAutenticado(), id);
        return ResponseEntity.noContent().build();
    }

    private LoginResponse respuestaDesafio(DosFactorService.Desafio desafio) {
        LoginResponse response = new LoginResponse();
        response.setRequiereDosFactor(true);
        response.setDesafio(desafio.desafio());
        response.setCorreoEnmascarado(desafio.correoEnmascarado());
        response.setSegundosReenvio(desafio.segundosReenvio());
        response.setMinutosCodigo(desafio.minutosCodigo());
        return response;
    }

    private DosFactorService.Contexto contexto(HttpServletRequest httpRequest) {
        // remoteAddr ya viene de X-Forwarded-For (server.forward-headers-strategy=framework).
        return new DosFactorService.Contexto(DescripcionNavegador.de(httpRequest.getHeader(HttpHeaders.USER_AGENT)),
                httpRequest.getRemoteAddr());
    }

    private static String leerCookie(HttpServletRequest httpRequest, String nombre) {
        Cookie[] cookies = httpRequest.getCookies();
        if (cookies == null) {
            return null;
        }
        return Arrays.stream(cookies).filter(c -> nombre.equals(c.getName()))
                .map(Cookie::getValue).findFirst().orElse(null);
    }

    private static Integer idAutenticado() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return ((UsuarioAutenticado) authentication.getPrincipal()).idUsuario();
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        httpResponse.addHeader(HttpHeaders.SET_COOKIE,
                cookieSesion("", 0, httpRequest));
        return ResponseEntity.ok().build();
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request.getGmail());
        return ResponseEntity.ok(Map.of(
                "mensaje", "Se envio un correo con las instrucciones para restablecer tu contrasena"
        ));
    }

    @PostMapping("/verify-reset-code")
    public ResponseEntity<Map<String, String>> verificarCodigo(@Valid @RequestBody VerificarCodigoRequest request) {
        authService.verificarCodigo(request.getGmail(), request.getCodigo());
        return ResponseEntity.ok(Map.of(
                "mensaje", "Codigo verificado correctamente"
        ));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request.getGmail(), request.getCodigo(), request.getNuevaContrasena());
        return ResponseEntity.ok(Map.of(
                "mensaje", "Contrasena actualizada correctamente"
        ));
    }

    private String cookieSesion(String valor, long maxAge, HttpServletRequest httpRequest) {
        return cookie(COOKIE_NOMBRE, valor, maxAge, httpRequest);
    }

    private String cookie(String nombre, String valor, long maxAge, HttpServletRequest httpRequest) {
        boolean secure = httpRequest.isSecure();
        return ResponseCookie.from(nombre, valor)
                .httpOnly(true)
                .secure(secure)
                .sameSite(secure ? "None" : "Lax")
                .path("/")
                .maxAge(maxAge)
                .build()
                .toString();
    }

    @GetMapping("/me")
    public UsuarioResponse me() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UsuarioAutenticado principal = (UsuarioAutenticado) authentication.getPrincipal();
        return usuarioService.getById(principal.idUsuario());
    }

    @PostMapping("/change-password")
    public ResponseEntity<Map<String, String>> cambiarContrasena(
            @Valid @RequestBody CambiarContrasenaRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UsuarioAutenticado principal = (UsuarioAutenticado) authentication.getPrincipal();
        // El token nuevo sigue atado al mismo dispositivo.
        String token = authService.cambiarContrasena(principal.idUsuario(),
                request.getContrasenaActual(), request.getNuevaContrasena(), principal.idDispositivo());
        long maxAge = Math.max(0L,
                (jwtService.parse(token).getExpiration().getTime() - System.currentTimeMillis()) / 1000L);
        httpResponse.addHeader(HttpHeaders.SET_COOKIE,
                cookieSesion(token, maxAge, httpRequest));
        return ResponseEntity.ok(Map.of(
                "mensaje", "Contrasena actualizada correctamente"
        ));
    }

    @GetMapping("/permisos")
    public PermisosRolResponse permisos() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UsuarioAutenticado principal = (UsuarioAutenticado) authentication.getPrincipal();
        return authService.permisosActuales(principal.idUsuario(), principal.roles());
    }
}
