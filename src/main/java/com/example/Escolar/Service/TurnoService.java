package com.example.Escolar.Service;

import com.example.Escolar.Dto.TurnoRequest;
import com.example.Escolar.Dto.TurnoResponse;
import com.example.Escolar.Exception.ResourceNotFoundException;
import com.example.Escolar.Model.Turno;
import com.example.Escolar.Repository.AccesoRepository;
import com.example.Escolar.Repository.TurnoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TurnoService {

    private static final DateTimeFormatter HORA_CORTA = DateTimeFormatter.ofPattern("HH:mm");

    private final TurnoRepository turnoRepository;
    private final AccesoRepository accesoRepository;

    public List<TurnoResponse> getAll() {
        return turnoRepository.findByAccesoNot(accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow()).stream()
                .map(this::toResponse)
                .toList();
    }

    public TurnoResponse getById(Integer id) {
        return toResponse(findTurno(id));
    }

    @Transactional
    public TurnoResponse create(TurnoRequest request) {
        validarNombreUnico(request.getNombre(), null);
        validarOrdenHoras(request);
        // Todavía no tiene id: se ubica al final de los que empiezan a la misma
        // hora, que es donde va a caer cuando se guarde y reciba su id.
        validarSolapamiento(request, null, Integer.MAX_VALUE);
        Turno turno = new Turno();
        turno.setNombre(request.getNombre().trim());
        aplicarDatos(turno, request);
        turno.setAcceso(accesoRepository.findById(request.getAccesoId() == null ? AccesoConstants.ACTIVO : request.getAccesoId()).orElseThrow());
        return toResponse(turnoRepository.save(turno));
    }

    @Transactional
    public TurnoResponse update(Integer id, TurnoRequest request) {
        Turno turno = findTurno(id);
        if (request.getNombre() != null && !request.getNombre().isBlank()) {
            validarNombreUnico(request.getNombre(), id);
            turno.setNombre(request.getNombre().trim());
        }
        validarOrdenHoras(request);
        // El propio turno sale de la cadena para no compararse consigo mismo, y
        // entra con las horas que se van a guardar, no con las que tiene ahora.
        validarSolapamiento(request, id, id);
        aplicarDatos(turno, request);
        if (request.getAccesoId() != null) {
            turno.setAcceso(accesoRepository.findById(request.getAccesoId()).orElseThrow());
        }
        return toResponse(turnoRepository.save(turno));
    }

    @Transactional
    public void delete(Integer id) {
        Turno turno = findTurno(id);
        turno.setAcceso(accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow());
        turnoRepository.save(turno);
    }

    private void aplicarDatos(Turno turno, TurnoRequest request) {
        turno.setHoraEntrada(request.getHoraEntrada());
        turno.setHoraEntradaLimite(request.getHoraEntradaLimite());
        turno.setHoraFaltaLimite(request.getHoraFaltaLimite());
        turno.setHoraSalida(request.getHoraSalida());
    }

    /**
     * Las cuatro horas de un turno van en cadena: entrada, limite de entrada,
     * limite de falta y salida. El turno se guarda completo o no se guarda.
     */
    private void validarOrdenHoras(TurnoRequest request) {
        validarOrden(request.getHoraEntrada(), request.getHoraEntradaLimite(),
                "El limite de puntualidad debe ser mayor a la hora de entrada");
        validarOrden(request.getHoraEntradaLimite(), request.getHoraFaltaLimite(),
                "El limite de tardanza debe ser mayor al limite de puntualidad");
        validarOrden(request.getHoraFaltaLimite(), request.getHoraSalida(),
                "La hora de salida debe ser mayor al limite de tardanza");
    }

    private void validarOrden(LocalTime anterior, LocalTime siguiente, String mensaje) {
        // TurnoRequest ya exige las cuatro horas, asi que no hace falta cubrir
        // el caso de que falte alguna.
        if (!anterior.isBefore(siguiente)) {
            throw new IllegalArgumentException(mensaje);
        }
    }

    /**
     * Un turno solo se cruza con el que lo precede en la jornada, nunca con los
     * que vienen despues: esos se validan contra el, no al reves. Por eso el
     * primero de la cadena siempre pasa, y por eso editar el turno de la manana
     * nunca falla por un turno posterior.
     *
     * El empate esta permitido, representa el cambio de turno: manana sale a las
     * 12:30 y la tarde entra a las 12:30 es una configuracion valida.
     */
    private void validarSolapamiento(TurnoRequest request, Integer idExcluir, Integer idPropio) {
        List<Turno> cadena = turnoRepository
                .findByAccesoNot(accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow())
                .stream()
                .filter(t -> idExcluir == null || !t.getIdTurno().equals(idExcluir))
                .sorted(Comparator.comparing(Turno::getHoraEntrada).thenComparing(Turno::getIdTurno))
                .collect(Collectors.toCollection(ArrayList::new));

        Turno candidato = new Turno();
        candidato.setIdTurno(idPropio);
        candidato.setNombre(request.getNombre() == null ? "" : request.getNombre().trim());
        candidato.setHoraEntrada(request.getHoraEntrada());
        candidato.setHoraSalida(request.getHoraSalida());

        // El empate por hora de entrada se resuelve con el id, asi que el
        // candidato entra en la posicion que le corresponde segun el suyo.
        int insercion = 0;
        while (insercion < cadena.size()
                && (cadena.get(insercion).getHoraEntrada().isBefore(candidato.getHoraEntrada())
                || (cadena.get(insercion).getHoraEntrada().equals(candidato.getHoraEntrada())
                && cadena.get(insercion).getIdTurno() < candidato.getIdTurno()))) {
            insercion++;
        }
        if (insercion == 0) {
            return;
        }

        Turno anterior = cadena.get(insercion - 1);
        if (candidato.getHoraEntrada().isBefore(anterior.getHoraSalida())) {
            throw new IllegalArgumentException("No puede iniciar antes de que termine el turno "
                    + anterior.getNombre() + " (" + anterior.getHoraSalida().format(HORA_CORTA) + ")");
        }
    }

    private void validarNombreUnico(String nombre, Integer idExcluir) {
        if (nombre == null || nombre.isBlank()) {
            return;
        }
        turnoRepository.findByNombreAndAccesoNot(nombre.trim(), accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow())
                .filter(t -> idExcluir == null || !t.getIdTurno().equals(idExcluir))
                .ifPresent(t -> {
                    throw new IllegalArgumentException("Ya existe un turno con ese nombre");
                });
    }

    private Turno findTurno(Integer id) {
        return turnoRepository.findByIdTurnoAndAccesoNot(id, accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow())
                .orElseThrow(() -> new ResourceNotFoundException("Turno no encontrado con id " + id));
    }

    private TurnoResponse toResponse(Turno turno) {
        TurnoResponse response = new TurnoResponse();
        response.setIdTurno(turno.getIdTurno());
        response.setNombre(turno.getNombre());
        response.setHoraEntrada(turno.getHoraEntrada());
        response.setHoraEntradaLimite(turno.getHoraEntradaLimite());
        response.setHoraFaltaLimite(turno.getHoraFaltaLimite());
        response.setHoraSalida(turno.getHoraSalida());
        response.setAccesoId(turno.getAcceso().getIdAcceso().longValue());
        return response;
    }
}
