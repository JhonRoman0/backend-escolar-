package com.example.Escolar.Service;

import com.example.Escolar.Model.AlumnoApoderado;
import com.example.Escolar.Model.Apoderado;
import com.example.Escolar.Model.Asignacion;
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
    public enum Alcance {
        TODO, DOCENTE, APODERADO, DOCENTE_Y_APODERADO, NINGUNO;

        public boolean incluyeDocente() {
            return this == DOCENTE || this == DOCENTE_Y_APODERADO;
        }

        public boolean incluyeApoderado() {
            return this == APODERADO || this == DOCENTE_Y_APODERADO;
        }
    }

    /** Operación que se autoriza; el alcance se calcula junto con el rol que concede la acción. */
    public enum Operacion { LECTURA, DETALLE, ESCRITURA, REPORTE, EXPORTACION }

    private static final Set<String> ACCIONES_ESCRITURA = Set.of("CREAR", "ACTUALIZAR", "ELIMINAR", "CONFIGURAR");
    private static final Set<String> ACCIONES_REPORTE = Set.of(
            "IMPRIMIR_EXPORTAR", "IMPRIMIR_FICHA", "IMPRIMIR_BOLETA", "EXPORTAR_SIAGIE", "VER_RESUMEN", "VER_COLEGIO");
    private static final Set<String> PERMISOS_VINCULADOS = Set.of(
            "NOTAS", "NOTAS_ANUALES", "EVALUACIONES", "ASIGNACIONES", "ASISTENCIAS", "HORARIOS",
            "PORTAL_APODERADO", "ALUMNOS", "MATRICULAS", "APODERADOS");

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

    public Alcance alcance(String permisoCodigo, Integer idUsuario, List<String> roles) {
        return alcance(permisoCodigo, Operacion.LECTURA, idUsuario, roles);
    }

    /**
     * Permisos y alcance se resuelven juntos, por rol y operación. La unión de
     * acciones de un rol nunca se combina con el vínculo de otro.
     */
    public Alcance alcance(String permisoCodigo, Operacion operacion, Integer idUsuario, List<String> roles) {
        if (esAdmin(roles)) {
            return Alcance.TODO;
        }

        boolean concedido = concesionesService.accionesPorPermisoYRol(idUsuario).values().stream()
                .anyMatch(permisos -> permisos.containsKey(permisoCodigo)
                        && permiteOperacion(operacion, permisos.get(permisoCodigo)));
        if (!concedido) {
            return Alcance.NINGUNO;
        }
        if (!PERMISOS_VINCULADOS.contains(permisoCodigo)) {
            return Alcance.TODO;
        }
        boolean docente = !asignacionesActivasDeDocente(idUsuario).isEmpty();
        boolean apoderado = operacion != Operacion.ESCRITURA
                && !"EVALUACIONES".equals(permisoCodigo)
                && tieneAlumnosVinculados(idUsuario);
        if (docente && apoderado) {
            return Alcance.DOCENTE_Y_APODERADO;
        }
        if (docente) {
            return Alcance.DOCENTE;
        }
        return apoderado ? Alcance.APODERADO : Alcance.NINGUNO;
    }

    public Alcance vinculoEfectivo(Integer idUsuario, List<String> roles) {
        return esAdmin(roles) ? Alcance.TODO : vinculo(idUsuario);
    }

    public Alcance vinculo(Integer idUsuario) {
        if (!asignacionesActivasDeDocente(idUsuario).isEmpty()) {
            return Alcance.DOCENTE;
        }
        return tieneAlumnosVinculados(idUsuario) ? Alcance.APODERADO : Alcance.NINGUNO;
    }

    private boolean tieneAlumnosVinculados(Integer idUsuario) {
        return apoderadoDeUsuario(idUsuario)
                .map(apoderado -> !alumnoApoderadoRepository.findByApoderado(apoderado).isEmpty())
                .orElse(false);
    }

    private boolean permiteOperacion(Operacion operacion, Set<String> acciones) {
        return switch (operacion) {
            case LECTURA, DETALLE -> true;
            case ESCRITURA -> acciones.stream().anyMatch(ACCIONES_ESCRITURA::contains);
            case REPORTE, EXPORTACION -> acciones.stream().anyMatch(ACCIONES_REPORTE::contains);
        };
    }

    public List<Asignacion> asignacionesActivasDeDocente(Integer idUsuario) {
        return docenteDeUsuario(idUsuario)
                .map(d -> asignacionRepository.findByDocenteIdDocenteAndAcceso(
                        d.getIdDocente(), accesoRepository.findById(AccesoConstants.ACTIVO).orElseThrow()))
                .orElseGet(List::of);
    }

    public boolean tieneAccion(String permisoCodigo, String accionCodigo, Integer idUsuario, List<String> roles) {
        return esAdmin(roles) || concesionesService.accionesPorPermiso(idUsuario)
                .getOrDefault(permisoCodigo, Set.of()).contains(accionCodigo);
    }

    /** Tiene el permiso, aunque sea solo para leer. */
    public boolean tienePermiso(String permisoCodigo, Integer idUsuario, List<String> roles) {
        return esAdmin(roles) || concesionesService.accionesPorPermiso(idUsuario).containsKey(permisoCodigo);
    }

    public Optional<Docente> docenteDeUsuario(Integer idUsuario) {
        if (idUsuario == null) {
            return Optional.empty();
        }
        return docenteRepository.findByUsuarioIdUsuario(idUsuario)
                .filter(d -> AccesoConstants.ACTIVO.equals(d.getAcceso().getIdAcceso()));
    }

    public Optional<Apoderado> apoderadoDeUsuario(Integer idUsuario) {
        if (idUsuario == null) {
            return Optional.empty();
        }
        return apoderadoRepository.findByUsuarioIdUsuarioAndAccesoNot(idUsuario, accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow())
                .filter(a -> AccesoConstants.ACTIVO.equals(a.getAcceso().getIdAcceso()));
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
        asignacionesActivasDeDocente(idUsuario)
                .forEach(asignacion -> ids.add(asignacion.getGradoSeccion().getIdGradoSeccion()));
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
        List<Integer> idsAlumnos = alumnoApoderadoRepository.findByApoderado(apoderado).stream()
                .filter(AlumnoApoderado::isActivo)
                .map(vinculo -> vinculo.getAlumno().getIdAlumno())
                .toList();
        if (idsAlumnos.isEmpty()) {
            return matriculas;
        }
        matriculas.addAll(matriculaRepository.findActivasPorAlumnoIds(
                idsAlumnos, accesoRepository.findById(AccesoConstants.ACTIVO).orElseThrow()));
        return matriculas;
    }

    public boolean esApoderadoDe(Integer idUsuario, Matricula matricula) {
        Integer idAlumno = matricula.getAlumnoApoderado().getAlumno().getIdAlumno();
        return apoderadoDeUsuario(idUsuario)
                .map(apoderado -> alumnoApoderadoRepository.findByApoderado(apoderado).stream()
                        .filter(AlumnoApoderado::isActivo)
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
                        .thenComparing(m -> m.getGradoSeccion().getAnioEscolar().getAnio())
                        // Solo desempata: una matrícula antigua puede apuntar a quien ya no es principal.
                        .thenComparing(m -> m.getAlumnoApoderado().getApoPrincipal()));
    }
}
