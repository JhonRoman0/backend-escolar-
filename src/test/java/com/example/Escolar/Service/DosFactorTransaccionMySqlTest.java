package com.example.Escolar.Service;

import com.example.Escolar.Config.DataSeeder;
import com.example.Escolar.Dto.LoginResponse;
import com.example.Escolar.Exception.CuentaBloqueadaException;
import com.example.Escolar.Exception.DesafioSeleccionRolInvalidoException;
import com.example.Escolar.Exception.DosFactorVencidoException;
import com.example.Escolar.Exception.ResourceNotFoundException;
import com.example.Escolar.Exception.VigenciaNoValidaException;
import com.example.Escolar.Model.Acceso;
import com.example.Escolar.Model.DispositivoUsuario;
import com.example.Escolar.Model.Rol;
import com.example.Escolar.Model.Usuario;
import com.example.Escolar.Model.UsuarioRol;
import com.example.Escolar.Repository.AccesoRepository;
import com.example.Escolar.Repository.DispositivoUsuarioRepository;
import com.example.Escolar.Repository.RolRepository;
import com.example.Escolar.Repository.UsuarioRepository;
import com.example.Escolar.Repository.UsuarioRolRepository;
import com.example.Escolar.Security.JwtAuthFilter;
import com.example.Escolar.Security.JwtService;
import com.example.Escolar.Util.HashSeguro;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.mockito.stubbing.Answer;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.AopTestUtils;
import org.springframework.test.util.ReflectionTestUtils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.AdditionalAnswers.delegatesTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

@EnabledIfEnvironmentVariable(named = "ESCOLAR_TEST_MYSQL_URL",
        matches = "jdbc:mysql://127\\.0\\.0\\.1:[0-9]+/[a-zA-Z0-9_]+_test(?:\\?.*)?")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, properties = {
        "spring.datasource.url=${ESCOLAR_TEST_MYSQL_URL}",
        "spring.datasource.username=${ESCOLAR_TEST_MYSQL_USER}",
        "spring.datasource.password=${ESCOLAR_TEST_MYSQL_PASSWORD:}",
        "spring.jpa.hibernate.ddl-auto=create",
        "app.seed-admin.enabled=false"
})
class DosFactorTransaccionMySqlTest {

    @MockitoBean
    private DataSeeder dataSeeder;
    @MockitoBean
    private EmailService emailService;
    @MockitoBean
    private EventosService eventosService;

    @Autowired
    private DosFactorService servicio;
    @Autowired
    private SeleccionRolDesafioService seleccionServicio;
    @Autowired
    private AuthService authService;
    @Autowired
    private UsuarioRepository usuarios;
    @Autowired
    private AccesoRepository accesos;
    @Autowired
    private RolRepository roles;
    @Autowired
    private UsuarioRolRepository usuarioRoles;
    @Autowired
    private DispositivoUsuarioRepository dispositivos;
    @Autowired
    private JwtService jwtService;
    @Autowired
    private JwtAuthFilter filtroJwt;
    @Autowired
    private JdbcTemplate jdbc;

    private record ResultadoConcurrente(boolean primero, Throwable error) {
    }

    private record ResultadoCambioRol(boolean primero, LoginResponse respuesta, Throwable error) {
    }

    private record RespuestaFiltro(MockHttpServletResponse response, MockFilterChain cadena) {
    }

    private final DosFactorService.Contexto contexto = new DosFactorService.Contexto("Chrome en Windows", "190.1.2.3");
    private final AtomicReference<String> codigoEnviado = new AtomicReference<>();
    private final AtomicInteger llamadasConsumo = new AtomicInteger();
    private final ThreadLocal<Boolean> esPrimeraSolicitud = new ThreadLocal<>();
    private CyclicBarrier barreraConsumo;
    private CountDownLatch ganadorAdquirio;
    private CountDownLatch perdedorVaIntentar;
    private UsuarioRepository repositorioReal;
    private JwtService jwtServiceReal;
    private Integer idUsuario;
    private Integer idRol;
    private Integer idRolActual;
    private Integer idRolSolicitado;
    private Integer idDispositivo;
    private String nombreRolActual;
    private String nombreRolSolicitado;

