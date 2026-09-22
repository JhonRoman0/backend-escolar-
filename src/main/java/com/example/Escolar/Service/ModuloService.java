package com.example.Escolar.Service;

import com.example.Escolar.Dto.ModuloRequest;
import com.example.Escolar.Dto.ModuloResponse;
import com.example.Escolar.Dto.PermisoNestedRequest;
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

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ModuloService {

    private final ModuloRepository moduloRepository;
    private final AccesoRepository accesoRepository;
    private final PermisoRepository permisoRepository;
    private final AccionRepository accionRepository;
    private final PermisoAccionRepository permisoAccionRepository;

    public List<ModuloResponse> getAll() {
        return moduloRepository.findByAccesoNot(accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow()).stream().map(this::toResponse).toList();
    }

    public ModuloResponse getById(Integer id) {
        return toResponse(findModulo(id));
    }

    @Transactional
    public ModuloResponse create(ModuloRequest request) {
        Modulo modulo = new Modulo();
        modulo.setModulo(request.getModulo());
        modulo.setIcono(request.getIcono());
        modulo.setAcceso(accesoRepository.findById(request.getAccesoId() != null ? request.getAccesoId() : AccesoConstants.ACTIVO).orElseThrow());
        modulo = moduloRepository.save(modulo);
        crearPermisos(modulo, request.getPermisos());
        return toResponse(modulo);
    }

    @Transactional
    public ModuloResponse update(Integer id, ModuloRequest request) {
        Modulo modulo = findModulo(id);
        modulo.setModulo(request.getModulo());
        modulo.setIcono(request.getIcono());
        if (request.getAccesoId() != null) {
            modulo.setAcceso(accesoRepository.findById(request.getAccesoId()).orElseThrow());
        }
        crearPermisos(modulo, request.getPermisos());
        return toResponse(moduloRepository.save(modulo));
    }

    @Transactional
    public void delete(Integer id) {
        Modulo modulo = findModulo(id);
        modulo.setAcceso(accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow());
        moduloRepository.save(modulo);
    }

    private void crearPermisos(Modulo modulo, List<PermisoNestedRequest> permisosRequest) {
        if (permisosRequest == null || permisosRequest.isEmpty()) {
            return;
        }
        for (PermisoNestedRequest p : permisosRequest) {
            if (permisoRepository.findByCodigoAndAccesoNot(p.getCodigo(), accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow()).isPresent()) {
                throw new IllegalArgumentException("Ya existe un permiso con el código " + p.getCodigo());
            }
            Permiso permiso = new Permiso();
            permiso.setCodigo(p.getCodigo());
            permiso.setNombre(p.getNombre());
            permiso.setModulo(modulo);
            permiso.setAcceso(accesoRepository.findById(AccesoConstants.ACTIVO).orElseThrow());
            permiso = permisoRepository.save(permiso);
            asignarAcciones(permiso, p.getAcciones());
        }
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

    private Modulo findModulo(Integer id) {
        return moduloRepository.findByIdModuloAndAccesoNot(id, accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow())
                .orElseThrow(() -> new ResourceNotFoundException("Módulo no encontrado con id " + id));
    }

    private ModuloResponse toResponse(Modulo modulo) {
        ModuloResponse response = new ModuloResponse();
        response.setIdModulo(modulo.getIdModulo());
        response.setModulo(modulo.getModulo());
        response.setIcono(modulo.getIcono());
        response.setAccesoId(modulo.getAcceso().getIdAcceso().longValue());
        List<Permiso> permisos = permisoRepository.findByModuloIdModulo(modulo.getIdModulo()).stream()
                .filter(p -> !p.getAcceso().getIdAcceso().equals(AccesoConstants.ELIMINADO))
                .toList();
        response.setPermisos(permisos.stream().map(this::toPermisoResponse).toList());
        return response;
    }

    private PermisoResponse toPermisoResponse(Permiso permiso) {
        PermisoResponse response = new PermisoResponse();
        response.setIdPermiso(permiso.getIdPermiso());
        response.setCodigo(permiso.getCodigo());
        response.setNombre(permiso.getNombre());
        response.setAccesoId(permiso.getAcceso().getIdAcceso().longValue());
        response.setAcciones(accionesDelPermiso(permiso.getIdPermiso()));
        return response;
    }

    List<String> accionesDelPermiso(Integer idPermiso) {
        return permisoAccionRepository.findByPermisoIdPermisoAndAccesoNot(idPermiso, accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow()).stream()
                .filter(pa -> !pa.getAccion().getAcceso().getIdAcceso().equals(AccesoConstants.ELIMINADO))
                .map(pa -> pa.getAccion().getCodigo())
                .sorted()
                .toList();
    }
}
