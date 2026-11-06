package com.example.Escolar.Service;

import com.example.Escolar.Dto.RecreoRequest;
import com.example.Escolar.Dto.RecreoResponse;
import com.example.Escolar.Dto.RecreoUpdateRequest;
import com.example.Escolar.Exception.ResourceNotFoundException;
import com.example.Escolar.Model.Acceso;
import com.example.Escolar.Model.AnioEscolar;
import com.example.Escolar.Model.Grado;
import com.example.Escolar.Model.GradoSeccion;
import com.example.Escolar.Model.HorarioClase;
import com.example.Escolar.Model.Nivel;
import com.example.Escolar.Model.Recreo;
import com.example.Escolar.Model.Turno;
import com.example.Escolar.Repository.AccesoRepository;
import com.example.Escolar.Repository.AnioEscolarRepository;
import com.example.Escolar.Repository.GradoRepository;
import com.example.Escolar.Repository.GradoSeccionRepository;
import com.example.Escolar.Repository.HorarioClaseRepository;
import com.example.Escolar.Repository.NivelRepository;
import com.example.Escolar.Repository.RecreoRepository;
import com.example.Escolar.Repository.TurnoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Recreos por grado y turno: aplican a todas las secciones del grado en ese
 * turno. Las asignaciones exigen recreo cada dia que tienen clase, asi que un
 * recreo no se puede quitar si deja un dia con clases sin recreo.
 */
@Service
@RequiredArgsConstructor
public class RecreoService {

    private static final String QUE = "recreos";

    private final RecreoRepository recreoRepository;
    private final AnioEscolarRepository anioEscolarRepository;
    private final NivelRepository nivelRepository;
    private final TurnoRepository turnoRepository;
    private final GradoRepository gradoRepository;
    private final GradoSeccionRepository gradoSeccionRepository;
    private final HorarioClaseRepository horarioClaseRepository;
    private final AccesoRepository accesoRepository;

    public List<RecreoResponse> getAll(Integer idAnio, Integer idNivel, Integer idTurno) {
        return recreoRepository.findByAccesoNot(accesoEliminado()).stream()
                .filter(r -> idAnio == null || r.getAnioEscolar().getIdAnio().equals(idAnio))
                .filter(r -> idNivel == null || r.getNivel().getIdNivel().equals(idNivel))
                .filter(r -> idTurno == null || r.getTurno().getIdTurno().equals(idTurno))
                .sorted(Comparator.comparing((Recreo r) -> r.getGrado().getIdGrado())
                        .thenComparing(Recreo::getDiaSemana)
                        .thenComparing(Recreo::getHoraInicio))
                .map(this::toResponse)
                .toList();
    }

    public RecreoResponse getById(Integer id) {
        return toResponse(findRecreo(id));
    }

    /** Crea un recreo por cada combinacion de grado, dia y franja, todos o ninguno. */
    @Transactional
    public List<RecreoResponse> create(RecreoRequest request) {
        AnioEscolar anio = findAnio(request.getIdAnio());
        Nivel nivel = findNivel(request.getIdNivel());
        Turno turno = findTurno(request.getIdTurno());

        Set<Byte> dias = new LinkedHashSet<>(request.getDias());
        dias.forEach(HorarioUtil::validarDia);
        List<RecreoRequest.Franja> franjas = request.getFranjas();
        for (int i = 0; i < franjas.size(); i++) {
            RecreoRequest.Franja a = franjas.get(i);
            HorarioUtil.validarDentroDelTurno(a.getHoraInicio(), a.getHoraFin(), turno);
            for (int j = 0; j < i; j++) {
                RecreoRequest.Franja b = franjas.get(j);
                if (HorarioUtil.seCruzan(a.getHoraInicio(), a.getHoraFin(), b.getHoraInicio(), b.getHoraFin())) {
                    throw new IllegalArgumentException("Los recreos " + HorarioUtil.rango(b.getHoraInicio(), b.getHoraFin())
                            + " y " + HorarioUtil.rango(a.getHoraInicio(), a.getHoraFin()) + " se cruzan");
                }
            }
        }

        List<Recreo> nuevos = new ArrayList<>();
        for (Integer idGrado : new LinkedHashSet<>(request.getIdGrados())) {
            Grado grado = findGrado(idGrado);
            if (!grado.getNivel().getIdNivel().equals(nivel.getIdNivel())) {
                throw new IllegalArgumentException("El grado " + grado.getNombre() + " no es del nivel " + nivel.getNombre());
            }
            if (seccionesDelGrado(anio, turno, grado).isEmpty()) {
                throw new IllegalArgumentException(grado.getNombre() + " no tiene secciones en el turno "
                        + turno.getNombre() + " del año " + anio.getAnio());
            }
            List<Recreo> existentes = recreosDelGrado(anio, turno, grado);
            // En un vigente solo se configura el grado que todavia no tiene recreos.
            ReglasAnioEscolar.validarCreable(anio, existentes.isEmpty(), QUE);
            List<HorarioClase> clases = clasesDelGrado(anio, turno, grado);

            for (Byte dia : dias) {
                for (RecreoRequest.Franja franja : franjas) {
                    validarSinCruces(grado, dia, franja.getHoraInicio(), franja.getHoraFin(), existentes, clases, null);
                    Recreo recreo = new Recreo();
                    recreo.setAnioEscolar(anio);
                    recreo.setNivel(nivel);
                    recreo.setTurno(turno);
                    recreo.setGrado(grado);
                    recreo.setDiaSemana(dia);
                    recreo.setHoraInicio(franja.getHoraInicio());
                    recreo.setHoraFin(franja.getHoraFin());
                    recreo.setAcceso(accesoActivo());
                    nuevos.add(recreo);
                }
            }
        }
        return recreoRepository.saveAll(nuevos).stream().map(this::toResponse).toList();
    }

