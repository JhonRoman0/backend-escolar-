package com.example.Escolar.Service;

import com.example.Escolar.Dto.RolResponse;
import com.example.Escolar.Dto.UploadResponse;
import com.example.Escolar.Dto.UsuarioReporteResponse;
import com.example.Escolar.Dto.UsuarioRequest;
import com.example.Escolar.Dto.UsuarioResponse;
import com.example.Escolar.Exception.ResourceNotFoundException;
import com.example.Escolar.Model.Rol;
import com.example.Escolar.Model.Usuario;
import com.example.Escolar.Model.UsuarioRol;
import com.example.Escolar.Repository.RolRepository;
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
import java.util.List;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    public static final byte ACCESO_ACTIVO = 1;
    public static final byte ACCESO_ELIMINADO = 2;

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final UsuarioRolRepository usuarioRolRepository;
    private final PasswordEncoder passwordEncoder;
    private final CloudinaryService cloudinaryService;

    public Page<UsuarioResponse> getAll(Pageable pageable) {
        return usuarioRepository.findByAccesoNot(ACCESO_ELIMINADO, pageable).map(this::toResponse);
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
        Usuario usuario = new Usuario();
        if (request.getAcceso() == null) {
            request.setAcceso(ACCESO_ACTIVO);
        }
        applyRequest(usuario, request);
        usuario.setCodigo(generarCodigo(request.getRolIds()));
        usuario.setFechaCreacion(LocalDate.now());
        Usuario saved = usuarioRepository.save(usuario);
        assignRoles(saved, request.getRolIds());
        return toResponse(saved);
    }

    @Transactional
    public UsuarioResponse update(Integer id, UsuarioRequest request) {
        Usuario usuario = findUsuario(id);
        validarDocumentoUnico(request.getDocumentoIdentidad(), id);
        applyRequest(usuario, request);
        if (request.getRolIds() != null) {
            usuarioRolRepository.deleteByUsuarioIdUsuario(id);
            assignRoles(usuario, request.getRolIds());
        }
        return toResponse(usuarioRepository.save(usuario));
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
        usuario.setAcceso(ACCESO_ELIMINADO);
        usuarioRepository.save(usuario);
    }

    @Transactional(readOnly = true)
    public List<UsuarioReporteResponse> reporte(LocalDate inicio, LocalDate fin, Integer idRol) {
        return usuarioRepository.findByFechaCreacionBetweenAndAccesoNot(inicio, fin, ACCESO_ELIMINADO).stream()
                .filter(u -> {
                    if (idRol == null) return true;
                    return usuarioRolRepository.findByUsuarioIdUsuario(u.getIdUsuario()).stream()
                            .anyMatch(ur -> ur.getRol().getIdRol().equals(idRol)
                                    && ur.getRol().getAcceso() != ACCESO_ELIMINADO);
                })
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
                            .filter(rol -> rol.getAcceso() != ACCESO_ELIMINADO)
                            .map(Rol::getNombre)
                            .toList());
                    return r;
                })
                .toList();
    }

    public List<UsuarioResponse> getUsuariosPorRol(Integer idRol) {
        rolRepository.findByIdRolAndAccesoNot(idRol, ACCESO_ELIMINADO)
                .orElseThrow(() -> new ResourceNotFoundException("Rol no encontrado con id " + idRol));
        return usuarioRolRepository.findByRolIdRol(idRol).stream()
                .map(UsuarioRol::getUsuario)
                .filter(u -> u.getAcceso() != ACCESO_ELIMINADO)
                .map(this::toResponse)
                .toList();
    }

    public List<RolResponse> getRolesDelUsuario(Integer idUsuario) {
        findUsuario(idUsuario);
        return usuarioRolRepository.findByUsuarioIdUsuario(idUsuario).stream()
                .map(ur -> toRolResponse(ur.getRol()))
                .filter(r -> r.getAcceso() != ACCESO_ELIMINADO)
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
                .filter(r -> r.getAcceso() != ACCESO_ELIMINADO)
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
            Rol rol = rolRepository.findByIdRolAndAccesoNot(rolId, ACCESO_ELIMINADO)
                    .orElseThrow(() -> new ResourceNotFoundException("Rol no encontrado con id " + rolId));
            UsuarioRol usuarioRol = new UsuarioRol();
            usuarioRol.setUsuario(usuario);
            usuarioRol.setRol(rol);
            usuarioRol.setFechaAsignacion(LocalDateTime.now());
            usuarioRolRepository.save(usuarioRol);
        }
    }

    private String generarCodigo(List<Integer> rolIds) {
        Rol rol = rolRepository.findByIdRolAndAccesoNot(rolIds.get(0), ACCESO_ELIMINADO)
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
        usuarioRepository.findByDocumentoIdentidadAndAccesoNot(documentoIdentidad, ACCESO_ELIMINADO)
                .filter(u -> idExcluir == null || !u.getIdUsuario().equals(idExcluir))
                .ifPresent(u -> {
                    throw new IllegalArgumentException("Ya existe un usuario con ese documento de identidad");
                });
    }

    private Usuario findUsuario(Integer id) {
        return usuarioRepository.findByIdUsuarioAndAccesoNot(id, ACCESO_ELIMINADO)
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
        if (request.getAcceso() != null) {
            usuario.setAcceso(request.getAcceso());
        }
        usuario.setGmail(request.getGmail());
        usuario.setFechaNaci(request.getFechaNaci());
        usuario.setUrlFoto(request.getUrlFoto());
        usuario.setPkUrlFoto(request.getPkUrlFoto());
    }

    private UsuarioResponse toResponse(Usuario usuario) {
        UsuarioResponse response = new UsuarioResponse();
        response.setIdUsuario(usuario.getIdUsuario());
        response.setNombre(usuario.getNombre());
        response.setApellidoPat(usuario.getApellidoPat());
        response.setApellidoMat(usuario.getApellidoMat());
        response.setCodigo(usuario.getCodigo());
        response.setDocumentoIdentidad(usuario.getDocumentoIdentidad());
        response.setAcceso(usuario.getAcceso());
        response.setGmail(usuario.getGmail());
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
        response.setAcceso(rol.getAcceso());
        return response;
    }
}
