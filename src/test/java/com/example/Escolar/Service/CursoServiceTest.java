package com.example.Escolar.Service;

import com.example.Escolar.Dto.CursoEstadoRequest;
import com.example.Escolar.Dto.CursoImportResponse;
import com.example.Escolar.Dto.CursoRequest;
import com.example.Escolar.Dto.CursoResponse;
import com.example.Escolar.Model.Acceso;
import com.example.Escolar.Model.AnioEscolar;
import com.example.Escolar.Model.Curso;
import com.example.Escolar.Repository.AccesoRepository;
import com.example.Escolar.Repository.AnioEscolarRepository;
import com.example.Escolar.Repository.AsignacionRepository;
import com.example.Escolar.Repository.CursoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CursoServiceTest {

    @Mock
    private CursoRepository cursoRepository;

    @Mock
    private AccesoRepository accesoRepository;

    @Mock
    private AnioEscolarRepository anioEscolarRepository;

    @Mock
    private AsignacionRepository asignacionRepository;

    @InjectMocks
    private CursoService cursoService;

    private Acceso accesoActivo;
    private Acceso accesoEliminado;
    private Acceso accesoInactivo;

    @BeforeEach
    void setUp() {
        accesoActivo = new Acceso();
        accesoActivo.setIdAcceso(AccesoConstants.ACTIVO);
        accesoActivo.setNombre("ACTIVO");

        accesoEliminado = new Acceso();
        accesoEliminado.setIdAcceso(AccesoConstants.ELIMINADO);
        accesoEliminado.setNombre("ELIMINADO");

        accesoInactivo = new Acceso();
        accesoInactivo.setIdAcceso(AccesoConstants.INACTIVO);
        accesoInactivo.setNombre("INACTIVO");

        lenient().when(accesoRepository.findById(AccesoConstants.ELIMINADO)).thenReturn(Optional.of(accesoEliminado));
        lenient().when(accesoRepository.findById(AccesoConstants.ACTIVO)).thenReturn(Optional.of(accesoActivo));
        lenient().when(accesoRepository.findById(AccesoConstants.INACTIVO)).thenReturn(Optional.of(accesoInactivo));
    }

    @Test
    void bloqueaInactivacionSiAnioEscolarEstaVigente() {
        AnioEscolar anioVigente = new AnioEscolar();
        anioVigente.setIdAnio(1);
        anioVigente.setEstado(CursoService.ESTADO_ANIO_VIGENTE);

        when(anioEscolarRepository.findByEstadoAndAccesoNot(eq(CursoService.ESTADO_ANIO_VIGENTE), any()))
                .thenReturn(Optional.of(anioVigente));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                cursoService.cambiarEstado(10, AccesoConstants.INACTIVO));

        assertTrue(ex.getMessage().contains("No se puede inactivar, modificar ni eliminar el curso"));
        verify(cursoRepository, never()).save(any());
    }

    @Test
    void permiteCambiarEstadoSiNoHayAnioEscolarVigente() {
        when(anioEscolarRepository.findByEstadoAndAccesoNot(eq(CursoService.ESTADO_ANIO_VIGENTE), any()))
                .thenReturn(Optional.empty());

        Curso curso = new Curso();
        curso.setIdCurso(10);
        curso.setNombre("Educación Física");
        curso.setAcceso(accesoActivo);

        when(cursoRepository.findByIdCursoAndAccesoNot(eq(10), any())).thenReturn(Optional.of(curso));
        when(cursoRepository.save(any(Curso.class))).thenAnswer(inv -> inv.getArgument(0));

        CursoResponse response = cursoService.cambiarEstado(10, AccesoConstants.INACTIVO);

        assertNotNull(response);
        assertEquals(AccesoConstants.INACTIVO, response.getAccesoId());
        verify(cursoRepository).save(curso);
    }

    @Test
    void bloqueaEliminacionSiTieneAsignacionesDocentesAsociadas() {
        when(anioEscolarRepository.findByEstadoAndAccesoNot(eq(CursoService.ESTADO_ANIO_VIGENTE), any()))
                .thenReturn(Optional.empty());

        Curso curso = new Curso();
        curso.setIdCurso(5);
        curso.setNombre("Comunicación");
        curso.setAcceso(accesoActivo);

        when(cursoRepository.findByIdCursoAndAccesoNot(eq(5), any())).thenReturn(Optional.of(curso));
        when(asignacionRepository.existsByCursoIdCursoAndAccesoNot(eq(5), any())).thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                cursoService.delete(5));

        assertTrue(ex.getMessage().contains("asignaciones académicas asociadas"));
        verify(cursoRepository, never()).save(any());
    }

    @Test
    void importaCursosExitosamenteDesdeExcelReal() throws Exception {
        File excelFile = new File("/home/jhonroman/Descargas/RegNotas_primero_secundaria.xlsx");
        if (!excelFile.exists()) {
            return;
        }

        try (FileInputStream fis = new FileInputStream(excelFile)) {
            MockMultipartFile multipartFile = new MockMultipartFile(
                    "archivo",
                    "RegNotas_primero_secundaria.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    fis
            );

            when(cursoRepository.findByCodigoAndAccesoNot(anyString(), any())).thenReturn(Optional.empty());
            when(cursoRepository.findByNombreAndAccesoNot(anyString(), any())).thenReturn(Optional.empty());
            when(cursoRepository.save(any(Curso.class))).thenAnswer(inv -> {
                Curso c = inv.getArgument(0);
                if (c.getIdCurso() == null) {
                    c.setIdCurso(100);
                }
                return c;
            });

            CursoImportResponse res = cursoService.importarDesdeExcel(multipartFile);

            assertNotNull(res);
            assertEquals(13, res.getTotalDetectados());
            assertEquals(13, res.getCreados());
            assertEquals(0, res.getActualizados());
            assertEquals(13, res.getCursos().size());

            // Verificar que se extrajo el código y el nombre
            assertTrue(res.getCursos().stream().anyMatch(c -> "017: COMU".equals(c.getCodigo()) && "COMUNICACIÓN".equals(c.getNombre())));
            assertTrue(res.getCursos().stream().anyMatch(c -> "063: MATE".equals(c.getCodigo()) && "MATEMÁTICA".equals(c.getNombre())));
        }
    }

    @Test
    void reactivaCursoSiEstabaEliminadoLogicamenteEnCreate() {
        Curso cursoEliminado = new Curso();
        cursoEliminado.setIdCurso(99);
        cursoEliminado.setCodigo("099: TALL");
        cursoEliminado.setNombre("Taller de Computación");
        cursoEliminado.setAcceso(accesoEliminado);

        when(cursoRepository.findByCodigoAndAccesoNot(eq("099: TALL"), any())).thenReturn(Optional.empty());
        when(cursoRepository.findByNombreAndAccesoNot(eq("Taller de Robótica"), any())).thenReturn(Optional.empty());
        when(cursoRepository.findByCodigo("099: TALL")).thenReturn(Optional.of(cursoEliminado));
        when(cursoRepository.save(any(Curso.class))).thenAnswer(inv -> inv.getArgument(0));

        com.example.Escolar.Dto.CursoRequest req = new com.example.Escolar.Dto.CursoRequest();
        req.setCodigo("099: TALL");
        req.setNombre("Taller de Robótica");

        CursoResponse res = cursoService.create(req);

        assertNotNull(res);
        assertEquals(99, res.getIdCurso());
        assertEquals("Taller de Robótica", res.getNombre());
        assertEquals("099: TALL", res.getCodigo());
        assertEquals(AccesoConstants.ACTIVO, res.getAccesoId());
        verify(cursoRepository).save(cursoEliminado);
    }
}

