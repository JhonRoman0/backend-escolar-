package com.example.Escolar.Service;

import com.example.Escolar.Dto.AulaRequest;
import com.example.Escolar.Dto.AulaResponse;
import com.example.Escolar.Model.Acceso;
import com.example.Escolar.Model.Asignacion;
import com.example.Escolar.Model.Aula;
import com.example.Escolar.Model.AnioEscolar;
import com.example.Escolar.Model.Grado;
import com.example.Escolar.Model.GradoSeccion;
import com.example.Escolar.Model.Nivel;
import com.example.Escolar.Model.Seccion;
import com.example.Escolar.Model.TipoAula;
import com.example.Escolar.Model.Turno;
import com.example.Escolar.Model.HorarioClase;
import com.example.Escolar.Repository.AccesoRepository;
import com.example.Escolar.Repository.AulaRepository;
import com.example.Escolar.Repository.GradoSeccionRepository;
import com.example.Escolar.Repository.HorarioClaseRepository;
import com.example.Escolar.Repository.MatriculaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.dao.DataRetrievalFailureException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AulaServiceTest {

    @Mock
    private AulaRepository aulaRepository;
    @Mock
    private AccesoRepository accesoRepository;
    @Mock
    private HorarioClaseRepository horarioClaseRepository;
    @Mock
    private GradoSeccionRepository gradoSeccionRepository;
    @Mock
    private MatriculaRepository matriculaRepository;
    @InjectMocks
    private AulaService aulaService;

    private Acceso activo;
    private Acceso eliminado;
    private Acceso inactivo;

    @BeforeEach
    void setUp() {
        activo = new Acceso();
        activo.setIdAcceso(AccesoConstants.ACTIVO);
        eliminado = new Acceso();
        eliminado.setIdAcceso(AccesoConstants.ELIMINADO);
        inactivo = new Acceso();
        inactivo.setIdAcceso(AccesoConstants.INACTIVO);

        when(accesoRepository.findById(AccesoConstants.ACTIVO)).thenReturn(Optional.of(activo));
        when(accesoRepository.findById(AccesoConstants.ELIMINADO)).thenReturn(Optional.of(eliminado));
        when(accesoRepository.findById(AccesoConstants.INACTIVO)).thenReturn(Optional.of(inactivo));
        when(aulaRepository.findByAccesoNot(eliminado)).thenReturn(new ArrayList<>());
        when(aulaRepository.save(any(Aula.class))).thenAnswer(inv -> inv.getArgument(0));
        when(aulaRepository.findByNombreIgnoreCase(any())).thenReturn(Optional.empty());
        when(aulaRepository.findByNombreIgnoreCaseAndIdAulaNot(any(), any())).thenReturn(Optional.empty());
        when(aulaRepository.findByIdAulaAndAccesoNot(any(), any())).thenAnswer(inv ->
                Optional.of(aula(inv.getArgument(0), "Aula 101", 30)));
    }

    private Aula aula(Integer id, String nombre, Integer capacidad) {
        Aula a = new Aula();
        a.setIdAula(id);
        a.setNombre(nombre);
        a.setCapacidad(capacidad);
        a.setAcceso(activo);
        return a;
    }

    private AulaRequest request(String nombre, Integer capacidad, Long accesoId) {
        AulaRequest r = new AulaRequest();
        r.setNombre(nombre);
        r.setCapacidad(capacidad);
        r.setAccesoId(accesoId);
        return r;
    }

    @Test
    void crearAceptaNombreNormalizado() {
        AulaRequest r = request("  Aula 101  ", 25, AccesoConstants.ACTIVO);
        assertDoesNotThrow(() -> aulaService.create(r));

        ArgumentCaptor<Aula> captor = ArgumentCaptor.forClass(Aula.class);
        verify(aulaRepository).save(captor.capture());
        assertEquals("Aula 101", captor.getValue().getNombre());
        assertEquals(25, captor.getValue().getCapacidad());
    }

    @Test
    void crearRechazaNombreDuplicadoEnMayusculas() {
        when(aulaRepository.findByNombreIgnoreCase("AULA 101"))
                .thenReturn(Optional.of(aula(1, "Aula 101", 30)));
        AulaRequest r = request("AULA 101", 25, AccesoConstants.ACTIVO);
        assertThrows(IllegalArgumentException.class, () -> aulaService.create(r));
    }

    @Test
    void crearRechazaAccesoEliminado() {
        AulaRequest r = request("Aula 303", 30, AccesoConstants.ELIMINADO);
        assertThrows(IllegalArgumentException.class, () -> aulaService.create(r));
        verify(aulaRepository, never()).save(any(Aula.class));
    }

    @Test
    void actualizarRechazaNombreDuplicadoExcluyendoMismaAula() {
        when(aulaRepository.findByNombreIgnoreCaseAndIdAulaNot("Aula 202", 1))
                .thenReturn(Optional.of(aula(2, "Aula 202", 30)));
        AulaRequest r = request("Aula 202", 30, AccesoConstants.ACTIVO);
        assertThrows(IllegalArgumentException.class, () -> aulaService.update(1, r));
    }

    @Test
    void actualizarAceptaConservarMismoNombre() {
        // Discrimina el camino correcto: si el service validara sin excluir el id
        // usaría findByNombreIgnoreCase y este stub lo haría fallar.
        when(aulaRepository.findByNombreIgnoreCase("Aula 101"))
                .thenReturn(Optional.of(aula(1, "Aula 101", 30)));
        AulaRequest r = request("Aula 101", 40, AccesoConstants.ACTIVO);
        assertDoesNotThrow(() -> aulaService.update(1, r));
    }

    @Test
    void eliminarRechazaSiTieneHorariosActivos() {
        when(horarioClaseRepository.countByAulaIdAulaAndAccesoNot(1, eliminado)).thenReturn(2L);

        assertThrows(IllegalArgumentException.class, () -> aulaService.delete(1));
        verify(horarioClaseRepository, never()).deleteAll(anyList());
        verify(aulaRepository, never()).delete(any(Aula.class));
    }

    @Test
    void eliminarBorraFisicamenteYPurgaHorariosEnPapelera() {
        when(horarioClaseRepository.countByAulaIdAulaAndAccesoNot(1, eliminado)).thenReturn(0L);
        when(horarioClaseRepository.findByAulaIdAula(1))
                .thenReturn(new ArrayList<>(List.of(new HorarioClase())));

        aulaService.delete(1);

        verify(horarioClaseRepository).deleteAll(anyList());
        verify(aulaRepository).delete(any(Aula.class));
        verify(aulaRepository, never()).save(any(Aula.class));
    }

    // ---- Tipo de aula ----

    @Test
    void unAulaNuevaEsSalonSalvoQueSeElijaOtroTipo() {
        assertEquals("SALON", aulaService.create(request("Aula 301", 30, AccesoConstants.ACTIVO)).getTipo());

        AulaRequest patio = request("Patio", 50, AccesoConstants.ACTIVO);
        patio.setTipo(TipoAula.COMPARTIDO);
        assertEquals("COMPARTIDO", aulaService.create(patio).getTipo());
    }

    /** El Aula 101 es el salon de 3.° A en la mañana de 2027. */
    private void esSalonDeTercerA() {
        esSalonDeTercerA(AnioEscolarService.ESTADO_VIGENTE);
    }

    private void esSalonDeTercerA(byte estadoAnio) {
        Grado grado = new Grado();
        grado.setNombre("3.° grado");
        Nivel primaria = new Nivel();
        primaria.setNombre("Primaria");
        grado.setNivel(primaria);
        Seccion a = new Seccion();
        a.setNombre("A");
        Turno manana = new Turno();
        manana.setNombre("Mañana");
        AnioEscolar anio = new AnioEscolar();
        anio.setIdAnio(2);
        anio.setAnio("2027");
        anio.setEstado(estadoAnio);
        GradoSeccion tercerA = new GradoSeccion();
        tercerA.setIdGradoSeccion(5);
        tercerA.setGrado(grado);
        tercerA.setSeccion(a);
        tercerA.setTurno(manana);
        tercerA.setAnioEscolar(anio);
        tercerA.setAcceso(activo);
        when(gradoSeccionRepository.findByAulaIdAulaAndAccesoNot(any(), any())).thenReturn(List.of(tercerA));
    }

    private void conHorarioEn(String anioTexto, byte estadoAnio) {
        conHorarioEn(anioTexto, estadoAnio, activo, activo, activo);
    }

    private void conHorarioEn(String anioTexto, byte estadoAnio, Acceso accesoHorario,
                              Acceso accesoAsignacion, Acceso accesoSeccion) {
        AnioEscolar anio = new AnioEscolar();
        anio.setIdAnio(10);
        anio.setAnio(anioTexto);
        anio.setEstado(estadoAnio);
        GradoSeccion seccion = new GradoSeccion();
        seccion.setIdGradoSeccion(6);
        seccion.setAcceso(accesoSeccion);
        Asignacion asignacion = new Asignacion();
        asignacion.setAnioEscolar(anio);
        asignacion.setGradoSeccion(seccion);
        asignacion.setAcceso(accesoAsignacion);
        HorarioClase horario = new HorarioClase();
        horario.setAula(aula(1, "Aula 101", 30));
        horario.setAsignacion(asignacion);
        horario.setAcceso(accesoHorario);
        when(horarioClaseRepository.findByAulaIdAulaAndAccesoNot(1, eliminado))
                .thenReturn(List.of(horario));
    }

    @Test
    void unSalonDeUnaSeccionNoSeEliminaNiCambiaDeTipo() {
        esSalonDeTercerA();

        IllegalArgumentException alEliminar = assertThrows(IllegalArgumentException.class, () -> aulaService.delete(1));
        assertTrue(alEliminar.getMessage().contains("es el salón de 3.° grado A"));

        AulaRequest aLaboratorio = request("Aula 101", 30, null);
        aLaboratorio.setTipo(TipoAula.AMBIENTE);
        IllegalArgumentException alCambiar = assertThrows(IllegalArgumentException.class,
                () -> aulaService.update(1, aLaboratorio));
        assertTrue(alCambiar.getMessage().contains("No se puede cambiar el tipo"));
        verify(aulaRepository, never()).delete(any(Aula.class));
    }

    @Test
    void laRespuestaDiceDeQueSeccionEsSalon() {
        esSalonDeTercerA();
        assertEquals(List.of(new AulaResponse.SeccionDelAula(2, "2027", "Primaria", "3.° grado", "A", "Mañana")),
                aulaService.getById(1).getSecciones());
    }

    // ---- Inactivación con asignaciones vigentes ----

    @Test
    void actualizarRechazaInactivarSiSeccionVigente() {
        esSalonDeTercerA();

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> aulaService.update(1, request("Aula 101", 30, AccesoConstants.INACTIVO)));

        assertTrue(error.getMessage().contains("No se puede inactivar el aula «Aula 101»"));
        assertTrue(error.getMessage().contains("es el salón de 3.° grado A"));
        assertTrue(error.getMessage().contains("Cambia el salón en Secciones"));
        verify(aulaRepository, never()).save(any(Aula.class));
    }

    @Test
    void actualizarPermiteInactivarSiSeccionEsDeAnioCerrado() {
        esSalonDeTercerA(AnioEscolarService.ESTADO_CERRADO);

        AulaResponse respuesta = aulaService.update(1, request("Aula 101", 30, AccesoConstants.INACTIVO));

        assertEquals(AccesoConstants.INACTIVO, respuesta.getAccesoId());
    }

    @Test
    void actualizarRechazaInactivarSiHorarioVigente() {
        conHorarioEn("2026", AnioEscolarService.ESTADO_VIGENTE);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> aulaService.update(1, request("Aula 101", 30, AccesoConstants.INACTIVO)));

        assertTrue(error.getMessage().contains("tiene 1 clase programada (2026: 1)"));
        verify(horarioClaseRepository).findByAulaIdAulaAndAccesoNot(1, eliminado);
        verify(aulaRepository, never()).save(any(Aula.class));
    }

    @Test
    void actualizarPermiteInactivarSiHorarioEsDeAnioCerrado() {
        conHorarioEn("2020", AnioEscolarService.ESTADO_CERRADO);

        assertDoesNotThrow(() -> aulaService.update(1, request("Aula 101", 30, AccesoConstants.INACTIVO)));
    }

    @Test
    void actualizarNoCuentaHorarioEliminado() {
        conHorarioEn("2026", AnioEscolarService.ESTADO_VIGENTE, eliminado, activo, activo);

        assertDoesNotThrow(() -> aulaService.update(1, request("Aula 101", 30, AccesoConstants.INACTIVO)));
    }

    @Test
    void actualizarNoCuentaAsignacionEliminada() {
        conHorarioEn("2026", AnioEscolarService.ESTADO_VIGENTE, activo, eliminado, activo);

        assertDoesNotThrow(() -> aulaService.update(1, request("Aula 101", 30, AccesoConstants.INACTIVO)));
    }

    @Test
    void actualizarNoCuentaSeccionEliminadaDelHorario() {
        conHorarioEn("2026", AnioEscolarService.ESTADO_VIGENTE, activo, activo, eliminado);

        assertDoesNotThrow(() -> aulaService.update(1, request("Aula 101", 30, AccesoConstants.INACTIVO)));
    }

    @Test
    void getByIdExponeSoloAsignacionesVigentes() {
        esSalonDeTercerA();
        conHorarioEn("2027", AnioEscolarService.ESTADO_POR_COMENZAR);

        AulaResponse respuesta = aulaService.getById(1);

        assertTrue(respuesta.getTieneAsignacionesVigentes());
        assertEquals(List.of(new AulaResponse.SeccionDelAula(2, "2027", "Primaria", "3.° grado", "A", "Mañana")),
                respuesta.getSeccionesVigentes());
        assertEquals(Map.of("2027", 1L), respuesta.getHorariosVigentesPorAnio());
    }

    @Test
    void getByIdNoCuentaAniosCerradosYPreservaElHistorial() {
        esSalonDeTercerA(AnioEscolarService.ESTADO_CERRADO);
        conHorarioEn("2020", AnioEscolarService.ESTADO_CERRADO);

        AulaResponse respuesta = aulaService.getById(1);

        assertFalse(respuesta.getTieneAsignacionesVigentes());
        assertTrue(respuesta.getSeccionesVigentes().isEmpty());
        assertTrue(respuesta.getHorariosVigentesPorAnio().isEmpty());
        assertEquals(1, respuesta.getSecciones().size());
        assertEquals(0, respuesta.getOcupacionActual());
    }

    @Test
    void actualizarConservaInactivoConAsignacionesVigentes() {
        Aula inactiva = aula(1, "Aula 101", 30);
        inactiva.setAcceso(inactivo);
        when(aulaRepository.findByIdAulaAndAccesoNot(1, eliminado)).thenReturn(Optional.of(inactiva));
        esSalonDeTercerA();

        AulaResponse respuesta = aulaService.update(1, request("Aula 101", 30, AccesoConstants.INACTIVO));

        assertEquals(AccesoConstants.INACTIVO, respuesta.getAccesoId());
    }

    @Test
    void actualizarPermiteReactivarConAsignacionesVigentes() {
        Aula inactiva = aula(1, "Aula 101", 30);
        inactiva.setAcceso(inactivo);
        when(aulaRepository.findByIdAulaAndAccesoNot(1, eliminado)).thenReturn(Optional.of(inactiva));
        esSalonDeTercerA();

        AulaResponse respuesta = aulaService.update(1, request("Aula 101", 30, AccesoConstants.ACTIVO));

        assertEquals(AccesoConstants.ACTIVO, respuesta.getAccesoId());
    }

    @Test
    void listadoNoConsultaHorariosPorAula() {
        when(aulaRepository.findByAccesoNot(eliminado))
                .thenReturn(new ArrayList<>(List.of(aula(1, "Aula 101", 30))));

        aulaService.getAll();

        verify(horarioClaseRepository, never()).findByAulaIdAulaAndAccesoNot(any(), any());
    }

    @Test
    void actualizarRechazaReducirCapacidadPorDebajoDeLaOcupacion() {
        esSalonDeTercerA();
        when(matriculaRepository.countByGradoSeccionIdGradoSeccionAndAccesoNot(5, eliminado)).thenReturn(25L);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> aulaService.update(1, request("Aula 101", 20, AccesoConstants.ACTIVO)));

        assertTrue(error.getMessage().contains("No se puede reducir la capacidad del aula «Aula 101» a 20"));
        assertTrue(error.getMessage().contains("25 alumnos matriculados"));
        verify(aulaRepository, never()).save(any(Aula.class));
    }

    @Test
    void actualizarPermiteReducirCapacidadHastaLaOcupacion() {
        esSalonDeTercerA();
        when(matriculaRepository.countByGradoSeccionIdGradoSeccionAndAccesoNot(5, eliminado)).thenReturn(20L);

        AulaResponse respuesta = aulaService.update(1, request("Aula 101", 20, AccesoConstants.ACTIVO));

        assertEquals(20, respuesta.getCapacidad());
        verify(aulaRepository).save(any(Aula.class));
    }

    @Test
    void actualizarPermiteEditarOtrosCamposSinConsultarOcupacionSiNoBajaLaCapacidad() {
        esSalonDeTercerA();
        when(matriculaRepository.countByGradoSeccionIdGradoSeccionAndAccesoNot(5, eliminado)).thenReturn(35L);

        AulaResponse respuesta = aulaService.update(1, request("Aula 101 bis", 30, AccesoConstants.ACTIVO));

        assertEquals("Aula 101 bis", respuesta.getNombre());
        verify(matriculaRepository, never()).countByGradoSeccionIdGradoSeccionAndAccesoNot(any(), any());
        verify(matriculaRepository, never()).ocupacionPorSecciones(any(), any());
    }

    @Test
    void actualizarNoReduceCapacidadSiFallaLaConsultaDeOcupacion() {
        esSalonDeTercerA();
        when(matriculaRepository.countByGradoSeccionIdGradoSeccionAndAccesoNot(5, eliminado))
                .thenThrow(new DataRetrievalFailureException("sin ocupación"));

        assertThrows(DataRetrievalFailureException.class,
                () -> aulaService.update(1, request("Aula 101", 20, AccesoConstants.ACTIVO)));
        verify(aulaRepository, never()).save(any(Aula.class));
    }

    @Test
    void getByIdDistingueOcupacionCeroDeDesconocida() {
        esSalonDeTercerA();
        when(matriculaRepository.countByGradoSeccionIdGradoSeccionAndAccesoNot(5, eliminado)).thenReturn(0L);
        assertEquals(0, aulaService.getById(1).getOcupacionActual());

        when(matriculaRepository.countByGradoSeccionIdGradoSeccionAndAccesoNot(5, eliminado))
                .thenThrow(new DataRetrievalFailureException("sin ocupación"));
        AulaResponse respuesta = aulaService.getById(1);
        assertNull(respuesta.getOcupacionActual());
        assertTrue(respuesta.getTieneAsignacionesVigentes());
    }

    @Test
    void getByIdCalculaMaximoPorSeccionSinSumarTurnosNiAnios() {
        esSalonDeTercerA();
        conHorarioEn("2028", AnioEscolarService.ESTADO_POR_COMENZAR);
        List<MatriculaRepository.OcupacionSeccion> conteos =
                List.of(ocupacionSeccion(5, 18), ocupacionSeccion(6, 25));
        when(matriculaRepository.ocupacionPorSecciones(any(), any())).thenReturn(conteos);

        assertEquals(25, aulaService.getById(1).getOcupacionActual());
    }

    @Test
    void getByIdNoCuentaOcupacionDeHorarioEliminado() {
        conHorarioEn("2026", AnioEscolarService.ESTADO_VIGENTE, eliminado, activo, activo);

        assertEquals(0, aulaService.getById(1).getOcupacionActual());
        verify(matriculaRepository, never()).countByGradoSeccionIdGradoSeccionAndAccesoNot(any(), any());
        verify(matriculaRepository, never()).ocupacionPorSecciones(any(), any());
    }

    @Test
    void getByIdNoCuentaOcupacionDeAsignacionEliminada() {
        conHorarioEn("2026", AnioEscolarService.ESTADO_VIGENTE, activo, eliminado, activo);

        assertEquals(0, aulaService.getById(1).getOcupacionActual());
        verify(matriculaRepository, never()).countByGradoSeccionIdGradoSeccionAndAccesoNot(any(), any());
        verify(matriculaRepository, never()).ocupacionPorSecciones(any(), any());
    }

    @Test
    void getByIdNoCuentaOcupacionDeSeccionEliminadaDelHorario() {
        conHorarioEn("2026", AnioEscolarService.ESTADO_VIGENTE, activo, activo, eliminado);

        assertEquals(0, aulaService.getById(1).getOcupacionActual());
        verify(matriculaRepository, never()).countByGradoSeccionIdGradoSeccionAndAccesoNot(any(), any());
        verify(matriculaRepository, never()).ocupacionPorSecciones(any(), any());
    }

    private MatriculaRepository.OcupacionSeccion ocupacionSeccion(Integer idGradoSeccion, long total) {
        MatriculaRepository.OcupacionSeccion ocupacion = mock(MatriculaRepository.OcupacionSeccion.class);
        when(ocupacion.getIdGradoSeccion()).thenReturn(idGradoSeccion);
        when(ocupacion.getTotal()).thenReturn(total);
        return ocupacion;
    }
}
