package com.example.Escolar.Service;

import com.example.Escolar.Dto.ModuloResponse;
import com.example.Escolar.Dto.PermisoRequest;
import com.example.Escolar.Dto.PermisoResponse;
import com.example.Escolar.Exception.ResourceNotFoundException;
import com.example.Escolar.Model.Accion;
import com.example.Escolar.Model.Modulo;
import com.example.Escolar.Model.Permiso;
import com.example.Escolar.Model.PermisoAccion;
import com.example.Escolar.Repository.AccionRepository;
import com.example.Escolar.Repository.ModuloRepository;
import com.example.Escolar.Repository.PermisoAccionRepository;
import com.example.Escolar.Repository.PermisoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PermisoService {

    public static final byte ACCESO_ACTIVO = 1;
    public static final byte ACCESO_ELIMINADO = 2;

    private final PermisoRepository permisoRepository;
    private final ModuloRepository moduloRepository;
    private final AccionRepository accionRepository;
    private final PermisoAccionRepository permisoAccionRepository;

    public List<PermisoResponse> getAll() {
        return permisoRepository.findByAccesoNot(ACCESO_ELIMINADO).stream().map(this::toResponse).toList();
    }

    public PermisoResponse getById(Integer id) {
        return toResponse(findPermiso(id));
    }

    @Transactional
    public PermisoResponse create(PermisoRequest request) {
        validarCodigoUnico(request.getCodigo(), null);
        Permiso permiso = new Permiso();
        applyRequest(permiso, request);
        permiso.setAcceso(request.getAcceso() != null ? request.getAcceso() : ACCESO_ACTIVO);
        permiso = permisoRepository.save(permiso);
        asignarAcciones(permiso, request.getAcciones());
        return toResponse(permiso);
    }

    @Transactional
    public PermisoResponse update(Integer id, PermisoRequest request) {
        Permiso permiso = findPermiso(id);
        validarCodigoUnico(request.getCodigo(), permiso.getIdPermiso());
        applyRequest(permiso, request);
        permiso = permisoRepository.save(permiso);
        if (request.getAcciones() != null) {
            permisoAccionRepository.deleteAll(permisoAccionRepository.findByPermisoIdPermiso(id));
            asignarAcciones(permiso, request.getAcciones());
        }
        return toResponse(permiso);
    }

    @Transactional
    public void delete(Integer id) {
        Permiso permiso = findPermiso(id);
        permiso.setAcceso(ACCESO_ELIMINADO);
        permisoRepository.save(permiso);
    }

    private Permiso findPermiso(Integer id) {
        return permisoRepository.findByIdPermisoAndAccesoNot(id, ACCESO_ELIMINADO)
                .orElseThrow(() -> new ResourceNotFoundException("Permiso no encontrado con id " + id));
    }

    private void applyRequest(Permiso permiso, PermisoRequest request) {
        Modulo modulo = moduloRepository.findByIdModuloAndAccesoNot(request.getIdModulo(), ACCESO_ELIMINADO)
                .orElseThrow(() -> new ResourceNotFoundException("Módulo no encontrado con id " + request.getIdModulo()));
        permiso.setCodigo(request.getCodigo());
        permiso.setNombre(request.getNombre());
        permiso.setModulo(modulo);
        if (request.getAcceso() != null) {
            permiso.setAcceso(request.getAcceso());
        }
    }

    private void validarCodigoUnico(String codigo, Integer idExcluido) {
        permisoRepository.findByCodigoAndAccesoNot(codigo, ACCESO_ELIMINADO)
                .filter(p -> idExcluido == null || !p.getIdPermiso().equals(idExcluido))
                .ifPresent(p -> {
                    throw new IllegalArgumentException("Ya existe un permiso con el código " + codigo);
                });
    }

    private void asignarAcciones(Permiso permiso, List<String> codigosAcciones) {
        if (codigosAcciones == null || codigosAcciones.isEmpty()) {
            return;
        }
        for (String codigo : codigosAcciones) {
            Accion accion = accionRepository.findByCodigoAndAccesoNot(codigo, ACCESO_ELIMINADO)
                    .orElseThrow(() -> new IllegalArgumentException("La acción " + codigo + " no existe"));
            if (permisoAccionRepository.findByPermisoIdPermisoAndAccionIdAccion(permiso.getIdPermiso(), accion.getIdAccion()).isEmpty()) {
                PermisoAccion permisoAccion = new PermisoAccion();
                permisoAccion.setPermiso(permiso);
                permisoAccion.setAccion(accion);
                permisoAccion.setAcceso(ACCESO_ACTIVO);
                permisoAccionRepository.save(permisoAccion);
            }
        }
    }

    private PermisoResponse toResponse(Permiso permiso) {
        PermisoResponse response = new PermisoResponse();
        response.setIdPermiso(permiso.getIdPermiso());
        response.setCodigo(permiso.getCodigo());
        response.setNombre(permiso.getNombre());
        response.setModulo(toModuloResponse(permiso.getModulo()));
        response.setAcceso(permiso.getAcceso());
        response.setAcciones(accionesDelPermiso(permiso.getIdPermiso()));
        return response;
    }

    private ModuloResponse toModuloResponse(Modulo modulo) {
        ModuloResponse response = new ModuloResponse();
        response.setIdModulo(modulo.getIdModulo());
        response.setModulo(modulo.getModulo());
        response.setIcono(modulo.getIcono());
        response.setAcceso(modulo.getAcceso());
        return response;
    }

    private List<String> accionesDelPermiso(Integer idPermiso) {
        return permisoAccionRepository.findByPermisoIdPermisoAndAccesoNot(idPermiso, ACCESO_ELIMINADO).stream()
                .filter(pa -> pa.getAccion().getAcceso() != ACCESO_ELIMINADO)
                .map(pa -> pa.getAccion().getCodigo())
                .sorted()
                .toList();
    }
}
