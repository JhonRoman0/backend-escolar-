package com.example.Escolar.Service;

import com.example.Escolar.Dto.CursoCopiaRequest;
import com.example.Escolar.Dto.CursoImportResponse;
import com.example.Escolar.Dto.CursoVistaPreviaResponse;
import com.example.Escolar.Dto.SubcursoRequest;
import com.example.Escolar.Model.Acceso;
import com.example.Escolar.Model.AnioEscolar;
import com.example.Escolar.Model.Competencia;
import com.example.Escolar.Model.Curso;
import com.example.Escolar.Model.Nivel;
import com.example.Escolar.Model.TipoCurso;
import com.example.Escolar.Repository.AccesoRepository;
import com.example.Escolar.Repository.AnioEscolarRepository;
import com.example.Escolar.Repository.AsignacionRepository;
import com.example.Escolar.Repository.CompetenciaRepository;
import com.example.Escolar.Repository.CursoRepository;
import com.example.Escolar.Repository.NivelRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static com.example.Escolar.Service.PlantillaCursosParserTest.excel;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CursoServiceTest {

    private static final int ID_SECUNDARIA = 3;

    @Mock
    private CursoRepository cursoRepository;
    @Mock
    private CompetenciaRepository competenciaRepository;
    @Mock
    private AccesoRepository accesoRepository;
    @Mock
    private AnioEscolarRepository anioEscolarRepository;
    @Mock
    private NivelRepository nivelRepository;
    @Mock
    private AsignacionRepository asignacionRepository;
    @Spy
    private PlantillaCursosParser parser = new PlantillaCursosParser();
    @InjectMocks
    private CursoService cursoService;

    private Acceso activo;
    private Acceso inactivo;
    private Acceso eliminado;
    private Nivel secundaria;
    private final AtomicInteger ids = new AtomicInteger(100);

    @BeforeEach
    void setUp() {
        activo = acceso(AccesoConstants.ACTIVO);
        inactivo = acceso(AccesoConstants.INACTIVO);
        eliminado = acceso(AccesoConstants.ELIMINADO);
        when(accesoRepository.findById(AccesoConstants.ACTIVO)).thenReturn(Optional.of(activo));
        when(accesoRepository.findById(AccesoConstants.INACTIVO)).thenReturn(Optional.of(inactivo));
        when(accesoRepository.findById(AccesoConstants.ELIMINADO)).thenReturn(Optional.of(eliminado));

        secundaria = nivel(ID_SECUNDARIA, "Secundaria");
        when(nivelRepository.findByAccesoNot(eliminado))
                .thenReturn(List.of(nivel(1, "Inicial"), nivel(2, "Primaria"), secundaria));
        when(nivelRepository.findByIdNivelAndAccesoNot(eq(ID_SECUNDARIA), any())).thenReturn(Optional.of(secundaria));

        when(cursoRepository.findByAnioEscolarIdAnioAndNivelIdNivelAndAccesoNot(anyInt(), anyInt(), any()))
                .thenReturn(new ArrayList<>());
        when(cursoRepository.save(any(Curso.class))).thenAnswer(inv -> conId(inv.getArgument(0)));
        when(competenciaRepository.save(any(Competencia.class))).thenAnswer(inv -> {
            Competencia c = inv.getArgument(0);
            if (c.getIdCompetencia() == null) {
                c.setIdCompetencia(ids.incrementAndGet());
            }
            return c;
        });
    }

    private Curso conId(Curso curso) {
        if (curso.getIdCurso() == null) {
            curso.setIdCurso(ids.incrementAndGet());
        }
        return curso;
    }

    private Acceso acceso(Long id) {
        Acceso a = new Acceso();
        a.setIdAcceso(id);
        return a;
    }

    private Nivel nivel(int id, String nombre) {
        Nivel n = new Nivel();
        n.setIdNivel(id);
        n.setNombre(nombre);
        n.setAcceso(activo);
        return n;
    }

    private AnioEscolar anio(int id, String valor, byte estado) {
        AnioEscolar a = new AnioEscolar();
        a.setIdAnio(id);
        a.setAnio(valor);
        a.setEstado(estado);
        a.setAcceso(activo);
        when(anioEscolarRepository.findByIdAnioAndAccesoNot(eq(id), any())).thenReturn(Optional.of(a));
        return a;
    }

    private Curso curso(int id, String codigo, String nombre, TipoCurso tipo, AnioEscolar anio) {
        Curso c = new Curso();
        c.setIdCurso(id);
        c.setCodigo(codigo);
        c.setNombre(nombre);
        c.setTipo(tipo);
        c.setAnioEscolar(anio);
        c.setNivel(secundaria);
        c.setAcceso(activo);
        when(cursoRepository.findByIdCursoAndAccesoNot(eq(id), any())).thenReturn(Optional.of(c));
        return c;
    }

    private Competencia competencia(int id, Curso curso, String codigo) {
        Competencia c = new Competencia();
        c.setIdCompetencia(id);
        c.setCurso(curso);
        c.setCodigo(codigo);
        c.setNombre("Competencia " + codigo);
        c.setOrden(Byte.parseByte(codigo));
        c.setAcceso(activo);
        return c;
    }

    private List<Curso> cursosGuardados() {
        ArgumentCaptor<Curso> captor = ArgumentCaptor.forClass(Curso.class);
        verify(cursoRepository, atLeastOnce()).save(captor.capture());
        return captor.getAllValues();
    }

    private List<Competencia> competenciasGuardadas() {
        ArgumentCaptor<Competencia> captor = ArgumentCaptor.forClass(Competencia.class);
        verify(competenciaRepository, atLeastOnce()).save(captor.capture());
        return captor.getAllValues();
    }

    // ── Importacion ────────────────────────────────────────────────────────

    @Test
    void importarCreaAreasTransversalesYCompetenciasEnElAnioYNivelDelArchivo() throws Exception {
        AnioEscolar anio2027 = anio(2, "2027", AnioEscolarService.ESTADO_POR_COMENZAR);

        CursoImportResponse res = cursoService.importar(excel("regnotas-secundaria.xlsx"), 2);

        assertEquals(13, res.getCreados());
        assertEquals(0, res.getActualizados());
        List<Curso> guardados = cursosGuardados();
        assertTrue(guardados.stream().allMatch(c -> c.getAnioEscolar() == anio2027 && c.getNivel() == secundaria));
        assertEquals(2, guardados.stream().filter(c -> c.getTipo() == TipoCurso.TRANSVERSAL).count());
        List<Competencia> deMatematica = competenciasGuardadas().stream()
                .filter(c -> c.getCurso().getCodigo().equals("063: MATE")).toList();
        assertEquals(List.of("01", "02", "03", "04"), deMatematica.stream().map(Competencia::getCodigo).toList());
    }

    @Test
    void reimportarActualizaPorCodigoEInactivaLoQueYaNoViene() throws Exception {
        AnioEscolar anio2027 = anio(2, "2027", AnioEscolarService.ESTADO_POR_COMENZAR);
        Curso matematica = curso(10, "063: MATE", "Matemática vieja", TipoCurso.AREA, anio2027);
        Curso vieja = curso(11, "099: VIEJ", "Área que ya no existe", TipoCurso.AREA, anio2027);
        when(cursoRepository.findByAnioEscolarIdAnioAndNivelIdNivelAndAccesoNot(eq(2), eq(ID_SECUNDARIA), any()))
                .thenReturn(List.of(matematica, vieja));

        CursoImportResponse res = cursoService.importar(excel("regnotas-secundaria.xlsx"), 2);

        assertEquals(12, res.getCreados());
        assertEquals(1, res.getActualizados());
        assertEquals(1, res.getInactivados());
        assertEquals("MATEMÁTICA", matematica.getNombre());
        assertEquals(AccesoConstants.ACTIVO, matematica.getAcceso().getIdAcceso());
        assertEquals(AccesoConstants.INACTIVO, vieja.getAcceso().getIdAcceso());
    }

    @Test
    void importarEnUnAnioVigenteConCursosFalla() throws Exception {
        anio(1, "2026", AnioEscolarService.ESTADO_VIGENTE);
        when(cursoRepository.existsByAnioEscolarIdAnioAndNivelIdNivelAndAccesoNot(eq(1), eq(ID_SECUNDARIA), any()))
                .thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> cursoService.importar(excel("regnotas-secundaria.xlsx"), 1));

        assertTrue(ex.getMessage().contains("vigente"));
        verify(cursoRepository, never()).save(any());
    }

    @Test
    void unAnioVigenteSinCursosEnEseNivelAdmiteLaImportacionInicial() throws Exception {
        anio(1, "2026", AnioEscolarService.ESTADO_VIGENTE);
        when(cursoRepository.existsByAnioEscolarIdAnioAndNivelIdNivelAndAccesoNot(eq(1), eq(ID_SECUNDARIA), any()))
                .thenReturn(false);

        assertEquals(13, cursoService.importar(excel("regnotas-secundaria.xlsx"), 1).getCreados());
    }

    @Test
    void importarEnUnAnioCerradoFalla() throws Exception {
        anio(1, "2025", AnioEscolarService.ESTADO_CERRADO);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> cursoService.importar(excel("regnotas-secundaria.xlsx"), 1));
        assertTrue(ex.getMessage().contains("cerrado"));
    }

    @Test
    void rechazaUnNivelQueNoExisteEnElSistema() throws Exception {
        anio(2, "2027", AnioEscolarService.ESTADO_POR_COMENZAR);
        when(nivelRepository.findByAccesoNot(eliminado)).thenReturn(List.of(nivel(2, "Primaria")));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> cursoService.importar(excel("regnotas-inicial.xlsx"), 2));
        assertTrue(ex.getMessage().contains("INICIAL - JARDÍN"));
    }

    @Test
    void laVistaPreviaNoGuardaYAvisaLosCambios() throws Exception {
        AnioEscolar anio2027 = anio(2, "2027", AnioEscolarService.ESTADO_POR_COMENZAR);
        Curso matematica = curso(10, "063: MATE", "MATEMÁTICA", TipoCurso.AREA, anio2027);
        Curso vieja = curso(11, "099: VIEJ", "ÁREA VIEJA", TipoCurso.AREA, anio2027);
        when(cursoRepository.findByAnioEscolarIdAnioAndNivelIdNivelAndAccesoNot(eq(2), eq(ID_SECUNDARIA), any()))
                .thenReturn(List.of(matematica, vieja));

        CursoVistaPreviaResponse res = cursoService.vistaPrevia(excel("regnotas-secundaria.xlsx"), 2);

        verify(cursoRepository, never()).save(any());
        verify(competenciaRepository, never()).save(any());
        assertEquals("Secundaria", res.getNivel());
        assertEquals("ACTUALIZA", res.getAreas().stream()
                .filter(a -> a.getCodigo().equals("063: MATE")).findFirst().orElseThrow().getAccion());
        assertEquals("NUEVA", res.getAreas().stream()
                .filter(a -> a.getCodigo().equals("017: COMU")).findFirst().orElseThrow().getAccion());
        assertEquals(List.of("ÁREA VIEJA"), res.getSeInactivan());
        assertTrue(res.getAdvertencias().stream().anyMatch(a -> a.contains("2026") && a.contains("2027")));
    }

    // ── Copia ──────────────────────────────────────────────────────────────

    @Test
    void copiarLlevaAreasCompetenciasYSubcursosConSusCompetencias() {
        AnioEscolar origen = anio(1, "2026", AnioEscolarService.ESTADO_VIGENTE);
        AnioEscolar destino = anio(2, "2027", AnioEscolarService.ESTADO_POR_COMENZAR);
        Curso matematica = curso(10, "063: MATE", "MATEMÁTICA", TipoCurso.AREA, origen);
        matematica.setTieneSubcursos(true);
        Competencia cantidad = competencia(50, matematica, "01");
        Competencia algebraica = competencia(51, matematica, "02");
        Curso algebra = curso(20, null, "Álgebra", TipoCurso.SUBCURSO, origen);
        algebra.setCursoPadre(matematica);
        algebra.setCompetencias(Set.of(algebraica));
        when(cursoRepository.findByAnioEscolarIdAnioAndNivelIdNivelAndAccesoNot(eq(1), eq(ID_SECUNDARIA), any()))
                .thenReturn(List.of(matematica, algebra));
        when(competenciaRepository.findByCursoIdCursoAndAccesoNot(eq(10), any()))
                .thenReturn(List.of(cantidad, algebraica));

        CursoCopiaRequest request = new CursoCopiaRequest();
        request.setIdAnioOrigen(1);
        request.setIdAnioDestino(2);
        request.setIdNivel(ID_SECUNDARIA);
        cursoService.copiar(request);

        List<Curso> copias = cursosGuardados();
        Curso areaCopiada = copias.stream().filter(c -> c.getTipo() == TipoCurso.AREA).findFirst().orElseThrow();
        Curso subcursoCopiado = copias.stream().filter(c -> c.getTipo() == TipoCurso.SUBCURSO).findFirst().orElseThrow();
        assertEquals(destino, areaCopiada.getAnioEscolar());
        assertTrue(areaCopiada.isTieneSubcursos());
        assertEquals(areaCopiada, subcursoCopiado.getCursoPadre());
        Competencia competenciaDelSubcurso = subcursoCopiado.getCompetencias().iterator().next();
        assertEquals("02", competenciaDelSubcurso.getCodigo());
        assertEquals(areaCopiada, competenciaDelSubcurso.getCurso());
    }

    @Test
    void copiarAUnAnioQueYaTieneCursosFalla() {
        anio(1, "2026", AnioEscolarService.ESTADO_VIGENTE);
        anio(2, "2027", AnioEscolarService.ESTADO_POR_COMENZAR);
        when(cursoRepository.existsByAnioEscolarIdAnioAndNivelIdNivelAndAccesoNot(eq(2), eq(ID_SECUNDARIA), any()))
                .thenReturn(true);
        CursoCopiaRequest request = new CursoCopiaRequest();
        request.setIdAnioOrigen(1);
        request.setIdAnioDestino(2);
        request.setIdNivel(ID_SECUNDARIA);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> cursoService.copiar(request));
        assertTrue(ex.getMessage().contains("ya tiene cursos"));
    }

    @Test
    void copiarAUnAnioVigenteFalla() {
        anio(1, "2025", AnioEscolarService.ESTADO_CERRADO);
        anio(2, "2026", AnioEscolarService.ESTADO_VIGENTE);
        CursoCopiaRequest request = new CursoCopiaRequest();
        request.setIdAnioOrigen(1);
        request.setIdAnioDestino(2);
        request.setIdNivel(ID_SECUNDARIA);

        assertThrows(IllegalArgumentException.class, () -> cursoService.copiar(request));
        verify(cursoRepository, never()).save(any());
    }

    // ── Subcursos ──────────────────────────────────────────────────────────

    @Test
    void unaTransversalNoPuedeTenerSubcursos() {
        AnioEscolar anio2027 = anio(2, "2027", AnioEscolarService.ESTADO_POR_COMENZAR);
        curso(10, "0006: DESEN TIC", "TIC", TipoCurso.TRANSVERSAL, anio2027);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> cursoService.cambiarSubcursos(10, true));
        assertTrue(ex.getMessage().contains("Solo las áreas"));
    }

    @Test
    void crearSubcursoGuardaSusCompetenciasDelArea() {
        AnioEscolar anio2027 = anio(2, "2027", AnioEscolarService.ESTADO_POR_COMENZAR);
        Curso matematica = curso(10, "063: MATE", "MATEMÁTICA", TipoCurso.AREA, anio2027);
        matematica.setTieneSubcursos(true);
        Competencia cantidad = competencia(50, matematica, "01");
        when(competenciaRepository.findByCursoIdCursoAndAccesoNot(eq(10), any())).thenReturn(List.of(cantidad));
        when(cursoRepository.findByCursoPadreIdCursoAndAccesoNot(eq(10), any())).thenReturn(List.of());
        when(cursoRepository.save(any(Curso.class))).thenAnswer(inv -> {
            Curso c = conId(inv.getArgument(0));
            when(cursoRepository.findByIdCursoAndAccesoNot(eq(c.getIdCurso()), any())).thenReturn(Optional.of(c));
            return c;
        });

        cursoService.crearSubcurso(10, subcurso(" Aritmética ", 50));

        Curso guardado = cursosGuardados().get(0);
        assertEquals("Aritmética", guardado.getNombre());
        assertEquals(TipoCurso.SUBCURSO, guardado.getTipo());
        assertEquals(matematica, guardado.getCursoPadre());
        assertEquals(anio2027, guardado.getAnioEscolar());
        assertEquals(Set.of(cantidad), guardado.getCompetencias());
    }

    @Test
    void unSubcursoSoloEligeCompetenciasDeSuArea() {
        AnioEscolar anio2027 = anio(2, "2027", AnioEscolarService.ESTADO_POR_COMENZAR);
        Curso matematica = curso(10, "063: MATE", "MATEMÁTICA", TipoCurso.AREA, anio2027);
        matematica.setTieneSubcursos(true);
        when(competenciaRepository.findByCursoIdCursoAndAccesoNot(eq(10), any()))
                .thenReturn(List.of(competencia(50, matematica, "01")));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> cursoService.crearSubcurso(10, subcurso("Álgebra", 99)));
        assertTrue(ex.getMessage().contains("competencias del área"));
        verify(cursoRepository, never()).save(any());
    }

    @Test
    void noSeRepiteElNombreDeUnSubcursoEnLaMismaArea() {
        AnioEscolar anio2027 = anio(2, "2027", AnioEscolarService.ESTADO_POR_COMENZAR);
        Curso matematica = curso(10, "063: MATE", "MATEMÁTICA", TipoCurso.AREA, anio2027);
        matematica.setTieneSubcursos(true);
        Curso algebra = curso(20, null, "Álgebra", TipoCurso.SUBCURSO, anio2027);
        when(cursoRepository.findByCursoPadreIdCursoAndAccesoNot(eq(10), any())).thenReturn(List.of(algebra));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> cursoService.crearSubcurso(10, subcurso("ÁLGEBRA", 50)));
        assertTrue(ex.getMessage().contains("ya tiene un subcurso"));
    }

    @Test
    void apagarLosSubcursosLosElimina() {
        AnioEscolar anio2027 = anio(2, "2027", AnioEscolarService.ESTADO_POR_COMENZAR);
        Curso matematica = curso(10, "063: MATE", "MATEMÁTICA", TipoCurso.AREA, anio2027);
        matematica.setTieneSubcursos(true);
        Curso algebra = curso(20, null, "Álgebra", TipoCurso.SUBCURSO, anio2027);
        when(cursoRepository.findByCursoPadreIdCursoAndAccesoNot(eq(10), any())).thenReturn(List.of(algebra));

        cursoService.cambiarSubcursos(10, false);

        assertEquals(AccesoConstants.ELIMINADO, algebra.getAcceso().getIdAcceso());
        assertTrue(!matematica.isTieneSubcursos());
    }

    private SubcursoRequest subcurso(String nombre, Integer... idCompetencias) {
        SubcursoRequest request = new SubcursoRequest();
        request.setNombre(nombre);
        request.setIdCompetencias(List.of(idCompetencias));
        return request;
    }

    // ── Estado y eliminacion ───────────────────────────────────────────────

    @ParameterizedTest
    @CsvSource({"1, AREA", "1, TRANSVERSAL", "1, SUBCURSO", "2, AREA", "2, TRANSVERSAL", "2, SUBCURSO"})
    void vigenteYCerradoNoPermitenEditarEliminarNiCambiarEstado(int estado, TipoCurso tipo) {
        AnioEscolar anio = anio(1, "2026", (byte) estado);
        Curso curso = curso(10, "063: MATE", "MATEMÁTICA", tipo, anio);

        assertThrows(IllegalArgumentException.class,
                () -> cursoService.cambiarEstado(10, AccesoConstants.INACTIVO));
        assertThrows(IllegalArgumentException.class, () -> cursoService.delete(10));
        assertThrows(IllegalArgumentException.class,
                () -> cursoService.actualizarSubcurso(10, subcurso("Nuevo nombre", 50)));

        assertEquals(AccesoConstants.ACTIVO, curso.getAcceso().getIdAcceso());
        assertEquals("MATEMÁTICA", curso.getNombre());
        verify(cursoRepository, never()).save(any());
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2})
    void vigenteYCerradoNoPermitenConfigurarNiCrearSubcursos(int estado) {
        AnioEscolar anio = anio(1, "2026", (byte) estado);
        Curso area = curso(10, "063: MATE", "MATEMÁTICA", TipoCurso.AREA, anio);
        assertThrows(IllegalArgumentException.class, () -> cursoService.cambiarSubcursos(10, true));
        area.setTieneSubcursos(true);
        assertThrows(IllegalArgumentException.class, () -> cursoService.cambiarSubcursos(10, false));
        assertThrows(IllegalArgumentException.class,
                () -> cursoService.crearSubcurso(10, subcurso("Álgebra", 50)));
        assertTrue(area.isTieneSubcursos());
        verify(cursoRepository, never()).save(any());
    }

    @Test
    void cambiarEstadoDelAreaNoCambiaElEstadoDeSusHijosNiSusDatosSiagie() {
        AnioEscolar anio = anio(2, "2027", AnioEscolarService.ESTADO_POR_COMENZAR);
        Curso area = curso(10, "063: MATE", "MATEMÁTICA", TipoCurso.AREA, anio);
        area.setTieneSubcursos(true);
        Curso hijo = curso(20, null, "Álgebra", TipoCurso.SUBCURSO, anio);
        hijo.setCursoPadre(area);

        cursoService.cambiarEstado(10, AccesoConstants.INACTIVO);

        assertEquals(AccesoConstants.INACTIVO, area.getAcceso().getIdAcceso());
        assertEquals(AccesoConstants.ACTIVO, hijo.getAcceso().getIdAcceso());
        assertEquals("063: MATE", area.getCodigo());
        assertEquals("MATEMÁTICA", area.getNombre());
        assertTrue(area.isTieneSubcursos());
        verify(cursoRepository, never()).save(hijo);
        cursoService.cambiarEstado(10, AccesoConstants.ACTIVO);
        assertEquals(AccesoConstants.ACTIVO, area.getAcceso().getIdAcceso());
    }

    @ParameterizedTest
    @CsvSource({"AREA", "TRANSVERSAL"})
    void noSeEditanManualmenteLosDatosSiagieAunqueElAnioSeaPorComenzar(TipoCurso tipo) {
        AnioEscolar anio = anio(2, "2027", AnioEscolarService.ESTADO_POR_COMENZAR);
        Curso principal = curso(10, "063: MATE", "MATEMÁTICA", tipo, anio);
        assertThrows(IllegalArgumentException.class,
                () -> cursoService.actualizarSubcurso(10, subcurso("Otro nombre", 50)));
        assertEquals("MATEMÁTICA", principal.getNombre());
        verify(cursoRepository, never()).save(any());
    }

    @Test
    void noSeHabilitanSubcursosSiElAreaTieneAsignaciones() {
        AnioEscolar anio = anio(2, "2027", AnioEscolarService.ESTADO_POR_COMENZAR);
        Curso area = curso(10, "063: MATE", "MATEMÁTICA", TipoCurso.AREA, anio);
        when(asignacionRepository.existsByCursoIdCursoAndAccesoNot(eq(10), any())).thenReturn(true);
        assertThrows(IllegalArgumentException.class, () -> cursoService.cambiarSubcursos(10, true));
        assertTrue(!area.isTieneSubcursos());
        verify(cursoRepository, never()).save(any());
    }

    @Test
    void noSeDeshabilitanSubcursosSiUnHijoTieneAsignaciones() {
        AnioEscolar anio = anio(2, "2027", AnioEscolarService.ESTADO_POR_COMENZAR);
        Curso area = curso(10, "063: MATE", "MATEMÁTICA", TipoCurso.AREA, anio);
        area.setTieneSubcursos(true);
        Curso hijo = curso(20, null, "Álgebra", TipoCurso.SUBCURSO, anio);
        when(cursoRepository.findByCursoPadreIdCursoAndAccesoNot(eq(10), any())).thenReturn(List.of(hijo));
        when(asignacionRepository.existsByCursoIdCursoAndAccesoNot(eq(20), any())).thenReturn(true);
        assertThrows(IllegalArgumentException.class, () -> cursoService.cambiarSubcursos(10, false));
        assertTrue(area.isTieneSubcursos());
        assertEquals(AccesoConstants.ACTIVO, hijo.getAcceso().getIdAcceso());
        verify(cursoRepository, never()).save(any());
    }

    @Test
    void unAreaDeshabilitadaNoPermiteCrearSubcursos() {
        AnioEscolar anio = anio(2, "2027", AnioEscolarService.ESTADO_POR_COMENZAR);
        curso(10, "063: MATE", "MATEMÁTICA", TipoCurso.AREA, anio);
        assertThrows(IllegalArgumentException.class,
                () -> cursoService.crearSubcurso(10, subcurso("Álgebra", 50)));
        verify(cursoRepository, never()).save(any());
    }

    @Test
    void noSeCambiaElEstadoDeUnCursoDeUnAnioVigente() {
        AnioEscolar anio2026 = anio(1, "2026", AnioEscolarService.ESTADO_VIGENTE);
        curso(10, "063: MATE", "MATEMÁTICA", TipoCurso.AREA, anio2026);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> cursoService.cambiarEstado(10, AccesoConstants.INACTIVO));
        assertTrue(ex.getMessage().contains("vigente"));
        verify(cursoRepository, never()).save(any());
    }

    @Test
    void noSeEliminaUnAreaSiUnSubcursoTieneAsignaciones() {
        AnioEscolar anio2027 = anio(2, "2027", AnioEscolarService.ESTADO_POR_COMENZAR);
        curso(10, "063: MATE", "MATEMÁTICA", TipoCurso.AREA, anio2027);
        Curso algebra = curso(20, null, "Álgebra", TipoCurso.SUBCURSO, anio2027);
        when(cursoRepository.findByCursoPadreIdCursoAndAccesoNot(eq(10), any())).thenReturn(List.of(algebra));
        when(asignacionRepository.existsByCursoIdCursoAndAccesoNot(eq(20), any())).thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> cursoService.delete(10));
        assertTrue(ex.getMessage().contains("Álgebra"));
        verify(cursoRepository, never()).save(any());
    }
}
