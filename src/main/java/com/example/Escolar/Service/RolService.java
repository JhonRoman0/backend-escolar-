package com.example.Escolar.Service;

import com.example.Escolar.Dto.RolRequest;
import com.example.Escolar.Dto.RolResponse;
import com.example.Escolar.Exception.ResourceNotFoundException;
import com.example.Escolar.Exception.RolConUsuariosException;
import com.example.Escolar.Model.Acceso;
import com.example.Escolar.Model.Rol;
import com.example.Escolar.Repository.AccesoRepository;
import com.example.Escolar.Repository.RolRepository;
import com.example.Escolar.Repository.UsuarioRolRepository;
import com.example.Escolar.Security.ConcesionesService;
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
    private final ConcesionesService concesionesService;

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
        validarNombreUnico(request.getNombre(), null);
        Rol rol = new Rol();
        rol.setNombre(request.getNombre().trim());
        rol.setColor(request.getColor());
        rol.setAcceso(accesoParaRol(request.getAccesoId() != null ? request.getAccesoId() : AccesoConstants.ACTIVO));
        return toResponse(rolRepository.save(rol));
    }

    @Transactional
    public RolResponse update(Integer id, RolRequest request) {
        Rol rol = findRol(id);
        validarRolNoProtegido(rol);
        validarNombreNoProtegido(request.getNombre());
        validarNombreUnico(request.getNombre(), id);
        Acceso nuevoAcceso = request.getAccesoId() == null ? null : accesoParaRol(request.getAccesoId());
        if (request.getAccesoId() != null
                && request.getAccesoId().equals(AccesoConstants.INACTIVO)
                && !rol.getAcceso().getIdAcceso().equals(AccesoConstants.INACTIVO)
                && tieneUsuarios(id)) {
            throw new RolConUsuariosException("No se puede inactivar el rol '" + rol.getNombre() + "' porque tiene usuarios asociados");
        }
        rol.setNombre(request.getNombre().trim());
        if (request.getColor() != null) {
            rol.setColor(request.getColor());
        }
        if (nuevoAcceso != null) {
            rol.setAcceso(nuevoAcceso);
        }
        RolResponse response = toResponse(rolRepository.save(rol));
        concesionesService.invalidarPermisosDelRol(id);
        return response;
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
        concesionesService.invalidarPermisosDelRol(id);
    }

    /** Un rol se activa o inactiva al editarlo; eliminarlo es con DELETE (revisa sus usuarios). */
    private Acceso accesoParaRol(Long accesoId) {
        if (!AccesoConstants.ACTIVO.equals(accesoId) && !AccesoConstants.INACTIVO.equals(accesoId)) {
            throw new IllegalArgumentException("Estado no válido: para eliminar un rol usa la opción Eliminar");
        }
        return accesoRepository.findById(accesoId).orElseThrow();
    }

    /** Sin distinguir mayúsculas ni espacios: "Docente" y " docente " son el mismo rol. */
    private void validarNombreUnico(String nombre, Integer idExcluido) {
        String buscado = nombre.trim();
        boolean repetido = rolRepository.findByAccesoNot(accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow())
                .stream()
                .anyMatch(r -> !r.getIdRol().equals(idExcluido) && r.getNombre() != null
                        && r.getNombre().trim().equalsIgnoreCase(buscado));
        if (repetido) {
            throw new IllegalArgumentException("Ya existe un rol llamado " + buscado);
        }
    }

    private boolean tieneUsuarios(Integer id) {
        return !usuarioRolRepository.findByRolIdRol(id).isEmpty();
    }

    private void validarRolNoProtegido(Rol rol) {
        if (RolConstants.esProtegido(rol.getNombre())) {
            throw new IllegalArgumentException("El rol " + rol.getNombre().trim() + " está protegido y no puede modificarse ni eliminarse");
        }
    }

    private void validarNombreNoProtegido(String nombre) {
        if (RolConstants.esProtegido(nombre)) {
            throw new IllegalArgumentException("No se puede usar el nombre " + nombre.trim() + " porque pertenece a un rol protegido del sistema");
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
