package com.example.Escolar.Security;

import com.example.Escolar.Controller.AuthController;
import com.example.Escolar.Dto.RolAsignadoSesionResponse.EstadoRol;
import com.example.Escolar.Dto.UsuarioResponse;
import com.example.Escolar.Model.Accion;
import com.example.Escolar.Model.Acceso;
import com.example.Escolar.Model.Asignacion;
import com.example.Escolar.Model.Docente;
import com.example.Escolar.Model.EstadoSuspension;
import com.example.Escolar.Model.Modulo;
import com.example.Escolar.Model.Permiso;
import com.example.Escolar.Model.PermisoAccion;
import com.example.Escolar.Model.Rol;
import com.example.Escolar.Model.RolPermiso;
import com.example.Escolar.Model.RolPermisoAccion;
import com.example.Escolar.Model.SuspensionDocente;
import com.example.Escolar.Model.Usuario;
import com.example.Escolar.Model.UsuarioRol;
import com.example.Escolar.Repository.AccesoRepository;
import com.example.Escolar.Repository.AccionRepository;
import com.example.Escolar.Repository.AlumnoApoderadoRepository;
import com.example.Escolar.Repository.ApoderadoRepository;
import com.example.Escolar.Repository.AsignacionRepository;
import com.example.Escolar.Repository.DocenteRepository;
import com.example.Escolar.Repository.MatriculaRepository;
import com.example.Escolar.Repository.PermisoAccionRepository;
import com.example.Escolar.Repository.PermisoRepository;
import com.example.Escolar.Repository.RolPermisoAccionRepository;
import com.example.Escolar.Repository.RolPermisoRepository;
import com.example.Escolar.Repository.RolRepository;
import com.example.Escolar.Repository.SuspensionDocenteRepository;
import com.example.Escolar.Repository.UsuarioRepository;
import com.example.Escolar.Repository.UsuarioRolRepository;
import com.example.Escolar.Repository.UsuarioVigenciaSesion;
import com.example.Escolar.Service.AccesoConstants;
import com.example.Escolar.Service.AccesoContextoService;
import com.example.Escolar.Service.AuthService;
import com.example.Escolar.Service.DosFactorService;
import com.example.Escolar.Service.EmailService;
import com.example.Escolar.Service.EventosService;
import com.example.Escolar.Service.RolAsignadoResolver;
import com.example.Escolar.Service.RolPermisoService;
import com.example.Escolar.Service.SeleccionRolDesafioService;
import com.example.Escolar.Service.UsuarioService;
import com.example.Escolar.Service.VigenciaService;
import com.example.Escolar.Util.FechaColegio;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AuthController.class)
@Import({SecurityConfig.class, AuthC3IntegradoTest.ServiciosReales.class})
@TestPropertySource(properties = {
        "jwt.secret=clave-secreta-de-pruebas-con-32-bytes-o-mas!!",
        "jwt.expiration=3600000"
})
class AuthC3IntegradoTest {

    private static final Integer ID_USUARIO = 1;
    private static final Integer ID_ROL_DOCENTE = 11;
    private static final Integer ID_ROL_ADMIN = 12;
    private static final Integer ID_PERMISO = 5;
    private static final Integer ID_ROL_PERMISO = 20;

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JwtService jwtService;
    @Autowired
    private ConcesionesService concesionesService;
    @Autowired
    private RbacAuthorizationManager rbacAuthorizationManager;

    @MockitoBean
    private UsuarioService usuarioService;
    @MockitoBean
    private DosFactorService dosFactorService;
    @MockitoBean
    private SeleccionRolDesafioService seleccionRolDesafioService;
    @MockitoBean
    private EmailService emailService;
    @MockitoBean
    private EventosService eventosService;
    @MockitoBean
    private ActorSeguridad actorSeguridad;
    @MockitoBean
    private UsuarioRepository usuarioRepository;
    @MockitoBean
    private UsuarioRolRepository usuarioRolRepository;
    @MockitoBean
    private DocenteRepository docenteRepository;
    @MockitoBean
    private SuspensionDocenteRepository suspensionDocenteRepository;
    @MockitoBean
    private AccesoRepository accesoRepository;
    @MockitoBean
    private RolPermisoRepository rolPermisoRepository;
    @MockitoBean
    private RolPermisoAccionRepository rolPermisoAccionRepository;
    @MockitoBean
    private RolRepository rolRepository;
    @MockitoBean
    private PermisoRepository permisoRepository;
    @MockitoBean
    private AccionRepository accionRepository;
    @MockitoBean
    private PermisoAccionRepository permisoAccionRepository;
    @MockitoBean
    private ApoderadoRepository apoderadoRepository;
    @MockitoBean
    private AlumnoApoderadoRepository alumnoApoderadoRepository;
    @MockitoBean
    private MatriculaRepository matriculaRepository;
    @MockitoBean
    private AsignacionRepository asignacionRepository;

