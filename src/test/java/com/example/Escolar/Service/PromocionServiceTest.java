package com.example.Escolar.Service;

import com.example.Escolar.Dto.ConfiguracionAcademicaResponse;
import com.example.Escolar.Model.*;
import com.example.Escolar.Repository.AccesoRepository;
import com.example.Escolar.Repository.NotaFinalAnualRepository;
import com.example.Escolar.Service.PromocionService.Situacion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PromocionServiceTest {
    @Mock MallaEvaluable mallaEvaluable;
    @Mock NotaFinalAnualRepository anualRepository;
    @Mock ConfiguracionAcademicaService configuracionService;
    @Mock AccesoRepository accesoRepository;
    @InjectMocks PromocionService service;

    private final Acceso eliminado = new Acceso(AccesoConstants.ELIMINADO, "Eliminado");
    private final List<MallaEvaluable.Area> areas = new ArrayList<>();
    private final List<NotaFinalAnual> notas = new ArrayList<>();
    private ConfiguracionAcademicaResponse config;
    private Matricula matricula;
    private int siguienteCompetencia = 1;

    @BeforeEach
    void preparar() {
        AnioEscolar anio = new AnioEscolar();
        anio.setIdAnio(1);
        Nivel nivel = new Nivel();
        nivel.setIdNivel(2);
        Grado grado = new Grado();
        grado.setNivel(nivel);
        GradoSeccion gs = new GradoSeccion();
        gs.setIdGradoSeccion(30);
        gs.setGrado(grado);
        gs.setAnioEscolar(anio);
        matricula = new Matricula();
        matricula.setIdMatricula(5);
        matricula.setGradoSeccion(gs);

        config = new ConfiguracionAcademicaResponse();
        config.setEscala(EscalaCalificacion.VIGESIMAL);
        config.setNotaMinima("11");
        config.setAreasParaRepetir(4);
        when(accesoRepository.findById(AccesoConstants.ELIMINADO)).thenReturn(Optional.of(eliminado));
        when(configuracionService.obtener(1, 2)).thenReturn(config);
        when(mallaEvaluable.areas(anio, nivel)).thenReturn(areas);
        when(anualRepository.findByMatriculaIdMatriculaAndAccesoNot(5, eliminado)).thenReturn(notas);
        when(anualRepository.existsByMatriculaGradoSeccionIdGradoSeccionAndAccesoNot(eq(30), any())).thenReturn(true);
    }

    /** Un área con esas notas del año (null: sin nota en esa competencia). */
    private void area(String nombre, TipoCurso tipo, String... calificaciones) {
        Curso curso = new Curso();
        curso.setNombre(nombre);
        curso.setTipo(tipo);
        List<Competencia> competencias = new ArrayList<>();
        for (String calificacion : calificaciones) {
            Competencia c = new Competencia();
            c.setIdCompetencia(siguienteCompetencia++);
            c.setCodigo("0" + competencias.size());
            competencias.add(c);
            if (calificacion != null) {
                NotaFinalAnual n = new NotaFinalAnual();
                n.setCompetencia(c);
                n.setCalificacion(calificacion);
                notas.add(n);
            }
        }
        areas.add(new MallaEvaluable.Area(curso, competencias));
    }

    private void area(String nombre, String... calificaciones) {
        area(nombre, TipoCurso.AREA, calificaciones);
    }

    @Test
    void enVigesimalElAreaSeApruebaPorPromedio() {
        assertFalse(PromocionService.areaDesaprobada(EscalaCalificacion.VIGESIMAL, List.of("10", "11"), "11"));
        assertTrue(PromocionService.areaDesaprobada(EscalaCalificacion.VIGESIMAL, List.of("10", "10", "11"), "11"));
    }

    @Test
    void enLiteralLaMitadOMasBajoLaMinimaDesapruebaElArea() {
        assertTrue(PromocionService.areaDesaprobada(EscalaCalificacion.LITERAL, List.of("C", "A"), "B"));
        assertFalse(PromocionService.areaDesaprobada(EscalaCalificacion.LITERAL, List.of("C", "B", "A"), "B"));
        assertTrue(PromocionService.areaDesaprobada(EscalaCalificacion.LITERAL, List.of("C", "C", "A"), "B"));
    }

    @Test
    void conTresAreasDesaprobadasEsPromovido() {
        area("Matemática", "08");
        area("Comunicación", "09");
        area("Ciencia", "10");
        area("Inglés", "15");

        var resultado = service.situacion(matricula);

        assertEquals(Situacion.PROMOVIDO, resultado.situacion());
        assertEquals(List.of("Matemática", "Comunicación", "Ciencia"), resultado.desaprobadas());
    }

    @Test
    void conCuatroAreasDesaprobadasRepite() {
        area("Matemática", "08");
        area("Comunicación", "09");
        area("Ciencia", "10");
        area("Inglés", "05");

        assertEquals(Situacion.REPITE, service.situacion(matricula).situacion());
    }

    @Test
    void lasTransversalesNoCuentan() {
        area("Matemática", "08");
        area("Comunicación", "09");
        area("Ciencia", "10");
        area("TIC", TipoCurso.TRANSVERSAL, "02");
        area("Autonomía", TipoCurso.TRANSVERSAL, (String) null);

        assertEquals(Situacion.PROMOVIDO, service.situacion(matricula).situacion());
    }

    @Test
    void sinTodasLasNotasDelAnioNoSeDecide() {
        area("Matemática", "15", null);

        var resultado = service.situacion(matricula);

        assertEquals(Situacion.FALTAN_NOTAS, resultado.situacion());
        assertEquals(List.of("Matemática C01"), resultado.faltantes());
    }

    @Test
    void unaSeccionSinNotasDelAnioLaDecideQuienMatricula() {
        when(anualRepository.existsByMatriculaGradoSeccionIdGradoSeccionAndAccesoNot(eq(30), any())).thenReturn(false);

        assertEquals(Situacion.SIN_NOTAS_DEL_ANIO, service.situacion(matricula).situacion());
    }

    @Test
    void enLiteralUsaLaMinimaDelNivel() {
        config.setEscala(EscalaCalificacion.LITERAL);
        config.setNotaMinima("A");
        config.setAreasParaRepetir(1);
        area("Matemática", "B", "A");

        assertEquals(Situacion.REPITE, service.situacion(matricula).situacion());
    }
}
