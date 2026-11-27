package com.example.Escolar.Service;

import com.example.Escolar.Config.DataSeeder;
import com.example.Escolar.Model.Acceso;
import com.example.Escolar.Model.AnioEscolar;
import com.example.Escolar.Model.Asignacion;
import com.example.Escolar.Model.CambioDocente;
import com.example.Escolar.Model.CambioDocenteAsignacion;
import com.example.Escolar.Model.Curso;
import com.example.Escolar.Model.Docente;
import com.example.Escolar.Model.EstadoCambioDocente;
import com.example.Escolar.Model.Grado;
import com.example.Escolar.Model.GradoSeccion;
import com.example.Escolar.Model.Nivel;
import com.example.Escolar.Model.OrigenOperacion;
import com.example.Escolar.Model.TipoCambioDocente;
import com.example.Escolar.Model.TipoCurso;
import com.example.Escolar.Model.Turno;
import com.example.Escolar.Model.Usuario;
import com.example.Escolar.Repository.AccesoRepository;
import com.example.Escolar.Repository.AnioEscolarRepository;
import com.example.Escolar.Repository.AsignacionRepository;
import com.example.Escolar.Repository.CambioDocenteAsignacionRepository;
import com.example.Escolar.Repository.CambioDocenteRepository;
import com.example.Escolar.Repository.CursoRepository;
import com.example.Escolar.Repository.DocenteRepository;
import com.example.Escolar.Repository.GradoRepository;
import com.example.Escolar.Repository.GradoSeccionRepository;
import com.example.Escolar.Repository.NivelRepository;
import com.example.Escolar.Repository.TurnoRepository;
import com.example.Escolar.Repository.UsuarioRepository;
import com.example.Escolar.Security.ConcesionesService;
import com.example.Escolar.Util.FechaColegio;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.AopTestUtils;
import org.springframework.test.util.ReflectionTestUtils;

import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.AdditionalAnswers.delegatesTo;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

@EnabledIfEnvironmentVariable(named = "ESCOLAR_TEST_MYSQL_URL",
        matches = "jdbc:mysql://127\\.0\\.0\\.1:[0-9]+/[a-zA-Z0-9_]+_test(?:\\?.*)?")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, properties = {
        "spring.datasource.url=${ESCOLAR_TEST_MYSQL_URL}",
        "spring.datasource.username=${ESCOLAR_TEST_MYSQL_USER}",
        "spring.datasource.password=${ESCOLAR_TEST_MYSQL_PASSWORD:}",
        "spring.jpa.hibernate.ddl-auto=create",
        "spring.datasource.hikari.connection-init-sql=SET SESSION optimizer_search_depth = 4, innodb_lock_wait_timeout = 30",
        "app.seed-admin.enabled=false"
})
class CambioDocenteConcurrenciaMySqlTest {

    private enum Ganador { CANCELAR, ACTIVAR }

    private record Resultado(Ganador operacion, Throwable error) {
    }

    @MockitoBean
    private DataSeeder dataSeeder;
    @MockitoBean
    private CambioDocenteJob cambioDocenteJob;
    @MockitoBean
    private SuspensionDocenteJob suspensionDocenteJob;
    @MockitoBean
    private EventosService eventosService;
    @MockitoBean
    private AvisoFaltaService avisoFaltaService;
    @MockitoBean
    private AccesoContextoService contexto;
    @MockitoBean
    private ConcesionesService concesionesService;

    @Autowired
    private CambioDocenteService service;
    @Autowired
    private CambioDocenteRepository cambioRepository;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private AccesoRepository accesos;
    @Autowired
    private UsuarioRepository usuarios;
    @Autowired
    private DocenteRepository docentes;
    @Autowired
    private AnioEscolarRepository anios;
    @Autowired
    private NivelRepository niveles;
    @Autowired
    private TurnoRepository turnos;
    @Autowired
    private GradoRepository grados;
    @Autowired
    private CursoRepository cursos;
    @Autowired
    private GradoSeccionRepository gradoSecciones;
    @Autowired
    private AsignacionRepository asignaciones;
    @Autowired
    private CambioDocenteAsignacionRepository detalles;

    private final AtomicReference<Ganador> ganador = new AtomicReference<>();
    private final ThreadLocal<Ganador> operacion = new ThreadLocal<>();
    private final AtomicInteger llamadasBloquear = new AtomicInteger();

