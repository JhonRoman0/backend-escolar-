package com.example.Escolar.Service;

import com.example.Escolar.Dto.AulaRequest;
import com.example.Escolar.Dto.AulaResponse;
import com.example.Escolar.Exception.ResourceNotFoundException;
import com.example.Escolar.Model.Acceso;
import com.example.Escolar.Model.AnioEscolar;
import com.example.Escolar.Model.Asignacion;
import com.example.Escolar.Model.Aula;
import com.example.Escolar.Model.GradoSeccion;
import com.example.Escolar.Model.HorarioClase;
import com.example.Escolar.Model.TipoAula;
import com.example.Escolar.Repository.AccesoRepository;
import com.example.Escolar.Repository.AulaRepository;
import com.example.Escolar.Repository.GradoSeccionRepository;
import com.example.Escolar.Repository.HorarioClaseRepository;
import com.example.Escolar.Repository.MatriculaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AulaService {

    private static final List<Byte> ESTADOS_VIGENTES =
            List.of(AnioEscolarService.ESTADO_VIGENTE, AnioEscolarService.ESTADO_POR_COMENZAR);

    private final AulaRepository aulaRepository;
    private final AccesoRepository accesoRepository;
    private final HorarioClaseRepository horarioClaseRepository;
    private final GradoSeccionRepository gradoSeccionRepository;
    private final MatriculaRepository matriculaRepository;

    private record UsoVigente(List<GradoSeccion> secciones, List<HorarioClase> horarios) {
        boolean bloquea() {
            return !secciones.isEmpty() || !horarios.isEmpty();
        }
    }

    public List<AulaResponse> getAll() {
        return aulaRepository.findByAccesoNot(accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow()).stream()
                .map(aula -> toResponse(aula, seccionesDelSalon(aula), null))
                .toList();
    }

    public AulaResponse getById(Integer id) {
        Aula aula = findAula(id);
        List<GradoSeccion> secciones = seccionesDelSalon(aula);
        UsoVigente uso = usoVigente(aula, secciones);
        AulaResponse response = toResponse(aula, secciones, uso);
        response.setOcupacionActual(ocupacionActualSegura(uso));
        return response;
    }

    @Transactional
    public AulaResponse create(AulaRequest request) {
        validarNombreUnico(request.getNombre(), null);
        Aula aula = new Aula();
        aula.setNombre(request.getNombre().trim());
        aula.setCapacidad(request.getCapacidad());
        aula.setTipo(request.getTipo() == null ? TipoAula.SALON : request.getTipo());
        aula.setAcceso(accesoPorId(request.getAccesoId() == null ? AccesoConstants.ACTIVO : request.getAccesoId()));
        return toResponse(aulaRepository.save(aula), List.of(), null);
    }

    @Transactional
    public AulaResponse update(Integer id, AulaRequest request) {
        Aula aula = findAula(id);
        if (request.getNombre() != null && !request.getNombre().isBlank()) {
            validarNombreUnico(request.getNombre(), id);
        }
        List<GradoSeccion> secciones = seccionesDelSalon(aula);
        UsoVigente uso = null;
        if (cambiaAInactivo(aula, request.getAccesoId())) {
            uso = usoVigente(aula, secciones);
            if (uso.bloquea()) {
                throw new IllegalArgumentException(mensajeNoInactivar(aula, uso));
            }
        }
        if (request.getTipo() != null && request.getTipo() != aula.getTipo() && !secciones.isEmpty()) {
            throw new IllegalArgumentException("No se puede cambiar el tipo: " + aula.getNombre()
                    + " es el salón de " + GradoSeccionService.nombreSeccion(secciones.get(0)));
        }
        if (request.getCapacidad() != null && request.getCapacidad() < aula.getCapacidad()) {
            if (uso == null) {
                uso = usoVigente(aula, secciones);
            }
            int ocupacion = ocupacionActual(uso);
            if (request.getCapacidad() < ocupacion) {
                throw new IllegalArgumentException("No se puede reducir la capacidad del aula «" + aula.getNombre()
                        + "» a " + request.getCapacidad() + ": tiene " + ocupacion
                        + (ocupacion == 1 ? " alumno matriculado" : " alumnos matriculados") + ".");
            }
        }
        if (request.getNombre() != null && !request.getNombre().isBlank()) {
            aula.setNombre(request.getNombre().trim());
        }
        if (request.getCapacidad() != null) {
            aula.setCapacidad(request.getCapacidad());
        }
        if (request.getTipo() != null && request.getTipo() != aula.getTipo()) {
            aula.setTipo(request.getTipo());
        }
        if (request.getAccesoId() != null) {
            aula.setAcceso(accesoPorId(request.getAccesoId()));
        }
        return toResponse(aulaRepository.save(aula), secciones, null);
    }

    @Transactional
    public void delete(Integer id) {
        Aula aula = findAula(id);
        List<GradoSeccion> secciones = seccionesDelSalon(aula);
        if (!secciones.isEmpty()) {
            throw new IllegalArgumentException("No se puede eliminar el aula «" + aula.getNombre()
                    + "» porque es el salón de " + GradoSeccionService.nombreSeccion(secciones.get(0))
                    + ". Asígnale otro salón a esa sección primero.");
        }
        Acceso eliminado = accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow();
        long horariosActivos = horarioClaseRepository.countByAulaIdAulaAndAccesoNot(id, eliminado);
        if (horariosActivos > 0) {
            throw new IllegalArgumentException("No se puede eliminar el aula «" + aula.getNombre()
                    + "» porque tiene " + horariosActivos
                    + (horariosActivos == 1 ? " horario asignado" : " horarios asignados")
                    + ". Quita primero las asignaciones que la usan.");
        }
        // Borrado físico y no lógico, por la misma razón que Año escolar
        // (AnioEscolarService.delete): nombre tiene unique=true, así que una
        // fila solo marcada como eliminada seguiría ocupando el nombre y no se
        // podría crear otra aula igual con un id nuevo, que es lo que se
        // necesita. Los horarios en papelera siguen con FK apuntando a esta
        // fila, así que se purgan antes de borrar.
        horarioClaseRepository.deleteAll(horarioClaseRepository.findByAulaIdAula(id));
        aulaRepository.delete(aula);
    }

    private void validarNombreUnico(String nombre, Integer idExcluir) {
        String nombreNormalizado = nombre.trim();
        boolean existe = (idExcluir == null
                ? aulaRepository.findByNombreIgnoreCase(nombreNormalizado)
                : aulaRepository.findByNombreIgnoreCaseAndIdAulaNot(nombreNormalizado, idExcluir))
                .isPresent();
        if (existe) {
            throw new IllegalArgumentException("Ya existe un aula con ese nombre");
        }
    }

    // El estado viaja como id de la tabla acceso: 1=Activo, 3=Inactivo. El 2
    // (Eliminado) no se acepta por API porque el borrado ya no es lógico:
    // mandarlo por un PATCH recrearía filas ocultas que ocupan el nombre.
    private Acceso accesoPorId(Long idAcceso) {
        if (idAcceso.equals(AccesoConstants.ELIMINADO)) {
            throw new IllegalArgumentException("Estado no válido: para quitar un aula, elimínala");
        }
        return accesoRepository.findById(idAcceso)
                .orElseThrow(() -> new IllegalArgumentException("Estado no válido"));
    }

    private List<GradoSeccion> seccionesDelSalon(Aula aula) {
        return gradoSeccionRepository.findByAulaIdAulaAndAccesoNot(aula.getIdAula(),
                accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow());
    }

    private Aula findAula(Integer id) {
        return aulaRepository.findByIdAulaAndAccesoNot(id, accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow())
                .orElseThrow(() -> new ResourceNotFoundException("Aula no encontrada con id " + id));
    }

    private boolean cambiaAInactivo(Aula aula, Long idAcceso) {
        return AccesoConstants.INACTIVO.equals(idAcceso)
                && !AccesoConstants.INACTIVO.equals(aula.getAcceso().getIdAcceso());
    }

    private UsoVigente usoVigente(Aula aula, List<GradoSeccion> seccionesSalon) {
        Acceso eliminado = accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow();
        List<GradoSeccion> secciones = seccionesSalon.stream()
                .filter(gs -> esAnioVigente(gs.getAnioEscolar()))
                .toList();
        List<HorarioClase> horarios = horarioClaseRepository
                .findByAulaIdAulaAndAccesoNot(aula.getIdAula(), eliminado).stream()
                .filter(h -> noEliminado(h.getAcceso(), eliminado))
                .filter(h -> h.getAsignacion() != null && noEliminado(h.getAsignacion().getAcceso(), eliminado))
                .filter(h -> h.getAsignacion().getGradoSeccion() != null
                        && noEliminado(h.getAsignacion().getGradoSeccion().getAcceso(), eliminado))
                .filter(h -> esAnioVigente(h.getAsignacion().getAnioEscolar()))
                .toList();
        return new UsoVigente(secciones, horarios);
    }

    private Integer ocupacionActualSegura(UsoVigente uso) {
        try {
            return ocupacionActual(uso);
        } catch (DataAccessException sinOcupacion) {
            return null;
        }
    }

    private int ocupacionActual(UsoVigente uso) {
        LinkedHashSet<Integer> ids = new LinkedHashSet<>();
        uso.secciones().stream()
                .map(GradoSeccion::getIdGradoSeccion)
                .filter(Objects::nonNull)
                .forEach(ids::add);
        uso.horarios().stream()
                .map(HorarioClase::getAsignacion)
                .filter(Objects::nonNull)
                .map(Asignacion::getGradoSeccion)
                .filter(Objects::nonNull)
                .map(GradoSeccion::getIdGradoSeccion)
                .filter(Objects::nonNull)
                .forEach(ids::add);
        if (ids.isEmpty()) {
            return 0;
        }
        Acceso eliminado = accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow();
        if (ids.size() == 1) {
            return (int) matriculaRepository.countByGradoSeccionIdGradoSeccionAndAccesoNot(ids.iterator().next(), eliminado);
        }
        return (int) matriculaRepository.ocupacionPorSecciones(ids, eliminado).stream()
                .mapToLong(MatriculaRepository.OcupacionSeccion::getTotal)
                .max()
                .orElse(0L);
    }

    private boolean esAnioVigente(AnioEscolar anio) {
        return anio != null && ESTADOS_VIGENTES.contains(anio.getEstado());
    }

    private boolean noEliminado(Acceso acceso, Acceso eliminado) {
        return acceso != null && !eliminado.getIdAcceso().equals(acceso.getIdAcceso());
    }

    private String mensajeNoInactivar(Aula aula, UsoVigente uso) {
        List<String> partes = new ArrayList<>();
        if (!uso.secciones().isEmpty()) {
            partes.add(uso.secciones().size() == 1
                    ? "es el salón de " + GradoSeccionService.nombreSeccion(uso.secciones().get(0))
                    : "es el salón de " + uso.secciones().size() + " secciones vigentes");
        }
        if (!uso.horarios().isEmpty()) {
            int total = uso.horarios().size();
            partes.add("tiene " + total + (total == 1 ? " clase programada" : " clases programadas")
                    + " (" + detallePorAnio(horariosPorAnio(uso.horarios())) + ")");
        }
        return "No se puede inactivar el aula «" + aula.getNombre() + "» porque " + String.join(" y ", partes)
                + ". Cambia el salón en Secciones o quita esas clases en Horarios antes de inactivarla.";
    }

    private Map<String, Long> horariosPorAnio(List<HorarioClase> horarios) {
        return horarios.stream().collect(Collectors.groupingBy(
                h -> h.getAsignacion().getAnioEscolar().getAnio(),
                () -> new TreeMap<String, Long>(Comparator.reverseOrder()),
                Collectors.counting()));
    }

    private String detallePorAnio(Map<String, Long> porAnio) {
        return porAnio.entrySet().stream()
                .map(entry -> entry.getKey() + ": " + entry.getValue())
                .collect(Collectors.joining(", "));
    }

    private AulaResponse toResponse(Aula aula, List<GradoSeccion> secciones, UsoVigente uso) {
        AulaResponse response = new AulaResponse();
        response.setIdAula(aula.getIdAula());
        response.setNombre(aula.getNombre());
        response.setCapacidad(aula.getCapacidad());
        response.setTipo(aula.getTipo().name());
        response.setSecciones(secciones.stream().map(this::seccionResponse).toList());
        response.setAccesoId(aula.getAcceso().getIdAcceso());
        if (uso != null) {
            response.setTieneAsignacionesVigentes(uso.bloquea());
            response.setSeccionesVigentes(uso.secciones().stream().map(this::seccionResponse).toList());
            response.setHorariosVigentesPorAnio(horariosPorAnio(uso.horarios()));
        }
        return response;
    }

    private AulaResponse.SeccionDelAula seccionResponse(GradoSeccion gs) {
        return new AulaResponse.SeccionDelAula(
                gs.getAnioEscolar().getIdAnio(),
                gs.getAnioEscolar().getAnio(),
                gs.getGrado().getNivel().getNombre(),
                gs.getGrado().getNombre(),
                gs.getSeccion() != null ? gs.getSeccion().getNombre() : "Única",
                gs.getTurno().getNombre());
    }
}
