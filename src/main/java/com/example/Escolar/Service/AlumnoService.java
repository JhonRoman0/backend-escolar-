package com.example.Escolar.Service;

import com.example.Escolar.Dto.AlumnoReporteResponse;
import com.example.Escolar.Dto.AlumnoRequest;
import com.example.Escolar.Dto.AlumnoResponse;
import com.example.Escolar.Dto.ApoderadoRequest;
import com.example.Escolar.Dto.UploadResponse;
import com.example.Escolar.Exception.ResourceNotFoundException;
import com.example.Escolar.Model.Alumno;
import com.example.Escolar.Model.AlumnoApoderado;
import com.example.Escolar.Model.Apoderado;
import com.example.Escolar.Model.Matricula;
import com.example.Escolar.Model.Rol;
import com.example.Escolar.Model.Usuario;
import com.example.Escolar.Model.UsuarioRol;
import com.example.Escolar.Repository.AccesoRepository;
import com.example.Escolar.Repository.AlumnoApoderadoRepository;
import com.example.Escolar.Repository.AlumnoRepository;
import com.example.Escolar.Repository.ApoderadoRepository;
import com.example.Escolar.Repository.MatriculaRepository;
import com.example.Escolar.Repository.RolRepository;
import com.example.Escolar.Repository.UsuarioRepository;
import com.example.Escolar.Repository.UsuarioRolRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AlumnoService {

    public static final byte PRINCIPAL = 1;

    public static final String ROL_APODERADO = "APODERADO";

    private final AlumnoRepository alumnoRepository;
    private final ApoderadoRepository apoderadoRepository;
    private final AlumnoApoderadoRepository alumnoApoderadoRepository;
    private final UsuarioRepository usuarioRepository;
    private final UsuarioRolRepository usuarioRolRepository;
    private final RolRepository rolRepository;
    private final MatriculaRepository matriculaRepository;
    private final PasswordEncoder passwordEncoder;
    private final CloudinaryService cloudinaryService;
    private final AccesoContextoService accesoContextoService;
    private final AccesoRepository accesoRepository;

    public Page<AlumnoResponse> getAll(Pageable pageable, Integer idUsuario, List<String> roles) {
        Set<Integer> visibles = idsAlumnosVisibles(idUsuario, roles);
        List<Alumno> contenido = alumnoRepository.findByAccesoNot(accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow(), pageable).getContent();
        List<AlumnoResponse> respuestas = contenido.stream()
                .filter(a -> visibles == null || visibles.contains(a.getIdAlumno()))
                .map(this::toResponse)
                .toList();
        return new PageImpl<>(respuestas, pageable, respuestas.size());
    }

    public AlumnoResponse getById(Integer id, Integer idUsuario, List<String> roles) {
        Alumno alumno = findAlumno(id);
        if (!esVisible(alumno.getIdAlumno(), idUsuario, roles)) {
            throw new ResourceNotFoundException("Alumno no encontrado con id " + id);
        }
        return toResponse(alumno);
    }

    public AlumnoResponse getByDocumento(String documento, Integer idUsuario, List<String> roles) {
        if (documento == null || documento.isBlank()) {
            throw new IllegalArgumentException("El documento de identidad es obligatorio");
        }
        Alumno alumno = alumnoRepository.findByDocumentoIdentidadAndAccesoNot(documento, accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow())
                .orElseThrow(() -> new ResourceNotFoundException("Alumno no encontrado con documento " + documento));
        if (!esVisible(alumno.getIdAlumno(), idUsuario, roles)) {
            throw new ResourceNotFoundException("Alumno no encontrado con documento " + documento);
        }
        return toResponse(alumno);
    }

    @Transactional
    public AlumnoResponse create(AlumnoRequest request) {
        validarDocumentoAlumnoUnico(request.getDocumentoIdentidad(), null);
        List<Apoderado> apoderados = resolverApoderados(request.getApoderados());

        Alumno alumno = new Alumno();
        aplicarDatos(alumno, request);
        alumno.setCodigo(generarCodigoAlumno(request.getDocumentoIdentidad()));
        alumno.setCodigoHash(generarCodigoHash(alumno.getCodigo()));
        alumno.setFechaIngreso(LocalDate.now());
        Alumno saved = alumnoRepository.save(alumno);

        vincularApoderados(saved, apoderados);

        return toResponse(saved);
    }

    @Transactional
    public AlumnoResponse update(Integer id, AlumnoRequest request) {
        Alumno alumno = findAlumno(id);
        validarDocumentoAlumnoUnico(request.getDocumentoIdentidad(), id);
        if (request.getApoderados() != null) {
            sincronizarApoderados(alumno, request.getApoderados());
        }
        aplicarDatos(alumno, request);
        return toResponse(alumnoRepository.save(alumno));
    }

    private void vincularApoderados(Alumno alumno, List<Apoderado> apoderados) {
        for (int i = 0; i < apoderados.size(); i++) {
            AlumnoApoderado vinculo = new AlumnoApoderado();
            vinculo.setAlumno(alumno);
            vinculo.setApoderado(apoderados.get(i));
            vinculo.setApoPrincipal(i == 0 ? PRINCIPAL : (byte) 0);
            alumnoApoderadoRepository.save(vinculo);
        }
    }

    private void sincronizarApoderados(Alumno alumno, List<ApoderadoRequest> requests) {
        List<Apoderado> nuevos = requests.isEmpty()
                ? List.of()
                : resolverApoderados(requests);
        List<AlumnoApoderado> vinculos = alumnoApoderadoRepository.findByAlumno(alumno);

        for (AlumnoApoderado vinculo : vinculos) {
            if (!nuevos.stream().anyMatch(a -> a.getIdApoderado().equals(vinculo.getApoderado().getIdApoderado()))) {
                alumnoApoderadoRepository.delete(vinculo);
            }
        }

        List<Apoderado> actuales = alumnoApoderadoRepository.findByAlumno(alumno).stream()
                .map(AlumnoApoderado::getApoderado)
                .toList();
        for (int i = 0; i < nuevos.size(); i++) {
            Apoderado apoderado = nuevos.get(i);
            boolean yaVinculado = actuales.stream()
                    .anyMatch(a -> a.getIdApoderado().equals(apoderado.getIdApoderado()));
            if (!yaVinculado) {
                AlumnoApoderado vinculo = new AlumnoApoderado();
                vinculo.setAlumno(alumno);
                vinculo.setApoderado(apoderado);
                vinculo.setApoPrincipal(i == 0 ? PRINCIPAL : (byte) 0);
                alumnoApoderadoRepository.save(vinculo);
            }
        }

        reasignarPrincipal(alumno);
    }

    private void reasignarPrincipal(Alumno alumno) {
        List<AlumnoApoderado> vinculos = alumnoApoderadoRepository.findByAlumno(alumno);
        for (int i = 0; i < vinculos.size(); i++) {
            AlumnoApoderado vinculo = vinculos.get(i);
            byte principal = i == 0 ? PRINCIPAL : (byte) 0;
            if (vinculo.getApoPrincipal() != principal) {
                vinculo.setApoPrincipal(principal);
                alumnoApoderadoRepository.save(vinculo);
            }
        }
    }

    @Transactional
    public void delete(Integer id) {
        Alumno alumno = findAlumno(id);
        alumno.setAcceso(accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow());
        alumnoRepository.save(alumno);
        List<AlumnoApoderado> vinculos = alumnoApoderadoRepository.findByAlumno(alumno);
        for (AlumnoApoderado vinculo : vinculos) {
            if (matriculaRepository.existsByAlumnoApoderado(vinculo)) {
                continue;
            }
            Apoderado apoderado = vinculo.getApoderado();
            alumnoApoderadoRepository.delete(vinculo);
            if (apoderado != null && noTieneOtrosAlumnosActivos(apoderado)) {
                marcarApoderadoEliminado(apoderado);
            }
        }
    }

    private boolean noTieneOtrosAlumnosActivos(Apoderado apoderado) {
        return alumnoApoderadoRepository.findByApoderado(apoderado).stream()
                .map(AlumnoApoderado::getAlumno)
                .noneMatch(a -> !a.getAcceso().getIdAcceso().equals(AccesoConstants.ELIMINADO));
    }

    private void marcarApoderadoEliminado(Apoderado apoderado) {
        apoderado.setAcceso(accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow());
        apoderadoRepository.save(apoderado);
        Usuario usuario = apoderado.getUsuario();
        if (usuario != null) {
            removerRolApoderado(usuario.getIdUsuario());
            eliminateIfNoOtherActiveRole(usuario);
        }
    }

    private void removerRolApoderado(Integer idUsuario) {
        rolRepository.findByNombre(ROL_APODERADO)
                .ifPresent(rolApoderado -> usuarioRolRepository.findByUsuarioIdUsuario(idUsuario).stream()
                        .filter(ur -> ur.getRol().getIdRol().equals(rolApoderado.getIdRol()))
                        .findFirst()
                        .ifPresent(usuarioRolRepository::delete));
    }

    private void eliminateIfNoOtherActiveRole(Usuario usuario) {
        boolean tieneOtrosRoles = usuarioRolRepository.findByUsuarioIdUsuario(usuario.getIdUsuario()).stream()
                .anyMatch(ur -> !ur.getRol().getAcceso().getIdAcceso().equals(AccesoConstants.ELIMINADO));
        if (!tieneOtrosRoles) {
            usuario.setAcceso(accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow());
            usuarioRepository.save(usuario);
        }
    }

    @Transactional
    public AlumnoResponse subirFoto(Integer id, MultipartFile file) {
        Alumno alumno = findAlumno(id);
        if (alumno.getPkUrlFoto() != null && !alumno.getPkUrlFoto().isBlank()) {
            cloudinaryService.delete(alumno.getPkUrlFoto());
        }
        UploadResponse upload = cloudinaryService.upload(file, "alumnos");
        alumno.setUrlFoto(upload.getUrl());
        alumno.setPkUrlFoto(upload.getPublicId());
        return toResponse(alumnoRepository.save(alumno));
    }

    @Transactional
    public void eliminarFoto(Integer id) {
        Alumno alumno = findAlumno(id);
        if (alumno.getPkUrlFoto() != null && !alumno.getPkUrlFoto().isBlank()) {
            cloudinaryService.delete(alumno.getPkUrlFoto());
            alumno.setUrlFoto(null);
            alumno.setPkUrlFoto(null);
            alumnoRepository.save(alumno);
        }
    }

    @Transactional(readOnly = true)
    public List<AlumnoReporteResponse> reporte(LocalDate inicio, LocalDate fin, Integer idGradoSeccion) {
        return alumnoRepository.findByFechaIngresoBetweenAndAccesoNot(inicio, fin, accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow()).stream()
                .filter(alumno -> {
                    if (idGradoSeccion == null) return true;
                    return alumnoApoderadoRepository.findByAlumno(alumno).stream()
                            .flatMap(aa -> matriculaRepository.findByAlumnoApoderadoAndAccesoNot(aa, accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow()).stream())
                            .anyMatch(m -> idGradoSeccion.equals(m.getGradoSeccion().getIdGradoSeccion()));
                })
                .map(alumno -> {
                    AlumnoReporteResponse r = new AlumnoReporteResponse();
                    r.setIdAlumno(alumno.getIdAlumno());
                    r.setNombre(alumno.getNombre() + " " + alumno.getApellidoPat() + " " + alumno.getApellidoMat());
                    r.setCodigo(alumno.getCodigo());
                    r.setDocumentoIdentidad(alumno.getDocumentoIdentidad());
                    r.setFechaNacimiento(alumno.getFechaNacimiento());
                    r.setFechaIngreso(alumno.getFechaIngreso());
                    String gs = alumnoApoderadoRepository.findByAlumno(alumno).stream()
                            .flatMap(aa -> matriculaRepository.findByAlumnoApoderadoAndAccesoNot(aa, accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow()).stream())
                            .map(m -> m.getGradoSeccion().getGrado().getNombre() + " "
                                    + (m.getGradoSeccion().getSeccion() != null ? m.getGradoSeccion().getSeccion().getNombre() : "Única"))
                            .findFirst().orElse("Sin matrícula");
                    r.setGradoSeccion(gs);
                    String apo = alumnoApoderadoRepository.findByAlumno(alumno).stream()
                            .filter(aa -> aa.getApoPrincipal() == PRINCIPAL)
                            .map(aa -> aa.getApoderado().getUsuario().getNombre() + " "
                                    + aa.getApoderado().getUsuario().getApellidoPat())
                            .findFirst().orElse("Sin apoderado");
                    r.setApoderado(apo);
                    return r;
                })
                .toList();
    }

    private List<Apoderado> resolverApoderados(List<ApoderadoRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            throw new IllegalArgumentException("Debe indicar al menos un apoderado");
        }
        validarDocumentosApoderadosUnicos(requests);
        return requests.stream().map(this::resolverApoderado).toList();
    }

    private void validarDocumentosApoderadosUnicos(List<ApoderadoRequest> requests) {
        Set<String> documentos = new HashSet<>();
        for (ApoderadoRequest request : requests) {
            String documento = request.getDocumentoIdentidad();
            if (documento == null || documento.isBlank()) {
                continue;
            }
            if (!documentos.add(documento)) {
                throw new IllegalArgumentException("No se puede asignar el mismo documento de identidad a dos apoderados");
            }
        }
    }

    private Apoderado resolverApoderado(ApoderadoRequest request) {
        if (request.getDocumentoIdentidad() == null || request.getDocumentoIdentidad().isBlank()) {
            return crearApoderado(request);
        }
        return apoderadoRepository
                .findByUsuarioDocumentoIdentidadAndAccesoNot(request.getDocumentoIdentidad(), accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow())
                .map(apoderado -> aplicarDatosApoderado(apoderado, request))
                .orElseGet(() -> reutilizarUsuarioExistente(request));
    }

    private Apoderado reutilizarUsuarioExistente(ApoderadoRequest request) {
        Usuario usuario = usuarioRepository
                .findByDocumentoIdentidadAndAccesoNot(request.getDocumentoIdentidad(), accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow())
                .orElse(null);
        if (usuario == null) {
            return crearApoderado(request);
        }
        Optional<Apoderado> apoderadoExistente = apoderadoRepository
                .findByUsuarioIdUsuarioAndAccesoNot(usuario.getIdUsuario(), accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow());
        if (apoderadoExistente.isPresent()) {
            return aplicarDatosApoderado(apoderadoExistente.get(), request);
        }
        asignarRolApoderado(usuario);
        Apoderado apoderado = new Apoderado();
        apoderado.setUsuario(usuario);
        apoderado.setCelular(request.getCelular());
        apoderado.setDireccion(request.getDireccion());
        apoderado.setParentesco(request.getParentesco());
        apoderado.setAcceso(accesoRepository.findById(AccesoConstants.ACTIVO).orElseThrow());
        return apoderadoRepository.save(apoderado);
    }

    private Apoderado aplicarDatosApoderado(Apoderado apoderado, ApoderadoRequest request) {
        boolean modificado = false;
        if (request.getCelular() != null) {
            apoderado.setCelular(request.getCelular());
            modificado = true;
        }
        if (request.getDireccion() != null) {
            apoderado.setDireccion(request.getDireccion());
            modificado = true;
        }
        if (request.getParentesco() != null) {
            apoderado.setParentesco(request.getParentesco());
            modificado = true;
        }
        if (modificado) {
            apoderadoRepository.save(apoderado);
        }
        return apoderado;
    }

    private Apoderado crearApoderado(ApoderadoRequest request) {
        if (request.getContraseña() == null || request.getContraseña().isBlank()) {
            throw new IllegalArgumentException("La contraseña del apoderado es obligatoria");
        }
        if (request.getFechaNaci() == null) {
            throw new IllegalArgumentException("La fecha de nacimiento del apoderado es obligatoria");
        }
        if (request.getNombre() == null || request.getNombre().isBlank()
                || request.getApellidoPat() == null || request.getApellidoPat().isBlank()
                || request.getApellidoMat() == null || request.getApellidoMat().isBlank()) {
            throw new IllegalArgumentException("Los datos del apoderado (nombre y apellidos) son obligatorios");
        }
        validarDocumentoUsuarioUnico(request.getDocumentoIdentidad(), null);
        validarGmailUnico(request.getGmail(), null);
        Usuario usuario = new Usuario();
        construirUsuario(usuario, request, accesoRepository.findById(AccesoConstants.ACTIVO).orElseThrow());
        usuario.setCodigo(generarCodigoApoderado());
        usuario.setFechaCreacion(LocalDate.now());
        Usuario saved = usuarioRepository.save(usuario);
        asignarRolApoderado(saved);

        Apoderado apoderado = new Apoderado();
        apoderado.setUsuario(saved);
        apoderado.setCelular(request.getCelular());
        apoderado.setDireccion(request.getDireccion());
        apoderado.setParentesco(request.getParentesco());
        apoderado.setAcceso(accesoRepository.findById(AccesoConstants.ACTIVO).orElseThrow());
        return apoderadoRepository.save(apoderado);
    }

    private void construirUsuario(Usuario usuario, ApoderadoRequest request, com.example.Escolar.Model.Acceso acceso) {
        usuario.setNombre(request.getNombre());
        usuario.setApellidoPat(request.getApellidoPat());
        usuario.setApellidoMat(request.getApellidoMat());
        if (request.getDocumentoIdentidad() != null && !request.getDocumentoIdentidad().isBlank()) {
            usuario.setDocumentoIdentidad(request.getDocumentoIdentidad());
        }
        usuario.setContraseña(passwordEncoder.encode(request.getContraseña()));
        usuario.setGmail(request.getGmail());
        usuario.setFechaNaci(request.getFechaNaci());
        if (usuario.getAcceso().getIdAcceso().equals(AccesoConstants.ELIMINADO)) {
            usuario.setAcceso(accesoRepository.findById(AccesoConstants.ACTIVO).orElseThrow());
        } else {
            usuario.setAcceso(acceso);
        }
    }

    private void asignarRolApoderado(Usuario usuario) {
        Rol rolApoderado = rolRepository.findByNombre(ROL_APODERADO)
                .orElseGet(() -> {
                    Rol rol = new Rol();
                    rol.setNombre(ROL_APODERADO);
                    rol.setAcceso(accesoRepository.findById(AccesoConstants.ACTIVO).orElseThrow());
                    return rolRepository.save(rol);
                });
        boolean yaTiene = usuarioRolRepository.findByUsuarioIdUsuario(usuario.getIdUsuario()).stream()
                .anyMatch(ur -> ur.getRol().getIdRol().equals(rolApoderado.getIdRol()));
        if (yaTiene) {
            return;
        }
        UsuarioRol usuarioRol = new UsuarioRol();
        usuarioRol.setUsuario(usuario);
        usuarioRol.setRol(rolApoderado);
        usuarioRol.setFechaAsignacion(LocalDateTime.now());
        usuarioRolRepository.save(usuarioRol);
    }

    private void aplicarDatos(Alumno alumno, AlumnoRequest request) {
        alumno.setNombre(request.getNombre().trim());
        alumno.setApellidoPat(request.getApellidoPat().trim());
        alumno.setApellidoMat(request.getApellidoMat().trim());
        alumno.setFechaNacimiento(request.getFechaNacimiento());
        alumno.setDireccion(request.getDireccion());
        if (request.getDocumentoIdentidad() != null && !request.getDocumentoIdentidad().isBlank()) {
            alumno.setDocumentoIdentidad(request.getDocumentoIdentidad());
        }
        if (request.getAccesoId() != null) {
            alumno.setAcceso(accesoRepository.findById(request.getAccesoId()).orElseThrow());
        }
    }

    private void validarDocumentoAlumnoUnico(String documento, Integer idExcluir) {
        if (documento == null || documento.isBlank()) {
            return;
        }
        alumnoRepository.findByDocumentoIdentidadAndAccesoNot(documento, accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow())
                .filter(a -> idExcluir == null || !a.getIdAlumno().equals(idExcluir))
                .ifPresent(a -> {
                    throw new IllegalArgumentException("Ya existe un alumno con ese documento de identidad");
                });
    }

    private void validarDocumentoUsuarioUnico(String documento, Integer idExcluir) {
        if (documento == null || documento.isBlank()) {
            return;
        }
        usuarioRepository.findByDocumentoIdentidadAndAccesoNot(documento, accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow())
                .filter(u -> idExcluir == null || !u.getIdUsuario().equals(idExcluir))
                .ifPresent(u -> {
                    throw new IllegalArgumentException("Ya existe un usuario con ese documento de identidad");
                });
    }

    private void validarGmailUnico(String gmail, Integer idExcluir) {
        if (gmail == null || gmail.isBlank()) {
            return;
        }
        usuarioRepository.findByGmailAndAccesoNot(gmail, accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow())
                .filter(u -> idExcluir == null || !u.getIdUsuario().equals(idExcluir))
                .ifPresent(u -> {
                    throw new IllegalArgumentException("Ya existe un usuario con ese correo electronico");
                });
    }

    private String generarCodigoAlumno(String documentoIdentidad) {
        if (documentoIdentidad == null || documentoIdentidad.isBlank()) {
            throw new IllegalArgumentException("El documento de identidad es obligatorio para generar el codigo del alumno");
        }
        if (documentoIdentidad.length() != 8) {
            throw new IllegalArgumentException("El DNI debe contener exactamente 8 digitos");
        }
        return "00000000" + documentoIdentidad;
    }

    private String generarCodigoApoderado() {
        String prefijo = "P" + LocalDate.now().getYear();
        long correlativo = usuarioRepository.countByCodigoStartingWith(prefijo) + 1;
        return String.format("%s%04d", prefijo, correlativo);
    }

    private String generarCodigoHash(String codigo) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(codigo.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalArgumentException("No se pudo generar el hash del código");
        }
    }

    private Set<Integer> idsAlumnosVisibles(Integer idUsuario, List<String> roles) {
        if (accesoContextoService.esGestion(roles)) {
            return null;
        }
        if (accesoContextoService.esPersonalDocente(roles)) {
            Set<Integer> ids = new HashSet<>();
            for (Matricula matricula : accesoContextoService.matriculasDeGradoSecciones(
                    accesoContextoService.gradoSeccionIdsDeDocente(idUsuario))) {
                ids.add(matricula.getAlumnoApoderado().getAlumno().getIdAlumno());
            }
            return ids;
        }
        if (accesoContextoService.esApoderado(roles)) {
            Set<Integer> ids = new HashSet<>();
            Apoderado apoderado = accesoContextoService.apoderadoDeUsuario(idUsuario).orElse(null);
            if (apoderado == null) {
                return ids;
            }
            alumnoApoderadoRepository.findByApoderado(apoderado)
                    .forEach(aa -> ids.add(aa.getAlumno().getIdAlumno()));
            return ids;
        }
        return new HashSet<>();
    }

    private boolean esVisible(Integer idAlumno, Integer idUsuario, List<String> roles) {
        Set<Integer> visibles = idsAlumnosVisibles(idUsuario, roles);
        return visibles == null || visibles.contains(idAlumno);
    }

    private Alumno findAlumno(Integer id) {
        return alumnoRepository.findByIdAlumnoAndAccesoNot(id, accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow())
                .orElseThrow(() -> new ResourceNotFoundException("Alumno no encontrado con id " + id));
    }

    private AlumnoResponse toResponse(Alumno alumno) {
        List<AlumnoApoderado> vinculos = alumnoApoderadoRepository.findByAlumno(alumno);
        return AlumnoResponse.fromEntity(alumno, vinculos);
    }
}
