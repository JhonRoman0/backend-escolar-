package com.example.Escolar.Controller;

import com.example.Escolar.Dto.LoginRequest;
import com.example.Escolar.Dto.LoginResponse;
import com.example.Escolar.Dto.UsuarioResponse;
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

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    public static final String COOKIE_NOMBRE = "escolar_token";

    private final AuthService authService;
    private final UsuarioService usuarioService;

    @Value("${jwt.expiration}")
    private long jwtExpirationMs;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request,
                                               HttpServletRequest httpRequest,
                                               HttpServletResponse httpResponse) {
        LoginResponse response = authService.login(request);
        httpResponse.addHeader(HttpHeaders.SET_COOKIE,
                cookieSesion(response.getToken(), jwtExpirationMs / 1000, httpRequest));
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        httpResponse.addHeader(HttpHeaders.SET_COOKIE,
                cookieSesion("", 0, httpRequest));
        return ResponseEntity.ok().build();
    }

    private String cookieSesion(String valor, long maxAge, HttpServletRequest httpRequest) {
        boolean secure = httpRequest.isSecure();
        return ResponseCookie.from(COOKIE_NOMBRE, valor)
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
}
