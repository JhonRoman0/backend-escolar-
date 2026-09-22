package com.example.Escolar.Service;

import com.example.Escolar.Dto.DocenteReporteResponse;
import com.example.Escolar.Dto.DocenteRequest;
import com.example.Escolar.Dto.DocenteResponse;
import com.example.Escolar.Dto.RolResponse;
import com.example.Escolar.Exception.DocenteConAsignacionesException;
import com.example.Escolar.Exception.ResourceNotFoundException;
import com.example.Escolar.Model.Docente;
import com.example.Escolar.Model.Rol;
import com.example.Escolar.Model.Usuario;
import com.example.Escolar.Model.UsuarioRol;
import com.example.Escolar.Repository.AccesoRepository;
import com.example.Escolar.Repository.AsignacionRepository;
import com.example.Escolar.Repository.DocenteRepository;
import com.example.Escolar.Repository.RolRepository;
import com.example.Escolar.Repository.UsuarioRepository;
import com.example.Escolar.Repository.UsuarioRolRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DocenteService {

    public static final String ROL_DOCENTE = "DOCENTE";

    private final DocenteRepository docenteRepository;
    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final UsuarioRolRepository usuarioRolRepository;
    private final PasswordEncoder passwordEncoder;
    private final AccesoRepository accesoRepository;
    private final AsignacionRepository asignacionRepository;

    public List<DocenteResponse> getAll() {
        return docenteRepository.findByAccesoNot(accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow()).stream()
                .map(this::toResponse)
                .toList();
    }

    public DocenteResponse getById(Integer id) {
        return toResponse(findDocente(id));
    }

    @Transactional
    public DocenteResponse create(DocenteRequest request) {
        validarDocumentoUnico(request.getDocumentoIdentidad(), null);
        validarGmailUnico(request.getGmail(), null);
        Usuario usuario = crearUsuario(request);
        Docente docente = new Docente();
        docente.setUsuario(usuario);
        aplicarDatosAcademicos(docente, request);
        if (request.getAccesoId() == null) {
            docente.setAcceso(accesoRepository.findById(AccesoConstants.ACTIVO).orElseThrow());
        } else {
            docente.setAcceso(accesoRepository.findById(request.getAccesoId()).orElseThrow());
        }
        return toResponse(docenteRepository.save(docente));
    }

    @Transactional
    public DocenteResponse update(Integer id, DocenteRequest request) {
        Docente docente = findDocente(id);
        Usuario usuario = docente.getUsuario();
        validarDocumentoUnico(request.getDocumentoIdentidad(), usuario.getIdUsuario());
        validarGmailUnico(request.getGmail(), usuario.getIdUsuario());
        construirUsuario(usuario, request, false);
        usuarioRepository.save(usuario);
        if (request.getAccesoId() != null) {
            docente.setAcceso(accesoRepository.findById(request.getAccesoId()).orElseThrow());
            if (request.getAccesoId() != null && request.getAccesoId() == AccesoConstants.ACTIVO.longValue()) {
                usuario.setAcceso(accesoRepository.findById(AccesoConstants.ACTIVO).orElseThrow());
                usuarioRepository.save(usuario);
            }
        }
        aplicarDatosAcademicos(docente, request);
        return toResponse(docenteRepository.save(docente));
    }

    @Transactional
    public void delete(Integer id) {
        Docente docente = findDocente(id);

        // Regla 1: solo se puede eliminar si esta suspendido (INACTIVO)
        if (!docente.getAcceso().getIdAcceso().equals(AccesoConstants.INACTIVO)) {
            throw new IllegalArgumentException(
                "No se puede eliminar un docente que no este suspendido. "
                + "Primero suspenda al docente y luego podra eliminarlo."
            );
        }

        // Regla 2: verificar que no tenga asignaciones activas
        long asignacionesActivas = asignacionRepository.countByDocenteAndAcceso(docente, accesoRepository.findById(AccesoConstants.ACTIVO).orElseThrow());
        if (asignacionesActivas > 0) {
            throw new DocenteConAsignacionesException(
                "No se puede eliminar al docente porque tiene " + asignacionesActivas
                + " asignacion(es) activa(s). Primero debe eliminar o reasignar sus asignaciones."
            );
        }

        docente.setAcceso(accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow());
        docenteRepository.save(docente);
        Usuario usuario = docente.getUsuario();
        removerRolDocente(usuario.getIdUsuario());
        boolean tieneOtrosRoles = usuarioRolRepository.findByUsuarioIdUsuario(usuario.getIdUsuario()).stream()
                .anyMatch(ur -> !ur.getRol().getAcceso().getIdAcceso().equals(AccesoConstants.ELIMINADO));
        if (!tieneOtrosRoles) {
            usuario.setAcceso(accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow());
            usuarioRepository.save(usuario);
        }
    }

    @Transactional(readOnly = true)
    public List<DocenteReporteResponse> reporte(LocalDate inicio, LocalDate fin, String especialidad, String tipoContrato) {
        return docenteRepository.findByFechaContratacionBetweenAndAccesoNot(inicio, fin, accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow()).stream()
                .filter(d -> {
                    if (especialidad != null && !especialidad.isBlank()) {
                        if (!especialidad.equalsIgnoreCase(d.getEspecialidad())) return false;
                    }
                    if (tipoContrato != null && !tipoContrato.isBlank()) {
                        if (!tipoContrato.equalsIgnoreCase(d.getTipoContrato())) return false;
                    }
                    return true;
                })
                .map(d -> {
                    DocenteReporteResponse r = new DocenteReporteResponse();
                    r.setIdDocente(d.getIdDocente());
                    r.setNombre(d.getUsuario().getNombre() + " " + d.getUsuario().getApellidoPat()
                            + " " + d.getUsuario().getApellidoMat());
                    r.setCodigo(d.getUsuario().getCodigo());
                    r.setEspecialidad(d.getEspecialidad());
                    r.setGradoAcademico(d.getGradoAcademico());
                    r.setTipoContrato(d.getTipoContrato());
                    r.setFechaContratacion(d.getFechaContratacion());
                    r.setDocumentoIdentidad(d.getUsuario().getDocumentoIdentidad());
                    return r;
                })
                .toList();
    }

    private void removerRolDocente(Integer idUsuario) {
        Rol rolDocente = rolRepository.findByNombre(ROL_DOCENTE)
                .orElse(null);
        if (rolDocente == null) {
            return;
        }
        usuarioRolRepository.findByUsuarioIdUsuario(idUsuario).stream()
                .filter(ur -> ur.getRol().getIdRol().equals(rolDocente.getIdRol()))
                .findFirst()
                .ifPresent(usuarioRolRepository::delete);
    }

    private Usuario crearUsuario(DocenteRequest request) {
        if (request.getContraseña() == null || request.getContraseña().isBlank()) {
            throw new IllegalArgumentException("La contraseña es obligatoria");
        }
        Usuario usuario = new Usuario();
        construirUsuario(usuario, request, true);
        usuario.setCodigo(generarCodigo());
        usuario.setFechaCreacion(LocalDate.now());
        Usuario saved = usuarioRepository.save(usuario);
        asignarRolDocente(saved);
        return saved;
    }

    private void construirUsuario(Usuario usuario, DocenteRequest request, boolean esCreacion) {
        usuario.setNombre(request.getNombre());
        usuario.setApellidoPat(request.getApellidoPat());
        usuario.setApellidoMat(request.getApellidoMat());
        if (request.getDocumentoIdentidad() != null && !request.getDocumentoIdentidad().isBlank()) {
            usuario.setDocumentoIdentidad(request.getDocumentoIdentidad());
        }
        if (request.getContraseña() != null && !request.getContraseña().isBlank()) {
            usuario.setContraseña(passwordEncoder.encode(request.getContraseña()));
        }
        usuario.setGmail(request.getGmail());
        usuario.setFechaNaci(request.getFechaNaci());
        if (esCreacion) {
            usuario.setAcceso(accesoRepository.findById(AccesoConstants.ACTIVO).orElseThrow());
        } else if (usuario.getAcceso().getIdAcceso().equals(AccesoConstants.ELIMINADO)) {
            usuario.setAcceso(accesoRepository.findById(AccesoConstants.ACTIVO).orElseThrow());
        }
    }

    private void asignarRolDocente(Usuario usuario) {
        Rol rolDocente = rolRepository.findByNombre(ROL_DOCENTE)
                .orElseGet(() -> {
                    Rol rol = new Rol();
                    rol.setNombre(ROL_DOCENTE);
                    rol.setAcceso(accesoRepository.findById(AccesoConstants.ACTIVO).orElseThrow());
                    return rolRepository.save(rol);
                });
        boolean yaTiene = usuarioRolRepository.findByUsuarioIdUsuario(usuario.getIdUsuario()).stream()
                .anyMatch(ur -> ur.getRol().getIdRol().equals(rolDocente.getIdRol()));
        if (yaTiene) {
            return;
        }
        UsuarioRol usuarioRol = new UsuarioRol();
        usuarioRol.setUsuario(usuario);
        usuarioRol.setRol(rolDocente);
        usuarioRol.setFechaAsignacion(LocalDateTime.now());
        usuarioRolRepository.save(usuarioRol);
    }

    private void aplicarDatosAcademicos(Docente docente, DocenteRequest request) {
        docente.setTipoContrato(request.getTipoContrato());
        docente.setFechaContratacion(request.getFechaContratacion());
        docente.setEspecialidad(request.getEspecialidad());
        docente.setGradoAcademico(request.getGradoAcademico());
    }

    private void validarDocumentoUnico(String documentoIdentidad, Integer idExcluir) {
        if (documentoIdentidad == null || documentoIdentidad.isBlank()) {
            return;
        }
        usuarioRepository.findByDocumentoIdentidadAndAccesoNot(documentoIdentidad, accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow())
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

    private String generarCodigo() {
        String prefijo = "D" + LocalDate.now().getYear();
        long correlativo = usuarioRepository.countByCodigoStartingWith(prefijo) + 1;
        return String.format("%s%04d", prefijo, correlativo);
    }

    private Docente findDocente(Integer id) {
        return docenteRepository.findByIdDocenteAndAccesoNot(id, accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow())
                .orElseThrow(() -> new ResourceNotFoundException("Docente no encontrado con id " + id));
    }

    private DocenteResponse toResponse(Docente docente) {
        DocenteResponse response = new DocenteResponse();
        response.setIdDocente(docente.getIdDocente());
        Usuario usuario = docente.getUsuario();
        response.setIdUsuario(usuario.getIdUsuario());
        response.setCodigo(usuario.getCodigo());
        response.setNombre(usuario.getNombre());
        response.setApellidoPat(usuario.getApellidoPat());
        response.setApellidoMat(usuario.getApellidoMat());
        response.setDocumentoIdentidad(usuario.getDocumentoIdentidad());
        response.setGmail(usuario.getGmail());
        response.setFechaNaci(usuario.getFechaNaci());
        response.setUrlFoto(usuario.getUrlFoto());
        response.setAccesoId(docente.getAcceso().getIdAcceso().longValue());
        response.setTipoContrato(docente.getTipoContrato());
        response.setFechaContratacion(docente.getFechaContratacion());
        response.setEspecialidad(docente.getEspecialidad());
        response.setGradoAcademico(docente.getGradoAcademico());
        response.setRoles(getRolesDelUsuario(usuario.getIdUsuario()));
        return response;
    }

    private List<RolResponse> getRolesDelUsuario(Integer idUsuario) {
        return usuarioRolRepository.findByUsuarioIdUsuario(idUsuario).stream()
                .map(UsuarioRol::getRol)
                .filter(r -> !r.getAcceso().getIdAcceso().equals(AccesoConstants.ELIMINADO))
                .map(this::toRolResponse)
                .toList();
    }

    private RolResponse toRolResponse(Rol rol) {
        RolResponse response = new RolResponse();
        response.setIdRol(rol.getIdRol());
        response.setNombre(rol.getNombre());
        response.setAccesoId(rol.getAcceso().getIdAcceso().longValue());
        return response;
    }
}
