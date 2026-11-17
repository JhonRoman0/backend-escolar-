package com.example.Escolar.Service;

import com.example.Escolar.Model.Acceso;
import com.example.Escolar.Model.AnioEscolar;
import com.example.Escolar.Model.DiaFeriado;
import com.example.Escolar.Model.Nivel;
import com.example.Escolar.Model.PeriodoEvaluacion;
import com.example.Escolar.Repository.AccesoRepository;
import com.example.Escolar.Repository.DiaFeriadoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CalendarioEscolarTest {
    @Mock ConfiguracionAcademicaService configuracionService;
    @Mock DiaFeriadoRepository feriadoRepository;
    @Mock AccesoRepository accesoRepository;
    @InjectMocks CalendarioEscolar calendario;

    private final AnioEscolar anio = new AnioEscolar();
    private final Nivel primaria = new Nivel();
    private final List<PeriodoEvaluacion> periodos = new ArrayList<>();
    private final List<DiaFeriado> feriados = new ArrayList<>();

    @BeforeEach
    void preparar() {
        anio.setIdAnio(1);
        anio.setFechaInicio(LocalDate.of(2026, 3, 2));
        anio.setFechaFin(LocalDate.of(2026, 12, 18));
        when(accesoRepository.findById(AccesoConstants.ELIMINADO)).thenReturn(Optional.of(new Acceso(AccesoConstants.ELIMINADO, "Eliminado")));
        when(configuracionService.periodos(anio, primaria)).thenReturn(periodos);
        when(feriadoRepository.findByFechaBetweenAndAccesoNot(any(), any(), any())).thenReturn(feriados);
        // Primer bimestre del 2 de marzo al 8 de mayo; segundo del 18 de mayo al 24 de julio.
        periodos.add(periodo(LocalDate.of(2026, 3, 2), LocalDate.of(2026, 5, 8)));
        periodos.add(periodo(LocalDate.of(2026, 5, 18), LocalDate.of(2026, 7, 24)));
    }

    private static PeriodoEvaluacion periodo(LocalDate inicio, LocalDate fin) {
        PeriodoEvaluacion p = new PeriodoEvaluacion();
        p.setFechaInicio(inicio);
        p.setFechaFin(fin);
        return p;
    }

    private DiaFeriado feriado(LocalDate fecha, AnioEscolar delAnio) {
        DiaFeriado f = new DiaFeriado();
        f.setFecha(fecha);
        f.setAnioEscolar(delAnio);
        return f;
    }

    @Test
    void unLunesDentroDeUnPeriodoEsDiaDeClase() {
        assertTrue(calendario.esDiaDeClase(anio, primaria, LocalDate.of(2026, 3, 9)));
    }

    @Test
    void elFinDeSemanaNoCuenta() {
        assertFalse(calendario.esDiaDeClase(anio, primaria, LocalDate.of(2026, 3, 7)));
    }

    @Test
    void fueraDelAnioEscolarNoCuenta() {
        assertFalse(calendario.esDiaDeClase(anio, primaria, LocalDate.of(2026, 2, 23)));
    }

    @Test
    void lasVacacionesEntrePeriodosNoCuentan() {
        assertFalse(calendario.esDiaDeClase(anio, primaria, LocalDate.of(2026, 5, 12)));
    }

    @Test
    void losFeriadosDelAnioYLosGeneralesNoCuentan() {
        feriados.add(feriado(LocalDate.of(2026, 3, 10), anio));
        feriados.add(feriado(LocalDate.of(2026, 3, 11), null));
        AnioEscolar otro = new AnioEscolar();
        otro.setIdAnio(2);
        feriados.add(feriado(LocalDate.of(2026, 3, 12), otro));

        assertFalse(calendario.esDiaDeClase(anio, primaria, LocalDate.of(2026, 3, 10)));
        assertFalse(calendario.esDiaDeClase(anio, primaria, LocalDate.of(2026, 3, 11)));
        // El feriado de otro año no afecta a este.
        assertTrue(calendario.esDiaDeClase(anio, primaria, LocalDate.of(2026, 3, 12)));
    }

    @Test
    void sinPeriodosBastaElAnio() {
        periodos.clear();
        assertTrue(calendario.esDiaDeClase(anio, primaria, LocalDate.of(2026, 5, 12)));
    }

    @Test
    void cuentaLosDiasDeUnaSemana() {
        assertEquals(5, calendario.diasDeClase(anio, primaria, LocalDate.of(2026, 3, 9), LocalDate.of(2026, 3, 15)).size());
    }
}
