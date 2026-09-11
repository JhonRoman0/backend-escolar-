package com.example.Escolar.Service;

import com.example.Escolar.Dto.RolRequest;
import com.example.Escolar.Dto.RolResponse;
import com.example.Escolar.Exception.ResourceNotFoundException;
import com.example.Escolar.Model.Rol;
import com.example.Escolar.Repository.RolRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RolService {

    public static final byte ACCESO_ACTIVO = 1;
    public static final byte ACCESO_ELIMINADO = 2;

    private final RolRepository rolRepository;

    public List<RolResponse> getAll() {
        return rolRepository.findByAccesoNot(ACCESO_ELIMINADO).stream().map(this::toResponse).toList();
    }

    public RolResponse getById(Integer id) {
        return toResponse(findRol(id));
    }

    @Transactional
    public RolResponse create(RolRequest request) {
        Rol rol = new Rol();
        rol.setNombre(request.getNombre());
        rol.setAcceso(request.getAcceso() != null ? request.getAcceso() : ACCESO_ACTIVO);
        return toResponse(rolRepository.save(rol));
    }

    @Transactional
    public RolResponse update(Integer id, RolRequest request) {
        Rol rol = findRol(id);
        rol.setNombre(request.getNombre());
        if (request.getAcceso() != null) {
            rol.setAcceso(request.getAcceso());
        }
        return toResponse(rolRepository.save(rol));
    }

    @Transactional
    public void delete(Integer id) {
        Rol rol = findRol(id);
        rol.setAcceso(ACCESO_ELIMINADO);
        rolRepository.save(rol);
    }

    private Rol findRol(Integer id) {
        return rolRepository.findByIdRolAndAccesoNot(id, ACCESO_ELIMINADO)
                .orElseThrow(() -> new ResourceNotFoundException("Rol no encontrado con id " + id));
    }

    private RolResponse toResponse(Rol rol) {
        RolResponse response = new RolResponse();
        response.setIdRol(rol.getIdRol());
        response.setNombre(rol.getNombre());
        response.setAcceso(rol.getAcceso());
        return response;
    }
}