    @BeforeEach
    void preparar() throws Exception {
        verificarBaseAislada();
        limpiar();
        jwtServiceReal = jwtService;
        codigoEnviado.set(null);
        llamadasConsumo.set(0);
        barreraConsumo = new CyclicBarrier(2);
        ganadorAdquirio = new CountDownLatch(1);
        perdedorVaIntentar = new CountDownLatch(1);
        repositorioReal = usuarios;
        sembrar();
    }

    @AfterEach
    void restaurarRepositorio() {
        restaurarRepositorio(servicio);
        restaurarRepositorio(authService);
        restaurarJwtService();
        SecurityContextHolder.clearContext();
    }

    private void restaurarRepositorio(Object servicioObjetivo) {
        Object objetivo = AopTestUtils.getTargetObject(servicioObjetivo);
        ReflectionTestUtils.setField(objetivo, "usuarioRepository", repositorioReal);
    }

    private void restaurarJwtService() {
        if (jwtServiceReal != null) {
            Object objetivo = AopTestUtils.getTargetObject(authService);
            ReflectionTestUtils.setField(objetivo, "jwtService", jwtServiceReal);
            jwtServiceReal = null;
        }
    }

    @Test
    void iniciarPersisteDesafioYCodigoConTransaccionReal() {
        DosFactorService.Desafio desafio = servicio.iniciar(usuario(), contexto);

        Integer filas = jdbc.queryForObject("select count(*) from usuario where id_usuario = ? "
                        + "and dos_factor_desafio = ? and dos_factor_codigo = ?", Integer.class,
                idUsuario, HashSeguro.sha256(desafio.desafio()), HashSeguro.sha256(codigo()));
        assertEquals(1, filas);
        verify(emailService, times(1)).enviarCodigoDosFactor(anyString(), any(), anyString(), anyInt(), any(), any());
    }

    @Test
    void reenviarPersisteNuevoCodigoConTransaccionReal() {
        DosFactorService.Desafio desafio = servicio.iniciar(usuario(), contexto);
        jdbc.update("update usuario set dos_factor_enviado = ? where id_usuario = ?",
                Timestamp.valueOf(LocalDateTime.now().minusSeconds(46)), idUsuario);

        DosFactorService.Desafio reenviado = servicio.reenviar(desafio.desafio(), contexto);

        assertEquals(desafio.desafio(), reenviado.desafio());
        Integer filas = jdbc.queryForObject("select count(*) from usuario where id_usuario = ? "
                        + "and dos_factor_desafio = ? and dos_factor_codigo = ?", Integer.class,
                idUsuario, HashSeguro.sha256(desafio.desafio()), HashSeguro.sha256(codigo()));
        assertEquals(1, filas);
        verify(emailService, times(2)).enviarCodigoDosFactor(anyString(), any(), anyString(), anyInt(), any(), any());
    }

    @Test
    void verificarPersistaIntentosBloqueoYLimpiezaAlLanzarExcepciones() {
        DosFactorService.Desafio desafio = servicio.iniciar(usuario(), contexto);
        String malo = otroCodigo(codigo());

        assertThrows(ResourceNotFoundException.class,
                () -> servicio.verificar(desafio.desafio(), malo, false, null, contexto));
        assertEquals(1, intentosFallidos());
        assertNull(fechaBloqueo());

        jdbc.update("update usuario set intentos_fallidos = ? where id_usuario = ?",
                AuthService.MAX_INTENTOS_FALLIDOS - 1, idUsuario);

        assertThrows(CuentaBloqueadaException.class,
                () -> servicio.verificar(desafio.desafio(), malo, false, null, contexto));
        assertEquals(AuthService.MAX_INTENTOS_FALLIDOS, intentosFallidos());
        assertNotNull(fechaBloqueo());
        assertNull(columna("dos_factor_desafio"));
        assertNull(columna("dos_factor_codigo"));
    }