    @Transactional
    public RecreoResponse update(Integer id, RecreoUpdateRequest request) {
        Recreo recreo = findRecreo(id);
        ReglasAnioEscolar.validarEditable(recreo.getAnioEscolar(), QUE);
        HorarioUtil.validarDia(request.getDiaSemana());
        HorarioUtil.validarDentroDelTurno(request.getHoraInicio(), request.getHoraFin(), recreo.getTurno());

        List<Recreo> existentes = recreosDelGrado(recreo.getAnioEscolar(), recreo.getTurno(), recreo.getGrado());
        List<HorarioClase> clases = clasesDelGrado(recreo.getAnioEscolar(), recreo.getTurno(), recreo.getGrado());
        validarSinCruces(recreo.getGrado(), request.getDiaSemana(), request.getHoraInicio(), request.getHoraFin(),
                existentes, clases, id);
        // Moverlo de dia es quitarlo del dia anterior.
        if (!request.getDiaSemana().equals(recreo.getDiaSemana())) {
            validarNoDejaDiaSinRecreo(recreo, existentes, clases);
        }

        recreo.setDiaSemana(request.getDiaSemana());
        recreo.setHoraInicio(request.getHoraInicio());
        recreo.setHoraFin(request.getHoraFin());
        return toResponse(recreoRepository.save(recreo));
    }

    @Transactional
    public void delete(Integer id) {
        Recreo recreo = findRecreo(id);
        ReglasAnioEscolar.validarEditable(recreo.getAnioEscolar(), QUE);
        validarNoDejaDiaSinRecreo(recreo,
                recreosDelGrado(recreo.getAnioEscolar(), recreo.getTurno(), recreo.getGrado()),
                clasesDelGrado(recreo.getAnioEscolar(), recreo.getTurno(), recreo.getGrado()));
        recreo.setAcceso(accesoEliminado());
        recreoRepository.save(recreo);
    }

    /** Quita todos los recreos del grado en ese turno; no si el grado ya tiene clases. */
    @Transactional
    public void deleteDelGrado(Integer idAnio, Integer idTurno, Integer idGrado) {
        AnioEscolar anio = findAnio(idAnio);
        Turno turno = findTurno(idTurno);
        Grado grado = findGrado(idGrado);
        ReglasAnioEscolar.validarEditable(anio, QUE);
        if (!clasesDelGrado(anio, turno, grado).isEmpty()) {
            throw new IllegalArgumentException("No se pueden quitar los recreos de " + grado.getNombre()
                    + ": sus secciones ya tienen clases asignadas");
        }
        List<Recreo> recreos = recreosDelGrado(anio, turno, grado);
        recreos.forEach(r -> r.setAcceso(accesoEliminado()));
        recreoRepository.saveAll(recreos);
    }

    private void validarSinCruces(Grado grado, Byte dia, LocalTime inicio, LocalTime fin,
                                  List<Recreo> existentes, List<HorarioClase> clases, Integer idExcluir) {
        for (Recreo otro : existentes) {
            if (!otro.getIdRecreo().equals(idExcluir) && otro.getDiaSemana().equals(dia)
                    && HorarioUtil.seCruzan(inicio, fin, otro.getHoraInicio(), otro.getHoraFin())) {
                throw new IllegalArgumentException(grado.getNombre() + " ya tiene recreo el " + HorarioUtil.dia(dia)
                        + " de " + HorarioUtil.rango(otro.getHoraInicio(), otro.getHoraFin()));
            }
        }
        for (HorarioClase clase : clases) {
            if (clase.getDiaSemana() == dia
                    && HorarioUtil.seCruzan(inicio, fin, clase.getHoraInicio(), clase.getHoraFin())) {
                throw new IllegalArgumentException("El recreo choca con " + clase.getAsignacion().getCurso().getNombre()
                        + " de " + nombreSeccion(clase.getAsignacion().getGradoSeccion()) + " el " + HorarioUtil.dia(dia)
                        + " (" + HorarioUtil.rango(clase.getHoraInicio(), clase.getHoraFin()) + ")");
            }
        }
    }

