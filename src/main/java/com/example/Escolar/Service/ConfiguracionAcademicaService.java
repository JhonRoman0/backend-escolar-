package com.example.Escolar.Service;

import com.example.Escolar.Dto.PeriodoEvaluacionResponse;
import com.example.Escolar.Model.EscalaCalificacion;
import com.example.Escolar.Model.PeriodoEvaluacion;
import com.example.Escolar.Model.TipoPeriodo;
import com.example.Escolar.Repository.PeriodoEvaluacionRepository;
import java.time.LocalDate;
import com.example.Escolar.Dto.ConfiguracionAcademicaRequest;
import com.example.Escolar.Dto.ConfiguracionAcademicaResponse;
import com.example.Escolar.Exception.ResourceNotFoundException;
import com.example.Escolar.Model.Acceso;
import com.example.Escolar.Model.AnioEscolar;
import com.example.Escolar.Model.ConfiguracionAcademica;
import com.example.Escolar.Model.Nivel;
import com.example.Escolar.Repository.AccesoRepository;
import com.example.Escolar.Repository.AnioEscolarRepository;
import com.example.Escolar.Repository.AsignacionRepository;
import com.example.Escolar.Repository.ConfiguracionAcademicaRepository;
import com.example.Escolar.Repository.NivelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Hora academica, escala de calificacion y periodos por año y nivel. Un año sin
 * configuracion guardada hereda la del año anterior de ese nivel, y si no hay
 * ninguna usa los valores de siempre: inicial libre, primaria y secundaria 45
 * minutos, escala literal y bimestres. Las fechas de los periodos no se heredan.
 */
@Service
@RequiredArgsConstructor
public class ConfiguracionAcademicaService {

    /** Hora pedagogica del MINEDU para primaria y secundaria. */
    public static final int MINUTOS_POR_DEFECTO = 45;
    public static final int MINUTOS_MINIMO = 30;
    public static final int MINUTOS_MAXIMO = 60;

    private final ConfiguracionAcademicaRepository configuracionRepository;
    private final AnioEscolarRepository anioEscolarRepository;
    private final NivelRepository nivelRepository;
    private final AsignacionRepository asignacionRepository;
    private final AccesoRepository accesoRepository;
    private final PeriodoEvaluacionRepository periodoRepository;

    /** Una fila por nivel, con la duracion vigente y de donde sale. */
    @Transactional(readOnly = true)
    public List<ConfiguracionAcademicaResponse> listar(Integer idAnio) {
        AnioEscolar anio = findAnio(idAnio);
        return nivelRepository.findByAccesoNot(accesoEliminado()).stream()
                .sorted(Comparator.comparing(Nivel::getIdNivel))
                .map(nivel -> resolver(anio, nivel))
                .toList();
    }

    @Transactional(readOnly = true)
    public ConfiguracionAcademicaResponse obtener(Integer idAnio, Integer idNivel) {
        return resolver(findAnio(idAnio), findNivel(idNivel));
    }

    /** Minutos de la hora academica para validar horarios; null: libre. */
    @Transactional(readOnly = true)
    public Integer minutosHoraAcademica(AnioEscolar anio, Nivel nivel) {
        return resolver(anio, nivel).getMinutosHoraAcademica();
    }

    /** Periodos guardados de un nivel en un año, en orden. */
    @Transactional(readOnly = true)
    public List<PeriodoEvaluacion> periodos(AnioEscolar anio, Nivel nivel) {
        return periodoRepository.findByAnioEscolarIdAnioAndNivelIdNivelOrderByNumero(anio.getIdAnio(), nivel.getIdNivel());
    }

    /**
     * Todo se edita en un año por comenzar. Vigente y cerrado quedan de solo
     * lectura, salvo un vigente que aun no tiene periodos: se configura una vez.
     * La hora academica tampoco cambia si el nivel ya tiene asignaciones.
     */
    @Transactional
    public ConfiguracionAcademicaResponse guardar(ConfiguracionAcademicaRequest request) {
        AnioEscolar anio = findAnio(request.getIdAnio());
        Nivel nivel = findNivel(request.getIdNivel());
        String bloqueo = motivoBloqueo(anio, nivel);
        if (bloqueo != null) {
            throw new IllegalArgumentException(bloqueo);
        }
        ConfiguracionAcademicaResponse actual = resolver(anio, nivel);

        Integer minutos = request.getMinutosHoraAcademica();
        boolean cambianMinutos = !Objects.equals(actual.getMinutosHoraAcademica(), minutos);
        if (cambianMinutos) {
            if (anio.getEstado() != AnioEscolarService.ESTADO_POR_COMENZAR) {
                throw new IllegalArgumentException("El año " + anio.getAnio()
                        + " está vigente: la hora académica ya no se puede cambiar");
            }
            if (minutos != null && (minutos < MINUTOS_MINIMO || minutos > MINUTOS_MAXIMO || minutos % 5 != 0)) {
                throw new IllegalArgumentException("La hora académica debe durar entre " + MINUTOS_MINIMO + " y "
                        + MINUTOS_MAXIMO + " minutos, de 5 en 5");
            }
            if (actual.getAsignaciones() > 0) {
                throw new IllegalArgumentException(nivel.getNombre() + " ya tiene " + actual.getAsignaciones()
                        + (actual.getAsignaciones() == 1 ? " asignación" : " asignaciones")
                        + " en el año " + anio.getAnio() + ": sus horarios se armaron con la duración actual");
            }
        }

        EscalaCalificacion escala = request.getEscala() != null ? request.getEscala() : actual.getEscala();
        TipoPeriodo tipo = request.getTipoPeriodo() != null ? request.getTipoPeriodo() : actual.getTipoPeriodo();
        if (request.getPeriodos() != null) {
            validarPeriodos(anio, tipo, request.getPeriodos());
        }

        ConfiguracionAcademica configuracion = fila(anio, nivel, actual);
        configuracion.setMinutosHoraAcademica(minutos);
        configuracion.setEscala(escala);
        configuracion.setTipoPeriodo(tipo);
        configuracionRepository.save(configuracion);

        if (request.getPeriodos() != null || tipo != actual.getTipoPeriodo()) {
            reemplazarPeriodos(anio, nivel, tipo, request.getPeriodos());
        }
        return resolver(anio, nivel);
    }