    @Test
    void verificarConcurrenteSoloConsumeUnaVezYRechazaReutilizacion() throws Exception {
        DosFactorService.Desafio desafio = servicio.iniciar(usuario(), contexto);
        String codigo = codigo();
        prepararBloqueo("consumirDesafioDosFactor");

        ResultadoConcurrente primero;
        ResultadoConcurrente segundo;
        try (ExecutorService executor = Executors.newFixedThreadPool(2, tarea -> {
            Thread hilo = new Thread(tarea, "dos-factor-concurrencia");
            hilo.setDaemon(true);
            return hilo;
        })) {
            Future<ResultadoConcurrente> primera = executor.submit(() -> verificarEnHilo(true, desafio.desafio(), codigo));
            Future<ResultadoConcurrente> segunda = executor.submit(() -> verificarEnHilo(false, desafio.desafio(), codigo));
            primero = primera.get(60, TimeUnit.SECONDS);
            segundo = segunda.get(60, TimeUnit.SECONDS);
        }

        assertEquals(2, llamadasConsumo.get(), "Ambas solicitudes deben llegar al consumo atómico");
        ResultadoConcurrente ganador = primero.error() == null ? primero : segundo;
        ResultadoConcurrente perdedor = primero.error() == null ? segundo : primero;
        assertTrue(ganador.error() == null, "Una solicitud debe verificar el código");
        assertNotNull(perdedor.error(), "La otra solicitud debe ser rechazada");
        assertTrue(perdedor.error() instanceof DosFactorVencidoException,
                "El perdedor debe recibir el error de desafío ya consumido");
        assertEquals(1, registrosConColumnaNula("dos_factor_desafio"));
        assertEquals(1, registrosConColumnaNula("dos_factor_codigo"));
        assertEquals(1, registrosConColumnaNula("dos_factor_desafio_expiracion"));
        assertEquals(1, registrosConColumnaNula("dos_factor_codigo_expiracion"));
        assertThrows(DosFactorVencidoException.class,
                () -> servicio.verificar(desafio.desafio(), codigo, false, null, contexto));
    }

    @Test
    void consumirSeleccionConcurrenteSoloConsumeUnaVezYRechazaReutilizacion() throws Exception {
        String desafio = seleccionServicio.emitir(usuario());
        prepararBloqueo("consumirDesafio");

        ResultadoConcurrente primero;
        ResultadoConcurrente segundo;
        try (ExecutorService executor = Executors.newFixedThreadPool(2, tarea -> {
            Thread hilo = new Thread(tarea, "seleccion-rol-concurrencia");
            hilo.setDaemon(true);
            return hilo;
        })) {
            Future<ResultadoConcurrente> primera = executor.submit(() -> consumirEnHilo(true, desafio, idRol));
            Future<ResultadoConcurrente> segunda = executor.submit(() -> consumirEnHilo(false, desafio, idRol));
            primero = primera.get(60, TimeUnit.SECONDS);
            segundo = segunda.get(60, TimeUnit.SECONDS);
        }

        assertEquals(2, llamadasConsumo.get(), "Ambas solicitudes deben llegar al consumo atómico");
        ResultadoConcurrente ganador = primero.error() == null ? primero : segundo;
        ResultadoConcurrente perdedor = primero.error() == null ? segundo : primero;
        assertTrue(ganador.error() == null, "Una solicitud debe consumir la selección de rol");
        assertNotNull(perdedor.error(), "La otra solicitud debe ser rechazada");
        assertTrue(perdedor.error() instanceof DesafioSeleccionRolInvalidoException,
                "El perdedor debe recibir el error de desafío ya consumido");
        assertEquals(1, registrosConColumnaNula("dos_factor_desafio"));
        assertEquals(1, registrosConColumnaNula("dos_factor_desafio_expiracion"));
        assertThrows(DesafioSeleccionRolInvalidoException.class, () -> seleccionServicio.consumir(desafio, idRol));
    }

