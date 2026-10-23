package com.example.Escolar.Service;

import com.example.Escolar.Dto.AulaRequest;
import com.example.Escolar.Dto.AulaResponse;
import com.example.Escolar.Exception.ResourceNotFoundException;
import com.example.Escolar.Model.Acceso;
import com.example.Escolar.Model.Aula;
import com.example.Escolar.Model.GradoSeccion;
import com.example.Escolar.Model.TipoAula;
import com.example.Escolar.Repository.AccesoRepository;
import com.example.Escolar.Repository.AulaRepository;
import com.example.Escolar.Repository.GradoSeccionRepository;
import com.example.Escolar.Repository.HorarioClaseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AulaService {

    private final AulaRepository aulaRepository;
    private final AccesoRepository accesoRepository;
    private final HorarioClaseRepository horarioClaseRepository;
    private final GradoSeccionRepository gradoSeccionRepository;

    public List<AulaResponse> getAll() {
        return aulaRepository.findByAccesoNot(accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow()).stream()
                .map(this::toResponse)
                .toList();
    }

    public AulaResponse getById(Integer id) {
        return toResponse(findAula(id));
    }

    @Transactional
    public AulaResponse create(AulaRequest request) {
        validarNombreUnico(request.getNombre(), null);
        Aula aula = new Aula();
        aula.setNombre(request.getNombre().trim());
        aula.setCapacidad(request.getCapacidad());
        aula.setTipo(request.getTipo() == null ? TipoAula.SALON : request.getTipo());
        aula.setAcceso(accesoPorId(request.getAccesoId() == null ? AccesoConstants.ACTIVO : request.getAccesoId()));
        return toResponse(aulaRepository.save(aula));
    }

    @Transactional
    public AulaResponse update(Integer id, AulaRequest request) {
        Aula aula = findAula(id);
        if (request.getNombre() != null && !request.getNombre().isBlank()) {
            validarNombreUnico(request.getNombre(), id);
            aula.setNombre(request.getNombre().trim());
        }
        if (request.getCapacidad() != null) {
            aula.setCapacidad(request.getCapacidad());
        }
        if (request.getTipo() != null && request.getTipo() != aula.getTipo()) {
            // Un salon que ya es de una seccion no deja de serlo por debajo.
            List<GradoSeccion> secciones = seccionesDelSalon(aula);
            if (!secciones.isEmpty()) {
                throw new IllegalArgumentException("No se puede cambiar el tipo: " + aula.getNombre()
                        + " es el salón de " + GradoSeccionService.nombreSeccion(secciones.get(0)));
            }
            aula.setTipo(request.getTipo());
        }
        if (request.getAccesoId() != null) {
            aula.setAcceso(accesoPorId(request.getAccesoId()));
        }
        return toResponse(aulaRepository.save(aula));
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

    private AulaResponse toResponse(Aula aula) {
        AulaResponse response = new AulaResponse();
        response.setIdAula(aula.getIdAula());
        response.setNombre(aula.getNombre());
        response.setCapacidad(aula.getCapacidad());
        response.setTipo(aula.getTipo().name());
        response.setSecciones(seccionesDelSalon(aula).stream()
                .map(gs -> GradoSeccionService.nombreSeccion(gs) + " – " + gs.getTurno().getNombre()
                        + " " + gs.getAnioEscolar().getAnio())
                .toList());
        response.setAccesoId(aula.getAcceso().getIdAcceso());
        return response;
    }
}