    /** La guardada, o una nueva que conserva lo que el nivel tenia heredado. */
    private ConfiguracionAcademica fila(AnioEscolar anio, Nivel nivel, ConfiguracionAcademicaResponse actual) {
        return configuracionRepository
                .findByAnioEscolarIdAnioAndNivelIdNivelAndAccesoNot(anio.getIdAnio(), nivel.getIdNivel(), accesoEliminado())
                .orElseGet(() -> {
                    ConfiguracionAcademica nueva = new ConfiguracionAcademica();
                    nueva.setAnioEscolar(anio);
                    nueva.setNivel(nivel);
                    nueva.setMinutosHoraAcademica(actual.getMinutosHoraAcademica());
                    nueva.setEscala(actual.getEscala());
                    nueva.setTipoPeriodo(actual.getTipoPeriodo());
                    nueva.setAcceso(accesoRepository.findById(AccesoConstants.ACTIVO).orElseThrow());
                    return nueva;
                });
    }

    /** Completos, en orden, sin solaparse y dentro de las fechas del año. */
    private void validarPeriodos(AnioEscolar anio, TipoPeriodo tipo,
                                 List<ConfiguracionAcademicaRequest.PeriodoRequest> periodos) {
        if (anio.getFechaInicio() == null || anio.getFechaFin() == null) {
            throw new IllegalArgumentException("Define primero las fechas de inicio y fin del año " + anio.getAnio());
        }
        if (periodos.size() != tipo.getCantidad()) {
            throw new IllegalArgumentException("Se necesitan " + tipo.getCantidad() + " periodos de tipo "
                    + tipo.nombre(1).split(" ")[1]);
        }
        LocalDate finAnterior = null;
        for (int i = 0; i < periodos.size(); i++) {
            var periodo = periodos.get(i);
            String nombre = tipo.nombre(i + 1);
            if (periodo.fechaInicio() == null || periodo.fechaFin() == null) {
                throw new IllegalArgumentException("Indica las fechas del " + nombre.toLowerCase());
            }
            if (periodo.fechaFin().isBefore(periodo.fechaInicio())) {
                throw new IllegalArgumentException("El " + nombre.toLowerCase() + " termina antes de empezar");
            }
            if (periodo.fechaInicio().isBefore(anio.getFechaInicio()) || periodo.fechaFin().isAfter(anio.getFechaFin())) {
                throw new IllegalArgumentException("El " + nombre.toLowerCase() + " debe estar dentro del año escolar ("
                        + anio.getFechaInicio().format(PeriodoEvaluacion.FECHA) + " al "
                        + anio.getFechaFin().format(PeriodoEvaluacion.FECHA) + ")");
            }
            if (finAnterior != null && !periodo.fechaInicio().isAfter(finAnterior)) {
                throw new IllegalArgumentException("El " + nombre.toLowerCase()
                        + " debe empezar después de que termine el anterior");
            }
            finAnterior = periodo.fechaFin();
        }
    }

    private void reemplazarPeriodos(AnioEscolar anio, Nivel nivel, TipoPeriodo tipo,
                                    List<ConfiguracionAcademicaRequest.PeriodoRequest> periodos) {
        periodoRepository.deleteAll(periodos(anio, nivel));
        // Sin flush, los nuevos chocan con la clave única de los que se borran.
        periodoRepository.flush();
        if (periodos == null) {
            return;
        }
        for (int i = 0; i < periodos.size(); i++) {
            PeriodoEvaluacion periodo = new PeriodoEvaluacion();
            periodo.setAnioEscolar(anio);
            periodo.setNivel(nivel);
            periodo.setNumero((byte) (i + 1));
            periodo.setFechaInicio(periodos.get(i).fechaInicio());
            periodo.setFechaFin(periodos.get(i).fechaFin());
            periodoRepository.save(periodo);
        }
    }

