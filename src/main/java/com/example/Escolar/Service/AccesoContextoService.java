package com.example.Escolar.Service;

import com.example.Escolar.Model.AlumnoApoderado;
import com.example.Escolar.Model.Apoderado;
import com.example.Escolar.Model.Docente;
import com.example.Escolar.Model.Matricula;
import com.example.Escolar.Repository.AccesoRepository;
import com.example.Escolar.Repository.AlumnoApoderadoRepository;
import com.example.Escolar.Repository.ApoderadoRepository;
import com.example.Escolar.Repository.AsignacionRepository;
import com.example.Escolar.Repository.DocenteRepository;
import com.example.Escolar.Repository.MatriculaRepository;
import com.example.Escolar.Security.UsuarioAutenticado;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AccesoContextoService {

    private final DocenteRepository docenteRepository;
    private final ApoderadoRepository apoderadoRepository;
    private final AlumnoApoderadoRepository alumnoApoderadoRepository;
    private final MatriculaRepository matriculaRepository;
    private final AsignacionRepository asignacionRepository;
    private final AccesoRepository accesoRepository;

    public Integer idUsuarioAutenticado() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UsuarioAutenticado principal = (UsuarioAutenticado) authentication.getPrincipal();
        return principal.idUsuario();
    }

    public List<String> rolesAutenticados() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UsuarioAutenticado principal = (UsuarioAutenticado) authentication.getPrincipal();
        return principal.roles();
    }

    public boolean esAdmin(List<String> roles) {
        return roles != null && roles.stream().anyMatch(r -> "ADMIN".equalsIgnoreCase(r));
    }

    public boolean esAdministrador(List<String> roles) {
        return roles != null && roles.stream().anyMatch(r -> "Administrador".equalsIgnoreCase(r));
    }

    public boolean esSecretaria(List<String> roles) {
        return roles != null && roles.stream().anyMatch(r -> "Secretaria".equalsIgnoreCase(r));
    }

    public boolean esDocente(List<String> roles) {
        return roles != null && roles.stream().anyMatch(r -> "DOCENTE".equalsIgnoreCase(r));
    }

    public boolean esAuxiliar(List<String> roles) {
        return roles != null && roles.stream().anyMatch(r -> "Auxiliar".equalsIgnoreCase(r));
    }

    public boolean esApoderado(List<String> roles) {
        return roles != null && roles.stream().anyMatch(r -> "APODERADO".equalsIgnoreCase(r));
    }

    public boolean esGestion(List<String> roles) {
        return esAdmin(roles) || esAdministrador(roles) || esSecretaria(roles);
    }

    public boolean esPersonalDocente(List<String> roles) {
        return esDocente(roles) || esAuxiliar(roles);
    }

    public Optional<Docente> docenteDeUsuario(Integer idUsuario) {
        if (idUsuario == null) {
            return Optional.empty();
        }
        return docenteRepository.findByUsuarioIdUsuario(idUsuario);
    }

    public Optional<Apoderado> apoderadoDeUsuario(Integer idUsuario) {
        if (idUsuario == null) {
            return Optional.empty();
        }
        return apoderadoRepository.findByUsuarioIdUsuarioAndAccesoNot(idUsuario, accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow());
    }

    public Set<Integer> gradoSeccionIdsDeApoderado(Apoderado apoderado) {
        Set<Integer> ids = new HashSet<>();
        if (apoderado == null) {
            return ids;
        }
        for (AlumnoApoderado alumnoApoderado : alumnoApoderadoRepository.findByApoderado(apoderado)) {
            matriculaRepository.findByAlumnoApoderadoAndAccesoNot(alumnoApoderado, accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow())
                    .ifPresent(m -> ids.add(m.getGradoSeccion().getIdGradoSeccion()));
        }
        return ids;
    }

    public Set<Integer> gradoSeccionIdsDeDocente(Integer idUsuario) {
        Set<Integer> ids = new HashSet<>();
        docenteDeUsuario(idUsuario).ifPresent(docente ->
                asignacionRepository.findByDocenteIdDocenteAndAccesoNot(docente.getIdDocente(), accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow())
                        .forEach(asignacion -> ids.add(asignacion.getGradoSeccion().getIdGradoSeccion())));
        return ids;
    }

    public List<Matricula> matriculasDeGradoSecciones(Set<Integer> gradoSeccionIds) {
        List<Matricula> matriculas = new ArrayList<>();
        if (gradoSeccionIds == null || gradoSeccionIds.isEmpty()) {
            return matriculas;
        }
        for (Integer idGradoSeccion : gradoSeccionIds) {
            matriculaRepository.findByGradoSeccionIdGradoSeccionAndAccesoNot(idGradoSeccion, accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow())
                    .forEach(matriculas::add);
        }
        return matriculas;
    }
}