    // Una asignacion exige recreo cada dia que tiene clase.
    private void validarNoDejaDiaSinRecreo(Recreo recreo, List<Recreo> existentes, List<HorarioClase> clases) {
        byte dia = recreo.getDiaSemana();
        boolean quedaOtro = existentes.stream()
                .anyMatch(r -> !r.getIdRecreo().equals(recreo.getIdRecreo()) && r.getDiaSemana() == dia);
        boolean hayClases = clases.stream().anyMatch(c -> c.getDiaSemana() == dia);
        if (!quedaOtro && hayClases) {
            throw new IllegalArgumentException("Es el único recreo de " + recreo.getGrado().getNombre() + " el "
                    + HorarioUtil.dia(dia) + " y ese día ya tiene clases asignadas");
        }
    }

    private List<Recreo> recreosDelGrado(AnioEscolar anio, Turno turno, Grado grado) {
        return recreoRepository.findByAnioEscolarIdAnioAndTurnoIdTurnoAndGradoIdGradoAndAccesoNot(
                anio.getIdAnio(), turno.getIdTurno(), grado.getIdGrado(), accesoEliminado());
    }

    /** Clases de todas las secciones del grado en ese turno y año. */
    private List<HorarioClase> clasesDelGrado(AnioEscolar anio, Turno turno, Grado grado) {
        List<Integer> secciones = seccionesDelGrado(anio, turno, grado);
        if (secciones.isEmpty()) {
            return List.of();
        }
        return horarioClaseRepository.findByAsignacionGradoSeccionIdGradoSeccionInAndAccesoNot(secciones, accesoEliminado())
                .stream()
                .filter(h -> !AccesoConstants.ELIMINADO.equals(h.getAsignacion().getAcceso().getIdAcceso()))
                .toList();
    }

    private List<Integer> seccionesDelGrado(AnioEscolar anio, Turno turno, Grado grado) {
        return gradoSeccionRepository
                .findByGradoIdGradoAndTurnoIdTurnoAndAnioEscolarIdAnioAndAccesoNot(
                        grado.getIdGrado(), turno.getIdTurno(), anio.getIdAnio(), accesoEliminado())
                .stream().map(GradoSeccion::getIdGradoSeccion).toList();
    }

    private String nombreSeccion(GradoSeccion gs) {
        return gs.getGrado().getNombre() + " " + (gs.getSeccion() != null ? gs.getSeccion().getNombre() : "Única");
    }

    private Recreo findRecreo(Integer id) {
        return recreoRepository.findByIdRecreoAndAccesoNot(id, accesoEliminado())
                .orElseThrow(() -> new ResourceNotFoundException("Recreo no encontrado con id " + id));
    }

    private AnioEscolar findAnio(Integer id) {
        return anioEscolarRepository.findByIdAnioAndAccesoNot(id, accesoEliminado())
                .orElseThrow(() -> new ResourceNotFoundException("Año escolar no encontrado con id " + id));
    }

    private Nivel findNivel(Integer id) {
        return nivelRepository.findByIdNivelAndAccesoNot(id, accesoEliminado())
                .orElseThrow(() -> new ResourceNotFoundException("Nivel no encontrado con id " + id));
    }

    private Turno findTurno(Integer id) {
        return turnoRepository.findByIdTurnoAndAccesoNot(id, accesoEliminado())
                .orElseThrow(() -> new ResourceNotFoundException("Turno no encontrado con id " + id));
    }

    private Grado findGrado(Integer id) {
        return gradoRepository.findByIdGradoAndAccesoNot(id, accesoEliminado())
                .orElseThrow(() -> new ResourceNotFoundException("Grado no encontrado con id " + id));
    }

    private Acceso accesoEliminado() {
        return accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow();
    }

    private Acceso accesoActivo() {
        return accesoRepository.findById(AccesoConstants.ACTIVO).orElseThrow();
    }

    private RecreoResponse toResponse(Recreo recreo) {
        RecreoResponse response = new RecreoResponse();
        response.setIdRecreo(recreo.getIdRecreo());
        response.setIdAnio(recreo.getAnioEscolar().getIdAnio());
        response.setAnio(recreo.getAnioEscolar().getAnio());
        response.setIdNivel(recreo.getNivel().getIdNivel());
        response.setNivel(recreo.getNivel().getNombre());
        response.setIdTurno(recreo.getTurno().getIdTurno());
        response.setTurno(recreo.getTurno().getNombre());
        response.setIdGrado(recreo.getGrado().getIdGrado());
        response.setGrado(recreo.getGrado().getNombre());
        response.setDiaSemana(recreo.getDiaSemana());
        response.setHoraInicio(recreo.getHoraInicio());
        response.setHoraFin(recreo.getHoraFin());
        response.setAccesoId(recreo.getAcceso().getIdAcceso());
        return response;
    }
}
