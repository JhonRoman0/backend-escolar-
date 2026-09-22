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
import com.example.Escolar.Repository.AccesoRepository;
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

    private final PermisoRepository permisoRepository;
    private final AccesoRepository accesoRepository;
    private final ModuloRepository moduloRepository;
    private final AccionRepository accionRepository;
    private final PermisoAccionRepository permisoAccionRepository;

    public List<PermisoResponse> getAll() {
        return permisoRepository.findByAccesoNot(accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow()).stream().map(this::toResponse).toList();
    }

    public PermisoResponse getById(Integer id) {
        return toResponse(findPermiso(id));
    }

    @Transactional
    public PermisoResponse create(PermisoRequest request) {
        validarCodigoUnico(request.getCodigo(), null);
        Permiso permiso = new Permiso();
        applyRequest(permiso, request);
        permiso.setAcceso(accesoRepository.findById(request.getAccesoId() != null ? request.getAccesoId() : AccesoConstants.ACTIVO).orElseThrow());
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
        permiso.setAcceso(accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow());
        permisoRepository.save(permiso);
    }

    private Permiso findPermiso(Integer id) {
        return permisoRepository.findByIdPermisoAndAccesoNot(id, accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow())
                .orElseThrow(() -> new ResourceNotFoundException("Permiso no encontrado con id " + id));
    }

    private void applyRequest(Permiso permiso, PermisoRequest request) {
        Modulo modulo = moduloRepository.findByIdModuloAndAccesoNot(request.getIdModulo(), accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow())
                .orElseThrow(() -> new ResourceNotFoundException("Módulo no encontrado con id " + request.getIdModulo()));
        permiso.setCodigo(request.getCodigo());
        permiso.setNombre(request.getNombre());
        permiso.setModulo(modulo);
        if (request.getAccesoId() != null) {
            permiso.setAcceso(accesoRepository.findById(request.getAccesoId()).orElseThrow());
        }
    }

    private void validarCodigoUnico(String codigo, Integer idExcluido) {
        permisoRepository.findByCodigoAndAccesoNot(codigo, accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow())
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
            Accion accion = accionRepository.findByCodigoAndAccesoNot(codigo, accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow())
                    .orElseThrow(() -> new IllegalArgumentException("La acción " + codigo + " no existe"));
            if (permisoAccionRepository.findByPermisoIdPermisoAndAccionIdAccion(permiso.getIdPermiso(), accion.getIdAccion()).isEmpty()) {
                PermisoAccion permisoAccion = new PermisoAccion();
                permisoAccion.setPermiso(permiso);
                permisoAccion.setAccion(accion);
                permisoAccion.setAcceso(accesoRepository.findById(AccesoConstants.ACTIVO).orElseThrow());
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
        response.setAccesoId(permiso.getAcceso().getIdAcceso().longValue());
        response.setAcciones(accionesDelPermiso(permiso.getIdPermiso()));
        return response;
    }

    private ModuloResponse toModuloResponse(Modulo modulo) {
        ModuloResponse response = new ModuloResponse();
        response.setIdModulo(modulo.getIdModulo());
        response.setModulo(modulo.getModulo());
        response.setIcono(modulo.getIcono());
        response.setAccesoId(modulo.getAcceso().getIdAcceso().longValue());
        return response;
    }

    private List<String> accionesDelPermiso(Integer idPermiso) {
        return permisoAccionRepository.findByPermisoIdPermisoAndAccesoNot(idPermiso, accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow()).stream()
                .filter(pa -> !pa.getAccion().getAcceso().getIdAcceso().equals(AccesoConstants.ELIMINADO))
                .map(pa -> pa.getAccion().getCodigo())
                .sorted()
                .toList();
    }
}
