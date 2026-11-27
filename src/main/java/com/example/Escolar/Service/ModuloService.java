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
import java.util.stream.Collectors;

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
        List<PermisoAccion> acciones = accionesVigentes(permiso.getIdPermiso());
        response.setAcciones(acciones.stream().map(pa -> pa.getAccion().getCodigo()).sorted().toList());
        response.setEtiquetas(acciones.stream()
                .filter(pa -> pa.getEtiqueta() != null)
                .collect(Collectors.toMap(pa -> pa.getAccion().getCodigo(), PermisoAccion::getEtiqueta, (a, b) -> a)));
        return response;
    }

    private List<PermisoAccion> accionesVigentes(Integer idPermiso) {
        return permisoAccionRepository.findByPermisoIdPermisoAndAccesoNot(idPermiso, accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow()).stream()
                .filter(pa -> !pa.getAccion().getAcceso().getIdAcceso().equals(AccesoConstants.ELIMINADO))
                .toList();
    }
}
