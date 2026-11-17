package com.example.Escolar.Service;

import com.example.Escolar.Model.AnioEscolar;
import com.example.Escolar.Model.DiaFeriado;
import com.example.Escolar.Model.Nivel;
import com.example.Escolar.Model.PeriodoEvaluacion;
import com.example.Escolar.Repository.AccesoRepository;
import com.example.Escolar.Repository.DiaFeriadoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Días de clase de un nivel en un año: dentro de las fechas del año, dentro de
 * algún periodo (las vacaciones entre periodos no cuentan), de lunes a viernes y
 * sin feriados (del año o generales). Sin periodos configurados basta el año.
 */
@Component
@RequiredArgsConstructor
public class CalendarioEscolar {

    private final ConfiguracionAcademicaService configuracionService;
    private final DiaFeriadoRepository feriadoRepository;
    private final AccesoRepository accesoRepository;

    public Set<LocalDate> diasDeClase(AnioEscolar anio, Nivel nivel, LocalDate inicio, LocalDate fin) {
        LocalDate desde = anio.getFechaInicio() != null && anio.getFechaInicio().isAfter(inicio) ? anio.getFechaInicio() : inicio;
        LocalDate hasta = anio.getFechaFin() != null && anio.getFechaFin().isBefore(fin) ? anio.getFechaFin() : fin;
        if (desde.isAfter(hasta)) {
            return Set.of();
        }
        List<PeriodoEvaluacion> periodos = configuracionService.periodos(anio, nivel);
        Set<LocalDate> feriados = feriadoRepository.findByFechaBetweenAndAccesoNot(desde, hasta,
                        accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow()).stream()
                .filter(f -> f.getAnioEscolar() == null || f.getAnioEscolar().getIdAnio().equals(anio.getIdAnio()))
                .map(DiaFeriado::getFecha)
                .collect(Collectors.toSet());
        return desde.datesUntil(hasta.plusDays(1))
                .filter(d -> d.getDayOfWeek() != DayOfWeek.SATURDAY && d.getDayOfWeek() != DayOfWeek.SUNDAY)
                .filter(d -> !feriados.contains(d))
                .filter(d -> periodos.isEmpty() || periodos.stream()
                        .anyMatch(p -> !d.isBefore(p.getFechaInicio()) && !d.isAfter(p.getFechaFin())))
                .collect(Collectors.toSet());
    }

    public boolean esDiaDeClase(AnioEscolar anio, Nivel nivel, LocalDate fecha) {
        return diasDeClase(anio, nivel, fecha, fecha).contains(fecha);
    }
}