    /** Null si se puede guardar. */
    private String motivoBloqueo(AnioEscolar anio, Nivel nivel) {
        if (anio.getEstado() == AnioEscolarService.ESTADO_POR_COMENZAR) {
            return null;
        }
        if (anio.getEstado() == AnioEscolarService.ESTADO_VIGENTE
                && periodoRepository.countByAnioEscolarIdAnioAndNivelIdNivel(anio.getIdAnio(), nivel.getIdNivel()) == 0) {
            return null;
        }
        return "El año " + anio.getAnio() + " está "
                + (anio.getEstado() == AnioEscolarService.ESTADO_VIGENTE ? "vigente" : "cerrado")
                + ": su configuración académica ya no se puede cambiar";
    }

    private ConfiguracionAcademicaResponse resolver(AnioEscolar anio, Nivel nivel) {
        Acceso eliminado = accesoEliminado();
        ConfiguracionAcademicaResponse response = new ConfiguracionAcademicaResponse();
        response.setIdAnio(anio.getIdAnio());
        response.setAnio(anio.getAnio());
        response.setIdNivel(nivel.getIdNivel());
        response.setNivel(nivel.getNombre());
        response.setAsignaciones(asignacionRepository.countByAnioEscolarIdAnioAndGradoSeccionGradoNivelIdNivelAndAccesoNot(
                anio.getIdAnio(), nivel.getIdNivel(), eliminado));
        LocalDate hoy = LocalDate.now();
        List<PeriodoEvaluacion> guardados = periodos(anio, nivel);
        response.setPeriodos(guardados.stream()
                .map(p -> new PeriodoEvaluacionResponse(p.getNumero(), tipoDe(p, guardados).nombre(p.getNumero()),
                        p.getFechaInicio(), p.getFechaFin(), p.estadoEn(hoy)))
                .toList());
        response.setMotivoBloqueo(motivoBloqueo(anio, nivel));
        response.setEditable(response.getMotivoBloqueo() == null);

        Optional<ConfiguracionAcademica> propia = configuracionRepository
                .findByAnioEscolarIdAnioAndNivelIdNivelAndAccesoNot(anio.getIdAnio(), nivel.getIdNivel(), eliminado);
        if (propia.isPresent()) {
            aplicarValores(response, propia.get());
            response.setOrigen("CONFIGURADO");
            return response;
        }
        // La del año anterior mas cercano de ese nivel.
        Optional<ConfiguracionAcademica> anterior = configuracionRepository
                .findByNivelIdNivelAndAccesoNot(nivel.getIdNivel(), eliminado).stream()
                .filter(c -> c.getAnioEscolar().getAnio().compareTo(anio.getAnio()) < 0)
                .max(Comparator.comparing(c -> c.getAnioEscolar().getAnio()));
        if (anterior.isPresent()) {
            aplicarValores(response, anterior.get());
            response.setOrigen("HEREDADO");
            response.setAnioOrigen(anterior.get().getAnioEscolar().getAnio());
            return response;
        }
        // Inicial se organiza por actividades: sin bloques fijos.
        boolean inicial = PlantillaCursosParser.clave(nivel.getNombre()).equals("INICIAL");
        response.setMinutosHoraAcademica(inicial ? null : MINUTOS_POR_DEFECTO);
        response.setEscala(EscalaCalificacion.LITERAL);
        response.setTipoPeriodo(TipoPeriodo.BIMESTRE);
        response.setOrigen("DEFECTO");
        return response;
    }

    private static void aplicarValores(ConfiguracionAcademicaResponse response, ConfiguracionAcademica fila) {
        response.setMinutosHoraAcademica(fila.getMinutosHoraAcademica());
        response.setEscala(fila.getEscala() != null ? fila.getEscala() : EscalaCalificacion.LITERAL);
        response.setTipoPeriodo(fila.getTipoPeriodo() != null ? fila.getTipoPeriodo() : TipoPeriodo.BIMESTRE);
    }

    /** Por su cantidad: 3 periodos guardados son trimestres. */
    private static TipoPeriodo tipoDe(PeriodoEvaluacion periodo, List<PeriodoEvaluacion> todos) {
        return todos.size() == TipoPeriodo.TRIMESTRE.getCantidad() ? TipoPeriodo.TRIMESTRE : TipoPeriodo.BIMESTRE;
    }

    private AnioEscolar findAnio(Integer idAnio) {
        return anioEscolarRepository.findByIdAnioAndAccesoNot(idAnio, accesoEliminado())
                .orElseThrow(() -> new ResourceNotFoundException("Año escolar no encontrado con id " + idAnio));
    }

    private Nivel findNivel(Integer idNivel) {
        return nivelRepository.findByIdNivelAndAccesoNot(idNivel, accesoEliminado())
                .orElseThrow(() -> new ResourceNotFoundException("Nivel no encontrado con id " + idNivel));
    }

    private Acceso accesoEliminado() {
        return accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow();
    }
}