    private LocalDate hoy;
    private Integer idCambio;
    private Integer idAsignacion;
    private Integer idTitular;
    private Integer idSustituto;
    private CyclicBarrier barrera;
    private CountDownLatch ganadorAdquirio;
    private CountDownLatch perdedorVaIntentar;
    private CambioDocenteRepository repositorioReal;

    @BeforeEach
    void preparar() throws Exception {
        verificarBaseAislada();
        hoy = FechaColegio.hoy();
        llamadasBloquear.set(0);
        barrera = new CyclicBarrier(2);
        ganadorAdquirio = new CountDownLatch(1);
        perdedorVaIntentar = new CountDownLatch(1);
        repositorioReal = cambioRepository;
        limpiar();
        sembrar();
        prepararBloqueo();
    }

    @AfterEach
    void restaurarRepositorio() {
        Object objetivo = AopTestUtils.getTargetObject(service);
        ReflectionTestUtils.setField(objetivo, "cambioRepository", repositorioReal);
    }

    @ParameterizedTest
    @EnumSource(Ganador.class)
    void cancelarYActivarConcurrentesRespetanUnSoloGanador(Ganador primero) throws Exception {
        ganador.set(primero);
        Ganador segundo = primero == Ganador.CANCELAR ? Ganador.ACTIVAR : Ganador.CANCELAR;

        Resultado resultadoPrimero;
        Resultado resultadoSegundo;
        try (ExecutorService executor = Executors.newFixedThreadPool(2, tarea -> {
            Thread hilo = new Thread(tarea, "cambio-docente-concurrencia");
            hilo.setDaemon(true);
            return hilo;
        })) {
            Future<Resultado> primera = executor.submit(() -> ejecutar(primero));
            Future<Resultado> segunda = executor.submit(() -> ejecutar(segundo));
            resultadoPrimero = primera.get(60, TimeUnit.SECONDS);
            resultadoSegundo = segunda.get(60, TimeUnit.SECONDS);
        }

        assertEquals(2, llamadasBloquear.get(), "Ambas operaciones deben bloquear la fila del cambio");
        Resultado cancelacion = primero == Ganador.CANCELAR ? resultadoPrimero : resultadoSegundo;
        Resultado activacion = primero == Ganador.ACTIVAR ? resultadoPrimero : resultadoSegundo;

        if (primero == Ganador.CANCELAR) {
            assertNull(cancelacion.error());
            assertNull(activacion.error());
            assertEquals("CANCELADO", estadoCambio());
            assertEquals(idTitular, docenteActual());
            assertNull(fechaTransferencia());
        } else {
            assertNull(activacion.error());
            assertNotNull(cancelacion.error());
            assertTrue(cancelacion.error() instanceof IllegalArgumentException);
            assertEquals("Solo se puede cancelar un cambio programado", cancelacion.error().getMessage());
            assertEquals("VIGENTE", estadoCambio());
            assertEquals(idSustituto, docenteActual());
            assertNotNull(fechaTransferencia());
        }
    }

    private Resultado ejecutar(Ganador operacionActual) {
        operacion.set(operacionActual);
        Throwable error = null;
        try {
            if (operacionActual == Ganador.CANCELAR) {
                service.cancelar(idCambio, "Concurrencia");
            } else {
                service.activarProgramado(idCambio, hoy);
            }
        } catch (Throwable ex) {
            error = ex;
        }
        return new Resultado(operacionActual, error);
    }

