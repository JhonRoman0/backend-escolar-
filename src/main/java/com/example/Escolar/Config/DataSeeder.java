package com.example.Escolar.Config;

import com.example.Escolar.Model.Accion;
import com.example.Escolar.Model.Acceso;
import com.example.Escolar.Model.EstadoAsistencia;
import com.example.Escolar.Model.Grado;
import com.example.Escolar.Model.GradoAcademico;
import com.example.Escolar.Model.GradoSeccion;
import com.example.Escolar.Model.Modulo;
import com.example.Escolar.Model.Nivel;
import com.example.Escolar.Model.Parentesco;
import com.example.Escolar.Model.Permiso;
import com.example.Escolar.Model.PermisoAccion;
import com.example.Escolar.Model.Rol;
import com.example.Escolar.Model.Seccion;
import com.example.Escolar.Model.TipoContrato;
import com.example.Escolar.Model.Turno;
import com.example.Escolar.Model.Usuario;
import com.example.Escolar.Model.UsuarioRol;
import com.example.Escolar.Repository.AccionRepository;
import com.example.Escolar.Repository.AccesoRepository;
import com.example.Escolar.Repository.EstadoAsistenciaRepository;
import com.example.Escolar.Repository.GradoRepository;
import com.example.Escolar.Repository.GradoAcademicoRepository;
import com.example.Escolar.Repository.GradoSeccionRepository;
import com.example.Escolar.Repository.ModuloRepository;
import com.example.Escolar.Repository.NivelRepository;
import com.example.Escolar.Repository.ParentescoRepository;
import com.example.Escolar.Repository.PermisoAccionRepository;
import com.example.Escolar.Repository.PermisoRepository;
import com.example.Escolar.Repository.RolRepository;
import com.example.Escolar.Repository.SeccionRepository;
import com.example.Escolar.Repository.TipoContratoRepository;
import com.example.Escolar.Repository.TurnoRepository;
import com.example.Escolar.Repository.UsuarioRepository;
import com.example.Escolar.Repository.UsuarioRolRepository;
import com.example.Escolar.Service.AccesoConstants;
import com.example.Escolar.Service.ExclusividadDniService;
import com.example.Escolar.Service.OrdenAcademicoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final ExclusividadDniService exclusividadDni;
    private final PlatformTransactionManager transactionManager;
    private final AccionRepository accionRepository;
    private final AccesoRepository accesoRepository;
    private final EstadoAsistenciaRepository estadoAsistenciaRepository;
    private final RolRepository rolRepository;
    private final UsuarioRepository usuarioRepository;
    private final UsuarioRolRepository usuarioRolRepository;
    private final ModuloRepository moduloRepository;
    private final PermisoRepository permisoRepository;
    private final PermisoAccionRepository permisoAccionRepository;
    private final CatalogoPermisosSync catalogoPermisosSync;
    private final NivelRepository nivelRepository;
    private final TurnoRepository turnoRepository;
    private final GradoRepository gradoRepository;
    private final GradoAcademicoRepository gradoAcademicoRepository;
    private final TipoContratoRepository tipoContratoRepository;
    private final SeccionRepository seccionRepository;
    private final GradoSeccionRepository gradoSeccionRepository;
    private final ParentescoRepository parentescoRepository;
    private final OrdenAcademicoService ordenAcademicoService;
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
        seedTurnos();
        seedGrados();
        seedGradosAcademicos();
        seedTiposContrato();
        migrarSeccionesInicial();
        ordenAcademicoService.inicializar();
        seedRepiteGrados();
        seedParentescos();
        seedRoles();
        seedModulosRbac();
        catalogoPermisosSync.sincronizar();
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
        //acceso.setIdAcceso(id);
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

    /**
     * Catalogo de grados academicos. Solo lectura: sin CRUD, se siembran como
     * valores de referencia y el formulario lo usa para sus Selects.
     */
    private void seedGradosAcademicos() {
        for (String nombre : List.of("Bachiller", "Maestro", "Doctor")) {
            crearGradoAcademicoSiFalta(nombre);
        }
    }

    private void crearGradoAcademicoSiFalta(String nombre) {
        if (gradoAcademicoRepository.findByNombreAndAccesoNot(nombre, accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow()).isPresent()) {
            return;
        }
        GradoAcademico grado = new GradoAcademico();
        grado.setNombre(nombre);
        grado.setAcceso(accesoRepository.findById(AccesoConstants.ACTIVO).orElseThrow());
        gradoAcademicoRepository.save(grado);
    }

    /**
     * Catalogo de tipos de contrato. Solo lectura: sin CRUD, se siembran como
     * valores de referencia y el formulario lo usa para sus Selects.
     */
    private void seedTiposContrato() {
        for (String nombre : List.of("Plazo indeterminado", "Sujeto a modalidad", "Tiempo parcial")) {
            crearTipoContratoSiFalta(nombre);
        }
    }

    private void crearTipoContratoSiFalta(String nombre) {
        if (tipoContratoRepository.findByNombreAndAccesoNot(nombre, accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow()).isPresent()) {
            return;
        }
        TipoContrato tipo = new TipoContrato();
        tipo.setNombre(nombre);
        tipo.setAcceso(accesoRepository.findById(AccesoConstants.ACTIVO).orElseThrow());
        tipoContratoRepository.save(tipo);
    }

    /**
     * El colegio trabaja solo con dos turnos, siempre. Se siembran con horarios
     * de ejemplo para que el sistema arranque utilizable, y despues cada colegio
     * los ajusta a su realidad desde el formulario de edicion.
     *
     * Solo corre si la tabla esta vacia: si el colegio ya cargo turnos a mano no
     * se toca ninguno, y en los arranques siguientes no vuelve a crear nada.
     */
    private void seedTurnos() {
        if (!turnoRepository.findByAccesoNot(accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow()).isEmpty()) {
            return;
        }
        // La tarde empieza un minuto despues de que termina la manana: los turnos
        // no pueden compartir ni un minuto. Entre los cuatro horarios de cada
        // turno hay margen para la llegada tarde y la falta.
        crearTurnoSiFalta("Mañana", LocalTime.of(7, 0), LocalTime.of(7, 20), LocalTime.of(7, 40), LocalTime.of(12, 30));
        crearTurnoSiFalta("Tarde", LocalTime.of(12, 31), LocalTime.of(12, 50), LocalTime.of(13, 10), LocalTime.of(17, 30));
    }

    private void crearTurnoSiFalta(String nombre, LocalTime horaEntrada, LocalTime horaEntradaLimite,
            LocalTime horaFaltaLimite, LocalTime horaSalida) {
        if (turnoRepository.findByNombreAndAccesoNot(nombre, accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow()).isPresent()) {
            return;
        }
        Turno turno = new Turno();
        turno.setNombre(nombre);
        turno.setHoraEntrada(horaEntrada);
        turno.setHoraEntradaLimite(horaEntradaLimite);
        turno.setHoraFaltaLimite(horaFaltaLimite);
        turno.setHoraSalida(horaSalida);
        turno.setAcceso(accesoRepository.findById(AccesoConstants.ACTIVO).orElseThrow());
        turnoRepository.save(turno);
    }

    /**
     * Catalogo base de grados. Los nombres van cortos a proposito ("1ro", no
     * "1ro de Primaria") porque el nivel ya esta en la fila y en la cascada:
     * repetirlo dentro del nombre solo estorba en tablas y selectores.
     *
     * La busqueda es por nombre Y nivel porque los nombres se repiten entre
     * niveles: "1ro" existe en Primaria y en Secundaria, y sin el nivel el
     * segundo no se crearia.
     *
     * Solo corre si la tabla esta vacia. Si el colegio ya cargo sus grados a
     * mano no se toca ninguno.
     */
    private void seedGrados() {
        if (!gradoRepository.findByAccesoNot(accesoNoEliminado()).isEmpty()) {
            return;
        }
        for (String nombre : List.of("3 años", "4 años", "5 años")) {
            crearGradoSiFalta(nombre, "Inicial");
        }
        for (String nombre : List.of("1ro", "2do", "3do", "4to", "5to", "6to")) {
            crearGradoSiFalta(nombre, "Primaria");
        }
        for (String nombre : List.of("1ro", "2do", "3do", "4to", "5to")) {
            crearGradoSiFalta(nombre, "Secundaria");
        }
    }

    private void crearGradoSiFalta(String nombre, String nombreNivel) {
        Nivel nivel = nivelRepository.findByNombreAndAccesoNot(nombreNivel, accesoNoEliminado()).orElse(null);
        if (nivel == null) {
            return;
        }
        if (gradoRepository.findByNombreAndNivelAndAccesoNot(nombre, nivel, accesoNoEliminado()).isPresent()) {
            return;
        }
        Grado grado = new Grado();
        grado.setNombre(nombre);
        grado.setNivel(nivel);
        grado.setAcceso(accesoRepository.findById(AccesoConstants.ACTIVO).orElseThrow());
        gradoRepository.save(grado);
    }

    /**
     * Solo llena los grados que aun no lo tienen: en Inicial y en el primer grado
     * de Primaria nadie repite. Despues se cambia desde Catalogos.
     */
    private void seedRepiteGrados() {
        for (Grado grado : gradoRepository.findByAccesoNot(accesoNoEliminado())) {
            if (grado.getRepite() != null) {
                continue;
            }
            String nivel = grado.getNivel().getNombre();
            boolean noRepite = "Inicial".equalsIgnoreCase(nivel)
                    || ("Primaria".equalsIgnoreCase(nivel) && Integer.valueOf(1).equals(grado.getOrden()));
            grado.setRepite(!noRepite);
            gradoRepository.save(grado);
        }
    }

    /** Catalogo sin pantalla: el formulario de matricula solo lo consulta. */
    private void seedParentescos() {
        for (String nombre : List.of("Padre", "Madre", "Abuelo/a", "Tío/a", "Hermano/a", "Tutor legal", "Otro")) {
            if (parentescoRepository.findByNombre(nombre).isPresent()) {
                continue;
            }
            Parentesco parentesco = new Parentesco();
            parentesco.setNombre(nombre);
            parentesco.setAcceso(accesoRepository.findById(AccesoConstants.ACTIVO).orElseThrow());
            parentescoRepository.save(parentesco);
        }
    }

    /**
     * Antes Inicial era el unico nivel sin secciones: cada grado guardaba una fila
     * de grado_seccion con la seccion en nulo y toda la app la mostraba como
     * "Unica". Ahora Inicial maneja letras como los demas niveles, asi que esa
     * fila se le engancha la seccion "A".
     *
     * No se borra ni se crea ninguna fila de grado_seccion: se reusa la misma,
     * con su id intacto, para que las matriculas y asignaciones que ya apuntan a
     * ella sigan apuntando a la misma seccion.
     *
     * Es idempotente: si la fila ya tiene seccion, no hace nada.
     */
    @Transactional
    public void migrarSeccionesInicial() {
        Nivel inicial = nivelRepository.findByNombreAndAccesoNot("Inicial", accesoNoEliminado()).orElse(null);
        if (inicial == null) {
            return;
        }
        List<Grado> gradosInicial = gradoRepository.findByNivelAndAccesoNot(inicial, accesoNoEliminado());
        for (Grado grado : gradosInicial) {
            List<GradoSeccion> filas = gradoSeccionRepository.findByGradoAndAccesoNot(grado, accesoNoEliminado());
            boolean yaTieneLetra = filas.stream().anyMatch(gs -> gs.getSeccion() != null);
            for (GradoSeccion gs : filas) {
                if (gs.getSeccion() != null) {
                    continue;
                }
                Seccion seccion = new Seccion();
                seccion.setNombre("A");
                seccion.setAcceso(accesoRepository.findById(AccesoConstants.ACTIVO).orElseThrow());
                seccion = seccionRepository.save(seccion);
                gs.setSeccion(seccion);
                gradoSeccionRepository.save(gs);
                log.info("Migracion Inicial: {} quedo con la seccion A sobre idGradoSeccion {}",
                        grado.getNombre(), gs.getIdGradoSeccion());
            }
            if (!yaTieneLetra && filas.isEmpty()) {
                log.debug("Migracion Inicial: {} todavia no tiene secciones, se crearan desde el modulo", grado.getNombre());
            }
        }
    }

    private Acceso accesoNoEliminado() {
        return accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow();
    }

    private void seedAcciones() {
        crearAccionSiFalta("CREAR", "Crear");
        crearAccionSiFalta("ACTUALIZAR", "Actualizar");
        crearAccionSiFalta("ELIMINAR", "Eliminar");
        crearAccionSiFalta("IMPRIMIR_EXPORTAR", "Imprimir/Exportar");
        crearAccionSiFalta("IMPRIMIR_FICHA", "Imprimir ficha");
        crearAccionSiFalta("EXPORTAR_SIAGIE", "Exportar SIAGIE");
        crearAccionSiFalta("IMPRIMIR_BOLETA", "Imprimir boleta");
        crearAccionSiFalta("VER_RESUMEN", "Ver resumen");
        crearAccionSiFalta("VER_COLEGIO", "Ver colegio");
        crearAccionSiFalta("CONFIGURAR", "Configurar");
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

    // Roles base, vacíos: sus módulos y acciones se configuran en Accesos.
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
                new PermisoSeed("ORDEN_GRADOS", "Orden de grados", List.of("ACTUALIZAR")),
                new PermisoSeed("ANIOS_ESCOLARES", "Años Escolares", crud()),
                new PermisoSeed("AULAS", "Aulas", crud()),
                new PermisoSeed("ASIGNACIONES", "Asignaciones", crud()),
                new PermisoSeed("RECREOS", "Recreos", crud()),
                new PermisoSeed("SUSPENSIONES_DOCENTE", "Suspensiones de Docente", List.of("CREAR")),
                new PermisoSeed("CAMBIOS_DOCENTE", "Cambios de Docente", List.of()),
                new PermisoSeed("COMPETENCIAS", "Competencias", List.of("ACTUALIZAR")),
                new PermisoSeed("CONFIGURACION_ACADEMICA", "Configuración académica", List.of("ACTUALIZAR")))));

        crearModuloSiFalta(new ModuloSeed("Estudiantes", "user-graduate", List.of(
                new PermisoSeed("ALUMNOS", "Alumnos", crudWithExport()),
                new PermisoSeed("APODERADOS", "Apoderados", crud()))));

        crearModuloSiFalta(new ModuloSeed("Matricula", "clipboard-list", List.of(
                new PermisoSeed("MATRICULAS", "Matrículas", crudWithExport()))));

        crearModuloSiFalta(new ModuloSeed("Pagos", "wallet", List.of(
                new PermisoSeed("PAGOS_MATRICULA", "Pagos de matrícula", crud()))));

        crearModuloSiFalta(new ModuloSeed("Consolidado", "clipboard-check", List.of(
                new PermisoSeed("NOTAS", "Notas", List.of("CREAR", "IMPRIMIR_EXPORTAR")),
                new PermisoSeed("EVALUACIONES", "Evaluaciones", crud()),
                new PermisoSeed("NOTAS_ANUALES", "Notas finales anuales", List.of("CREAR", "ACTUALIZAR")))));

        crearModuloSiFalta(new ModuloSeed("Portal", "globe", List.of(
                new PermisoSeed("PUBLICACIONES", "Publicaciones", crud()),
                new PermisoSeed("EVENTOS", "Eventos", crud()),
                new PermisoSeed("GALERIAS", "Galerías", crud()),
                new PermisoSeed("CONTACTOS", "Contactos", List.of("CREAR", "ACTUALIZAR")),
                new PermisoSeed("AJUSTES", "Ajustes", crud()))));

        crearModuloSiFalta(new ModuloSeed("Colegio", "school", List.of(
                new PermisoSeed("COLEGIOS", "Datos del Colegio", crud()))));

        crearModuloSiFalta(new ModuloSeed("Asistencia", "user-check", List.of(
                new PermisoSeed("ASISTENCIAS", "Asistencia", List.of("CREAR", "ACTUALIZAR", "ELIMINAR", "CONFIGURAR", "IMPRIMIR_EXPORTAR")))));

        crearModuloSiFalta(new ModuloSeed("Horario", "horario", List.of(
                new PermisoSeed("HORARIOS", "Horarios", List.of("VER_COLEGIO", "IMPRIMIR_EXPORTAR")))));
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
        // Los permisos del catalogo los maneja sincronizarCatalogoPermisos.
        if (CatalogoPermisos.acciones(seed.codigo()).isEmpty()) {
            seed.acciones().forEach(a -> crearPermisoAccionSiFalta(permiso, a));
        }
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

    public void seedAdmin() {
        // run() llama al mismo bean: una anotación @Transactional aquí se omitiría.
        TransactionTemplate transaccion = new TransactionTemplate(transactionManager);
        transaccion.setIsolationLevel(TransactionDefinition.ISOLATION_SERIALIZABLE);
        transaccion.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        try {
            transaccion.executeWithoutResult(status -> seedAdminEnTransaccion());
        } catch (ConcurrencyFailureException ex) {
            log.warn("Se omitió la inicialización de la cuenta inicial por cambios concurrentes");
        }
    }

    private void seedAdminEnTransaccion() {
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
            // Una cuenta inicial antigua puede tener DNI aunque el alta inicial no lo asigne.
            if (usuario.getDocumentoIdentidad() != null && !usuario.getDocumentoIdentidad().isBlank()) {
                try {
                    exclusividadDni.validarUsuario(usuario.getDocumentoIdentidad());
                } catch (IllegalArgumentException ex) {
                    log.warn("Se omitió la reactivación de la cuenta inicial: {}", ex.getMessage());
                    return;
                }
            }
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
