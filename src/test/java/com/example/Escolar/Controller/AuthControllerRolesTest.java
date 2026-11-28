package com.example.Escolar.Controller;

import com.example.Escolar.Dto.ModuloPermisosResponse;
import com.example.Escolar.Dto.PermisoAccionResponse;
import com.example.Escolar.Dto.PermisosRolResponse;
import com.example.Escolar.Dto.RolAsignadoSesionResponse;
import com.example.Escolar.Dto.RolAsignadoSesionResponse.EstadoRol;
import com.example.Escolar.Exception.GlobalExceptionHandler;
import com.example.Escolar.Security.JwtService;
import com.example.Escolar.Security.UsuarioAutenticado;
import com.example.Escolar.Service.AuthService;
import com.example.Escolar.Service.DosFactorService;
import com.example.Escolar.Service.RolAsignadoResolver;
import com.example.Escolar.Service.SeleccionRolDesafioService;
import com.example.Escolar.Service.UsuarioService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AuthControllerRolesTest {

    @Mock
    private AuthService authService;
    @Mock
    private UsuarioService usuarioService;
    @Mock
    private JwtService jwtService;
    @Mock
    private DosFactorService dosFactorService;
    @Mock
    private SeleccionRolDesafioService seleccionRolDesafioService;
    @Mock
    private RolAsignadoResolver rolAsignadoResolver;

    @InjectMocks
    private AuthController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void preparar() {
        ReflectionTestUtils.setField(controller, "jwtExpirationMs", 3_600_000L);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @AfterEach
    void limpiarContexto() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void rolesUsaElPrincipalAutenticadoYConservaLaDisponibilidad() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        new UsuarioAutenticado(7, "ADM20240001", List.of("Docente")), null, List.of()));
        when(rolAsignadoResolver.resolver(7)).thenReturn(List.of(
                new RolAsignadoSesionResponse(2, "Docente", EstadoRol.ACTIVO),
                new RolAsignadoSesionResponse(3, "Administrador", EstadoRol.INACTIVO),
                new RolAsignadoSesionResponse(4, "Portero", EstadoRol.SUSPENDIDO)));

        mockMvc.perform(get("/auth/roles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].idRol").value(2))
                .andExpect(jsonPath("$[0].estado").value("ACTIVO"))
                .andExpect(jsonPath("$[1].estado").value("INACTIVO"))
                .andExpect(jsonPath("$[2].estado").value("SUSPENDIDO"));

        verify(rolAsignadoResolver).resolver(7);
    }

    @Test
    void permisosUsaElRolActivoDelPrincipalYCoherentementeConBackend() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        new UsuarioAutenticado(7, "USR001", List.of("Docente", "ADMIN"), null, 11),
                        null, List.of()));

        PermisoAccionResponse permiso = new PermisoAccionResponse();
        permiso.setIdPermiso(5);
        permiso.setCodigo("DOCENTES");
        permiso.setAccionesConcedidas(List.of("ACTUALIZAR"));
        ModuloPermisosResponse modulo = new ModuloPermisosResponse();
        modulo.setIdModulo(3);
        modulo.setModulo("Docentes");
        modulo.setPermisos(List.of(permiso));
        PermisosRolResponse permisos = new PermisosRolResponse();
        permisos.setIdRol(11);
        permisos.setNombreRol("Docente");
        permisos.setModulos(List.of(modulo));
        when(authService.permisosActuales(7, 11)).thenReturn(permisos);

        mockMvc.perform(get("/auth/permisos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idRol").value(11))
                .andExpect(jsonPath("$.nombreRol").value("Docente"))
                .andExpect(jsonPath("$.modulos[0].permisos[0].codigo").value("DOCENTES"))
                .andExpect(jsonPath("$.modulos[0].permisos[0].accionesConcedidas[0]").value("ACTUALIZAR"));

        verify(authService).permisosActuales(7, 11);
        verify(authService, never()).permisosActuales(7, 12);
    }
}