    private void prepararBloqueo() {
        Answer<Object> delegado = delegatesTo(repositorioReal);
        Answer<Object> answer = invocacion -> {
            if ("bloquear".equals(invocacion.getMethod().getName())) {
                if (llamadasBloquear.incrementAndGet() > 2) {
                    return repositorioReal.bloquear(idCambio);
                }
                esperar(barrera);
                if (operacion.get() == ganador.get()) {
                    var resultado = repositorioReal.bloquear(idCambio);
                    ganadorAdquirio.countDown();
                    esperar(perdedorVaIntentar);
                    esperarBloqueoDelPerdedor();
                    return resultado;
                }
                esperar(ganadorAdquirio);
                perdedorVaIntentar.countDown();
                return repositorioReal.bloquear(idCambio);
            }
            return delegado.answer(invocacion);
        };
        CambioDocenteRepository cambioInstrumentado = mock(CambioDocenteRepository.class,
                withSettings().defaultAnswer(answer));
        Object objetivo = AopTestUtils.getTargetObject(service);
        ReflectionTestUtils.setField(objetivo, "cambioRepository", cambioInstrumentado);
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
                throw new IllegalStateException("Sincronización de bloqueo agotada");
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Sincronización de bloqueo interrumpida", ex);
        }
    }

    private void esperarBloqueoDelPerdedor() {
        String url = System.getenv("ESCOLAR_TEST_MYSQL_URL");
        String usuario = System.getenv("ESCOLAR_TEST_MYSQL_USER");
        String clave = System.getenv("ESCOLAR_TEST_MYSQL_PASSWORD");
        long limite = System.nanoTime() + TimeUnit.SECONDS.toNanos(20);
        try (Connection conexion = DriverManager.getConnection(url, usuario, clave);
             Statement statement = conexion.createStatement()) {
            while (System.nanoTime() < limite) {
                try (ResultSet resultado = statement.executeQuery(
                        "SELECT COUNT(*) FROM performance_schema.data_lock_waits")) {
                    if (resultado.next() && resultado.getInt(1) > 0) {
                        return;
                    }
                }
                Thread.sleep(20);
            }
            fail("La segunda transacción no intentó adquirir el bloqueo mientras la primera lo mantenía");
        } catch (Exception ex) {
            throw new IllegalStateException("No se pudo verificar la espera real del bloqueo", ex);
        }
    }

    private void verificarBaseAislada() throws Exception {
        String url = System.getenv("ESCOLAR_TEST_MYSQL_URL");
        assertTrue(url != null && url.startsWith("jdbc:mysql:"));
        try (Connection conexion = DriverManager.getConnection(url,
                System.getenv("ESCOLAR_TEST_MYSQL_USER"),
                System.getenv("ESCOLAR_TEST_MYSQL_PASSWORD"))) {
            assertTrue(conexion.getCatalog() != null && conexion.getCatalog().endsWith("_test"),
                    "La base de pruebas debe terminar en _test");
        }
    }

    private void limpiar() {
        jdbc.update("DELETE FROM cambio_docente_asignacion");
        jdbc.update("DELETE FROM cambio_docente");
        jdbc.update("DELETE FROM asignacion");
        jdbc.update("DELETE FROM gradoSeccion");
        jdbc.update("DELETE FROM curso");
        jdbc.update("DELETE FROM grado");
        jdbc.update("DELETE FROM nivel");
        jdbc.update("DELETE FROM turno");
        jdbc.update("DELETE FROM docente");
        jdbc.update("DELETE FROM usuario");
        jdbc.update("DELETE FROM anioEscolar");
        jdbc.update("DELETE FROM acceso");
    }

    private void sembrar() {
        jdbc.update("INSERT INTO acceso (id_acceso, nombre) VALUES (?, ?)",
                AccesoConstants.ACTIVO, "Activo");
        jdbc.update("INSERT INTO acceso (id_acceso, nombre) VALUES (?, ?)",
                AccesoConstants.ELIMINADO, "Eliminado");
        Acceso activo = accesos.findById(AccesoConstants.ACTIVO).orElseThrow();

        Usuario registro = usuario(activo, "CONC-ADM");
        when(contexto.idUsuarioAutenticado()).thenReturn(registro.getIdUsuario());
        Docente titular = docente(usuario(activo, "CONC-TIT"), activo);
        Docente sustituto = docente(usuario(activo, "CONC-SUS"), activo);
        idTitular = titular.getIdDocente();
        idSustituto = sustituto.getIdDocente();

        AnioEscolar anio = new AnioEscolar();
        anio.setAnio("2026");
        anio.setEstado(AnioEscolarService.ESTADO_VIGENTE);
        anio.setAcceso(activo);
        anio = anios.save(anio);

        Nivel nivel = new Nivel();
        nivel.setNombre("CONC-NIV");
        nivel.setOrden(1);
        nivel.setAcceso(activo);
        nivel = niveles.save(nivel);

        Turno turno = new Turno();
        turno.setNombre("CONC-TUR");
        turno.setHoraEntrada(java.time.LocalTime.of(8, 0));
        turno.setHoraEntradaLimite(java.time.LocalTime.of(8, 15));
        turno.setHoraFaltaLimite(java.time.LocalTime.of(8, 30));
        turno.setHoraSalida(java.time.LocalTime.of(13, 0));
        turno.setAcceso(activo);
        turno = turnos.save(turno);

        Grado grado = new Grado();
        grado.setNombre("1");
        grado.setNivel(nivel);
        grado.setAcceso(activo);
        grado.setOrden(1);
        grado.setRepite(false);
        grado = grados.save(grado);

        Curso curso = new Curso();
        curso.setNombre("CONC-CUR");
        curso.setCodigo("CONC-CUR");
        curso.setAnioEscolar(anio);
        curso.setNivel(nivel);
        curso.setTipo(TipoCurso.AREA);
        curso.setTieneSubcursos(false);
        curso.setAcceso(activo);
        curso = cursos.save(curso);

        GradoSeccion gradoSeccion = new GradoSeccion();
        gradoSeccion.setGrado(grado);
        gradoSeccion.setTurno(turno);
        gradoSeccion.setAnioEscolar(anio);
        gradoSeccion.setAcceso(activo);
        gradoSeccion = gradoSecciones.save(gradoSeccion);

        Asignacion asignacion = new Asignacion();
        asignacion.setCurso(curso);
        asignacion.setDocente(titular);
        asignacion.setGradoSeccion(gradoSeccion);
        asignacion.setAnioEscolar(anio);
        asignacion.setAcceso(activo);
        asignacion = asignaciones.save(asignacion);
        idAsignacion = asignacion.getIdAsignacion();

        CambioDocente cambio = new CambioDocente();
        cambio.setDocenteAnterior(titular);
        cambio.setDocenteNuevo(sustituto);
        cambio.setMotivo("Concurrencia");
        cambio.setFechaCambio(hoy);
        cambio.setUsuarioRegistro(registro);
        cambio.setAcceso(activo);
        cambio.setTipo(TipoCambioDocente.TEMPORAL);
        cambio.setEstado(EstadoCambioDocente.PROGRAMADO);
        cambio.setAnioEscolar(anio);
        cambio.setFechaInicio(hoy);
        cambio.setIncidencia(false);
        cambio.setOrigenRegistro(OrigenOperacion.MANUAL);
        cambio.setOrigenUltimaOperacion(OrigenOperacion.MANUAL);
        cambio = cambioRepository.save(cambio);
        idCambio = cambio.getIdCambio();

        CambioDocenteAsignacion detalle = new CambioDocenteAsignacion();
        detalle.setCambio(cambio);
        detalle.setAsignacion(asignacion);
        detalle.setDocenteDesde(titular);
        detalle.setDocenteHacia(sustituto);
        detalle.setSinCobertura(false);
        detalle.setDevuelta(false);
        detalle.setAcceso(activo);
        detalles.save(detalle);
    }

    private Usuario usuario(Acceso activo, String codigo) {
        Usuario usuario = new Usuario();
        usuario.setNombre("Concurrencia");
        usuario.setApellidoPat("Prueba");
        usuario.setApellidoMat(codigo);
        usuario.setCodigo(codigo);
        usuario.setContraseña("sin-acceso");
        usuario.setAcceso(activo);
        usuario.setFechaNaci(LocalDate.of(1990, 1, 1));
        usuario.setFechaCreacion(hoy);
        usuario.setFechaInicioVigencia(hoy);
        usuario.setIntentosFallidos(0);
        usuario.setIntentosCodigoReset(0);
        usuario.setDebeCambiarContrasena(false);
        usuario.setVersionToken(0);
        return usuarios.save(usuario);
    }

    private Docente docente(Usuario usuario, Acceso activo) {
        Docente docente = new Docente();
        docente.setUsuario(usuario);
        docente.setAcceso(activo);
        return docentes.save(docente);
    }

    private String estadoCambio() {
        return jdbc.queryForObject("SELECT estado FROM cambio_docente WHERE id_cambio = ?",
                String.class, idCambio);
    }

    private Integer docenteActual() {
        return jdbc.queryForObject("SELECT id_docente FROM asignacion WHERE id_asignacion = ?",
                Integer.class, idAsignacion);
    }

    private LocalDate fechaTransferencia() {
        Date fecha = jdbc.queryForObject("SELECT fecha_transferencia FROM cambio_docente_asignacion WHERE id_cambio = ?",
                Date.class, idCambio);
        return fecha == null ? null : fecha.toLocalDate();
    }
}
