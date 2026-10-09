package com.example.Escolar.Service;

import com.example.Escolar.Dto.RolResponse;
import com.example.Escolar.Dto.UploadResponse;
import com.example.Escolar.Dto.UsuarioReporteResponse;
import com.example.Escolar.Dto.UsuarioRequest;
import com.example.Escolar.Dto.UsuarioResponse;
import com.example.Escolar.Exception.ResourceNotFoundException;
import com.example.Escolar.Exception.UsuarioReactivacionException;
import com.example.Escolar.Model.Acceso;
import com.example.Escolar.Model.Docente;
import com.example.Escolar.Model.DocenteNivel;
import com.example.Escolar.Model.GradoAcademico;
import com.example.Escolar.Model.Nivel;
import com.example.Escolar.Model.Rol;
import com.example.Escolar.Model.TipoContrato;
import com.example.Escolar.Model.Usuario;
import com.example.Escolar.Model.UsuarioRol;
import com.example.Escolar.Repository.AccesoRepository;
import com.example.Escolar.Repository.DocenteNivelRepository;
import com.example.Escolar.Repository.DocenteRepository;
import com.example.Escolar.Repository.GradoAcademicoRepository;
import com.example.Escolar.Repository.NivelRepository;
import com.example.Escolar.Repository.RolRepository;
import com.example.Escolar.Repository.TipoContratoRepository;
import com.example.Escolar.Repository.UsuarioRepository;
import com.example.Escolar.Repository.UsuarioRolRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    public static final String ROL_DOCENTE = "DOCENTE";

    private final UsuarioRepository usuarioRepository;
    private final AccesoRepository accesoRepository;
    private final RolRepository rolRepository;
    private final UsuarioRolRepository usuarioRolRepository;
    private final PasswordEncoder passwordEncoder;
    private final CloudinaryService cloudinaryService;
    private final DocenteRepository docenteRepository;
    private final DocenteNivelRepository docenteNivelRepository;
    private final NivelRepository nivelRepository;
    private final GradoAcademicoRepository gradoAcademicoRepository;
    private final TipoContratoRepository tipoContratoRepository;
    private final EmailService emailService;

    public Page<UsuarioResponse> getAll(Pageable pageable) {
        return getAll(pageable, null, null, null);
    }

    public Page<UsuarioResponse> getAll(Pageable pageable, String q, Integer idRol, Long idAcceso) {
        String patron = (q == null || q.isBlank()) ? null : "%" + q.trim().toLowerCase() + "%";
        return usuarioRepository
                .buscarFiltrados(AccesoConstants.ELIMINADO, null, null, patron, idRol, idAcceso, pageable)
                .map(this::toResponse);
    }

    public UsuarioResponse getById(Integer id) {
        return toResponse(findUsuario(id));
    }

    @Transactional
    public UsuarioResponse create(UsuarioRequest request) {
        if (request.getContraseña() == null || request.getContraseña().isBlank()) {
            throw new IllegalArgumentException("La contraseña es obligatoria");
        }
        if (request.getRolIds() == null || request.getRolIds().isEmpty()) {
            throw new IllegalArgumentException("Debe asignar al menos un rol");
        }
        validarDocumentoUnico(request.getDocumentoIdentidad(), null);
        validarGmailUnico(request.getGmail(), null);
        detectarEliminadoParaReactivar(request.getDocumentoIdentidad());
        Usuario usuario = new Usuario();
        if (request.getAccesoId() == null) {
            request.setAccesoId(AccesoConstants.ACTIVO.longValue());
        }
        applyRequest(usuario, request);
        usuario.setCodigo(generarCodigo(request.getRolIds()));
        usuario.setFechaCreacion(LocalDate.now());
        Usuario saved = usuarioRepository.save(usuario);
        assignRoles(saved, request.getRolIds());
        if (seleccionaDocente(request.getRolIds())) {
            crearDocente(saved, request);
        }
        UsuarioResponse response = toResponse(saved);
        response.setCredencialesEnviadas(enviarCredencialesAcceso(saved, request));
        return response;
    }

    @Transactional
    public UsuarioResponse update(Integer id, UsuarioRequest request) {
        Usuario usuario = findUsuario(id);
        return actualizarRegistro(usuario, id, request);
    }

    /**
     * Reactiva un usuario eliminado logicamente reutilizando la misma fila y su
     * codigo. La contraseña, los datos personales y los roles pasan a ser
     * exactamente los enviados en la peticion (no se conserva la contraseña
     * anterior). Si no incluye rol DOCENTE, el registro Docente asociado
     * permanece eliminado.
     */
    @Transactional
    public UsuarioResponse reactivar(Integer id, UsuarioRequest request) {
        if (request.getContraseña() == null || request.getContraseña().isBlank()) {
            throw new IllegalArgumentException("La contraseña es obligatoria para reactivar el usuario");
        }
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con id " + id));
        if (!usuario.getAcceso().getIdAcceso().equals(AccesoConstants.ELIMINADO)) {
            throw new IllegalArgumentException("El usuario no esta eliminado y no requiere reactivacion");
        }
        usuario.setAcceso(accesoRepository.findById(AccesoConstants.ACTIVO).orElseThrow());
        usuario.setIntentosFallidos(0);
        usuario.setFechaBloqueo(null);
        usuario.setResetToken(null);
        usuario.setResetTokenExpiracion(null);
        return actualizarRegistro(usuario, id, request, true);
    }

    private UsuarioResponse actualizarRegistro(Usuario usuario, Integer id, UsuarioRequest request) {
        return actualizarRegistro(usuario, id, request, false);
    }

    private UsuarioResponse actualizarRegistro(Usuario usuario, Integer id, UsuarioRequest request, boolean enviarCredenciales) {
        // Se excluye el id propio para permitir conservar su DNI/correo; si los
        // nuevos datos fueran de otra cuenta activa, aqui se detectaria el choque.
        validarDocumentoUnico(request.getDocumentoIdentidad(), id);
        validarGmailUnico(request.getGmail(), id);
        applyRequest(usuario, request);

        if (request.getRolIds() != null) {
            Optional<Docente> docenteExistente = docenteRepository.findByUsuarioIdUsuario(id);
            boolean eraDocenteActivo = docenteExistente.isPresent()
                    && !docenteExistente.get().getAcceso().getIdAcceso().equals(AccesoConstants.ELIMINADO);
            boolean seraDocente = seleccionaDocente(request.getRolIds());

            usuarioRolRepository.deleteByUsuarioIdUsuario(id);
            assignRoles(usuario, request.getRolIds());

            if (eraDocenteActivo && !seraDocente) {
                eliminarDocente(docenteExistente.get());
            } else if (seraDocente) {
                crearOReactivarDocente(usuario, docenteExistente.orElse(null), request);
            }
        }
        UsuarioResponse response = toResponse(usuarioRepository.save(usuario));
        if (enviarCredenciales) {
            response.setCredencialesEnviadas(enviarCredencialesAcceso(usuario, request));
        }
        return response;
    }

    private boolean enviarCredencialesAcceso(Usuario usuario, UsuarioRequest request) {
        String gmail = request.getGmail();
        if (gmail == null || gmail.isBlank()) {
            return false;
        }
        String nombre = (usuario.getNombre() + " " + usuario.getApellidoPat()).trim();
        return emailService.enviarCredencialesAcceso(gmail, nombre, usuario.getCodigo(), request.getContraseña());
    }

    @Transactional
    public void desbloquear(Integer id) {
        Usuario usuario = findUsuario(id);
        usuario.setIntentosFallidos(0);
        usuario.setFechaBloqueo(null);
        usuarioRepository.save(usuario);
    }

    @Transactional
    public void delete(Integer id) {
        Usuario usuario = findUsuario(id);
        usuario.setAcceso(accesoEliminado());
        usuarioRepository.save(usuario);
        // Si el usuario era docente, su registro Docente (y sus niveles) tambien
        // se elimina logicamente para que no quede activo sin acceso. La
        // reactivacion lo restaura.
        docenteRepository.findByUsuarioIdUsuario(id).ifPresent(docente -> {
            if (!docente.getAcceso().getIdAcceso().equals(AccesoConstants.ELIMINADO)) {
                eliminarDocente(docente);
            }
        });
    }

    @Transactional(readOnly = true)
    public List<UsuarioReporteResponse> reporte(LocalDate inicio, LocalDate fin, String q, Integer idRol, Long idAcceso) {
        String patron = (q == null || q.isBlank()) ? null : "%" + q.trim().toLowerCase() + "%";
        return usuarioRepository
                .buscarFiltrados(AccesoConstants.ELIMINADO, inicio, fin, patron, idRol, idAcceso, Pageable.unpaged())
                .getContent().stream()
                .map(u -> {
                    UsuarioReporteResponse r = new UsuarioReporteResponse();
                    r.setIdUsuario(u.getIdUsuario());
                    r.setNombre(u.getNombre() + " " + u.getApellidoPat() + " " + u.getApellidoMat());
                    r.setCodigo(u.getCodigo());
                    r.setDocumentoIdentidad(u.getDocumentoIdentidad());
                    r.setGmail(u.getGmail());
                    r.setFechaCreacion(u.getFechaCreacion());
                    r.setRoles(usuarioRolRepository.findByUsuarioIdUsuario(u.getIdUsuario()).stream()
                            .map(UsuarioRol::getRol)
                            .filter(rol -> !rol.getAcceso().getIdAcceso().equals(AccesoConstants.ELIMINADO))
                            .map(Rol::getNombre)
                            .toList());
                    return r;
                })
                .toList();
    }

    public List<UsuarioResponse> getUsuariosPorRol(Integer idRol) {
        rolRepository.findByIdRolAndAccesoNot(idRol, accesoEliminado())
                .orElseThrow(() -> new ResourceNotFoundException("Rol no encontrado con id " + idRol));
        return usuarioRolRepository.findByRolIdRol(idRol).stream()
                .map(UsuarioRol::getUsuario)
                .filter(u -> !u.getAcceso().getIdAcceso().equals(AccesoConstants.ELIMINADO))
                .map(this::toResponse)
                .toList();
    }

    public List<RolResponse> getRolesDelUsuario(Integer idUsuario) {
        findUsuario(idUsuario);
        return usuarioRolRepository.findByUsuarioIdUsuario(idUsuario).stream()
                .map(ur -> toRolResponse(ur.getRol()))
                .filter(r -> !r.getAccesoId().equals(AccesoConstants.ELIMINADO.longValue()))
                .toList();
    }

    @Transactional
    public UsuarioResponse subirFoto(Integer id, MultipartFile file) {
        Usuario usuario = findUsuario(id);
        if (usuario.getPkUrlFoto() != null && !usuario.getPkUrlFoto().isBlank()) {
            cloudinaryService.delete(usuario.getPkUrlFoto());
        }
        UploadResponse upload = cloudinaryService.upload(file, obtenerCarpetaPorRol(usuario));
        usuario.setUrlFoto(upload.getUrl());
        usuario.setPkUrlFoto(upload.getPublicId());
        return toResponse(usuarioRepository.save(usuario));
    }

    private String obtenerCarpetaPorRol(Usuario usuario) {
        return usuarioRolRepository.findByUsuarioIdUsuario(usuario.getIdUsuario()).stream()
                .map(UsuarioRol::getRol)
                .filter(r -> !r.getAcceso().getIdAcceso().equals(AccesoConstants.ELIMINADO))
                .findFirst()
                .map(rol -> "usuarios/" + sanitizarNombreCarpeta(rol.getNombre()))
                .orElseThrow(() -> new IllegalArgumentException("El usuario debe tener un rol para subir su foto"));
    }

    private String sanitizarNombreCarpeta(String nombre) {
        String normalizado = nombre.toLowerCase()
                .replace('á', 'a').replace('é', 'e').replace('í', 'i')
                .replace('ó', 'o').replace('ú', 'u').replace('ñ', 'n');
        return normalizado.replace(' ', '-');
    }

    @Transactional
    public void eliminarFoto(Integer id) {
        Usuario usuario = findUsuario(id);
        if (usuario.getPkUrlFoto() != null && !usuario.getPkUrlFoto().isBlank()) {
            cloudinaryService.delete(usuario.getPkUrlFoto());
            usuario.setUrlFoto(null);
            usuario.setPkUrlFoto(null);
            usuarioRepository.save(usuario);
        }
    }

    private void assignRoles(Usuario usuario, List<Integer> rolIds) {
        if (rolIds == null) {
            return;
        }
        for (Integer rolId : rolIds) {
            Rol rol = rolRepository.findByIdRolAndAccesoNot(rolId, accesoEliminado())
                    .orElseThrow(() -> new ResourceNotFoundException("Rol no encontrado con id " + rolId));
            UsuarioRol usuarioRol = new UsuarioRol();
            usuarioRol.setUsuario(usuario);
            usuarioRol.setRol(rol);
            usuarioRol.setFechaAsignacion(LocalDateTime.now());
            usuarioRolRepository.save(usuarioRol);
        }
    }

    private boolean seleccionaDocente(List<Integer> rolIds) {
        if (rolIds == null) {
            return false;
        }
        for (Integer rolId : rolIds) {
            Rol rol = rolRepository.findByIdRolAndAccesoNot(rolId, accesoEliminado()).orElse(null);
            if (rol != null && ROL_DOCENTE.equalsIgnoreCase(rol.getNombre())) {
                return true;
            }
        }
        return false;
    }

    private void crearOReactivarDocente(Usuario usuario, Docente existente, UsuarioRequest request) {
        // Un docente activo ya existe: al editar el usuario desde el modal los
        // datos especificos del docente se gestionan en la pestaña Docentes, así
        // que aquí se ignoran sin validar ni sobrescribir.
        if (existente != null) {
            if (existente.getAcceso().getIdAcceso().equals(AccesoConstants.ELIMINADO)) {
                existente.setAcceso(accesoRepository.findById(AccesoConstants.ACTIVO).orElseThrow());
                aplicarDatosDocente(existente, request);
                Docente guardado = docenteRepository.save(existente);
                sincronizarNiveles(guardado, request.getNiveles());
            }
            return;
        }
        crearDocente(usuario, request);
    }

    private void crearDocente(Usuario usuario, UsuarioRequest request) {
        validarDatosDocente(request);
        Docente docente = new Docente();
        docente.setUsuario(usuario);
        aplicarDatosDocente(docente, request);
        docente.setAcceso(accesoRepository.findById(AccesoConstants.ACTIVO).orElseThrow());
        Docente guardado = docenteRepository.save(docente);
        sincronizarNiveles(guardado, request.getNiveles());
    }

    private void validarDatosDocente(UsuarioRequest request) {
        if (request.getTipoContratoId() == null) {
            throw new IllegalArgumentException("El tipo de contrato es obligatorio para un docente");
        }
        if (request.getGradoAcademicoId() == null) {
            throw new IllegalArgumentException("El grado academico es obligatorio para un docente");
        }
        if (request.getFechaContratacion() == null) {
            throw new IllegalArgumentException("La fecha de contratacion es obligatoria para un docente");
        }
        if (request.getNiveles() == null || request.getNiveles().isEmpty()) {
            throw new IllegalArgumentException("Debe asignar al menos un nivel al docente");
        }
    }

    private void aplicarDatosDocente(Docente docente, UsuarioRequest request) {
        if (request.getTipoContratoId() != null) {
            TipoContrato tipoContrato = tipoContratoRepository
                    .findByIdTipoContratoAndAccesoNot(request.getTipoContratoId(), accesoEliminado())
                    .orElseThrow(() -> new ResourceNotFoundException("Tipo de contrato no encontrado con id " + request.getTipoContratoId()));
            docente.setTipoContrato(tipoContrato);
        }
        if (request.getGradoAcademicoId() != null) {
            GradoAcademico gradoAcademico = gradoAcademicoRepository
                    .findByIdGradoAcademicoAndAccesoNot(request.getGradoAcademicoId(), accesoEliminado())
                    .orElseThrow(() -> new ResourceNotFoundException("Grado academico no encontrado con id " + request.getGradoAcademicoId()));
            docente.setGradoAcademico(gradoAcademico);
        }
        if (request.getFechaContratacion() != null) {
            docente.setFechaContratacion(request.getFechaContratacion());
        }
    }

    private void sincronizarNiveles(Docente docente, List<Integer> niveles) {
        docenteNivelRepository.deleteByDocente(docente);
        if (niveles == null || niveles.isEmpty()) {
            return;
        }
        for (Integer idNivel : new LinkedHashSet<>(niveles)) {
            Nivel nivel = nivelRepository.findByIdNivelAndAccesoNot(idNivel, accesoEliminado())
                    .orElseThrow(() -> new ResourceNotFoundException("Nivel no encontrado con id " + idNivel));
            DocenteNivel vinculo = new DocenteNivel();
            vinculo.setDocente(docente);
            vinculo.setNivel(nivel);
            vinculo.setAcceso(accesoRepository.findById(AccesoConstants.ACTIVO).orElseThrow());
            docenteNivelRepository.save(vinculo);
        }
    }

    private void eliminarDocente(Docente docente) {
        docente.setAcceso(accesoEliminado());
        docenteRepository.save(docente);
        docenteNivelRepository.deleteByDocente(docente);
    }

    private String generarCodigo(List<Integer> rolIds) {
        Rol rol = rolRepository.findByIdRolAndAccesoNot(rolIds.get(0), accesoEliminado())
                .orElseThrow(() -> new ResourceNotFoundException("Rol no encontrado con id " + rolIds.get(0)));
        String inicial = String.valueOf(rol.getNombre().charAt(0)).toUpperCase();
        String prefijo = inicial + LocalDate.now().getYear();
        long correlativo = usuarioRepository.countByCodigoStartingWith(prefijo) + 1;
        return String.format("%s%04d", prefijo, correlativo);
    }

    private void validarDocumentoUnico(String documentoIdentidad, Integer idExcluir) {
        if (documentoIdentidad == null || documentoIdentidad.isBlank()) {
            return;
        }
        usuarioRepository.findByDocumentoIdentidadAndAccesoNot(documentoIdentidad, accesoEliminado())
                .filter(u -> idExcluir == null || !u.getIdUsuario().equals(idExcluir))
                .ifPresent(u -> {
                    throw new IllegalArgumentException("Ya existe un usuario con ese documento de identidad");
                });
    }

    private void validarGmailUnico(String gmail, Integer idExcluir) {
        if (gmail == null || gmail.isBlank()) {
            return;
        }
        usuarioRepository.findByGmailAndAccesoNot(gmail, accesoEliminado())
                .filter(u -> idExcluir == null || !u.getIdUsuario().equals(idExcluir))
                .ifPresent(u -> {
                    throw new IllegalArgumentException("Ya existe un usuario con ese correo electronico");
                });
    }

    /**
     * Si el DNI ya pertenece a un usuario eliminado logicamente, no se intenta
     * crear una fila nueva (lo impediria la constraint UNIQUE de
     * documento_identidad): se avisa para que el cliente ofrezca reactivar el
     * registro anterior.
     */
    private void detectarEliminadoParaReactivar(String documentoIdentidad) {
        if (documentoIdentidad == null || documentoIdentidad.isBlank()) {
            return;
        }
        usuarioRepository.findByDocumentoIdentidad(documentoIdentidad).ifPresent(u -> {
            if (u.getAcceso().getIdAcceso().equals(AccesoConstants.ELIMINADO)) {
                String nombreCompleto = u.getNombre() + " " + u.getApellidoPat() + " " + u.getApellidoMat();
                throw new UsuarioReactivacionException(u.getIdUsuario(), nombreCompleto, u.getCodigo(), u.getFechaCreacion());
            }
        });
    }

    private Usuario findUsuario(Integer id) {
        return usuarioRepository.findByIdUsuarioAndAccesoNot(id, accesoEliminado())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con id " + id));
    }

    private void applyRequest(Usuario usuario, UsuarioRequest request) {
        usuario.setNombre(request.getNombre());
        usuario.setApellidoPat(request.getApellidoPat());
        usuario.setApellidoMat(request.getApellidoMat());
        if (request.getDocumentoIdentidad() != null && !request.getDocumentoIdentidad().isBlank()) {
            usuario.setDocumentoIdentidad(request.getDocumentoIdentidad());
        }
        if (request.getContraseña() != null && !request.getContraseña().isBlank()) {
            usuario.setContraseña(passwordEncoder.encode(request.getContraseña()));
        }
        if (request.getAccesoId() != null) {
            usuario.setAcceso(accesoRepository.findById(request.getAccesoId()).orElseThrow());
        }
        usuario.setGmail(request.getGmail());
        usuario.setCelular(request.getCelular());
        usuario.setFechaNaci(request.getFechaNaci());
        if (request.getUrlFoto() != null) {
            usuario.setUrlFoto(request.getUrlFoto());
        }
        if (request.getPkUrlFoto() != null) {
            usuario.setPkUrlFoto(request.getPkUrlFoto());
        }
    }

    private Acceso accesoEliminado() {
        return accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow();
    }

    private UsuarioResponse toResponse(Usuario usuario) {
        UsuarioResponse response = new UsuarioResponse();
        response.setIdUsuario(usuario.getIdUsuario());
        response.setNombre(usuario.getNombre());
        response.setApellidoPat(usuario.getApellidoPat());
        response.setApellidoMat(usuario.getApellidoMat());
        response.setCodigo(usuario.getCodigo());
        response.setDocumentoIdentidad(usuario.getDocumentoIdentidad());
        response.setAccesoId(usuario.getAcceso().getIdAcceso().longValue());
        response.setGmail(usuario.getGmail());
        response.setCelular(usuario.getCelular());
        response.setFechaNaci(usuario.getFechaNaci());
        response.setUrlFoto(usuario.getUrlFoto());
        response.setPkUrlFoto(usuario.getPkUrlFoto());
        List<RolResponse> roles = getRolesDelUsuario(usuario.getIdUsuario());
        response.setRoles(roles);
        response.setNombreRol(roles.isEmpty() ? null : roles.get(0).getNombre());
        response.setIntentosFallidos(usuario.getIntentosFallidos());
        response.setFechaBloqueo(usuario.getFechaBloqueo());
        return response;
    }

    private RolResponse toRolResponse(Rol rol) {
        RolResponse response = new RolResponse();
        response.setIdRol(rol.getIdRol());
        response.setNombre(rol.getNombre());
        response.setAccesoId(rol.getAcceso().getIdAcceso().longValue());
        return response;
    }
}