package com.example.Escolar.Service;

import com.example.Escolar.Dto.InscripcionAlumnoRequest;
import com.example.Escolar.Dto.InscripcionApoderadoRequest;
import com.example.Escolar.Dto.InscripcionRequest;
import com.example.Escolar.Dto.MatriculaEdicionRequest;
import com.example.Escolar.Dto.MatriculaResponse;
import com.example.Escolar.Dto.UploadResponse;
import com.example.Escolar.Model.Acceso;
import com.example.Escolar.Model.Alumno;
import com.example.Escolar.Model.AlumnoApoderado;
import com.example.Escolar.Model.AnioEscolar;
import com.example.Escolar.Model.Apoderado;
import com.example.Escolar.Model.Grado;
import com.example.Escolar.Model.GradoSeccion;
import com.example.Escolar.Model.Matricula;
import com.example.Escolar.Model.MetodoPago;
import com.example.Escolar.Model.Nivel;
import com.example.Escolar.Model.PagoMatricula;
import com.example.Escolar.Model.Parentesco;
import com.example.Escolar.Model.Rol;
import com.example.Escolar.Model.Sexo;
import com.example.Escolar.Model.TipoPago;
import com.example.Escolar.Model.Usuario;
import com.example.Escolar.Model.UsuarioRol;
import com.example.Escolar.Repository.AccesoRepository;
import com.example.Escolar.Repository.AlumnoApoderadoRepository;
import com.example.Escolar.Repository.AlumnoRepository;
import com.example.Escolar.Repository.ApoderadoRepository;
import com.example.Escolar.Repository.GradoSeccionRepository;
import com.example.Escolar.Repository.HistorialMatriculaRepository;
import com.example.Escolar.Repository.MatriculaRepository;
import com.example.Escolar.Repository.PagoMatriculaRepository;
import com.example.Escolar.Repository.RolRepository;
import com.example.Escolar.Repository.UsuarioRepository;
import com.example.Escolar.Repository.UsuarioRolRepository;
import com.example.Escolar.Service.EmailService.TipoEnvioCredenciales;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class InscripcionServiceTest {

    @Mock
    private AlumnoRepository alumnoRepository;
    @Mock
    private ApoderadoRepository apoderadoRepository;
    @Mock
    private AlumnoApoderadoRepository alumnoApoderadoRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private UsuarioRolRepository usuarioRolRepository;
    @Mock
    private RolRepository rolRepository;
    @Mock
    private MatriculaRepository matriculaRepository;
    @Mock
    private HistorialMatriculaRepository historialMatriculaRepository;
    @Mock
    private PagoMatriculaRepository pagoMatriculaRepository;
    @Mock
    private GradoSeccionRepository gradoSeccionRepository;
    @Mock
    private AccesoRepository accesoRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private CloudinaryService cloudinaryService;
    @Mock
    private EmailService emailService;
    @Mock
    private PagoConfigService pagoConfigService;
    @Mock
    private MatriculaReglasService matriculaReglasService;
    @Mock
    private MatriculaService matriculaService;
    @InjectMocks
    private InscripcionService inscripcionService;

    private final Nivel primaria = nivel(2, "Primaria");
    private final Nivel secundaria = nivel(3, "Secundaria");
    private final AnioEscolar anio2027 = new AnioEscolar();
    private final MetodoPago yape = metodo(1, "Yape", true);
    private final MetodoPago efectivo = metodo(2, "Efectivo", false);
    private final Rol rolApoderado = new Rol();
    private GradoSeccion primeroA;
    private GradoSeccion secundariaA;

    @BeforeEach
    void setUp() {
        when(accesoRepository.findById(any())).thenAnswer(i -> {
            Acceso acceso = new Acceso();
            acceso.setIdAcceso(i.getArgument(0));
            return Optional.of(acceso);
        });
        anio2027.setIdAnio(2);
        anio2027.setAnio("2027");
        anio2027.setEstado(AnioEscolarService.ESTADO_POR_COMENZAR);
        primeroA = seccion(10, primaria);
        secundariaA = seccion(20, secundaria);
        when(gradoSeccionRepository.findByIdGradoSeccionAndAccesoNot(eq(10), any())).thenReturn(Optional.of(primeroA));
        when(gradoSeccionRepository.findByIdGradoSeccionAndAccesoNot(eq(20), any())).thenReturn(Optional.of(secundariaA));
        when(pagoConfigService.montoDe(eq(primaria), any())).thenReturn(new BigDecimal("220"));
        when(pagoConfigService.montoDe(eq(secundaria), any())).thenReturn(new BigDecimal("300"));
        when(pagoConfigService.metodoActivo(1)).thenReturn(yape);
        when(pagoConfigService.metodoActivo(2)).thenReturn(efectivo);
        when(pagoConfigService.parentesco(any())).thenReturn(new Parentesco());

        Usuario registrador = new Usuario();
        registrador.setIdUsuario(1);
        when(usuarioRepository.findByIdUsuarioAndAccesoNot(eq(1), any())).thenReturn(Optional.of(registrador));
        rolApoderado.setIdRol(4);
        rolApoderado.setNombre("Apoderado");
        when(rolRepository.findByNombre("Apoderado")).thenReturn(Optional.of(rolApoderado));

        when(alumnoRepository.save(any())).thenAnswer(i -> {
            Alumno alumno = i.getArgument(0);
            alumno.setIdAlumno(9);
            return alumno;
        });
        when(usuarioRepository.save(any())).thenAnswer(i -> {
            Usuario usuario = i.getArgument(0);
            if (usuario.getIdUsuario() == null) {
                usuario.setIdUsuario(30);
            }
            return usuario;
        });
        when(apoderadoRepository.save(any())).thenAnswer(i -> {
            Apoderado apoderado = i.getArgument(0);
            if (apoderado.getIdApoderado() == null) {
                apoderado.setIdApoderado(40);
            }
            return apoderado;
        });
        when(alumnoApoderadoRepository.save(any())).thenAnswer(i -> {
            AlumnoApoderado vinculo = i.getArgument(0);
            if (vinculo.getIdAlumnoApoderado() == null) {
                vinculo.setIdAlumnoApoderado(60);
            }
            return vinculo;
        });
        when(matriculaRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(passwordEncoder.encode(anyString())).thenReturn("hash");
        when(usuarioRepository.countByCodigoStartingWith(anyString())).thenReturn(0L);
        UploadResponse subida = new UploadResponse();
        subida.setUrl("https://img/x.jpg");
        subida.setPublicId("x");
        when(cloudinaryService.upload(any(), anyString())).thenReturn(subida);
        when(matriculaService.toResponse(any())).thenReturn(new MatriculaResponse());
    }

    private static Nivel nivel(int id, String nombre) {
        Nivel nivel = new Nivel();
        nivel.setIdNivel(id);
        nivel.setNombre(nombre);
        return nivel;
    }

    private static MetodoPago metodo(int id, String nombre, boolean pideDatos) {
        MetodoPago metodo = new MetodoPago();
        metodo.setIdMetodoPago(id);
        metodo.setNombre(nombre);
        metodo.setRequiereCodigo(pideDatos);
        metodo.setRequiereComprobante(pideDatos);
        return metodo;
    }

    private GradoSeccion seccion(int id, Nivel nivel) {
        Grado grado = new Grado();
        grado.setNombre("1ro");
        grado.setNivel(nivel);
        GradoSeccion gs = new GradoSeccion();
        gs.setIdGradoSeccion(id);
        gs.setGrado(grado);
        gs.setAnioEscolar(anio2027);
        return gs;
    }

    private InscripcionRequest solicitud(Integer idMetodoPago, String codigo) {
        InscripcionAlumnoRequest alumno = new InscripcionAlumnoRequest();
        alumno.setDocumentoIdentidad("70000001");
        alumno.setNombre("Luis");
        alumno.setApellidoPat("García");
        alumno.setApellidoMat("Torres");
        alumno.setFechaNacimiento(LocalDate.of(2019, 3, 1));
        alumno.setSexo(Sexo.MASCULINO);
        alumno.setDireccion("Av. Lima 123");

        InscripcionApoderadoRequest apoderado = new InscripcionApoderadoRequest();
        apoderado.setDocumentoIdentidad("40000001");
        apoderado.setNombre("Ana");
        apoderado.setApellidoPat("Torres");
        apoderado.setApellidoMat("Ruiz");
        apoderado.setFechaNaci(LocalDate.of(1990, 1, 1));
        apoderado.setSexo(Sexo.FEMENINO);
        apoderado.setDireccion("Av. Lima 123");
        apoderado.setIdParentesco(2);
        apoderado.setCelular("987654321");
        apoderado.setGmail("ana@mail.com");
        apoderado.setContraseña("Clave#123");

        InscripcionRequest request = new InscripcionRequest();
        request.setAlumno(alumno);
        request.setApoderados(List.of(apoderado));
        if (idMetodoPago != null) {
            InscripcionRequest.DatosMatricula datos = new InscripcionRequest.DatosMatricula();
            datos.setIdGradoSeccion(10);
            datos.setIdMetodoPago(idMetodoPago);
            datos.setCodigoTransaccion(codigo);
            request.setMatricula(datos);
        }
        return request;
    }

    private static MockMultipartFile foto() {
        return new MockMultipartFile("foto", "foto.jpg", "image/jpeg", new byte[]{1, 2, 3});
    }

    @Test
    void validarNoGuardaNada() {
        inscripcionService.validar(solicitud(null, null));

        verify(alumnoRepository, never()).save(any());
        verify(usuarioRepository, never()).save(any());
        verify(apoderadoRepository, never()).save(any());
        verify(alumnoApoderadoRepository, never()).save(any());
    }

    @Test
    void validarRechazaUnCorreoQueYaUsaOtroUsuario() {
        when(usuarioRepository.findByGmailAndAccesoNot(eq("ana@mail.com"), any())).thenReturn(Optional.of(new Usuario()));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> inscripcionService.validar(solicitud(null, null)));
        assertEquals("El correo ana@mail.com ya lo usa otro usuario", ex.getMessage());
    }

    @Test
    void sinElCodigoQuePideElMetodoNoSubeNiGuardaNada() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> inscripcionService.inscribir(solicitud(1, null), foto(), foto(), 1));

        assertEquals("El código de transacción es obligatorio para Yape", ex.getMessage());
        verify(cloudinaryService, never()).upload(any(), anyString());
        verify(matriculaRepository, never()).save(any());
    }

    @Test
    void unApoderadoNuevoRecibeSusAccesosYQuedaComoPrincipal() {
        inscripcionService.inscribir(solicitud(2, null), foto(), null, 1);

        verify(emailService).enviarCredencialesAcceso(eq("ana@mail.com"), anyString(), anyString(), eq("Clave#123"),
                eq(TipoEnvioCredenciales.CREACION));
        ArgumentCaptor<Matricula> matricula = ArgumentCaptor.forClass(Matricula.class);
        verify(matriculaRepository).save(matricula.capture());
        assertEquals(new BigDecimal("220"), matricula.getValue().getMonto());
        assertEquals(1, matricula.getValue().getAlumnoApoderado().getApoPrincipal());
        // Efectivo no pide comprobante: solo se sube la foto del alumno.
        verify(cloudinaryService).upload(any(), eq("alumnos"));
        verify(cloudinaryService, never()).upload(any(), eq("comprobantes"));
    }

    @Test
    void unApoderadoExistenteSoloSeVincula() {
        Usuario usuario = new Usuario();
        usuario.setIdUsuario(31);
        Apoderado existente = new Apoderado();
        existente.setIdApoderado(41);
        existente.setUsuario(usuario);
        when(apoderadoRepository.findByUsuarioDocumentoIdentidadAndAccesoNot(eq("40000001"), any()))
                .thenReturn(Optional.of(existente));

        inscripcionService.inscribir(solicitud(2, null), foto(), null, 1);

        verify(usuarioRepository, never()).save(any());
        verify(apoderadoRepository, never()).save(any());
        verify(emailService, never()).enviarCredencialesAcceso(any(), any(), any(), any(), any());
        ArgumentCaptor<AlumnoApoderado> vinculo = ArgumentCaptor.forClass(AlumnoApoderado.class);
        verify(alumnoApoderadoRepository).save(vinculo.capture());
        assertEquals(41, vinculo.getValue().getApoderado().getIdApoderado());
    }

    private Matricula matriculaEn(GradoSeccion gs) {
        Alumno alumno = new Alumno();
        alumno.setIdAlumno(9);
        AlumnoApoderado vinculo = new AlumnoApoderado();
        vinculo.setAlumno(alumno);
        Matricula matricula = new Matricula();
        matricula.setIdMatricula(5);
        matricula.setGradoSeccion(gs);
        matricula.setAlumnoApoderado(vinculo);
        when(matriculaService.findMatricula(5)).thenReturn(matricula);
        return matricula;
    }

    private PagoMatricula editarA(Integer idGradoSeccion, String pagadoNeto) {
        when(matriculaService.pagadoNeto(5)).thenReturn(new BigDecimal(pagadoNeto));
        MatriculaEdicionRequest request = new MatriculaEdicionRequest();
        request.setIdGradoSeccion(idGradoSeccion);
        request.setIdMetodoPago(1);
        request.setCodigoTransaccion("OP-77");
        inscripcionService.editar(5, request, foto(), 1);
        ArgumentCaptor<PagoMatricula> pago = ArgumentCaptor.forClass(PagoMatricula.class);
        verify(pagoMatriculaRepository).save(pago.capture());
        return pago.getValue();
    }

    @Test
    void subirDeNivelCobraLaDiferencia() {
        Matricula matricula = matriculaEn(primeroA);

        PagoMatricula pago = editarA(20, "220");

        assertEquals(TipoPago.DIFERENCIA, pago.getTipo());
        assertEquals(new BigDecimal("80"), pago.getMonto());
        assertEquals(new BigDecimal("300"), matricula.getMonto());
        assertEquals("x", pago.getPkComprobante());
    }

    @Test
    void bajarDeNivelRegistraLaDevolucion() {
        matriculaEn(secundariaA);

        PagoMatricula pago = editarA(10, "300");

        assertEquals(TipoPago.DEVOLUCION, pago.getTipo());
        assertEquals(new BigDecimal("80"), pago.getMonto());
    }

    @Test
    void enUnAnioVigenteLaSeccionNoSeEdita() {
        anio2027.setEstado(AnioEscolarService.ESTADO_VIGENTE);
        matriculaEn(primeroA);
        MatriculaEdicionRequest request = new MatriculaEdicionRequest();
        request.setIdGradoSeccion(20);

        assertThrows(IllegalArgumentException.class, () -> inscripcionService.editar(5, request, null, 1));
        verify(pagoMatriculaRepository, never()).save(any());
    }

    @Test
    void soloSeEliminaLaMatriculaDeUnAnioPorComenzar() {
        anio2027.setEstado(AnioEscolarService.ESTADO_VIGENTE);
        matriculaEn(primeroA);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> inscripcionService.eliminar(5));
        assertEquals("Solo se puede eliminar la matrícula de un año por comenzar", ex.getMessage());
    }

    @Test
    void alEliminarLaUnicaMatriculaSeDescartanElAlumnoYSuApoderado() {
        Matricula matricula = matriculaEn(primeroA);
        Alumno alumno = matricula.getAlumnoApoderado().getAlumno();
        alumno.setAcceso(new Acceso());
        alumno.getAcceso().setIdAcceso(AccesoConstants.ACTIVO);
        Usuario usuario = new Usuario();
        usuario.setIdUsuario(31);
        Apoderado apoderado = new Apoderado();
        apoderado.setIdApoderado(41);
        apoderado.setUsuario(usuario);
        apoderado.setAcceso(new Acceso());
        apoderado.getAcceso().setIdAcceso(AccesoConstants.ACTIVO);
        AlumnoApoderado vinculo = matricula.getAlumnoApoderado();
        vinculo.setApoderado(apoderado);
        when(matriculaRepository.findByAlumnoApoderadoAlumnoIdAlumnoAndAccesoNot(anyInt(), any())).thenReturn(List.of());
        when(alumnoApoderadoRepository.findByAlumno(alumno)).thenReturn(List.of(vinculo));
        when(alumnoApoderadoRepository.findByApoderado(apoderado)).thenReturn(List.of(vinculo));
        UsuarioRol soloApoderado = new UsuarioRol();
        soloApoderado.setRol(rolApoderado);
        when(usuarioRolRepository.findByUsuarioIdUsuario(31)).thenReturn(List.of(soloApoderado));

        inscripcionService.eliminar(5);

        assertEquals(AccesoConstants.ELIMINADO, matricula.getAcceso().getIdAcceso());
        assertEquals(AccesoConstants.ELIMINADO, alumno.getAcceso().getIdAcceso());
        assertEquals(AccesoConstants.ELIMINADO, apoderado.getAcceso().getIdAcceso());
        assertEquals(AccesoConstants.ELIMINADO, usuario.getAcceso().getIdAcceso());
        verify(usuarioRolRepository).delete(soloApoderado);
    }

    private Alumno alumnoCon(int apoderados) {
        Alumno alumno = new Alumno();
        alumno.setIdAlumno(9);
        alumno.setDocumentoIdentidad("70000001");
        List<AlumnoApoderado> vinculos = new java.util.ArrayList<>();
        for (int i = 0; i < apoderados; i++) {
            Usuario usuario = new Usuario();
            usuario.setDocumentoIdentidad("4100000" + i);
            Apoderado apoderado = new Apoderado();
            apoderado.setIdApoderado(70 + i);
            apoderado.setUsuario(usuario);
            apoderado.setAcceso(new Acceso());
            apoderado.getAcceso().setIdAcceso(AccesoConstants.ACTIVO);
            AlumnoApoderado vinculo = new AlumnoApoderado();
            vinculo.setIdAlumnoApoderado(80 + i);
            vinculo.setAlumno(alumno);
            vinculo.setApoderado(apoderado);
            vinculo.setApoPrincipal(i == 0 ? (byte) 1 : (byte) 0);
            vinculos.add(vinculo);
        }
        when(alumnoRepository.findByIdAlumnoAndAccesoNot(eq(9), any())).thenReturn(Optional.of(alumno));
        when(alumnoApoderadoRepository.findByAlumno(alumno)).thenReturn(vinculos);
        return alumno;
    }

    @Test
    void unAlumnoNoTieneMasDeDosApoderados() {
        alumnoCon(2);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> inscripcionService.agregarApoderado(9, solicitud(null, null).getApoderados().get(0)));
        assertEquals("El alumno ya tiene dos apoderados", ex.getMessage());
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void elApoderadoAgregadoQuedaComoSecundarioYRecibeSusAccesos() {
        alumnoCon(1);

        inscripcionService.agregarApoderado(9, solicitud(null, null).getApoderados().get(0));

        ArgumentCaptor<AlumnoApoderado> vinculo = ArgumentCaptor.forClass(AlumnoApoderado.class);
        verify(alumnoApoderadoRepository).save(vinculo.capture());
        assertEquals(0, vinculo.getValue().getApoPrincipal());
        verify(emailService).enviarCredencialesAcceso(eq("ana@mail.com"), anyString(), anyString(), eq("Clave#123"),
                eq(TipoEnvioCredenciales.CREACION));
    }
}
