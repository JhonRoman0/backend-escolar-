package com.example.Escolar.Service;

import com.example.Escolar.Dto.ApoderadoBusquedaResponse;
import com.example.Escolar.Dto.ApoderadoResponse;
import com.example.Escolar.Dto.InscripcionAlumnoRequest;
import com.example.Escolar.Dto.InscripcionAlumnoResponse;
import com.example.Escolar.Dto.InscripcionApoderadoRequest;
import com.example.Escolar.Dto.InscripcionRequest;
import com.example.Escolar.Dto.MatriculaEdicionRequest;
import com.example.Escolar.Dto.MatriculaResponse;
import com.example.Escolar.Dto.UploadResponse;
import com.example.Escolar.Exception.ResourceNotFoundException;
import com.example.Escolar.Model.Acceso;
import com.example.Escolar.Model.Alumno;
import com.example.Escolar.Model.AlumnoApoderado;
import com.example.Escolar.Model.AnioEscolar;
import com.example.Escolar.Model.Apoderado;
import com.example.Escolar.Model.GradoSeccion;
import com.example.Escolar.Model.Matricula;
import com.example.Escolar.Model.MetodoPago;
import com.example.Escolar.Model.PagoMatricula;
import com.example.Escolar.Model.Rol;
import com.example.Escolar.Model.TipoPago;
import com.example.Escolar.Model.Usuario;
import com.example.Escolar.Model.UsuarioRol;
import com.example.Escolar.Repository.AccesoRepository;
import com.example.Escolar.Repository.AlumnoApoderadoRepository;
import com.example.Escolar.Repository.AlumnoRepository;
import com.example.Escolar.Repository.ApoderadoRepository;
import com.example.Escolar.Repository.GradoSeccionRepository;
import com.example.Escolar.Repository.HistorialMatriculaRepository;
import com.example.Escolar.Repository.MatriculaRepository;
import com.example.Escolar.Repository.PagoMatriculaRepository;
import com.example.Escolar.Repository.RolRepository;
import com.example.Escolar.Repository.UsuarioRepository;
import com.example.Escolar.Repository.UsuarioRolRepository;
import com.example.Escolar.Service.EmailService.TipoEnvioCredenciales;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Asistente de matrícula: alumno, apoderados y matrícula se guardan juntos en una
 * transacción. Si algo falla no queda nada (las fotos subidas se borran) y los
 * correos con accesos solo salen cuando la matrícula se confirmó.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InscripcionService {

    public static final String ESTADO_APODERADO = "APODERADO";
    public static final String ESTADO_USUARIO = "USUARIO";
    public static final String ESTADO_NUEVO = "NUEVO";
    private static final String ROL_APODERADO = "Apoderado";
    private static final byte PRINCIPAL = 1;

    private record Credencial(String gmail, String nombre, String codigo, String contraseña, TipoEnvioCredenciales tipo) {
    }

    private final AlumnoRepository alumnoRepository;
    private final ApoderadoRepository apoderadoRepository;
    private final AlumnoApoderadoRepository alumnoApoderadoRepository;
    private final UsuarioRepository usuarioRepository;
    private final UsuarioRolRepository usuarioRolRepository;
    private final RolRepository rolRepository;
    private final MatriculaRepository matriculaRepository;
    private final HistorialMatriculaRepository historialMatriculaRepository;
    private final PagoMatriculaRepository pagoMatriculaRepository;
    private final GradoSeccionRepository gradoSeccionRepository;
    private final AccesoRepository accesoRepository;
    private final PasswordEncoder passwordEncoder;
    private final CloudinaryService cloudinaryService;
    private final EmailService emailService;
    private final PagoConfigService pagoConfigService;
    private final MatriculaReglasService matriculaReglasService;
    private final MatriculaService matriculaService;

    // ---------- Búsquedas del asistente ----------

    public InscripcionAlumnoResponse buscarAlumno(String dni) {
        InscripcionAlumnoResponse response = new InscripcionAlumnoResponse();
        response.setDocumentoIdentidad(dni);
        Optional<Alumno> encontrado = alumnoRepository.findByDocumentoIdentidadAndAccesoNot(dni, eliminado());
        if (encontrado.isEmpty()) {
            return response;
        }
        Alumno alumno = encontrado.get();
        response.setExiste(true);
        response.setIdAlumno(alumno.getIdAlumno());
        response.setNombre(alumno.getNombre());
        response.setApellidoPat(alumno.getApellidoPat());
        response.setApellidoMat(alumno.getApellidoMat());
        response.setFechaNacimiento(alumno.getFechaNacimiento());
        response.setSexo(alumno.getSexo() == null ? null : alumno.getSexo().name());
        response.setDireccion(alumno.getDireccion());
        response.setTieneDiscapacidad(Boolean.TRUE.equals(alumno.getTieneDiscapacidad()));
        response.setDiscapacidad(alumno.getDiscapacidad());
        response.setUrlFoto(alumno.getUrlFoto());
        response.setUltimaMatricula(matriculaRepository.findByAlumnoApoderadoAlumnoIdAlumnoAndAccesoNot(alumno.getIdAlumno(), eliminado()).stream()
                .max(Comparator.comparing(m -> m.getGradoSeccion().getAnioEscolar().getAnio()))
                .map(m -> MatriculaReglasService.nombreGrado(m.getGradoSeccion().getGrado()) + " "
                        + m.getGradoSeccion().getAnioEscolar().getAnio())
                .orElse(null));
        response.setApoderados(alumnoApoderadoRepository.findByAlumno(alumno).stream()
                .filter(v -> esActivo(v.getApoderado().getAcceso()))
                .sorted(Comparator.comparing(AlumnoApoderado::getApoPrincipal).reversed())
                .map(v -> datosApoderado(v.getApoderado(), alumno.getIdAlumno()))
                .toList());
        return response;
    }

    public ApoderadoBusquedaResponse buscarApoderado(String dni, Integer idAlumno) {
        Optional<Apoderado> apoderado = apoderadoRepository.findByUsuarioDocumentoIdentidadAndAccesoNot(dni, eliminado());
        if (apoderado.isPresent()) {
            return datosApoderado(apoderado.get(), idAlumno);
        }
        ApoderadoBusquedaResponse response = new ApoderadoBusquedaResponse();
        response.setDocumentoIdentidad(dni);
        response.setHijos(List.of());
        Optional<Usuario> usuario = usuarioRepository.findByDocumentoIdentidadAndAccesoNot(dni, eliminado());
        if (usuario.isEmpty()) {
            response.setEstado(ESTADO_NUEVO);
            return response;
        }
        response.setEstado(ESTADO_USUARIO);
        response.setNombre(usuario.get().getNombre());
        response.setApellidoPat(usuario.get().getApellidoPat());
        response.setApellidoMat(usuario.get().getApellidoMat());
        response.setFechaNaci(usuario.get().getFechaNaci());
        response.setGmail(usuario.get().getGmail());
        response.setCelular(usuario.get().getCelular());
        return response;
    }

    private ApoderadoBusquedaResponse datosApoderado(Apoderado apoderado, Integer idAlumno) {
        Usuario usuario = apoderado.getUsuario();
        ApoderadoBusquedaResponse response = new ApoderadoBusquedaResponse();
        response.setEstado(ESTADO_APODERADO);
        response.setIdApoderado(apoderado.getIdApoderado());
        response.setDocumentoIdentidad(usuario.getDocumentoIdentidad());
        response.setNombre(usuario.getNombre());
        response.setApellidoPat(usuario.getApellidoPat());
        response.setApellidoMat(usuario.getApellidoMat());
        response.setFechaNaci(usuario.getFechaNaci());
        response.setGmail(usuario.getGmail());
        response.setSexo(apoderado.getSexo() == null ? null : apoderado.getSexo().name());
        response.setDireccion(apoderado.getDireccion());
        response.setCelular(apoderado.getCelular());
        List<AlumnoApoderado> vinculos = alumnoApoderadoRepository.findByApoderado(apoderado).stream()
                .filter(v -> esActivo(v.getAlumno().getAcceso()))
                .toList();
        response.setHijos(vinculos.stream()
                .map(v -> v.getAlumno().getNombre() + " " + v.getAlumno().getApellidoPat() + " " + v.getAlumno().getApellidoMat())
                .toList());
        vinculos.stream()
                .filter(v -> v.getAlumno().getIdAlumno().equals(idAlumno) && v.getParentesco() != null)
                .findFirst()
                .ifPresent(v -> response.setIdParentesco(v.getParentesco().getIdParentesco()));
        return response;
    }

    // ---------- Paso 2: validar sin guardar ----------

    /** Revisa alumno y apoderados como lo haría el guardado, sin escribir nada. */
    public void validar(InscripcionRequest request) {
        InscripcionAlumnoRequest alumno = request.getAlumno();
        if (alumno.isTieneDiscapacidad() && vacio(alumno.getDiscapacidad())) {
            throw new IllegalArgumentException("Describe la discapacidad del alumno");
        }
        Set<String> documentos = new HashSet<>();
        Set<String> correos = new HashSet<>();
        documentos.add(alumno.getDocumentoIdentidad());
        for (InscripcionApoderadoRequest apoderado : request.getApoderados()) {
            if (!documentos.add(apoderado.getDocumentoIdentidad())) {
                throw new IllegalArgumentException(apoderado.getDocumentoIdentidad().equals(alumno.getDocumentoIdentidad())
                        ? "El apoderado no puede tener el mismo DNI que el alumno"
                        : "Los dos apoderados no pueden tener el mismo DNI");
            }
            pagoConfigService.parentesco(apoderado.getIdParentesco());
            validarApoderado(apoderado, correos);
        }
    }

    /** Campos que se piden según quién es: apoderado existente, usuario con otro rol o persona nueva. */
    private void validarApoderado(InscripcionApoderadoRequest apoderado, Set<String> correos) {
        String estado = estadoApoderado(apoderado.getDocumentoIdentidad());
        if (ESTADO_APODERADO.equals(estado)) {
            return;
        }
        exigir(apoderado.getSexo() != null, "El sexo del apoderado es obligatorio");
        exigir(!vacio(apoderado.getDireccion()), "La dirección del apoderado es obligatoria");
        exigir(!vacio(apoderado.getCelular()), "El celular del apoderado es obligatorio");
        if (ESTADO_USUARIO.equals(estado)) {
            return;
        }
        exigir(!vacio(apoderado.getNombre()) && !vacio(apoderado.getApellidoPat()) && !vacio(apoderado.getApellidoMat()),
                "El nombre y los apellidos del apoderado son obligatorios");
        exigir(apoderado.getFechaNaci() != null, "La fecha de nacimiento del apoderado es obligatoria");
        exigir(!vacio(apoderado.getGmail()), "El correo del apoderado es obligatorio");
        exigir(!vacio(apoderado.getContraseña()), "La contraseña del apoderado es obligatoria");
        String gmail = apoderado.getGmail().trim().toLowerCase();
        if (!correos.add(gmail)) {
            throw new IllegalArgumentException("Los dos apoderados no pueden tener el mismo correo");
        }
        usuarioRepository.findByGmailAndAccesoNot(apoderado.getGmail().trim(), eliminado())
                .ifPresent(u -> {
                    throw new IllegalArgumentException("El correo " + apoderado.getGmail().trim() + " ya lo usa otro usuario");
                });
    }

    // ---------- Estudiantes: agregar un apoderado ----------

    /** Agrega el segundo apoderado a un alumno ya matriculado, con la misma lógica del asistente. */
    @Transactional
    public ApoderadoResponse agregarApoderado(Integer idAlumno, InscripcionApoderadoRequest datos) {
        Alumno alumno = alumnoRepository.findByIdAlumnoAndAccesoNot(idAlumno, eliminado())
                .orElseThrow(() -> new ResourceNotFoundException("Alumno no encontrado con id " + idAlumno));
        List<AlumnoApoderado> vinculos = alumnoApoderadoRepository.findByAlumno(alumno).stream()
                .filter(v -> esActivo(v.getApoderado().getAcceso()))
                .toList();
        if (vinculos.size() >= 2) {
            throw new IllegalArgumentException("El alumno ya tiene dos apoderados");
        }
        if (datos.getDocumentoIdentidad().equals(alumno.getDocumentoIdentidad())) {
            throw new IllegalArgumentException("El apoderado no puede tener el mismo DNI que el alumno");
        }
        if (vinculos.stream().anyMatch(v -> datos.getDocumentoIdentidad().equals(v.getApoderado().getUsuario().getDocumentoIdentidad()))) {
            throw new IllegalArgumentException("Ese apoderado ya está vinculado al alumno");
        }
        pagoConfigService.parentesco(datos.getIdParentesco());
        validarApoderado(datos, new HashSet<>());

        List<Credencial> credenciales = new ArrayList<>();
        Apoderado apoderado = resolverApoderado(datos, credenciales);
        AlumnoApoderado vinculo = alumnoApoderadoRepository.findByAlumno(alumno).stream()
                .filter(v -> v.getApoderado().getIdApoderado().equals(apoderado.getIdApoderado()))
                .findFirst()
                .orElseGet(() -> {
                    AlumnoApoderado nuevo = new AlumnoApoderado();
                    nuevo.setAlumno(alumno);
                    nuevo.setApoderado(apoderado);
                    return nuevo;
                });
        vinculo.setParentesco(pagoConfigService.parentesco(datos.getIdParentesco()));
        vinculo.setApoPrincipal(vinculos.isEmpty() ? PRINCIPAL : (byte) 0);
        vinculo = alumnoApoderadoRepository.save(vinculo);

        alConfirmar(() -> credenciales.forEach(c ->
                emailService.enviarCredencialesAcceso(c.gmail(), c.nombre(), c.codigo(), c.contraseña(), c.tipo())));
        return ApoderadoResponse.fromVinculo(vinculo);
    }

    private String estadoApoderado(String dni) {
        if (apoderadoRepository.findByUsuarioDocumentoIdentidadAndAccesoNot(dni, eliminado()).isPresent()) {
            return ESTADO_APODERADO;
        }
        if (usuarioRepository.findByDocumentoIdentidadAndAccesoNot(dni, eliminado()).isPresent()) {
            return ESTADO_USUARIO;
        }
        return ESTADO_NUEVO;
    }

    // ---------- Paso 3: crear ----------

    @Transactional
    public MatriculaResponse inscribir(InscripcionRequest request, MultipartFile foto, MultipartFile comprobante, Integer idUsuarioRegistro) {
        if (request.getMatricula() == null) {
            throw new IllegalArgumentException("Faltan los datos de la matrícula");
        }
        validar(request);
        InscripcionRequest.DatosMatricula datos = request.getMatricula();
        Usuario registrador = usuarioRepository.findByIdUsuarioAndAccesoNot(idUsuarioRegistro, eliminado())
                .orElseThrow(() -> new IllegalArgumentException("No se pudo identificar al usuario que realiza la matrícula"));

        // Primero todo lo que puede rechazar la matrícula, antes de subir fotos.
        Optional<Alumno> existente = alumnoRepository.findByDocumentoIdentidadAndAccesoNot(
                request.getAlumno().getDocumentoIdentidad(), eliminado());
        GradoSeccion gradoSeccion = findGradoSeccion(datos.getIdGradoSeccion());
        matriculaReglasService.validarSeccion(gradoSeccion, existente.orElse(null), null);
        AnioEscolar anio = gradoSeccion.getAnioEscolar();
        BigDecimal monto = pagoConfigService.montoDe(gradoSeccion.getGrado().getNivel(), anio);
        MetodoPago metodo = pagoConfigService.metodoActivo(datos.getIdMetodoPago());
        validarDatosDePago(metodo, datos.getCodigoTransaccion(), comprobante);
        boolean tieneFoto = existente.map(a -> !vacio(a.getUrlFoto())).orElse(false);
        if (!tieneFoto && (foto == null || foto.isEmpty())) {
            throw new IllegalArgumentException("La foto del alumno es obligatoria");
        }

        List<Credencial> credenciales = new ArrayList<>();
        Alumno alumno = existente.orElseGet(() -> guardarAlumnoNuevo(request.getAlumno()));
        if (foto != null && !foto.isEmpty()) {
            cambiarFoto(alumno, foto);
        }
        AlumnoApoderado principal = vincularApoderados(alumno, request.getApoderados(), credenciales);

        Matricula matricula = new Matricula();
        matricula.setAlumnoApoderado(principal);
        matricula.setGradoSeccion(gradoSeccion);
        matricula.setUsuario(registrador);
        matricula.setMonto(monto);
        matricula.setObservaciones(vacio(datos.getObservaciones()) ? null : datos.getObservaciones().trim());
        matricula.setFechaRegistro(LocalDate.now());
        matricula.setAcceso(acceso(AccesoConstants.ACTIVO));
        matricula = matriculaRepository.save(matricula);
        matriculaService.crearHistorial(matricula, gradoSeccion, "Matrícula inicial");
        registrarPago(matricula, TipoPago.PAGO, monto, metodo, datos.getCodigoTransaccion(), comprobante, registrador);

        alConfirmar(() -> credenciales.forEach(c ->
                emailService.enviarCredencialesAcceso(c.gmail(), c.nombre(), c.codigo(), c.contraseña(), c.tipo())));
        return matriculaService.toResponse(matricula);
    }

    private Alumno guardarAlumnoNuevo(InscripcionAlumnoRequest datos) {
        // Un alumno eliminado con el mismo DNI se reactiva: el DNI es único.
        Alumno alumno = alumnoRepository.findByDocumentoIdentidad(datos.getDocumentoIdentidad()).orElseGet(Alumno::new);
        alumno.setDocumentoIdentidad(datos.getDocumentoIdentidad());
        alumno.setNombre(datos.getNombre().trim());
        alumno.setApellidoPat(datos.getApellidoPat().trim());
        alumno.setApellidoMat(datos.getApellidoMat().trim());
        alumno.setFechaNacimiento(datos.getFechaNacimiento());
        alumno.setSexo(datos.getSexo());
        alumno.setDireccion(datos.getDireccion().trim());
        alumno.setTieneDiscapacidad(datos.isTieneDiscapacidad());
        alumno.setDiscapacidad(datos.isTieneDiscapacidad() ? datos.getDiscapacidad().trim() : null);
        alumno.setCodigo("00000000" + datos.getDocumentoIdentidad());
        alumno.setCodigoHash(sha256(alumno.getCodigo()));
        alumno.setFechaIngreso(LocalDate.now());
        alumno.setAcceso(acceso(AccesoConstants.ACTIVO));
        return alumnoRepository.save(alumno);
    }

    private void cambiarFoto(Alumno alumno, MultipartFile foto) {
        String anterior = alumno.getPkUrlFoto();
        UploadResponse subida = subir(foto, "alumnos");
        alumno.setUrlFoto(subida.getUrl());
        alumno.setPkUrlFoto(subida.getPublicId());
        alumnoRepository.save(alumno);
        if (!vacio(anterior)) {
            alConfirmar(() -> borrarArchivo(anterior));
        }
    }

    /** Crea, reactiva o reutiliza a cada apoderado y lo vincula; el primero queda como principal. */
    private AlumnoApoderado vincularApoderados(Alumno alumno, List<InscripcionApoderadoRequest> datos, List<Credencial> credenciales) {
        List<AlumnoApoderado> actuales = alumnoApoderadoRepository.findByAlumno(alumno);
        AlumnoApoderado principal = null;
        for (int i = 0; i < datos.size(); i++) {
            Apoderado apoderado = resolverApoderado(datos.get(i), credenciales);
            AlumnoApoderado vinculo = actuales.stream()
                    .filter(v -> v.getApoderado().getIdApoderado().equals(apoderado.getIdApoderado()))
                    .findFirst()
                    .orElseGet(() -> {
                        AlumnoApoderado nuevo = new AlumnoApoderado();
                        nuevo.setAlumno(alumno);
                        nuevo.setApoderado(apoderado);
                        return nuevo;
                    });
            vinculo.setParentesco(pagoConfigService.parentesco(datos.get(i).getIdParentesco()));
            vinculo.setApoPrincipal(i == 0 ? PRINCIPAL : (byte) 0);
            vinculo = alumnoApoderadoRepository.save(vinculo);
            if (i == 0) {
                principal = vinculo;
            }
        }
        Integer idPrincipal = principal.getIdAlumnoApoderado();
        for (AlumnoApoderado otro : actuales) {
            if (!otro.getIdAlumnoApoderado().equals(idPrincipal) && otro.getApoPrincipal() == PRINCIPAL) {
                otro.setApoPrincipal((byte) 0);
                alumnoApoderadoRepository.save(otro);
            }
        }
        return principal;
    }

    private Apoderado resolverApoderado(InscripcionApoderadoRequest datos, List<Credencial> credenciales) {
        String dni = datos.getDocumentoIdentidad();
        Optional<Apoderado> activo = apoderadoRepository.findByUsuarioDocumentoIdentidadAndAccesoNot(dni, eliminado());
        if (activo.isPresent()) {
            return activo.get();
        }
        Optional<Usuario> usuarioActivo = usuarioRepository.findByDocumentoIdentidadAndAccesoNot(dni, eliminado());
        Usuario usuario;
        if (usuarioActivo.isPresent()) {
            // Ya tiene cuenta con otro rol: se reutiliza sin tocar su acceso.
            usuario = usuarioActivo.get();
        } else {
            usuario = usuarioRepository.findByDocumentoIdentidad(dni).orElseGet(Usuario::new);
            boolean reactivado = usuario.getIdUsuario() != null;
            usuario.setDocumentoIdentidad(dni);
            usuario.setNombre(datos.getNombre().trim());
            usuario.setApellidoPat(datos.getApellidoPat().trim());
            usuario.setApellidoMat(datos.getApellidoMat().trim());
            usuario.setFechaNaci(datos.getFechaNaci());
            usuario.setGmail(datos.getGmail().trim());
            usuario.setCelular(datos.getCelular());
            usuario.setContraseña(passwordEncoder.encode(datos.getContraseña()));
            usuario.setDebeCambiarContrasena(true);
            usuario.setIntentosFallidos(0);
            usuario.setFechaBloqueo(null);
            if (!reactivado) {
                usuario.setCodigo(generarCodigoApoderado());
                usuario.setFechaCreacion(LocalDate.now());
            }
            usuario.setAcceso(acceso(AccesoConstants.ACTIVO));
            usuario = usuarioRepository.save(usuario);
            credenciales.add(new Credencial(usuario.getGmail(), usuario.getNombre() + " " + usuario.getApellidoPat(),
                    usuario.getCodigo(), datos.getContraseña(),
                    reactivado ? TipoEnvioCredenciales.REACTIVACION : TipoEnvioCredenciales.CREACION));
        }
        asignarRolApoderado(usuario);

        Apoderado apoderado = apoderadoRepository.findByUsuarioIdUsuario(usuario.getIdUsuario()).orElseGet(Apoderado::new);
        apoderado.setUsuario(usuario);
        apoderado.setSexo(datos.getSexo());
        apoderado.setDireccion(datos.getDireccion().trim());
        apoderado.setCelular(datos.getCelular());
        apoderado.setAcceso(acceso(AccesoConstants.ACTIVO));
        return apoderadoRepository.save(apoderado);
    }

    private void asignarRolApoderado(Usuario usuario) {
        Rol rol = rolRepository.findByNombre(ROL_APODERADO)
                .orElseThrow(() -> new IllegalStateException("No existe el rol Apoderado"));
        boolean yaLoTiene = usuarioRolRepository.findByUsuarioIdUsuario(usuario.getIdUsuario()).stream()
                .anyMatch(ur -> ur.getRol().getIdRol().equals(rol.getIdRol()));
        if (yaLoTiene) {
            return;
        }
        UsuarioRol usuarioRol = new UsuarioRol();
        usuarioRol.setUsuario(usuario);
        usuarioRol.setRol(rol);
        usuarioRol.setFechaAsignacion(LocalDateTime.now());
        usuarioRolRepository.save(usuarioRol);
    }

    // ---------- Editar ----------

    @Transactional
    public MatriculaResponse editar(Integer id, MatriculaEdicionRequest request, MultipartFile comprobante, Integer idUsuario) {
        Matricula matricula = matriculaService.findMatricula(id);
        AnioEscolar anio = matricula.getGradoSeccion().getAnioEscolar();
        if (anio.getEstado() == AnioEscolarService.ESTADO_CERRADO) {
            throw new IllegalArgumentException("La matrícula de un año cerrado no se puede editar");
        }
        GradoSeccion actual = matricula.getGradoSeccion();
        boolean cambiaSeccion = request.getIdGradoSeccion() != null
                && !request.getIdGradoSeccion().equals(actual.getIdGradoSeccion());
        if (cambiaSeccion && anio.getEstado() != AnioEscolarService.ESTADO_POR_COMENZAR) {
            throw new IllegalArgumentException("En un año vigente la sección se cambia con Cambio de sección");
        }
        if (request.getObservaciones() != null) {
            matricula.setObservaciones(vacio(request.getObservaciones()) ? null : request.getObservaciones().trim());
        }
        if (cambiaSeccion) {
            GradoSeccion nueva = findGradoSeccion(request.getIdGradoSeccion());
            if (!nueva.getAnioEscolar().getIdAnio().equals(anio.getIdAnio())) {
                throw new IllegalArgumentException("La sección nueva debe ser del mismo año escolar");
            }
            matriculaReglasService.validarSeccion(nueva, matricula.getAlumnoApoderado().getAlumno(), matricula.getIdMatricula());
            if (!nueva.getGrado().getNivel().getIdNivel().equals(actual.getGrado().getNivel().getIdNivel())) {
                ajustarPagoPorCambioDeNivel(matricula, nueva, request, comprobante, idUsuario);
            }
            matriculaService.cerrarHistorialVigente(matricula);
            matricula.setGradoSeccion(nueva);
            matriculaService.crearHistorial(matricula, nueva, "Edición de matrícula");
        }
        return matriculaService.toResponse(matriculaRepository.save(matricula));
    }

    /** Al pasar a un nivel con otro precio se registra la diferencia o la devolución sobre lo pagado neto. */
    private void ajustarPagoPorCambioDeNivel(Matricula matricula, GradoSeccion nueva, MatriculaEdicionRequest request,
                                             MultipartFile comprobante, Integer idUsuario) {
        BigDecimal nuevoMonto = pagoConfigService.montoDe(nueva.getGrado().getNivel(), nueva.getAnioEscolar());
        BigDecimal diferencia = nuevoMonto.subtract(matriculaService.pagadoNeto(matricula.getIdMatricula()));
        matricula.setMonto(nuevoMonto);
        if (diferencia.signum() == 0) {
            return;
        }
        MetodoPago metodo = pagoConfigService.metodoActivo(request.getIdMetodoPago());
        validarDatosDePago(metodo, request.getCodigoTransaccion(), comprobante);
        Usuario usuario = usuarioRepository.findByIdUsuarioAndAccesoNot(idUsuario, eliminado())
                .orElseThrow(() -> new IllegalArgumentException("No se pudo identificar al usuario que edita la matrícula"));
        registrarPago(matricula, diferencia.signum() > 0 ? TipoPago.DIFERENCIA : TipoPago.DEVOLUCION,
                diferencia.abs(), metodo, request.getCodigoTransaccion(), comprobante, usuario);
    }

    // ---------- Eliminar ----------

    /**
     * Solo en un año por comenzar. Libera el cupo; si era la única matrícula del
     * alumno también se descartan el alumno y los apoderados que no tengan otros
     * hijos. Los pagos quedan como historial: no se devuelve nada.
     */
    @Transactional
    public void eliminar(Integer id) {
        Matricula matricula = matriculaService.findMatricula(id);
        if (matricula.getGradoSeccion().getAnioEscolar().getEstado() != AnioEscolarService.ESTADO_POR_COMENZAR) {
            throw new IllegalArgumentException("Solo se puede eliminar la matrícula de un año por comenzar");
        }
        Acceso eliminado = eliminado();
        matricula.setAcceso(eliminado);
        matriculaRepository.save(matricula);
        historialMatriculaRepository.findByMatriculaIdMatriculaAndAccesoNot(id, eliminado)
                .forEach(h -> {
                    h.setAcceso(eliminado);
                    historialMatriculaRepository.save(h);
                });

        Alumno alumno = matricula.getAlumnoApoderado().getAlumno();
        if (!matriculaRepository.findByAlumnoApoderadoAlumnoIdAlumnoAndAccesoNot(alumno.getIdAlumno(), eliminado).isEmpty()) {
            return;
        }
        alumno.setAcceso(eliminado);
        alumnoRepository.save(alumno);
        for (AlumnoApoderado vinculo : alumnoApoderadoRepository.findByAlumno(alumno)) {
            Apoderado apoderado = vinculo.getApoderado();
            boolean tieneOtrosHijos = alumnoApoderadoRepository.findByApoderado(apoderado).stream()
                    .anyMatch(v -> !v.getAlumno().getIdAlumno().equals(alumno.getIdAlumno()) && esActivo(v.getAlumno().getAcceso()));
            if (!tieneOtrosHijos && esActivo(apoderado.getAcceso())) {
                descartarApoderado(apoderado);
            }
        }
    }

    private void descartarApoderado(Apoderado apoderado) {
        apoderado.setAcceso(eliminado());
        apoderadoRepository.save(apoderado);
        Usuario usuario = apoderado.getUsuario();
        List<UsuarioRol> roles = usuarioRolRepository.findByUsuarioIdUsuario(usuario.getIdUsuario());
        roles.stream()
                .filter(ur -> ROL_APODERADO.equalsIgnoreCase(ur.getRol().getNombre()))
                .forEach(usuarioRolRepository::delete);
        boolean tieneOtroRol = roles.stream().anyMatch(ur -> !ROL_APODERADO.equalsIgnoreCase(ur.getRol().getNombre()));
        if (!tieneOtroRol) {
            usuario.setAcceso(eliminado());
            usuarioRepository.save(usuario);
        }
    }

    // ---------- Pagos y archivos ----------

    private void validarDatosDePago(MetodoPago metodo, String codigo, MultipartFile comprobante) {
        if (metodo.isRequiereCodigo() && vacio(codigo)) {
            throw new IllegalArgumentException("El código de transacción es obligatorio para " + metodo.getNombre());
        }
        if (metodo.isRequiereComprobante() && (comprobante == null || comprobante.isEmpty())) {
            throw new IllegalArgumentException("La foto del comprobante es obligatoria para " + metodo.getNombre());
        }
    }

    private void registrarPago(Matricula matricula, TipoPago tipo, BigDecimal monto, MetodoPago metodo,
                               String codigo, MultipartFile comprobante, Usuario usuario) {
        PagoMatricula pago = new PagoMatricula();
        pago.setMatricula(matricula);
        pago.setTipo(tipo);
        pago.setMonto(monto);
        pago.setMetodoPago(metodo);
        pago.setCodigoTransaccion(metodo.isRequiereCodigo() && !vacio(codigo) ? codigo.trim() : null);
        if (metodo.isRequiereComprobante()) {
            UploadResponse subida = subir(comprobante, "comprobantes");
            pago.setUrlComprobante(subida.getUrl());
            pago.setPkComprobante(subida.getPublicId());
        }
        pago.setFecha(LocalDateTime.now());
        pago.setUsuario(usuario);
        pago.setAcceso(acceso(AccesoConstants.ACTIVO));
        pagoMatriculaRepository.save(pago);
    }

    /** Sube el archivo y lo borra si la transacción se deshace, para no dejar fotos sueltas. */
    private UploadResponse subir(MultipartFile archivo, String carpeta) {
        UploadResponse subida = cloudinaryService.upload(archivo, carpeta);
        alDeshacer(() -> borrarArchivo(subida.getPublicId()));
        return subida;
    }

    private void borrarArchivo(String publicId) {
        try {
            cloudinaryService.delete(publicId);
        } catch (RuntimeException e) {
            log.warn("No se pudo borrar el archivo {} de Cloudinary: {}", publicId, e.getMessage());
        }
    }

    private void alConfirmar(Runnable accion) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            accion.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                accion.run();
            }
        });
    }

    private void alDeshacer(Runnable accion) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == STATUS_ROLLED_BACK) {
                    accion.run();
                }
            }
        });
    }

    // ---------- Utilidades ----------

    private GradoSeccion findGradoSeccion(Integer id) {
        return gradoSeccionRepository.findByIdGradoSeccionAndAccesoNot(id, eliminado())
                .orElseThrow(() -> new ResourceNotFoundException("Sección no encontrada con id " + id));
    }

    private String generarCodigoApoderado() {
        String prefijo = "P" + LocalDate.now().getYear();
        long correlativo = usuarioRepository.countByCodigoStartingWith(prefijo) + 1;
        return String.format("%s%04d", prefijo, correlativo);
    }

    private static String sha256(String texto) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(texto.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("No se pudo generar el hash del código", e);
        }
    }

    private static void exigir(boolean condicion, String mensaje) {
        if (!condicion) {
            throw new IllegalArgumentException(mensaje);
        }
    }

    private static boolean vacio(String texto) {
        return texto == null || texto.isBlank();
    }

    private static boolean esActivo(Acceso acceso) {
        return !Objects.equals(acceso.getIdAcceso(), AccesoConstants.ELIMINADO);
    }

    private Acceso acceso(Long id) {
        return accesoRepository.findById(id).orElseThrow();
    }

    private Acceso eliminado() {
        return acceso(AccesoConstants.ELIMINADO);
    }
}
