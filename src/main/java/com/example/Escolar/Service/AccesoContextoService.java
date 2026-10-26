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
import com.example.Escolar.Security.ConcesionesService;
import com.example.Escolar.Security.UsuarioAutenticado;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
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
    private final ConcesionesService concesionesService;

    /** Qué parte de los datos de un módulo ve el usuario. */
    public enum Alcance { TODO, DOCENTE, APODERADO }

    private static final Set<String> ACCIONES_ESCRITURA = Set.of("CREAR", "ACTUALIZAR", "ELIMINAR");

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

    // Con escritura en el permiso del módulo lo gestiona y ve todo. Solo con lectura,
    // el docente ve lo suyo, el apoderado lo de sus hijos y cualquier otro usuario todo.
    public Alcance alcance(String permisoCodigo, Integer idUsuario, List<String> roles) {
        if (esAdmin(roles)) {
            return Alcance.TODO;
        }
        Set<String> acciones = concesionesService.accionesPorPermiso(idUsuario).getOrDefault(permisoCodigo, Set.of());
        if (acciones.stream().anyMatch(ACCIONES_ESCRITURA::contains)) {
            return Alcance.TODO;
        }
        return vinculo(idUsuario);
    }

    /** A quién está vinculado el usuario: a un docente, a un apoderado o a ninguno (TODO). */
    public Alcance vinculo(Integer idUsuario) {
        if (docenteDeUsuario(idUsuario).isPresent()) {
            return Alcance.DOCENTE;
        }
        if (apoderadoDeUsuario(idUsuario).isPresent()) {
            return Alcance.APODERADO;
        }
        return Alcance.TODO;
    }

    public boolean tieneAccion(String permisoCodigo, String accionCodigo, Integer idUsuario, List<String> roles) {
        return esAdmin(roles) || concesionesService.accionesPorPermiso(idUsuario)
                .getOrDefault(permisoCodigo, Set.of()).contains(accionCodigo);
    }

    public Optional<Docente> docenteDeUsuario(Integer idUsuario) {
        if (idUsuario == null) {
            return Optional.empty();
        }
        // Al quitarle el rol Docente su registro queda eliminado: ya no cuenta como vínculo.
        return docenteRepository.findByUsuarioIdUsuario(idUsuario)
                .filter(d -> !d.getAcceso().getIdAcceso().equals(AccesoConstants.ELIMINADO));
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
        matriculasDeHijos(apoderado).forEach(m -> ids.add(m.getGradoSeccion().getIdGradoSeccion()));
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

    /** Matrículas de todos los hijos del apoderado (principal o secundario), de cualquier año. */
    public List<Matricula> matriculasDeHijos(Apoderado apoderado) {
        List<Matricula> matriculas = new ArrayList<>();
        if (apoderado == null) {
            return matriculas;
        }
        for (AlumnoApoderado vinculo : alumnoApoderadoRepository.findByApoderado(apoderado)) {
            matriculas.addAll(matriculaRepository.findByAlumnoApoderadoAlumnoIdAlumnoAndAccesoNot(
                    vinculo.getAlumno().getIdAlumno(), accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow()));
        }
        return matriculas;
    }

    public boolean esApoderadoDe(Integer idUsuario, Matricula matricula) {
        Integer idAlumno = matricula.getAlumnoApoderado().getAlumno().getIdAlumno();
        return apoderadoDeUsuario(idUsuario)
                .map(apoderado -> alumnoApoderadoRepository.findByApoderado(apoderado).stream()
                        .anyMatch(v -> v.getAlumno().getIdAlumno().equals(idAlumno)))
                .orElse(false);
    }

    /**
     * Un alumno puede tener a la vez la matrícula del año vigente y la del año por
     * comenzar. La actual es la del vigente; si no la hay, la del año más reciente.
     */
    public Optional<Matricula> matriculaActual(Integer idAlumno) {
        return elegirActual(matriculaRepository.findByAlumnoApoderadoAlumnoIdAlumnoAndAccesoNot(
                idAlumno, accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow()));
    }

    public static Optional<Matricula> elegirActual(List<Matricula> matriculas) {
        return matriculas.stream()
                .max(Comparator.comparing((Matricula m) -> m.getGradoSeccion().getAnioEscolar().getEstado() == AnioEscolarService.ESTADO_VIGENTE)
                        .thenComparing(m -> m.getGradoSeccion().getAnioEscolar().getAnio()));
    }
}