    private final AtomicInteger version = new AtomicInteger();
    private Acceso activo;
    private Acceso inactivo;
    private Acceso eliminado;
    private Docente docente;
    private Rol rolDocente;
    private Rol rolAdmin;

    @BeforeEach
    void preparar() {
        version.set(0);
        ReflectionTestUtils.setField(concesionesService, "recientes", new ConcurrentHashMap<>());
        activo = acceso(AccesoConstants.ACTIVO, "Activo");
        inactivo = acceso(AccesoConstants.INACTIVO, "Inactivo");
        eliminado = acceso(AccesoConstants.ELIMINADO, "Eliminado");
        rolDocente = rol(ID_ROL_DOCENTE, "DOCENTE", activo);
        rolAdmin = rol(ID_ROL_ADMIN, "ADMIN", activo);

        Usuario usuario = new Usuario();
        usuario.setIdUsuario(ID_USUARIO);
        usuario.setAcceso(activo);
        docente = new Docente();
        docente.setIdDocente(10);
        docente.setUsuario(usuario);
        docente.setAcceso(activo);

        when(accesoRepository.findById(AccesoConstants.ACTIVO)).thenReturn(Optional.of(activo));
        when(accesoRepository.findById(AccesoConstants.INACTIVO)).thenReturn(Optional.of(inactivo));
        when(accesoRepository.findById(AccesoConstants.ELIMINADO)).thenReturn(Optional.of(eliminado));
        when(usuarioRepository.findVigenciaSesion(eq(ID_USUARIO), eq(AccesoConstants.ELIMINADO)))
                .thenAnswer(invocation -> Optional.of(sesion(version.get())));
        when(usuarioRepository.tieneDispositivo(any(), any())).thenReturn(true);
        when(usuarioRolRepository.findByUsuarioIdUsuario(ID_USUARIO))
                .thenReturn(List.of(vinculo(rolDocente), vinculo(rolAdmin)));
        when(usuarioRolRepository.findByRolIdRol(ID_ROL_DOCENTE))
                .thenReturn(List.of(vinculo(rolDocente, ID_USUARIO, activo)));
        when(docenteRepository.findByUsuarioIdUsuario(ID_USUARIO)).thenReturn(Optional.of(docente));
        when(suspensionDocenteRepository.findByDocenteAndAccesoNot(any(), eq(eliminado))).thenReturn(List.of());
        when(asignacionRepository.findByDocenteIdDocenteAndAcceso(eq(10), any()))
                .thenReturn(List.of(new Asignacion()));

        Permiso permiso = permiso(ID_PERMISO, "USUARIOS", activo);
        RolPermiso rolPermiso = new RolPermiso();
        rolPermiso.setIdRolPermiso(ID_ROL_PERMISO);
        rolPermiso.setRol(rolDocente);
        rolPermiso.setPermiso(permiso);
        rolPermiso.setAcceso(activo);
        Accion accion = accion("ACTUALIZAR", activo);
        RolPermisoAccion concedida = new RolPermisoAccion();
        concedida.setAcceso(activo);
        concedida.setAccion(accion);
        PermisoAccion disponible = new PermisoAccion();
        disponible.setPermiso(permiso);
        disponible.setAccion(accion);
        disponible.setAcceso(activo);

        when(rolPermisoRepository.findByRolIdRol(ID_ROL_DOCENTE)).thenReturn(List.of(rolPermiso));
        when(rolPermisoAccionRepository.findByRolPermisoIdRolPermiso(ID_ROL_PERMISO)).thenReturn(List.of(concedida));
        when(permisoAccionRepository.findByPermisoIdPermisoAndAccesoNot(ID_PERMISO, eliminado))
                .thenReturn(List.of(disponible));
        when(usuarioService.getById(ID_USUARIO)).thenReturn(usuarioMe());
    }

