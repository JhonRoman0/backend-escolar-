package com.example.Escolar.Service;

import com.example.Escolar.Dto.RolRequest;
import com.example.Escolar.Dto.RolResponse;
import com.example.Escolar.Exception.ResourceNotFoundException;
import com.example.Escolar.Exception.RolConUsuariosException;
import com.example.Escolar.Model.Rol;
import com.example.Escolar.Repository.AccesoRepository;
import com.example.Escolar.Repository.RolRepository;
import com.example.Escolar.Repository.UsuarioRolRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RolService {

    private final RolRepository rolRepository;
    private final AccesoRepository accesoRepository;
    private final UsuarioRolRepository usuarioRolRepository;

    public List<RolResponse> getAll(boolean soloActivos) {
        if (soloActivos) {
            return rolRepository.findByAcceso(accesoRepository.findById(AccesoConstants.ACTIVO).orElseThrow()).stream().map(this::toResponse).toList();
        }
        return rolRepository.findByAccesoNot(accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow()).stream().map(this::toResponse).toList();
    }

    public RolResponse getById(Integer id) {
        return toResponse(findRol(id));
    }

    @Transactional
    public RolResponse create(RolRequest request) {
        validarNombreNoProtegido(request.getNombre());
        Rol rol = new Rol();
        rol.setNombre(request.getNombre());
        rol.setColor(request.getColor());
        rol.setAcceso(accesoRepository.findById(request.getAccesoId() != null ? request.getAccesoId() : AccesoConstants.ACTIVO).orElseThrow());
        return toResponse(rolRepository.save(rol));
    }

    @Transactional
    public RolResponse update(Integer id, RolRequest request) {
        Rol rol = findRol(id);
        validarRolNoProtegido(rol);
        validarNombreNoProtegido(request.getNombre());
        if (request.getAccesoId() != null
                && request.getAccesoId().equals(AccesoConstants.INACTIVO)
                && !rol.getAcceso().getIdAcceso().equals(AccesoConstants.INACTIVO)
                && tieneUsuarios(id)) {
            throw new RolConUsuariosException("No se puede inactivar el rol '" + rol.getNombre() + "' porque tiene usuarios asociados");
        }
        rol.setNombre(request.getNombre());
        if (request.getColor() != null) {
            rol.setColor(request.getColor());
        }
        if (request.getAccesoId() != null) {
            rol.setAcceso(accesoRepository.findById(request.getAccesoId()).orElseThrow());
        }
        return toResponse(rolRepository.save(rol));
    }

    @Transactional
    public void delete(Integer id) {
        Rol rol = findRol(id);
        validarRolNoProtegido(rol);
        if (tieneUsuarios(id)) {
            throw new RolConUsuariosException("No se puede eliminar el rol '" + rol.getNombre() + "' porque tiene usuarios asociados");
        }
        rol.setAcceso(accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow());
        rolRepository.save(rol);
    }

    private boolean tieneUsuarios(Integer id) {
        return !usuarioRolRepository.findByRolIdRol(id).isEmpty();
    }

    private void validarRolNoProtegido(Rol rol) {
        if (RolConstants.esAdmin(rol.getNombre())) {
            throw new IllegalArgumentException("El rol " + RolConstants.ADMIN + " está protegido y no puede modificarse ni eliminarse");
        }
    }

    private void validarNombreNoProtegido(String nombre) {
        if (RolConstants.esAdmin(nombre)) {
            throw new IllegalArgumentException("No se puede usar el nombre " + RolConstants.ADMIN + " porque pertenece al rol protegido del sistema");
        }
    }

    private Rol findRol(Integer id) {
        return rolRepository.findByIdRolAndAccesoNot(id, accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow())
                .orElseThrow(() -> new ResourceNotFoundException("Rol no encontrado con id " + id));
    }

    private RolResponse toResponse(Rol rol) {
        RolResponse response = new RolResponse();
        response.setIdRol(rol.getIdRol());
        response.setNombre(rol.getNombre());
        response.setColor(rol.getColor());
        response.setAccesoId(rol.getAcceso().getIdAcceso().longValue());
        return response;
    }
}
