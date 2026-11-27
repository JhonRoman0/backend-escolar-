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
import com.example.Escolar.Security.ConcesionesService;
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
    private final ConcesionesService concesionesService;

    public List<PermisoResponse> getAll() {
        return permisoRepository.findByAccesoNot(accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow()).stream().map(this::toResponse).toList();
    }

    public PermisoResponse getById(Integer id) {
        return toResponse(findPermiso(id));
    }

    private Permiso findPermiso(Integer id) {
        return permisoRepository.findByIdPermisoAndAccesoNot(id, accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow())
                .orElseThrow(() -> new ResourceNotFoundException("Permiso no encontrado con id " + id));
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