    @Test
    void cambiarRolConcurrenteAplicaCasUnaVezEInvalidaJwtAnterior() throws Exception {
        String tokenAnterior = jwtService.generarToken(idUsuario, usuario().getCodigo(), idRolActual,
                List.of(nombreRolActual), false, null, 0, idDispositivo);
        prepararBloqueo(authService, "usuarioRepository", "incrementarVersionTokenSiCoincide",
                this::esperarBloqueoCambioDeRol);

        ResultadoCambioRol primero;
        ResultadoCambioRol segundo;
        try (ExecutorService executor = Executors.newFixedThreadPool(2, tarea -> {
            Thread hilo = new Thread(tarea, "cambio-rol-concurrencia");
            hilo.setDaemon(true);
            return hilo;
        })) {
            Future<ResultadoCambioRol> primera = executor.submit(() -> cambiarRolEnHilo(true, 0));
            Future<ResultadoCambioRol> segunda = executor.submit(() -> cambiarRolEnHilo(false, 0));
            primero = primera.get(60, TimeUnit.SECONDS);
            segundo = segunda.get(60, TimeUnit.SECONDS);
        }

        assertEquals(2, llamadasConsumo.get(), "Ambas solicitudes deben llegar al CAS");
        ResultadoCambioRol ganador = primero.error() == null ? primero : segundo;
        ResultadoCambioRol perdedor = primero.error() == null ? segundo : primero;
        assertNotNull(ganador.respuesta(), "El ganador debe devolver sesión: " + ganador.error());
        assertNull(ganador.error(), "El ganador no debe fallar: " + ganador.error());
        assertNull(perdedor.respuesta(), "El perdedor no debe devolver sesión");
        assertNotNull(perdedor.error(), "El perdedor debe fallar");
        assertTrue(perdedor.error() instanceof VigenciaNoValidaException,
                () -> "El perdedor debe recibir VIGENCIA_TOKEN_OBSOLETO, pero recibió: " + perdedor.error());
        assertEquals(VigenciaNoValidaException.CODIGO_TOKEN_OBSOLETO,
                ((VigenciaNoValidaException) perdedor.error()).getCodigo());
        assertEquals(1, versionPersistida(), "La versión debe aumentar exactamente una vez");

        Claims claims = jwtService.parse(ganador.respuesta().getToken());
        assertEquals(idRolSolicitado, jwtService.getIdRolActivo(claims));
        assertEquals(List.of(nombreRolSolicitado), claims.get("roles", List.class));
        assertEquals(1, jwtService.getVersionToken(claims));
        assertEquals(idDispositivo, jwtService.getDispositivo(claims));

        RespuestaFiltro anterior = filtrar(tokenAnterior);
        assertEquals(403, anterior.response().getStatus());
        assertTrue(anterior.response().getContentAsString()
                .contains(VigenciaNoValidaException.CODIGO_TOKEN_OBSOLETO));
        assertNull(anterior.cadena().getRequest());

        RespuestaFiltro nuevo = filtrar(ganador.respuesta().getToken());
        assertEquals(200, nuevo.response().getStatus());
        assertNotNull(nuevo.cadena().getRequest());

        assertEquals(1, dispositivos.count());
        DispositivoUsuario registrado = dispositivos.findById(idDispositivo).orElseThrow();
        assertNotNull(registrado.getConfiableHasta());
        assertEquals(HashSeguro.sha256("cookie-confianza"), registrado.getTokenHash());
    }

