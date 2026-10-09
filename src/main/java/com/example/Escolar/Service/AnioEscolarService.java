package com.example.Escolar.Service;

import com.example.Escolar.Dto.AnioEscolarRequest;
import com.example.Escolar.Dto.AnioEscolarResponse;
import com.example.Escolar.Exception.AnioEscolarConGradosException;
import com.example.Escolar.Exception.ResourceNotFoundException;
import com.example.Escolar.Model.Acceso;
import com.example.Escolar.Model.AnioEscolar;
import com.example.Escolar.Repository.AccesoRepository;
import com.example.Escolar.Repository.AnioEscolarRepository;
import com.example.Escolar.Repository.AsignacionRepository;
import com.example.Escolar.Repository.GradoSeccionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AnioEscolarService {

    // El ciclo de un año lectivo tiene tres estados. CERRADO es terminal: bloquea
    // la edicion de lo ya concluido y solo se alcanza por fecha.
    public static final byte ESTADO_VIGENTE = 1;
    public static final byte ESTADO_CERRADO = 2;
    // Permite convivir un año futuro con el vigente: habilita matriculas
    // anticipadas sin cerrar el año que ya esta en curso.
    public static final byte ESTADO_POR_COMENZAR = 3;

    // Un año en cualquiera de estos dos estados se cierra solo al pasar su
    // fecha de fin. CERRADO queda fuera porque ya termino.
    private static final List<Byte> ESTADOS_CERRABLES = List.of(ESTADO_VIGENTE, ESTADO_POR_COMENZAR);

    private final AnioEscolarRepository anioEscolarRepository;
    private final AccesoRepository accesoRepository;
    private final GradoSeccionRepository gradoSeccionRepository;
    private final AsignacionRepository asignacionRepository;

    // Transaccional porque antes de leer cierra los años vencidos. La anotacion
    // va en el metodo publico y no en cerrarAniosVencidos: al llamarse desde
    // dentro de la misma clase pasaria por "this" y Spring no aplicaria proxy.
    @Transactional
    public List<AnioEscolarResponse> getAll() {
        cerrarAniosVencidos();
        return anioEscolarRepository.findByAccesoNot(accesoEliminado()).stream()
                .map(this::toResponse)
                .toList();
    }

    public AnioEscolarResponse getById(Integer id) {
        return toResponse(findAnio(id));
    }

    @Transactional
    public AnioEscolarResponse create(AnioEscolarRequest request) {
        validarVentanaDeAnio(request.getAnio());
        validarAnioUnico(request.getAnio(), null);
        validarSinAnioPorComenzar();
        validarFechas(request.getFechaInicio(), request.getFechaFin());
        // Por defecto nace por comenzar. Crear un año nunca debe tumbar al
        // vigente: pasarlo a vigente es una transicion posterior y consciente.
        byte estado = request.getEstado() == null ? ESTADO_POR_COMENZAR : request.getEstado();
        validarFechaFinSiVigente(estado, request.getFechaFin());
        if (estado == ESTADO_VIGENTE) {
            validarVigenteSegunCalendario(request.getAnio());
        }

        AnioEscolar anioEscolar = new AnioEscolar();
        anioEscolar.setAnio(request.getAnio().trim());
        anioEscolar.setEstado(estado);
        anioEscolar.setFechaInicio(request.getFechaInicio());
        anioEscolar.setFechaFin(request.getFechaFin());
        anioEscolar.setBloqueoHorariosPorFecha(
            request.getBloqueoHorariosPorFecha() == null ? true : request.getBloqueoHorariosPorFecha()
        );
        anioEscolar.setAcceso(accesoRepository.findById(request.getAccesoId() == null ? AccesoConstants.ACTIVO : request.getAccesoId()).orElseThrow());
        if (estado == ESTADO_VIGENTE) {
            cerrarOtrosVigentes(null);
        }
        return toResponse(anioEscolarRepository.save(anioEscolar));
    }

    @Transactional
    public AnioEscolarResponse update(Integer id, AnioEscolarRequest request) {
        AnioEscolar anioEscolar = findAnio(id);
        // El cerrado congela todo el anio, no solo el estado. El guard va antes de
        // tocar cualquier campo porque un PUT puede llegar sin estado (peticion
        // parcial) y en ese caso validarTransicion no se ejecuta.
        if (anioEscolar.getEstado() == ESTADO_CERRADO) {
            throw new IllegalArgumentException("El año está cerrado y no se puede editar");
        }
        // Se captura antes de aplicar request.estado: la congelación aplica al
        // año que ya era vigente al entrar, no al que la misma petición promueve.
        boolean eraVigente = anioEscolar.getEstado() == ESTADO_VIGENTE;
        if (request.getAnio() != null && !request.getAnio().isBlank()) {
            validarAnioUnico(request.getAnio(), id);
            anioEscolar.setAnio(request.getAnio().trim());
        }
        if (request.getEstado() != null) {
            validarTransicion(anioEscolar.getEstado(), request.getEstado());
            anioEscolar.setEstado(request.getEstado());
            if (request.getEstado() == ESTADO_VIGENTE) {
                cerrarOtrosVigentes(id);
            }
        }
        if (request.getFechaInicio() != null) {
            validarFechaInicioCongelada(eraVigente, anioEscolar.getFechaInicio(), request.getFechaInicio());
            anioEscolar.setFechaInicio(request.getFechaInicio());
        }
        if (request.getFechaFin() != null) {
            anioEscolar.setFechaFin(request.getFechaFin());
        }
        if (request.getBloqueoHorariosPorFecha() != null) {
            anioEscolar.setBloqueoHorariosPorFecha(request.getBloqueoHorariosPorFecha());
        }
        if (request.getAccesoId() != null) {
            anioEscolar.setAcceso(accesoRepository.findById(request.getAccesoId()).orElseThrow());
        }
        // Validar fechas despues de aplicar cambios
        validarFechas(anioEscolar.getFechaInicio(), anioEscolar.getFechaFin());
        validarFechaFinSiVigente(anioEscolar.getEstado(), anioEscolar.getFechaFin());
        if (anioEscolar.getEstado() == ESTADO_VIGENTE) {
            validarVigenteSegunCalendario(anioEscolar.getAnio());
        }
        return toResponse(anioEscolarRepository.save(anioEscolar));
    }

    // Transicion de estado aislada. Va aparte de update para que el cliente la
    // promueva desde la fila de la tabla sin mandar el formulario entero, y sin
    // arriesgarse a pisar fechas o nombre en el camino.
    @Transactional
    public AnioEscolarResponse cambiarEstado(Integer id, Byte estado) {
        AnioEscolar anioEscolar = findAnio(id);
        validarTransicion(anioEscolar.getEstado(), estado);
        validarFechaFinSiVigente(estado, anioEscolar.getFechaFin());
        if (estado == ESTADO_VIGENTE) {
            validarVigenteSegunCalendario(anioEscolar.getAnio());
        }

        anioEscolar.setEstado(estado);
        if (estado == ESTADO_VIGENTE) {
            cerrarOtrosVigentes(id);
        }
        return toResponse(anioEscolarRepository.save(anioEscolar));
    }

    // Borrado fisico y no logico, a diferencia del resto del sistema. La razon
    // es que anio tiene unique=true en la base: si la fila solo se marcara como
    // eliminada seguiria ocupando el valor y el año no podria volver a crearse,
    // que es justo lo que se necesita cuando se creo por error y no tiene nada
    // asociado. Las claves foráneas de gradoSeccion y asignacion apuntan aqui,
    // asi que revisar ambas es lo que protege la integridad.
    @Transactional
    public void delete(Integer id) {
        AnioEscolar anioEscolar = findAnio(id);
        Acceso eliminado = accesoEliminado();
        long grados = gradoSeccionRepository.countByAnioEscolarIdAnioAndAccesoNot(id, eliminado);
        long asignaciones = asignacionRepository.countByAnioEscolarIdAnioAndAccesoNot(id, eliminado);

        if (grados > 0 || asignaciones > 0) {
            List<String> detalles = new ArrayList<>();
            if (grados > 0) {
                detalles.add(grados + (grados == 1 ? " grado" : " grados"));
            }
            if (asignaciones > 0) {
                detalles.add(asignaciones + (asignaciones == 1 ? " asignación" : " asignaciones"));
            }
            throw new AnioEscolarConGradosException(
                "No se puede eliminar el año " + anioEscolar.getAnio()
                    + " porque tiene " + String.join(" y ", detalles) + " asociados"
            );
        }
        anioEscolarRepository.delete(anioEscolar);
    }

    // Cierra todo año cuya fecha de fin ya paso. Cubre vigente y por comenzar,
    // no solo vigente: si alguien edito la fecha de fin de un año que aun no
    // empezaba, asi es como tambien se cierra.
    // No dispara cerrarOtrosVigentes a proposito: el cierre por fecha es una
    // consecuencia del calendario, no un relevo de manos.
    private void cerrarAniosVencidos() {
        List<AnioEscolar> vencidos = anioEscolarRepository.findByEstadoInAndFechaFinBeforeAndAccesoNot(
                ESTADOS_CERRABLES, LocalDate.now(), accesoEliminado()
        );
        if (vencidos.isEmpty()) {
            return;
        }
        vencidos.forEach(a -> a.setEstado(ESTADO_CERRADO));
        anioEscolarRepository.saveAll(vencidos);
    }

    private void cerrarOtrosVigentes(Integer idExcluir) {
        anioEscolarRepository.findByAccesoNot(accesoEliminado()).stream()
                .filter(a -> a.getEstado() == ESTADO_VIGENTE)
                .filter(a -> idExcluir == null || !a.getIdAnio().equals(idExcluir))
                .forEach(a -> a.setEstado(ESTADO_CERRADO));
    }

    private void validarTransicion(byte actual, byte nuevo) {
        if (actual == nuevo) {
            return;
        }
        if (actual == ESTADO_CERRADO) {
            throw new IllegalArgumentException(
                "El año está cerrado y no admite cambios de estado"
            );
        }
        if (nuevo == ESTADO_POR_COMENZAR) {
            throw new IllegalArgumentException(
                "No se puede volver a 'por comenzar' un año que ya está vigente"
            );
        }
    }

    // Sin fecha de fin un vigente no cerraria nunca: el barrido solo mira
    // fechas no nulas, asi que se quedaria abierto indefinidamente.
    private void validarFechaFinSiVigente(byte estado, LocalDate fechaFin) {
        if (estado == ESTADO_VIGENTE && fechaFin == null) {
            throw new IllegalArgumentException(
                "Un año vigente necesita fecha de fin para poder cerrarse solo"
            );
        }
    }

    // Mientras exista un año esperando en por comenzar no se admite crear otro:
    // el siguiente se registra recien cuando el pendiente pasa a vigente. Sin
    // esta regla se acumularian por comenzar sin uso y la ventana de dos años
    // dejaria al sistema con dos ciclos planificados a la vez.
    private void validarSinAnioPorComenzar() {
        anioEscolarRepository.findByEstadoInAndAccesoNot(List.of(ESTADO_POR_COMENZAR), accesoEliminado())
                .stream()
                .map(AnioEscolar::getAnio)
                .min(Comparator.naturalOrder())
                .ifPresent(pendiente -> {
                    throw new IllegalArgumentException(
                        "No se puede crear un nuevo año mientras el año " + pendiente
                            + " siga en estado 'por comenzar'. Cámbialo a vigente para poder crear el siguiente"
                    );
                });
    }

    // Un año solo se activa como vigente cuando su valor ya llego en el
    // calendario: en diciembre de 2026 no se pone vigente el 2027, aunque
    // exista y tenga sus fechas cargadas.
    private void validarVigenteSegunCalendario(String anio) {
        int valor = Integer.parseInt(anio.trim());
        int actual = LocalDate.now().getYear();
        if (valor > actual) {
            throw new IllegalArgumentException(
                "El año " + anio.trim() + " solo se puede activar como vigente cuando la fecha calendario llegue al "
                    + anio.trim()
            );
        }
    }

    // La fecha de inicio de un vigente es historia: marca el periodo en el que
    // ya se atendio matricula. Se congela al llegar a VIGENTE y no se vuelve a
    // tocar. Compara valor y no presencia porque el formulario siempre reenvia
    // la fecha, incluso con el selector deshabilitado.
    private void validarFechaInicioCongelada(boolean eraVigente, LocalDate actual, LocalDate nueva) {
        if (!eraVigente || nueva.equals(actual)) {
            return;
        }
        throw new IllegalArgumentException("El año vigente no admite cambios en su fecha de inicio");
    }

    // Solo se admiten el año en curso y el siguiente: los años futuros se
    // planifican cuando corresponde y los pasados no se crean, se corrigen
    // sobre el registro que ya existe.
    //
    // La ventana se deriva del calendario en vez de fijarse en el codigo para
    // que en enero se corra sola a [2027, 2028] sin tocar nada. Y es una ventana
    // sobre el valor de `anio`, no un tope de filas: los cerrados historicos se
    // acumulan sin estorbar, porque el borrado de un año con grados o
    // asignaciones esta bloqueado y un tope dejaria al sistema atrapado.
    //
    // Solo se valida al crear. Si update() lo validara, en enero un año
    // POR_COMENZAR del año anterior quedaria inmodificable para siempre.
    private void validarVentanaDeAnio(String anio) {
        int valor = Integer.parseInt(anio.trim());
        int actual = LocalDate.now().getYear();
        if (valor < actual || valor > actual + 1) {
            throw new IllegalArgumentException(
                "Solo se pueden registrar el año " + actual + " y el " + (actual + 1)
            );
        }
    }

    private void validarAnioUnico(String anio, Integer idExcluir) {
        if (anio == null || anio.isBlank()) {
            return;
        }
        anioEscolarRepository.findByAnioAndAccesoNot(anio.trim(), accesoEliminado())
                .filter(a -> idExcluir == null || !a.getIdAnio().equals(idExcluir))
                .ifPresent(a -> {
                    throw new IllegalArgumentException("Ya existe un año escolar con ese año");
                });
    }

    private AnioEscolar findAnio(Integer id) {
        return anioEscolarRepository.findByIdAnioAndAccesoNot(id, accesoEliminado())
                .orElseThrow(() -> new ResourceNotFoundException("Año escolar no encontrado con id " + id));
    }

    private void validarFechas(LocalDate fechaInicio, LocalDate fechaFin) {
        if (fechaInicio != null && fechaFin != null) {
            if (!fechaFin.isAfter(fechaInicio)) {
                throw new IllegalArgumentException("La fecha de fin debe ser posterior a la fecha de inicio");
            }
        }
    }

    private Acceso accesoEliminado() {
        return accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow();
    }

    private AnioEscolarResponse toResponse(AnioEscolar anioEscolar) {
        AnioEscolarResponse response = new AnioEscolarResponse();
        response.setIdAnio(anioEscolar.getIdAnio());
        response.setAnio(anioEscolar.getAnio());
        response.setEstado(anioEscolar.getEstado());
        response.setFechaInicio(anioEscolar.getFechaInicio());
        response.setFechaFin(anioEscolar.getFechaFin());
        response.setBloqueoHorariosPorFecha(anioEscolar.getBloqueoHorariosPorFecha());
        response.setAccesoId(anioEscolar.getAcceso().getIdAcceso().longValue());
        return response;
    }
}