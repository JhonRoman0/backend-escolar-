package com.example.Escolar.Config;

import com.example.Escolar.Model.Accion;
import com.example.Escolar.Model.Acceso;
import com.example.Escolar.Model.Competencia;
import com.example.Escolar.Model.Curso;
import com.example.Escolar.Model.EstadoAsistencia;
import com.example.Escolar.Model.Modulo;
import com.example.Escolar.Model.Nivel;
import com.example.Escolar.Model.Permiso;
import com.example.Escolar.Model.PermisoAccion;
import com.example.Escolar.Model.Rol;
import com.example.Escolar.Model.RolPermiso;
import com.example.Escolar.Model.RolPermisoAccion;
import com.example.Escolar.Model.Usuario;
import com.example.Escolar.Model.UsuarioRol;
import com.example.Escolar.Repository.AccionRepository;
import com.example.Escolar.Repository.AccesoRepository;
import com.example.Escolar.Repository.CompetenciaRepository;
import com.example.Escolar.Repository.CursoRepository;
import com.example.Escolar.Repository.EstadoAsistenciaRepository;
import com.example.Escolar.Repository.ModuloRepository;
import com.example.Escolar.Repository.NivelRepository;
import com.example.Escolar.Repository.PermisoAccionRepository;
import com.example.Escolar.Repository.PermisoRepository;
import com.example.Escolar.Repository.RolPermisoAccionRepository;
import com.example.Escolar.Repository.RolPermisoRepository;
import com.example.Escolar.Repository.RolRepository;
import com.example.Escolar.Repository.UsuarioRepository;
import com.example.Escolar.Repository.UsuarioRolRepository;
import com.example.Escolar.Service.AccesoConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final AccionRepository accionRepository;
    private final AccesoRepository accesoRepository;
    private final EstadoAsistenciaRepository estadoAsistenciaRepository;
    private final RolRepository rolRepository;
    private final UsuarioRepository usuarioRepository;
    private final UsuarioRolRepository usuarioRolRepository;
    private final ModuloRepository moduloRepository;
    private final PermisoRepository permisoRepository;
    private final PermisoAccionRepository permisoAccionRepository;
    private final RolPermisoRepository rolPermisoRepository;
    private final RolPermisoAccionRepository rolPermisoAccionRepository;
    private final NivelRepository nivelRepository;
    private final CompetenciaRepository competenciaRepository;
    private final CursoRepository cursoRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${jwt.admin.codigo}")
    private String adminCodigo;

    @Value("${jwt.admin.password}")
    private String adminPassword;

    @Value("${jwt.admin.nombre}")
    private String adminNombre;

    @Value("${jwt.admin.apellido-pat}")
    private String adminApellidoPat;

    @Value("${jwt.admin.apellido-mat}")
    private String adminApellidoMat;

    @Value("${jwt.admin.gmail}")
    private String adminGmail;

    @Value("${jwt.admin.rol}")
    private String adminRol;

    @Value("${app.seed-admin.enabled:true}")
    private boolean seedAdminEnabled;

    @Override
    public void run(String... args) {
        seedAcceso();
        seedAcciones();
        seedEstadosAsistencia();
        seedNiveles();
        seedCompetencias();
        seedRoles();
        seedModulosRbac();
        seedMatrizRbac();
        if (seedAdminEnabled) {
            seedAdmin();
        }
    }

    private void seedAcceso() {
        crearAccesoSiFalta(AccesoConstants.ACTIVO, "Activo");
        crearAccesoSiFalta(AccesoConstants.ELIMINADO, "Eliminado");
        crearAccesoSiFalta(AccesoConstants.INACTIVO, "Inactivo");
    }

    private void crearAccesoSiFalta(Long id, String nombre) {
        if (accesoRepository.findById(id).isPresent()) {
            return;
        }
        Acceso acceso = new Acceso();
        acceso.setIdAcceso(id);
        acceso.setNombre(nombre);
        accesoRepository.save(acceso);
    }

    private void seedEstadosAsistencia() {
        crearEstadoAsistenciaSiFalta("Puntual");
        crearEstadoAsistenciaSiFalta("Tardanza");
        crearEstadoAsistenciaSiFalta("Inasistencia");
        crearEstadoAsistenciaSiFalta("Justificada");
    }

    private void crearEstadoAsistenciaSiFalta(String nombre) {
        if (estadoAsistenciaRepository.findByNombreIgnoreCaseAndAccesoNot(nombre, accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow()).isPresent()) {
            return;
        }
        EstadoAsistencia estado = new EstadoAsistencia();
        estado.setNombre(nombre);
        estado.setAcceso(accesoRepository.findById(AccesoConstants.ACTIVO).orElseThrow());
        estadoAsistenciaRepository.save(estado);
    }

    private void seedNiveles() {
        crearNivelSiFalta("Inicial");
        crearNivelSiFalta("Primaria");
        crearNivelSiFalta("Secundaria");
    }

    private void crearNivelSiFalta(String nombre) {
        if (nivelRepository.findByNombreAndAccesoNot(nombre, accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow()).isPresent()) {
            return;
        }
        Nivel nivel = new Nivel();
        nivel.setNombre(nombre);
        nivel.setAcceso(accesoRepository.findById(AccesoConstants.ACTIVO).orElseThrow());
        nivelRepository.save(nivel);
    }

    private void seedCompetencias() {
        crearCompetenciaSiFalta("Comunicación", (byte) 1, "Se comunica oralmente en su lengua materna");
        crearCompetenciaSiFalta("Comunicación", (byte) 2, "Lee diversos tipos de textos escritos en lengua materna");
        crearCompetenciaSiFalta("Comunicación", (byte) 3, "Escribe diversos tipos de textos en lengua materna");

        crearCompetenciaSiFalta("Matemática", (byte) 1, "Resuelve problemas de cantidad");
        crearCompetenciaSiFalta("Matemática", (byte) 2, "Resuelve problemas de forma, movimiento y localización");
        crearCompetenciaSiFalta("Matemática", (byte) 3, "Resuelve problemas de regularidad, equivalencia y cambio");
        crearCompetenciaSiFalta("Matemática", (byte) 4, "Resuelve problemas de gestión de datos e incertidumbre");

        crearCompetenciaSiFalta("Personal Social", (byte) 1, "Construye su identidad");
        crearCompetenciaSiFalta("Personal Social", (byte) 2, "Convive y participa democráticamente");
        crearCompetenciaSiFalta("Personal Social", (byte) 3, "Construye interpretaciones históricas");
        crearCompetenciaSiFalta("Personal Social", (byte) 4, "Gestiona responsablemente el espacio y el ambiente");
        crearCompetenciaSiFalta("Personal Social", (byte) 5, "Gestiona responsablemente los recursos económicos");

        crearCompetenciaSiFalta("Ciencia y Tecnología", (byte) 1, "Indaga mediante métodos científicos para construir conocimientos");
        crearCompetenciaSiFalta("Ciencia y Tecnología", (byte) 2, "Explica el mundo natural y artificial basándose en conocimientos científicos");
        crearCompetenciaSiFalta("Ciencia y Tecnología", (byte) 3, "Diseña soluciones tecnológicas para resolver problemas de su entorno");

        crearCompetenciaSiFalta("Educación Física", (byte) 1, "Se desenvuelve de manera autónoma a través de su motricidad");
        crearCompetenciaSiFalta("Educación Física", (byte) 2, "Asume una vida saludable");
        crearCompetenciaSiFalta("Educación Física", (byte) 3, "Interactúa a través de sus habilidades sociomotrices");

        crearCompetenciaSiFalta("Arte y Cultura", (byte) 1, "Aprecia de manera crítica manifestaciones artístico-culturales");
        crearCompetenciaSiFalta("Arte y Cultura", (byte) 2, "Crea proyectos artísticos desde los lenguajes artísticos");

        crearCompetenciaSiFalta("Inglés", (byte) 1, "Se comunica oralmente en lengua extranjera");
        crearCompetenciaSiFalta("Inglés", (byte) 2, "Lee diversos tipos de textos en lengua extranjera");
        crearCompetenciaSiFalta("Inglés", (byte) 3, "Escribe diversos tipos de textos en lengua extranjera");

        crearCompetenciaSiFalta("Religión", (byte) 1, "Fortalece su dimensión espiritual yax de vida");
        crearCompetenciaSiFalta("Religión", (byte) 2, "Aprecia manifestaciones religiosas y/o espirituales");
    }

    private void crearCompetenciaSiFalta(String nombreCurso, byte orden, String nombreCompetencia) {
        Curso curso = cursoRepository.findByNombreAndAccesoNot(nombreCurso, accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow()).orElse(null);
        if (curso == null) {
            return;
        }
        if (competenciaRepository.findByCursoIdCursoAndNombreAndAccesoNot(
                curso.getIdCurso(), nombreCompetencia, accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow()).isPresent()) {
            return;
        }
        Competencia competencia = new Competencia();
        competencia.setCurso(curso);
        competencia.setNombre(nombreCompetencia);
        competencia.setOrden(orden);
        competencia.setAcceso(accesoRepository.findById(AccesoConstants.ACTIVO).orElseThrow());
        competenciaRepository.save(competencia);
    }

    private void seedAcciones() {
        crearAccionSiFalta("CREAR", "Crear");
        crearAccionSiFalta("ACTUALIZAR", "Actualizar");
        crearAccionSiFalta("ELIMINAR", "Eliminar");
        crearAccionSiFalta("IMPRIMIR_EXPORTAR", "Imprimir/Exportar");
        crearAccionSiFalta("AUTORIZAR", "Autorizar");
    }

    private void crearAccionSiFalta(String codigo, String nombre) {
        if (accionRepository.findByCodigoAndAccesoNot(codigo, accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow()).isPresent()) {
            return;
        }
        Accion accion = new Accion();
        accion.setCodigo(codigo);
        accion.setNombre(nombre);
        accion.setAcceso(accesoRepository.findById(AccesoConstants.ACTIVO).orElseThrow());
        accionRepository.save(accion);
    }

    private void seedRoles() {
        crearRolSiFalta("Administrador");
        crearRolSiFalta("Secretaria");
        crearRolSiFalta("Docente");
        crearRolSiFalta("Apoderado");
        crearRolSiFalta("Auxiliar");
    }

    private Rol crearRolSiFalta(String nombre) {
        return rolRepository.findByNombre(nombre).orElseGet(() -> {
            Rol rol = new Rol();
            rol.setNombre(nombre);
            rol.setAcceso(accesoRepository.findById(AccesoConstants.ACTIVO).orElseThrow());
            return rolRepository.save(rol);
        });
    }

    private void seedModulosRbac() {
        crearModuloSiFalta(new ModuloSeed("Seguridad", "shield", List.of(
                new PermisoSeed("USUARIOS", "Usuarios", crudWithExport()),
                new PermisoSeed("ROLES", "Roles", crud()),
                new PermisoSeed("MODULOS", "Módulos", crud()),
                new PermisoSeed("PERMISOS", "Permisos", crud()),
                new PermisoSeed("ACCIONES", "Acciones", crud()),
                new PermisoSeed("ROLES_PERMISOS", "Roles y Permisos", crud()))));

        crearModuloSiFalta(new ModuloSeed("Académico", "book", List.of(
                new PermisoSeed("DOCENTES", "Docentes", crudWithExport()),
                new PermisoSeed("CURSOS", "Cursos", crud()),
                new PermisoSeed("TURNOS", "Turnos", crud()),
                new PermisoSeed("GRADOS", "Grados", crud()),
                new PermisoSeed("ANIOS_ESCOLARES", "Años Escolares", crud()),
                new PermisoSeed("AULAS", "Aulas", crud()),
                new PermisoSeed("ASIGNACIONES", "Asignaciones", crud()),
                new PermisoSeed("RECREOS", "Recreos", crud()),
                new PermisoSeed("SUSPENSIONES_DOCENTE", "Suspensiones de Docente", List.of("CREAR")),
                new PermisoSeed("CAMBIOS_DOCENTE", "Cambios de Docente", List.of()))));

        crearModuloSiFalta(new ModuloSeed("Estudiantes", "user-graduate", List.of(
                new PermisoSeed("ALUMNOS", "Alumnos", crudWithExport()),
                new PermisoSeed("APODERADOS", "Apoderados", crud()))));

        crearModuloSiFalta(new ModuloSeed("Matricula", "clipboard-list", List.of(
                new PermisoSeed("MATRICULAS", "Matrículas", crudWithExport()))));

        crearModuloSiFalta(new ModuloSeed("Consolidado", "clipboard-check", List.of(
                new PermisoSeed("CONSOLIDADOS", "Consolidados", List.of("CREAR", "ACTUALIZAR", "IMPRIMIR_EXPORTAR")),
                new PermisoSeed("COMPETENCIAS", "Competencias", crud()),
                new PermisoSeed("NOTAS", "Notas", List.of("CREAR", "AUTORIZAR", "IMPRIMIR_EXPORTAR")))));

        crearModuloSiFalta(new ModuloSeed("Portal", "globe", List.of(
                new PermisoSeed("PUBLICACIONES", "Publicaciones", crud()),
                new PermisoSeed("EVENTOS", "Eventos", crud()),
                new PermisoSeed("GALERIAS", "Galerías", crud()),
                new PermisoSeed("CONTACTOS", "Contactos", List.of("CREAR", "ACTUALIZAR")),
                new PermisoSeed("AJUSTES", "Ajustes", crud()))));

        crearModuloSiFalta(new ModuloSeed("Colegio", "school", List.of(
                new PermisoSeed("COLEGIOS", "Datos del Colegio", crud()))));

        crearModuloSiFalta(new ModuloSeed("Asistencia", "user-check", List.of(
                new PermisoSeed("ASISTENCIAS", "Asistencia", List.of("CREAR", "ACTUALIZAR", "ELIMINAR", "IMPRIMIR_EXPORTAR")))));

        crearModuloSiFalta(new ModuloSeed("Horario", "horario", List.of(
                new PermisoSeed("HORARIOS", "Horarios", List.of("CREAR", "ACTUALIZAR", "ELIMINAR", "IMPRIMIR_EXPORTAR")))));

        crearModuloSiFalta(new ModuloSeed("Plantilla SIAGIE", "file-import", List.of(
                new PermisoSeed("PLANTILLAS", "Plantillas SIAGIE", List.of("CREAR", "IMPRIMIR_EXPORTAR")))));
    }

    private void seedMatrizRbac() {
        asignarPermiso("Administrador", "USUARIOS", crudWithExport());
        asignarPermiso("Administrador", "ROLES", crud());
        asignarPermiso("Administrador", "MODULOS", crud());
        asignarPermiso("Administrador", "PERMISOS", crud());
        asignarPermiso("Administrador", "ACCIONES", crud());
        asignarPermiso("Administrador", "ROLES_PERMISOS", crud());
        asignarPermiso("Administrador", "DOCENTES", crudWithExport());
        asignarPermiso("Administrador", "CURSOS", crud());
        asignarPermiso("Administrador", "TURNOS", crud());
        asignarPermiso("Administrador", "GRADOS", crud());
        asignarPermiso("Administrador", "ANIOS_ESCOLARES", crud());
        asignarPermiso("Administrador", "AULAS", crud());
        asignarPermiso("Administrador", "ASIGNACIONES", crud());
        asignarPermiso("Administrador", "RECREOS", crud());
        asignarPermiso("Administrador", "SUSPENSIONES_DOCENTE", List.of("CREAR"));
        asignarPermiso("Administrador", "CAMBIOS_DOCENTE", List.of());
        asignarPermiso("Administrador", "ALUMNOS", crudWithExport());
        asignarPermiso("Administrador", "APODERADOS", crud());
        asignarPermiso("Administrador", "MATRICULAS", crudWithExport());
        asignarPermiso("Administrador", "CONSOLIDADOS", List.of("CREAR", "ACTUALIZAR", "IMPRIMIR_EXPORTAR"));
        asignarPermiso("Administrador", "COMPETENCIAS", crud());
        asignarPermiso("Administrador", "NOTAS", List.of("CREAR", "AUTORIZAR", "IMPRIMIR_EXPORTAR"));
        asignarPermiso("Administrador", "PUBLICACIONES", crud());
        asignarPermiso("Administrador", "EVENTOS", crud());
        asignarPermiso("Administrador", "GALERIAS", crud());
        asignarPermiso("Administrador", "CONTACTOS", List.of("CREAR", "ACTUALIZAR"));
        asignarPermiso("Administrador", "AJUSTES", crud());
        asignarPermiso("Administrador", "COLEGIOS", crud());
        asignarPermiso("Administrador", "ASISTENCIAS", List.of("CREAR", "ACTUALIZAR", "ELIMINAR", "IMPRIMIR_EXPORTAR"));

        asignarPermiso("Secretaria", "USUARIOS", List.of());
        asignarPermiso("Secretaria", "ROLES", List.of());
        asignarPermiso("Secretaria", "DOCENTES", List.of("CREAR", "ACTUALIZAR"));
        asignarPermiso("Secretaria", "CURSOS", List.of("CREAR", "ACTUALIZAR"));
        asignarPermiso("Secretaria", "TURNOS", List.of("CREAR", "ACTUALIZAR"));
        asignarPermiso("Secretaria", "GRADOS", List.of("CREAR", "ACTUALIZAR"));
        asignarPermiso("Secretaria", "ANIOS_ESCOLARES", List.of("CREAR", "ACTUALIZAR"));
        asignarPermiso("Secretaria", "AULAS", List.of("CREAR", "ACTUALIZAR"));
        asignarPermiso("Secretaria", "ASIGNACIONES", List.of("CREAR", "ACTUALIZAR"));
        asignarPermiso("Secretaria", "RECREOS", List.of("CREAR", "ACTUALIZAR"));
        asignarPermiso("Secretaria", "SUSPENSIONES_DOCENTE", List.of("CREAR"));
        asignarPermiso("Secretaria", "CAMBIOS_DOCENTE", List.of());
        asignarPermiso("Secretaria", "ALUMNOS", List.of("CREAR", "ACTUALIZAR"));
        asignarPermiso("Secretaria", "APODERADOS", List.of("CREAR", "ACTUALIZAR"));
        asignarPermiso("Secretaria", "MATRICULAS", List.of("CREAR", "ACTUALIZAR"));
        asignarPermiso("Secretaria", "CONSOLIDADOS", List.of());
        asignarPermiso("Secretaria", "COMPETENCIAS", List.of());
        asignarPermiso("Secretaria", "NOTAS", List.of());
        asignarPermiso("Secretaria", "PUBLICACIONES", List.of("CREAR", "ACTUALIZAR"));
        asignarPermiso("Secretaria", "EVENTOS", List.of("CREAR", "ACTUALIZAR"));
        asignarPermiso("Secretaria", "GALERIAS", List.of("CREAR", "ACTUALIZAR"));
        asignarPermiso("Secretaria", "CONTACTOS", List.of("CREAR", "ACTUALIZAR"));
        asignarPermiso("Secretaria", "AJUSTES", List.of("CREAR", "ACTUALIZAR"));
        asignarPermiso("Secretaria", "COLEGIOS", List.of());
        asignarPermiso("Secretaria", "ASISTENCIAS", List.of("CREAR", "ACTUALIZAR"));

        asignarPermiso("Auxiliar", "ASISTENCIAS", List.of("CREAR", "ACTUALIZAR"));
        asignarPermiso("Apoderado", "ALUMNOS", List.of());
        asignarPermiso("Apoderado", "APODERADOS", List.of());
        asignarPermiso("Apoderado", "ASIGNACIONES", List.of());
        asignarPermiso("Apoderado", "RECREOS", List.of());
        asignarPermiso("Apoderado", "MATRICULAS", List.of());
        asignarPermiso("Apoderado", "CONSOLIDADOS", List.of());
        asignarPermiso("Apoderado", "COMPETENCIAS", List.of());
        asignarPermiso("Apoderado", "NOTAS", List.of());
        asignarPermiso("Apoderado", "COLEGIOS", List.of());
        asignarPermiso("Apoderado", "ASISTENCIAS", List.of());

        asignarPermiso("Docente", "DOCENTES", List.of());
        asignarPermiso("Docente", "CURSOS", List.of());
        asignarPermiso("Docente", "TURNOS", List.of());
        asignarPermiso("Docente", "GRADOS", List.of());
        asignarPermiso("Docente", "ANIOS_ESCOLARES", List.of());
        asignarPermiso("Docente", "AULAS", List.of());
        asignarPermiso("Docente", "ALUMNOS", List.of());
        asignarPermiso("Docente", "APODERADOS", List.of());
        asignarPermiso("Docente", "ASIGNACIONES", List.of());
        asignarPermiso("Docente", "RECREOS", List.of());
        asignarPermiso("Docente", "SUSPENSIONES_DOCENTE", List.of());
        asignarPermiso("Docente", "CAMBIOS_DOCENTE", List.of());
        asignarPermiso("Docente", "MATRICULAS", List.of());
        asignarPermiso("Docente", "CONSOLIDADOS", List.of("CREAR", "ACTUALIZAR"));
        asignarPermiso("Docente", "COMPETENCIAS", List.of());
        asignarPermiso("Docente", "NOTAS", List.of("CREAR"));
        asignarPermiso("Docente", "COLEGIOS", List.of());
        asignarPermiso("Docente", "ASISTENCIAS", List.of());

        asignarPermiso("Administrador", "HORARIOS", List.of("CREAR", "ACTUALIZAR", "ELIMINAR", "IMPRIMIR_EXPORTAR"));
        asignarPermiso("Secretaria", "HORARIOS", List.of("CREAR", "ACTUALIZAR"));
        asignarPermiso("Docente", "HORARIOS", List.of("IMPRIMIR_EXPORTAR"));
        asignarPermiso("Apoderado", "HORARIOS", List.of("IMPRIMIR_EXPORTAR"));
        asignarPermiso("Auxiliar", "HORARIOS", List.of());
        asignarPermiso("Auxiliar", "RECREOS", List.of());

        asignarPermiso("Administrador", "PLANTILLAS", List.of("CREAR", "IMPRIMIR_EXPORTAR"));
        asignarPermiso("Secretaria", "PLANTILLAS", List.of());
        asignarPermiso("Docente", "PLANTILLAS", List.of());
        asignarPermiso("Apoderado", "PLANTILLAS", List.of());
    }

    private List<String> crud() {
        return List.of("CREAR", "ACTUALIZAR", "ELIMINAR");
    }

    private List<String> crudWithExport() {
        return List.of("CREAR", "ACTUALIZAR", "ELIMINAR", "IMPRIMIR_EXPORTAR");
    }

    private void crearModuloSiFalta(ModuloSeed seed) {
        Optional<Modulo> mod = moduloRepository.findByModuloAndAccesoNot(seed.nombre(), accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow());
        final Modulo modulo;
        if (mod.isPresent()) {
            modulo = mod.get();
        } else {
            Modulo nuevo = new Modulo();
            nuevo.setModulo(seed.nombre());
            nuevo.setIcono(seed.icono());
            nuevo.setAcceso(accesoRepository.findById(AccesoConstants.ACTIVO).orElseThrow());
            modulo = moduloRepository.save(nuevo);
        }
        seed.permisos().forEach(p -> crearPermisoSiFalta(modulo, p));
    }

    private void crearPermisoSiFalta(Modulo modulo, PermisoSeed seed) {
        Optional<Permiso> per = permisoRepository.findByCodigoAndAccesoNot(seed.codigo(), accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow());
        final Permiso permiso;
        if (per.isPresent()) {
            permiso = per.get();
        } else {
            Permiso nuevo = new Permiso();
            nuevo.setCodigo(seed.codigo());
            nuevo.setNombre(seed.nombre());
            nuevo.setModulo(modulo);
            nuevo.setAcceso(accesoRepository.findById(AccesoConstants.ACTIVO).orElseThrow());
            permiso = permisoRepository.save(nuevo);
        }
        seed.acciones().forEach(a -> crearPermisoAccionSiFalta(permiso, a));
    }

    private void crearPermisoAccionSiFalta(Permiso permiso, String codigoAccion) {
        Accion accion = accionRepository.findByCodigoAndAccesoNot(codigoAccion, accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow())
                .orElseThrow(() -> new IllegalStateException("Acción no sembrada: " + codigoAccion));
        if (permisoAccionRepository.findByPermisoIdPermisoAndAccionIdAccion(permiso.getIdPermiso(), accion.getIdAccion()).isPresent()) {
            return;
        }
        PermisoAccion pa = new PermisoAccion();
        pa.setPermiso(permiso);
        pa.setAccion(accion);
        pa.setAcceso(accesoRepository.findById(AccesoConstants.ACTIVO).orElseThrow());
        permisoAccionRepository.save(pa);
    }

    private void asignarPermiso(String nombreRol, String codigoPermiso, List<String> acciones) {
        Rol rol = rolRepository.findByNombre(nombreRol).orElse(null);
        Permiso permiso = permisoRepository.findByCodigoAndAccesoNot(codigoPermiso, accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow()).orElse(null);
        if (rol == null || permiso == null) {
            return;
        }
        RolPermiso rolPermiso = rolPermisoRepository.findByRolIdRolAndPermisoIdPermiso(rol.getIdRol(), permiso.getIdPermiso())
                .orElseGet(() -> {
                    RolPermiso rp = new RolPermiso();
                    rp.setRol(rol);
                    rp.setPermiso(permiso);
                    rp.setFechaAsignacion(LocalDateTime.now());
                    rp.setAcceso(accesoRepository.findById(AccesoConstants.ACTIVO).orElseThrow());
                    return rolPermisoRepository.save(rp);
                });
        acciones.forEach(a -> asignarAccionConcedidaSiFalta(rolPermiso, a));
    }

    private void asignarAccionConcedidaSiFalta(RolPermiso rolPermiso, String codigoAccion) {
        Accion accion = accionRepository.findByCodigoAndAccesoNot(codigoAccion, accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow()).orElse(null);
        if (accion == null) {
            return;
        }
        if (rolPermisoAccionRepository.findByRolPermisoIdRolPermisoAndAccionIdAccionAndAccesoNot(
                rolPermiso.getIdRolPermiso(), accion.getIdAccion(), accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow()).isPresent()) {
            return;
        }
        RolPermisoAccion rpa = new RolPermisoAccion();
        rpa.setRolPermiso(rolPermiso);
        rpa.setAccion(accion);
        rpa.setAcceso(accesoRepository.findById(AccesoConstants.ACTIVO).orElseThrow());
        rolPermisoAccionRepository.save(rpa);
    }

    @Transactional
    public void seedAdmin() {
        Rol rolAdmin = rolRepository.findByNombre(adminRol)
                .orElseGet(this::crearRolAdmin);

        Optional<Usuario> existente = usuarioRepository.findByCodigoAndAccesoNot(adminCodigo, accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow());
        if (existente.isPresent()) {
            asignarRolSiFalta(existente.get(), rolAdmin);
            return;
        }

        Optional<Usuario> eliminado = usuarioRepository.findByCodigo(adminCodigo);
        if (eliminado.isPresent() && eliminado.get().getAcceso().getIdAcceso().equals(AccesoConstants.ELIMINADO)) {
            Usuario usuario = eliminado.get();
            usuario.setAcceso(accesoRepository.findById(AccesoConstants.ACTIVO).orElseThrow());
            usuario.setContraseña(passwordEncoder.encode(adminPassword));
            if (usuario.getFechaCreacion() == null) {
                usuario.setFechaCreacion(LocalDate.now());
            }
            usuarioRepository.save(usuario);
            asignarRolSiFalta(usuario, rolAdmin);
            return;
        }

        Usuario usuario = new Usuario();
        usuario.setNombre(adminNombre);
        usuario.setApellidoPat(adminApellidoPat);
        usuario.setApellidoMat(adminApellidoMat);
        usuario.setCodigo(adminCodigo);
        usuario.setContraseña(passwordEncoder.encode(adminPassword));
        usuario.setAcceso(accesoRepository.findById(AccesoConstants.ACTIVO).orElseThrow());
        usuario.setGmail(adminGmail);
        usuario.setFechaNaci(LocalDate.of(2000, 1, 1));
        usuario.setFechaCreacion(LocalDate.now());
        usuarioRepository.save(usuario);
        asignarRolSiFalta(usuario, rolAdmin);
    }

    private Rol crearRolAdmin() {
        Rol rol = new Rol();
        rol.setNombre(adminRol);
        rol.setAcceso(accesoRepository.findById(AccesoConstants.ACTIVO).orElseThrow());
        return rolRepository.save(rol);
    }

    private void asignarRolSiFalta(Usuario usuario, Rol rol) {
        boolean yaTiene = usuarioRolRepository.findByUsuarioIdUsuario(usuario.getIdUsuario()).stream()
                .anyMatch(ur -> ur.getRol().getIdRol().equals(rol.getIdRol()));
        if (yaTiene) {
            return;
        }
        UsuarioRol usuarioRol = new UsuarioRol();
        usuarioRol.setUsuario(usuario);
        usuarioRol.setRol(rol);
        usuarioRol.setFechaAsignacion(LocalDateTime.now());
        usuarioRolRepository.save(usuarioRol);
    }

    private record ModuloSeed(String nombre, String icono, List<PermisoSeed> permisos) {
    }

    private record PermisoSeed(String codigo, String nombre, List<String> acciones) {
    }
}