    @Test
    void cambiarRolHaceRollbackSiFallaLaEmisionDelJwtTrasElCas() throws Exception {
        assertTrue(AopUtils.isAopProxy(authService), "AuthService debe invocarse mediante su proxy transaccional real");
        String tokenAnterior = jwtService.generarToken(idUsuario, usuario().getCodigo(), idRolActual,
                List.of(nombreRolActual), false, null, 0, idDispositivo);

        Answer<Object> delegadoRepositorio = delegatesTo(repositorioReal);
        AtomicInteger actualizados = new AtomicInteger();
        UsuarioRepository repositorioConEspia = mock(UsuarioRepository.class, withSettings().defaultAnswer(invocacion -> {
            Object resultado = delegadoRepositorio.answer(invocacion);
            if ("incrementarVersionTokenSiCoincide".equals(invocacion.getMethod().getName())) {
                actualizados.set(((Number) resultado).intValue());
            }
            return resultado;
        }));

        Answer<Object> delegadoJwt = delegatesTo(jwtServiceReal);
        JwtService jwtConFallo = mock(JwtService.class, withSettings().defaultAnswer(invocacion -> {
            if (actualizados.get() == 1) {
                throw new IllegalStateException("Fallo controlado al emitir el JWT");
            }
            return delegadoJwt.answer(invocacion);
        }));

        Object objetivo = AopTestUtils.getTargetObject(authService);
        ReflectionTestUtils.setField(objetivo, "usuarioRepository", repositorioConEspia);
        ReflectionTestUtils.setField(objetivo, "jwtService", jwtConFallo);

        IllegalStateException fallo = assertThrows(IllegalStateException.class,
                () -> authService.cambiarRol(idUsuario, idRolSolicitado, idRolActual, idDispositivo, 0));
        assertEquals("Fallo controlado al emitir el JWT", fallo.getMessage());
        assertEquals(1, actualizados.get(), "El CAS debe ejecutarse correctamente antes del fallo controlado");
        assertEquals(0, versionTokenIndependiente(), "El rollback debe conservar la versión original");

        RespuestaFiltro anterior = filtrar(tokenAnterior);
        assertEquals(200, anterior.response().getStatus());
        assertNotNull(anterior.cadena().getRequest());

        assertEquals(1, dispositivos.count());
        DispositivoUsuario registrado = dispositivos.findById(idDispositivo).orElseThrow();
        assertNotNull(registrado.getConfiableHasta());
        assertEquals(HashSeguro.sha256("cookie-confianza"), registrado.getTokenHash());
    }

    private ResultadoCambioRol cambiarRolEnHilo(boolean primero, int version) {
        esPrimeraSolicitud.set(primero);
        LoginResponse respuesta = null;
        Throwable error = null;
        try {
            respuesta = authService.cambiarRol(idUsuario, idRolSolicitado, idRolActual, idDispositivo, version);
        } catch (Throwable ex) {
            error = ex;
        } finally {
            esPrimeraSolicitud.remove();
        }
        return new ResultadoCambioRol(primero, respuesta, error);
    }

