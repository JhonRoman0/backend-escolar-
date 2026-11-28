package com.example.Escolar.Service;

import com.example.Escolar.Dto.RolAsignadoSesionResponse;
import com.example.Escolar.Dto.RolAsignadoSesionResponse.EstadoRol;
import com.example.Escolar.Model.Acceso;
import com.example.Escolar.Model.Docente;
import com.example.Escolar.Model.EstadoSuspension;
import com.example.Escolar.Model.Rol;
import com.example.Escolar.Model.SuspensionDocente;
import com.example.Escolar.Model.UsuarioRol;
import com.example.Escolar.Repository.AccesoRepository;
import com.example.Escolar.Repository.DocenteRepository;
import com.example.Escolar.Repository.SuspensionDocenteRepository;
import com.example.Escolar.Repository.UsuarioRolRepository;
import com.example.Escolar.Util.FechaColegio;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RolAsignadoResolver {

    private final UsuarioRolRepository usuarioRolRepository;
    private final DocenteRepository docenteRepository;
    private final SuspensionDocenteRepository suspensionDocenteRepository;
    private final AccesoRepository accesoRepository;

    public List<RolAsignadoSesionResponse> resolver(Integer idUsuario) {
        List<Rol> roles = usuarioRolRepository.findByUsuarioIdUsuario(idUsuario).stream()
                .map(UsuarioRol::getRol)
                .filter(Objects::nonNull)
                .filter(rol -> !eliminado(rol))
                .toList();
        return resolver(idUsuario, roles);
    }

    public List<RolAsignadoSesionResponse> resolver(Integer idUsuario, List<Rol> roles) {
        List<Rol> rolesValidas = roles == null ? List.of() : roles.stream()
                .filter(Objects::nonNull)
                .filter(rol -> !eliminado(rol))
                .toList();
        boolean requiereDocente = rolesValidas.stream().anyMatch(rol -> activo(rol) && esDocente(rol));
        Optional<Docente> docente = requiereDocente
                ? docenteRepository.findByUsuarioIdUsuario(idUsuario)
                : Optional.empty();
        List<SuspensionDocente> suspensiones = docente
                .map(d -> suspensionDocenteRepository.findByDocenteAndAccesoNot(d, eliminado()))
                .orElseGet(List::of);
        LocalDate hoy = FechaColegio.hoy();

        return rolesValidas.stream()
                .map(rol -> new RolAsignadoSesionResponse(
                        rol.getIdRol(), rol.getNombre(), estado(rol, docente.orElse(null), suspensiones, hoy)))
                .toList();
    }

    private EstadoRol estado(Rol rol, Docente docente, List<SuspensionDocente> suspensiones, LocalDate hoy) {
        if (!activo(rol)) {
            return EstadoRol.INACTIVO;
        }
        if (!esDocente(rol)) {
            return EstadoRol.ACTIVO;
        }
        if (docente != null && suspensiones.stream().anyMatch(s -> aplicable(s, docente, hoy))) {
            return EstadoRol.SUSPENDIDO;
        }
        if (!activo(docente)) {
            return EstadoRol.INACTIVO;
        }
        return EstadoRol.ACTIVO;
    }

    private boolean activo(Rol rol) {
        return rol.getAcceso() != null && AccesoConstants.ACTIVO.equals(rol.getAcceso().getIdAcceso());
    }

    private boolean activo(Docente docente) {
        return docente != null && docente.getAcceso() != null
                && AccesoConstants.ACTIVO.equals(docente.getAcceso().getIdAcceso());
    }

    private boolean eliminado(Rol rol) {
        return rol.getAcceso() != null && AccesoConstants.ELIMINADO.equals(rol.getAcceso().getIdAcceso());
    }

    private boolean esDocente(Rol rol) {
        return rol.getNombre() != null
                && UsuarioService.ROL_DOCENTE.equalsIgnoreCase(rol.getNombre().trim());
    }

    private boolean aplicable(SuspensionDocente suspension, Docente docente, LocalDate hoy) {
        if (suspension.getAcceso() != null
                && AccesoConstants.ELIMINADO.equals(suspension.getAcceso().getIdAcceso())) {
            return false;
        }
        if (suspension.getDocente() == null
                || !Objects.equals(suspension.getDocente().getIdDocente(), docente.getIdDocente())) {
            return false;
        }
        EstadoSuspension estado = suspension.getEstado();
        if (estado == null || estado == EstadoSuspension.VIGENTE) {
            return true;
        }
        return estado == EstadoSuspension.PROGRAMADA
                && suspension.getFechaInicio() != null
                && !suspension.getFechaInicio().isAfter(hoy);
    }

    private Acceso eliminado() {
        return accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow();
    }
}