    @AfterEach
    void limpiarContexto() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void rutasDeRolYCambioRechazanSinCookieYConJwtObsoletoConCadenaReal() throws Exception {
        mockMvc.perform(getRuta("/auth/roles"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
        mockMvc.perform(postRuta("/auth/cambiar-rol")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idRol\":12}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));

        mockMvc.perform(getRuta("/auth/roles").cookie(cookie(token(ID_ROL_DOCENTE, 1))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("VIGENCIA_TOKEN_OBSOLETO"));
        mockMvc.perform(postRuta("/auth/cambiar-rol")
                        .cookie(cookie(token(ID_ROL_DOCENTE, 1)))
                        .header("X-Requested-With", "XMLHttpRequest")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idRol\":12}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("VIGENCIA_TOKEN_OBSOLETO"));
    }

    @Test
    void suspensionDocenteRevocaSuRolYOtroRolSigueElegible() throws Exception {
        when(suspensionDocenteRepository.findByDocenteAndAccesoNot(any(), eq(eliminado)))
                .thenReturn(List.of(suspension(docente, EstadoSuspension.VIGENTE, FechaColegio.hoy(), null, activo)));

        mockMvc.perform(getRuta("/auth/roles").cookie(cookie(token(ID_ROL_DOCENTE, 0))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("VIGENCIA_ROL_NO_DISPONIBLE"));

        mockMvc.perform(getRuta("/auth/roles").cookie(cookie(token(ID_ROL_ADMIN, 0))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].estado").value("SUSPENDIDO"))
                .andExpect(jsonPath("$[1].estado").value("ACTIVO"));
    }

    @Test
    void suspensionProgramadaAlcanzadaBloqueaAntesDelJob() throws Exception {
        when(suspensionDocenteRepository.findByDocenteAndAccesoNot(any(), eq(eliminado)))
                .thenReturn(List.of(suspension(docente, EstadoSuspension.PROGRAMADA, FechaColegio.hoy(), null, activo)));

        mockMvc.perform(getRuta("/auth/roles").cookie(cookie(token(ID_ROL_DOCENTE, 0))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("VIGENCIA_ROL_NO_DISPONIBLE"));
    }

    @Test
    void rolDesactivadoDuranteLaSesionRevocaElAcceso() throws Exception {
        when(usuarioRolRepository.findByUsuarioIdUsuario(ID_USUARIO))
                .thenReturn(List.of(vinculo(rol(ID_ROL_DOCENTE, "DOCENTE", inactivo)), vinculo(rolAdmin)));

        mockMvc.perform(getRuta("/auth/permisos").cookie(cookie(token(ID_ROL_DOCENTE, 0))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("VIGENCIA_ROL_NO_DISPONIBLE"));
    }

    @Test
    void permisosYCcesoRbacSeMantienenCoherentesConServiciosReales() throws Exception {
        String tokenInicial = token(ID_ROL_DOCENTE, 0);

        mockMvc.perform(getRuta("/auth/permisos").cookie(cookie(tokenInicial)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idRol").value(ID_ROL_DOCENTE))
                .andExpect(jsonPath("$.nombreRol").value("DOCENTE"))
                .andExpect(jsonPath("$.modulos[0].permisos[0].codigo").value("USUARIOS"))
                .andExpect(jsonPath("$.modulos[0].permisos[0].accionesConcedidas[0]").value("ACTUALIZAR"));
        assertTrue(autorizaRuta("/usuarios", 0));

        when(rolPermisoRepository.findByRolIdRol(ID_ROL_DOCENTE)).thenReturn(List.of());
        version.set(1);
        concesionesService.invalidarPermisosDelRol(ID_ROL_DOCENTE);

        mockMvc.perform(getRuta("/auth/permisos").cookie(cookie(tokenInicial)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("VIGENCIA_TOKEN_OBSOLETO"));

        mockMvc.perform(getRuta("/auth/permisos").cookie(cookie(token(ID_ROL_DOCENTE, 1))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.modulos.length()").value(0));
        assertFalse(autorizaRuta("/usuarios", 1));
    }

    @Test
    void meYLogoutSiguenDisponiblesConRolNoDisponibleSinFiltrarDetalles() throws Exception {
        when(usuarioRolRepository.findByUsuarioIdUsuario(ID_USUARIO))
                .thenReturn(List.of(vinculo(rol(ID_ROL_DOCENTE, "DOCENTE", inactivo))));
        String token = token(ID_ROL_DOCENTE, 0);

        mockMvc.perform(getRuta("/auth/me").cookie(cookie(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idUsuario").value(ID_USUARIO))
                .andExpect(jsonPath("$.nombre").value("Ana"))
                .andExpect(jsonPath("$.roles.length()").value(0))
                .andExpect(jsonPath("$..*").value(org.hamcrest.Matchers.not(
                        containsString("VIGENCIA_ROL_NO_DISPONIBLE"))));

        mockMvc.perform(postRuta("/auth/logout").cookie(cookie(token)))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("escolar_token=;")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Max-Age=0")));
    }

    @Test
    void cambiosDeConcesionesNoSeMezclanEntreRoles() {
        assertTrue(concesionesService.rolActivoValido(ID_USUARIO, ID_ROL_DOCENTE));
        assertTrue(concesionesService.rolActivoValido(ID_USUARIO, ID_ROL_ADMIN));
        assertTrue(concesionesService.accionesPorPermiso(ID_USUARIO, ID_ROL_DOCENTE).containsKey("USUARIOS"));
        assertTrue(concesionesService.accionesPorPermiso(ID_USUARIO, ID_ROL_ADMIN).isEmpty());
    }

    private boolean autorizaRuta(String ruta, Integer versionToken) {
        UsuarioAutenticado principal = new UsuarioAutenticado(
                ID_USUARIO, "DOC001", List.of("DOCENTE"), null, ID_ROL_DOCENTE, versionToken);
        var authentication = new UsernamePasswordAuthenticationToken(principal, null, List.of());
        MockHttpServletRequest request = new MockHttpServletRequest("GET", ruta);
        return rbacAuthorizationManager.authorize(() -> authentication,
                new RequestAuthorizationContext(request)).isGranted();
    }

    private String token(Integer idRolActivo, Integer versionToken) {
        return jwtService.generarToken(ID_USUARIO, "DOC001", idRolActivo, List.of("DOCENTE"),
                false, null, versionToken, null);
    }

    private Cookie cookie(String token) {
        return new Cookie(AuthController.COOKIE_NOMBRE, token);
    }

    private MockHttpServletRequestBuilder getRuta(String ruta) {
        return get(ruta).servletPath(ruta);
    }

    private MockHttpServletRequestBuilder postRuta(String ruta) {
        return post(ruta).servletPath(ruta);
    }

    private UsuarioResponse usuarioMe() {
        UsuarioResponse response = new UsuarioResponse();
        response.setIdUsuario(ID_USUARIO);
        response.setNombre("Ana");
        response.setRoles(List.of());
        return response;
    }

    private UsuarioVigenciaSesion sesion(Integer versionToken) {
        return new UsuarioVigenciaSesion() {
            @Override
            public Integer getIdUsuario() {
                return ID_USUARIO;
            }

            @Override
            public LocalDate getFechaInicioVigencia() {
                return FechaColegio.hoy().minusDays(1);
            }

            @Override
            public LocalDate getFechaFinVigencia() {
                return null;
            }

            @Override
            public Integer getVersionToken() {
                return versionToken;
            }

            @Override
            public Long getAccesoId() {
                return AccesoConstants.ACTIVO;
            }
        };
    }

    private Acceso acceso(Long id, String nombre) {
        return new Acceso(id, nombre);
    }

    private Rol rol(Integer id, String nombre, Acceso acceso) {
        Rol rol = new Rol();
        rol.setIdRol(id);
        rol.setNombre(nombre);
        rol.setAcceso(acceso);
        return rol;
    }

    private UsuarioRol vinculo(Rol rol) {
        UsuarioRol usuarioRol = new UsuarioRol();
        usuarioRol.setRol(rol);
        return usuarioRol;
    }

    private UsuarioRol vinculo(Rol rol, Integer idUsuario, Acceso acceso) {
        Usuario usuario = new Usuario();
        usuario.setIdUsuario(idUsuario);
        usuario.setAcceso(acceso);
        UsuarioRol usuarioRol = new UsuarioRol();
        usuarioRol.setRol(rol);
        usuarioRol.setUsuario(usuario);
        return usuarioRol;
    }

    private Permiso permiso(Integer id, String codigo, Acceso acceso) {
        Modulo modulo = new Modulo();
        modulo.setIdModulo(3);
        modulo.setModulo("Usuarios");
        modulo.setAcceso(acceso);
        Permiso permiso = new Permiso();
        permiso.setIdPermiso(id);
        permiso.setCodigo(codigo);
        permiso.setNombre("Usuarios");
        permiso.setAcceso(acceso);
        permiso.setModulo(modulo);
        return permiso;
    }

    private Accion accion(String codigo, Acceso acceso) {
        Accion accion = new Accion();
        accion.setCodigo(codigo);
        accion.setAcceso(acceso);
        return accion;
    }

    private SuspensionDocente suspension(Docente docente, EstadoSuspension estado,
                                         LocalDate inicio, LocalDate fin, Acceso acceso) {
        SuspensionDocente suspension = new SuspensionDocente();
        suspension.setDocente(docente);
        suspension.setEstado(estado);
        suspension.setFechaInicio(inicio);
        suspension.setFechaFin(fin);
        suspension.setAcceso(acceso);
        return suspension;
    }

    @TestConfiguration
    static class ServiciosReales {
        @Bean
        JwtService jwtService(@Value("${jwt.secret}") String secret, @Value("${jwt.expiration}") long expiration) {
            return new JwtService(secret, expiration);
        }

        @Bean
        VigenciaService vigenciaService() {
            return new VigenciaService();
        }

        @Bean
        ApiPermisoRegistro apiPermisoRegistro() {
            return new ApiPermisoRegistro();
        }

        @Bean
        RolAsignadoResolver rolAsignadoResolver(UsuarioRolRepository usuarioRolRepository,
                                                DocenteRepository docenteRepository,
                                                SuspensionDocenteRepository suspensionDocenteRepository,
                                                AccesoRepository accesoRepository) {
            return new RolAsignadoResolver(usuarioRolRepository, docenteRepository,
                    suspensionDocenteRepository, accesoRepository);
        }

        @Bean
        ConcesionesService concesionesService(UsuarioRolRepository usuarioRolRepository,
                                              RolPermisoRepository rolPermisoRepository,
                                              RolPermisoAccionRepository rolPermisoAccionRepository,
                                              UsuarioRepository usuarioRepository,
                                              RolAsignadoResolver rolAsignadoResolver) {
            return new ConcesionesService(usuarioRolRepository, rolPermisoRepository,
                    rolPermisoAccionRepository, usuarioRepository, rolAsignadoResolver);
        }

        @Bean
        AccesoContextoService accesoContextoService(DocenteRepository docenteRepository,
                                                    ApoderadoRepository apoderadoRepository,
                                                    AlumnoApoderadoRepository alumnoApoderadoRepository,
                                                    MatriculaRepository matriculaRepository,
                                                    AsignacionRepository asignacionRepository,
                                                    AccesoRepository accesoRepository,
                                                    ConcesionesService concesionesService) {
            return new AccesoContextoService(docenteRepository, apoderadoRepository,
                    alumnoApoderadoRepository, matriculaRepository, asignacionRepository,
                    accesoRepository, concesionesService);
        }

        @Bean
        RolPermisoService rolPermisoService(RolPermisoRepository rolPermisoRepository,
                                            AccesoRepository accesoRepository,
                                            RolPermisoAccionRepository rolPermisoAccionRepository,
                                            RolRepository rolRepository,
                                            PermisoRepository permisoRepository,
                                            AccionRepository accionRepository,
                                            PermisoAccionRepository permisoAccionRepository,
                                            ConcesionesService concesionesService,
                                            ActorSeguridad actorSeguridad) {
            return new RolPermisoService(rolPermisoRepository, accesoRepository,
                    rolPermisoAccionRepository, rolRepository, permisoRepository,
                    accionRepository, permisoAccionRepository, concesionesService, actorSeguridad);
        }

        @Bean
        AuthService authService(UsuarioRepository usuarioRepository,
                                org.springframework.security.crypto.password.PasswordEncoder passwordEncoder,
                                JwtService jwtService,
                                UsuarioService usuarioService,
                                RolPermisoService rolPermisoService,
                                AccesoRepository accesoRepository,
                                EmailService emailService,
                                AccesoContextoService accesoContextoService,
                                VigenciaService vigenciaService,
                                SeleccionRolDesafioService seleccionRolDesafioService,
                                RolAsignadoResolver rolAsignadoResolver) {
            return new AuthService(usuarioRepository, passwordEncoder, jwtService, usuarioService,
                    rolPermisoService, accesoRepository, emailService, accesoContextoService,
                    vigenciaService, seleccionRolDesafioService, rolAsignadoResolver);
        }

        @Bean
        RbacAuthorizationManager rbacAuthorizationManager(ApiPermisoRegistro apiPermisoRegistro,
                                                          ConcesionesService concesionesService) {
            return new RbacAuthorizationManager(apiPermisoRegistro, concesionesService);
        }
    }
}