    private RespuestaFiltro filtrar(String token) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/usuarios");
        request.setServletPath("/usuarios");
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain cadena = new MockFilterChain();
        filtroJwt.doFilter(request, response, cadena);
        return new RespuestaFiltro(response, cadena);
    }

    private int versionPersistida() {
        return versionTokenIndependiente();
    }

    private int versionTokenIndependiente() {
        String consulta = "select version_token from usuario where id_usuario = ?";
        try (Connection conexion = conexionIndependiente();
             PreparedStatement statement = conexion.prepareStatement(consulta)) {
            statement.setInt(1, idUsuario);
            try (ResultSet resultado = statement.executeQuery()) {
                assertTrue(resultado.next(), "El usuario debe existir al consultar su versión");
                return resultado.getInt(1);
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("No se pudo consultar versionToken de forma independiente", ex);
        }
    }

    private ResultadoConcurrente verificarEnHilo(boolean primero, String desafio, String codigo) {
        esPrimeraSolicitud.set(primero);
        Throwable error = null;
        try {
            servicio.verificar(desafio, codigo, false, null, contexto);
        } catch (Throwable ex) {
            error = ex;
        } finally {
            esPrimeraSolicitud.remove();
        }
        return new ResultadoConcurrente(primero, error);
    }

    private ResultadoConcurrente consumirEnHilo(boolean primero, String desafio, Integer rol) {
        esPrimeraSolicitud.set(primero);
        Throwable error = null;
        try {
            seleccionServicio.consumir(desafio, rol);
        } catch (Throwable ex) {
            error = ex;
        } finally {
            esPrimeraSolicitud.remove();
        }
        return new ResultadoConcurrente(primero, error);
    }

    private void prepararBloqueo(String metodoConsumo) {
        prepararBloqueo(servicio, "usuarioRepository", metodoConsumo, this::esperarBloqueoDelPerdedor);
    }

    private void prepararBloqueo(Object servicioObjetivo, String campoRepositorio, String metodoConsumo,
                                 Runnable verificacionBloqueo) {
        Answer<Object> delegado = delegatesTo(repositorioReal);
        Answer<Object> answer = invocacion -> {
            if (!metodoConsumo.equals(invocacion.getMethod().getName())) {
                return delegado.answer(invocacion);
            }
            if (llamadasConsumo.incrementAndGet() > 2) {
                return delegado.answer(invocacion);
            }
            esperar(barreraConsumo);
            if (Boolean.TRUE.equals(esPrimeraSolicitud.get())) {
                Object resultado = delegado.answer(invocacion);
                ganadorAdquirio.countDown();
                esperar(perdedorVaIntentar);
                verificacionBloqueo.run();
                return resultado;
            }
            esperar(ganadorAdquirio);
            perdedorVaIntentar.countDown();
            return delegado.answer(invocacion);
        };
        UsuarioRepository instrumentado = mock(UsuarioRepository.class, withSettings().defaultAnswer(answer));
        Object objetivo = AopTestUtils.getTargetObject(servicioObjetivo);
        ReflectionTestUtils.setField(objetivo, campoRepositorio, instrumentado);
    }

    private void esperar(CyclicBarrier barrera) {
        try {
            barrera.await(30, TimeUnit.SECONDS);
        } catch (Exception ex) {
            throw new IllegalStateException("Las dos transacciones no alcanzaron el punto de sincronización", ex);
        }
    }

    private void esperar(CountDownLatch latch) {
        try {
            if (!latch.await(30, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Sincronización de consumo agotada");
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Sincronización de consumo interrumpida", ex);
        }
    }

    private void esperarBloqueoDelPerdedor() {
        esperarBloqueoEnUsuario("La segunda transacción no intentó el consumo mientras la primera mantenía el bloqueo");
    }

    private void esperarBloqueoCambioDeRol() {
        esperarBloqueoEnUsuario("La segunda transacción no intentó el cambio de rol mientras la primera mantenía el bloqueo");
    }

    private void esperarBloqueoEnUsuario(String mensaje) {
        long limite = System.nanoTime() + TimeUnit.SECONDS.toNanos(20);
        String consulta = "SELECT COUNT(*) FROM performance_schema.data_lock_waits w "
                + "JOIN performance_schema.data_locks l ON l.ENGINE_LOCK_ID = w.BLOCKING_ENGINE_LOCK_ID "
                + "WHERE l.OBJECT_SCHEMA = ? AND l.OBJECT_NAME = 'usuario'";
        try (Connection conexion = conexionIndependiente();
             PreparedStatement statement = conexion.prepareStatement(consulta)) {
            statement.setString(1, conexion.getCatalog());
            while (System.nanoTime() < limite) {
                try (ResultSet resultado = statement.executeQuery()) {
                    if (resultado.next() && resultado.getInt(1) > 0) {
                        return;
                    }
                }
                Thread.onSpinWait();
            }
            fail(mensaje);
        } catch (Exception ex) {
            throw new IllegalStateException("No se pudo verificar la espera real del bloqueo", ex);
        }
    }

    private int registrosConColumnaNula(String nombre) {
        return jdbc.queryForObject("select count(*) from usuario where id_usuario = ? and " + nombre + " is null",
                Integer.class, idUsuario);
    }

    private Connection conexionIndependiente() throws SQLException {
        return DriverManager.getConnection(
                System.getenv("ESCOLAR_TEST_MYSQL_URL"),
                System.getenv("ESCOLAR_TEST_MYSQL_USER"),
                System.getenv("ESCOLAR_TEST_MYSQL_PASSWORD"));
    }

    private void verificarBaseAislada() throws Exception {
        String url = System.getenv("ESCOLAR_TEST_MYSQL_URL");
        assertTrue(url != null && url.startsWith("jdbc:mysql://127.0.0.1:"));
        try (Connection conexion = conexionIndependiente()) {
            String nombre = conexion.getCatalog();
            assertNotNull(nombre);
            assertTrue(nombre.endsWith("_test"), "La base de pruebas debe terminar en _test");
            assertFalse(nombre.toUpperCase(Locale.ROOT).contains("ESCOLAR"),
                    "La base de pruebas no puede ser ESCOLAR");
        }
    }

    private void limpiar() {
        jdbc.update("DELETE FROM dispositivo_usuario");
        jdbc.update("DELETE FROM usuarioRol");
        jdbc.update("DELETE FROM rol");
        jdbc.update("DELETE FROM usuario");
        jdbc.update("DELETE FROM acceso");
    }

    private void sembrar() {
        jdbc.update("INSERT INTO acceso (id_acceso, nombre) VALUES (?, ?)", AccesoConstants.ACTIVO, "Activo");
        jdbc.update("INSERT INTO acceso (id_acceso, nombre) VALUES (?, ?)", AccesoConstants.ELIMINADO, "Eliminado");
        Acceso activo = accesos.findById(AccesoConstants.ACTIVO).orElseThrow();

        Usuario registro = new Usuario();
        registro.setCodigo("TX2FA");
        registro.setNombre("Ana");
        registro.setApellidoPat("Perez");
        registro.setApellidoMat("Prueba");
        registro.setContraseña("sin-acceso-interactivo");
        registro.setGmail("ana.perez@gmail.com");
        registro.setAcceso(activo);
        registro.setFechaNaci(LocalDate.of(1990, 1, 1));
        registro.setFechaCreacion(LocalDate.now());
        registro.setFechaInicioVigencia(LocalDate.now());
        registro.setIntentosFallidos(0);
        registro.setIntentosCodigoReset(0);
        registro.setDebeCambiarContrasena(false);
        registro.setVersionToken(0);
        idUsuario = usuarios.save(registro).getIdUsuario();

        Rol primero = roles.save(new Rol(null, "CONC-ROL-A", "#123456", activo));
        Rol segundo = roles.save(new Rol(null, "CONC-ROL-B", "#654321", activo));
        idRol = segundo.getIdRol();
        idRolActual = primero.getIdRol();
        idRolSolicitado = segundo.getIdRol();
        nombreRolActual = primero.getNombre();
        nombreRolSolicitado = segundo.getNombre();
        for (Rol rol : List.of(primero, segundo)) {
            UsuarioRol asignacion = new UsuarioRol();
            asignacion.setUsuario(registro);
            asignacion.setRol(rol);
            asignacion.setFechaAsignacion(LocalDateTime.now());
            usuarioRoles.save(asignacion);
        }

        DispositivoUsuario dispositivo = new DispositivoUsuario();
        dispositivo.setUsuario(registro);
        dispositivo.setTokenHash(HashSeguro.sha256("cookie-confianza"));
        dispositivo.setNavegador("Chrome en Windows");
        dispositivo.setIp("190.1.2.3");
        dispositivo.setPrimerUso(LocalDateTime.now().minusDays(1));
        dispositivo.setUltimoUso(LocalDateTime.now());
        dispositivo.setConfiableHasta(LocalDateTime.now().plusDays(7));
        dispositivo.setHuellaConfianza(HashSeguro.sha256("huella-confianza"));
        idDispositivo = dispositivos.save(dispositivo).getIdDispositivo();

        when(emailService.enviarCodigoDosFactor(anyString(), any(), anyString(), anyInt(), any(), any()))
                .thenAnswer(invocacion -> {
                    codigoEnviado.set(invocacion.getArgument(2));
                    return true;
                });
    }

    private Usuario usuario() {
        return usuarios.findById(idUsuario).orElseThrow();
    }

    private String codigo() {
        String codigo = codigoEnviado.get();
        assertNotNull(codigo);
        return codigo;
    }

    private String otroCodigo(String valido) {
        return "111111".equals(valido) ? "222222" : "111111";
    }

    private int intentosFallidos() {
        return jdbc.queryForObject("select intentos_fallidos from usuario where id_usuario = ?", Integer.class, idUsuario);
    }

    private LocalDateTime fechaBloqueo() {
        Timestamp fecha = jdbc.queryForObject("select fecha_bloqueo from usuario where id_usuario = ?", Timestamp.class, idUsuario);
        return fecha == null ? null : fecha.toLocalDateTime();
    }

    private String columna(String nombre) {
        return jdbc.queryForObject("select " + nombre + " from usuario where id_usuario = ?", String.class, idUsuario);
    }
}
