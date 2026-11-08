package com.example.Escolar.Service;

import com.example.Escolar.Dto.ConfiguracionAcademicaResponse;
import com.example.Escolar.Exception.ResourceNotFoundException;
import com.example.Escolar.Model.*;
import com.example.Escolar.Repository.*;
import org.apache.poi.ss.usermodel.DataValidation;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.io.ByteArrayInputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SiagieExportServiceTest {
    @Mock NotaRepository notaRepository;
    @Mock CursoRepository cursoRepository;
    @Mock CompetenciaRepository competenciaRepository;
    @Mock MatriculaRepository matriculaRepository;
    @Mock GradoSeccionRepository gradoSeccionRepository;
    @Mock ColegioRepository colegioRepository;
    @Mock AccesoRepository accesoRepository;
    @Mock ConfiguracionAcademicaService configuracionService;
    @Mock NotaFinalService notaFinalService;
    SiagieExportService service;

    private final Acceso activo = new Acceso(AccesoConstants.ACTIVO, "Activo");
    private final Acceso eliminado = new Acceso(AccesoConstants.ELIMINADO, "Eliminado");
    private final List<String> roles = List.of("ADMIN");
    private ConfiguracionAcademicaResponse config;
    private Competencia c1;
    private Competencia c2;
    private Competencia tic;
    private Matricula ana;
    private Matricula luis;

    @BeforeEach
    void preparar() {
        // La malla es real: así se prueba también el orden de las áreas.
        service = new SiagieExportService(notaRepository, matriculaRepository, gradoSeccionRepository, colegioRepository,
                accesoRepository, configuracionService, notaFinalService,
                new MallaEvaluable(cursoRepository, competenciaRepository, accesoRepository));
        when(accesoRepository.findById(AccesoConstants.ELIMINADO)).thenReturn(Optional.of(eliminado));
        AnioEscolar anio = new AnioEscolar();
        anio.setIdAnio(2);
        anio.setAnio("2027");
        Nivel nivel = new Nivel();
        nivel.setIdNivel(3);
        nivel.setNombre("Secundaria");
        Grado grado = new Grado();
        grado.setNombre("1ro");
        grado.setNivel(nivel);
        Seccion seccion = new Seccion();
        seccion.setNombre("A");
        GradoSeccion gs = new GradoSeccion();
        gs.setIdGradoSeccion(26);
        gs.setGrado(grado);
        gs.setSeccion(seccion);
        gs.setAnioEscolar(anio);
        gs.setAcceso(activo);
        when(gradoSeccionRepository.findById(26)).thenReturn(Optional.of(gs));
        when(notaFinalService.puedeVerSeccion(any(), any(), any())).thenReturn(true);

        Colegio colegio = new Colegio();
        colegio.setNombre("BAUTISTA LOGOS");
        colegio.setCodigoColegio("1340397-0");
        when(colegioRepository.findFirstByAccesoNot(eliminado)).thenReturn(Optional.of(colegio));

        PeriodoEvaluacion periodo = new PeriodoEvaluacion();
        periodo.setNumero((byte) 1);
        when(configuracionService.periodos(any(), any())).thenReturn(List.of(periodo));
        config = new ConfiguracionAcademicaResponse();
        config.setEscala(EscalaCalificacion.LITERAL);
        config.setTipoPeriodo(TipoPeriodo.BIMESTRE);
        when(configuracionService.obtener(2, 3)).thenReturn(config);

        // Llegan desordenadas: las transversales van al final, el resto por código.
        Curso transversal = curso(2, "0006: DESEN TIC", "SE DESENVUELVE EN ENTORNOS VIRTUALES", TipoCurso.TRANSVERSAL);
        Curso mate = curso(1, "063: MATE", "MATEMÁTICA", TipoCurso.AREA);
        Curso arte = curso(3, "0001: ART Y CULT", "ARTE Y CULTURA", TipoCurso.AREA);
        when(cursoRepository.findByAnioEscolarIdAnioAndNivelIdNivelAndAccesoNot(2, 3, eliminado))
                .thenReturn(List.of(transversal, mate, arte));
        c1 = competencia(11, "01", "Resuelve problemas de cantidad");
        c2 = competencia(12, "02", "Resuelve problemas de regularidad");
        tic = competencia(21, "01", "Se desenvuelve en entornos virtuales");
        when(competenciaRepository.findByCursoIdCursoAndAccesoNot(1, eliminado)).thenReturn(List.of(c2, c1));
        when(competenciaRepository.findByCursoIdCursoAndAccesoNot(2, eliminado)).thenReturn(List.of(tic));
        when(competenciaRepository.findByCursoIdCursoAndAccesoNot(3, eliminado)).thenReturn(List.of());

        ana = matricula(1, "ANA", "ALVA", gs);
        luis = matricula(2, "LUIS", "ZAPATA", gs);
        when(matriculaRepository.findByGradoSeccionIdGradoSeccionAndAccesoNot(26, eliminado)).thenReturn(List.of(luis, ana));
        when(notaRepository.findByMatriculaGradoSeccionIdGradoSeccionAndBimestreAndAccesoNot(26, (byte) 1, eliminado))
                .thenReturn(List.of(nota(ana, c1, "B", "Reconoce cantidades pero le falta practicar."), nota(luis, c1, "AD", null)));
    }

    private Curso curso(int id, String codigo, String nombre, TipoCurso tipo) {
        Curso c = new Curso();
        c.setIdCurso(id);
        c.setCodigo(codigo);
        c.setNombre(nombre);
        c.setTipo(tipo);
        c.setAcceso(activo);
        return c;
    }

    private Competencia competencia(int id, String codigo, String nombre) {
        Competencia c = new Competencia();
        c.setIdCompetencia(id);
        c.setCodigo(codigo);
        c.setNombre(nombre);
        c.setOrden(Byte.parseByte(codigo));
        c.setAcceso(activo);
        return c;
    }

    private Matricula matricula(int id, String nombre, String apellido, GradoSeccion gs) {
        Alumno a = new Alumno();
        a.setNombre(nombre);
        a.setApellidoPat(apellido);
        a.setApellidoMat("RUIZ");
        a.setCodigo("000000000" + id);
        AnioEscolar anio = gs.getAnioEscolar();
        AlumnoApoderado vinculo = new AlumnoApoderado();
        vinculo.setAlumno(a);
        Matricula m = new Matricula();
        m.setIdMatricula(id);
        m.setAlumnoApoderado(vinculo);
        m.setGradoSeccion(gs);
        m.setAcceso(activo);
        assertNotNull(anio);
        return m;
    }

    private Nota nota(Matricula m, Competencia c, String calificacion, String conclusion) {
        Nota n = new Nota();
        n.setMatricula(m);
        n.setCompetencia(c);
        n.setCalificacion(calificacion);
        n.setConclusionDescriptiva(conclusion);
        return n;
    }

    private Workbook libro() throws Exception {
        return WorkbookFactory.create(new ByteArrayInputStream(service.exportar(26, 1, 7, roles).contenido()));
    }

    private static String texto(Sheet hoja, int fila, int col) {
        var row = hoja.getRow(fila);
        return row == null || row.getCell(col) == null ? "" : row.getCell(col).toString();
    }

    @Test
    void tieneLasHojasDelOriginalEnSuOrden() throws Exception {
        try (Workbook libro = libro()) {
            assertEquals(List.of("Generalidades", "063-MATE", "0006-DESEN TIC"),
                    IntStream.range(0, libro.getNumberOfSheets()).mapToObj(libro::getSheetName).toList());
        }
    }

    @Test
    void generalidadesLlevaColegioPeriodoGradoYSeccion() throws Exception {
        try (Workbook libro = libro()) {
            Sheet g = libro.getSheet("Generalidades");
            assertEquals("1340397-0", texto(g, 3, 2));
            assertEquals("SECUNDARIA", texto(g, 3, 6));
            assertEquals("PRIMER BIMESTRE", texto(g, 9, 2));
            assertEquals("1RO", texto(g, 9, 5));
            assertEquals("A", texto(g, 9, 8));
            assertEquals("063: MATE", texto(g, 12, 1));
            // Barra verde del original sobre "Institución educativa".
            var barra = (org.apache.poi.xssf.usermodel.XSSFCellStyle) g.getRow(2).getCell(1).getCellStyle();
            assertEquals("FF026749", barra.getFillForegroundXSSFColor().getARGBHex());
        }
    }

    @Test
    void laHojaDelAreaSinColumnaIdYPorApellidos() throws Exception {
        try (Workbook libro = libro()) {
            Sheet mate = libro.getSheet("063-MATE");
            assertEquals("Cód. Estudiante", texto(mate, 0, 0));
            assertEquals(List.of("01", "01", "02", "02"), List.of(texto(mate, 0, 2), texto(mate, 0, 3), texto(mate, 0, 4), texto(mate, 0, 5)));
            assertEquals("NL", texto(mate, 1, 2));
            assertEquals("ALVA RUIZ, ANA", texto(mate, 2, 1));
            assertEquals("B", texto(mate, 2, 2));
            assertEquals("Reconoce cantidades pero le falta practicar.", texto(mate, 2, 3));
            assertEquals("AD", texto(mate, 3, 2));
            assertEquals("", texto(mate, 3, 4));
        }
    }

    @Test
    void validaLaNotaSegunLaEscala() throws Exception {
        try (Workbook libro = libro()) {
            List<? extends DataValidation> validaciones = libro.getSheet("063-MATE").getDataValidations();
            assertTrue(validaciones.stream().anyMatch(v -> v.getValidationConstraint().getExplicitListValues() != null
                    && List.of(v.getValidationConstraint().getExplicitListValues()).containsAll(List.of("AD", "A", "B", "C"))));
            assertEquals(2, validaciones.size());
        }
    }

    @Test
    void enVigesimalLaNotaEsNumero() throws Exception {
        config.setEscala(EscalaCalificacion.VIGESIMAL);
        when(notaRepository.findByMatriculaGradoSeccionIdGradoSeccionAndBimestreAndAccesoNot(26, (byte) 1, eliminado))
                .thenReturn(List.of(nota(ana, c1, "15", null)));

        try (Workbook libro = libro()) {
            Sheet mate = libro.getSheet("063-MATE");
            assertEquals(15.0, mate.getRow(2).getCell(2).getNumericCellValue());
            assertEquals("Nota", texto(mate, 1, 2));
        }
    }

    @Test
    void cuentaLasNotasQueFaltan() {
        SiagieExportService.Pendientes p = service.pendientes(26, 1, 7, roles);

        // 2 alumnos × (2 de Matemática + 1 transversal) = 6; hay 2.
        assertEquals(6, p.esperadas());
        assertEquals(4, p.faltantes());
        assertEquals(2, p.areas().size());
    }

    @Test
    void unaSeccionAjenaNoSeExporta() {
        when(notaFinalService.puedeVerSeccion(any(), any(), any())).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> service.exportar(26, 1, 7, roles));
    }

    @Test
    void elNombreDeLaHojaEsValidoYUnico() {
        Set<String> usados = new HashSet<>();
        Curso c = curso(9, null, "CIENCIA: TECNOLOGÍA / AMBIENTE Y MUCHO MÁS TEXTO LARGO", TipoCurso.AREA);

        String primero = SiagieExportService.nombreHoja(c, usados);
        String segundo = SiagieExportService.nombreHoja(c, usados);

        assertTrue(primero.length() <= 31 && !primero.contains(":") && !primero.contains("/"), primero);
        assertNotEquals(primero, segundo);
    }
}
