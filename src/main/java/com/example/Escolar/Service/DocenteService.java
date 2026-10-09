package com.example.Escolar.Service;

import com.example.Escolar.Dto.DocenteReporteResponse;
import com.example.Escolar.Dto.DocenteRequest;
import com.example.Escolar.Dto.DocenteResponse;
import com.example.Escolar.Dto.NivelResponse;
import com.example.Escolar.Dto.RolResponse;
import com.example.Escolar.Exception.DocenteConAsignacionesException;
import com.example.Escolar.Exception.ResourceNotFoundException;
import com.example.Escolar.Model.Acceso;
import com.example.Escolar.Model.Docente;
import com.example.Escolar.Model.DocenteNivel;
import com.example.Escolar.Model.Nivel;
import com.example.Escolar.Model.Rol;
import com.example.Escolar.Model.Usuario;
import com.example.Escolar.Model.UsuarioRol;
import com.example.Escolar.Repository.AccesoRepository;
import com.example.Escolar.Repository.AsignacionRepository;
import com.example.Escolar.Repository.DocenteNivelRepository;
import com.example.Escolar.Repository.DocenteRepository;
import com.example.Escolar.Repository.GradoAcademicoRepository;
import com.example.Escolar.Repository.NivelRepository;
import com.example.Escolar.Repository.RolRepository;
import com.example.Escolar.Repository.TipoContratoRepository;
import com.example.Escolar.Repository.UsuarioRepository;
import com.example.Escolar.Repository.UsuarioRolRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;

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
    private final GradoAcademicoRepository gradoAcademicoRepository;
    private final TipoContratoRepository tipoContratoRepository;
    private final NivelRepository nivelRepository;
    private final DocenteNivelRepository docenteNivelRepository;

    public List<DocenteResponse> getAll() {
        return docenteRepository.findByAccesoNot(accesoNoEliminado()).stream()
                .map(this::toResponse)
                .toList();
    }

    public DocenteResponse getById(Integer id) {
        return toResponse(findDocente(id));
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
            if (request.getAccesoId() == AccesoConstants.ACTIVO.longValue()) {
                usuario.setAcceso(accesoRepository.findById(AccesoConstants.ACTIVO).orElseThrow());
                usuarioRepository.save(usuario);
            }
        }
        aplicarDatosAcademicos(docente, request);
        sincronizarNiveles(docente, request.getNiveles());
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

        docente.setAcceso(accesoNoEliminado());
        docenteRepository.save(docente);
        Usuario usuario = docente.getUsuario();
        removerRolDocente(usuario.getIdUsuario());
        boolean tieneOtrosRoles = usuarioRolRepository.findByUsuarioIdUsuario(usuario.getIdUsuario()).stream()
                .anyMatch(ur -> !ur.getRol().getAcceso().getIdAcceso().equals(AccesoConstants.ELIMINADO));
        if (!tieneOtrosRoles) {
            usuario.setAcceso(accesoNoEliminado());
            usuarioRepository.save(usuario);
        }
    }

    @Transactional(readOnly = true)
    public List<DocenteReporteResponse> reporte(LocalDate inicio, LocalDate fin, Integer tipoContratoId) {
        return docenteRepository.findByFechaContratacionBetweenAndAccesoNot(inicio, fin, accesoNoEliminado()).stream()
                .filter(d -> {
                    if (tipoContratoId == null) {
                        return true;
                    }
                    return d.getTipoContrato() != null
                            && tipoContratoId.equals(d.getTipoContrato().getIdTipoContrato());
                })
                .map(d -> {
                    DocenteReporteResponse r = new DocenteReporteResponse();
                    r.setIdDocente(d.getIdDocente());
                    r.setNombre(d.getUsuario().getNombre() + " " + d.getUsuario().getApellidoPat()
                            + " " + d.getUsuario().getApellidoMat());
                    r.setCodigo(d.getUsuario().getCodigo());
                    r.setGradoAcademico(d.getGradoAcademico() != null ? d.getGradoAcademico().getNombre() : null);
                    r.setTipoContrato(d.getTipoContrato() != null ? d.getTipoContrato().getNombre() : null);
                    r.setNiveles(nivelesDe(d).stream().map(NivelResponse::getNombre).toList());
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

    private void aplicarDatosAcademicos(Docente docente, DocenteRequest request) {
        if (request.getTipoContratoId() != null) {
            docente.setTipoContrato(tipoContratoRepository
                    .findByIdTipoContratoAndAccesoNot(request.getTipoContratoId(), accesoNoEliminado())
                    .orElseThrow(() -> new ResourceNotFoundException("Tipo de contrato no encontrado con id " + request.getTipoContratoId())));
        } else {
            docente.setTipoContrato(null);
        }
        docente.setFechaContratacion(request.getFechaContratacion());
        if (request.getGradoAcademicoId() != null) {
            docente.setGradoAcademico(gradoAcademicoRepository
                    .findByIdGradoAcademicoAndAccesoNot(request.getGradoAcademicoId(), accesoNoEliminado())
                    .orElseThrow(() -> new ResourceNotFoundException("Grado academico no encontrado con id " + request.getGradoAcademicoId())));
        } else {
            docente.setGradoAcademico(null);
        }
    }

    private void sincronizarNiveles(Docente docente, List<Integer> niveles) {
        docenteNivelRepository.deleteByDocente(docente);
        if (niveles == null || niveles.isEmpty()) {
            return;
        }
        for (Integer idNivel : new LinkedHashSet<>(niveles)) {
            Nivel nivel = nivelRepository.findByIdNivelAndAccesoNot(idNivel, accesoNoEliminado())
                    .orElseThrow(() -> new ResourceNotFoundException("Nivel no encontrado con id " + idNivel));
            DocenteNivel vinculo = new DocenteNivel();
            vinculo.setDocente(docente);
            vinculo.setNivel(nivel);
            vinculo.setAcceso(accesoRepository.findById(AccesoConstants.ACTIVO).orElseThrow());
            docenteNivelRepository.save(vinculo);
        }
    }

    private List<NivelResponse> nivelesDe(Docente docente) {
        return docenteNivelRepository.findByDocenteAndAccesoNot(docente, accesoNoEliminado()).stream()
                .map(DocenteNivel::getNivel)
                .map(nivel -> {
                    NivelResponse response = new NivelResponse();
                    response.setIdNivel(nivel.getIdNivel());
                    response.setNombre(nivel.getNombre());
                    return response;
                })
                .toList();
    }

    private void validarDocumentoUnico(String documentoIdentidad, Integer idExcluir) {
        if (documentoIdentidad == null || documentoIdentidad.isBlank()) {
            return;
        }
        usuarioRepository.findByDocumentoIdentidadAndAccesoNot(documentoIdentidad, accesoNoEliminado())
                .filter(u -> idExcluir == null || !u.getIdUsuario().equals(idExcluir))
                .ifPresent(u -> {
                    throw new IllegalArgumentException("Ya existe un usuario con ese documento de identidad");
                });
    }

    private void validarGmailUnico(String gmail, Integer idExcluir) {
        if (gmail == null || gmail.isBlank()) {
            return;
        }
        usuarioRepository.findByGmailAndAccesoNot(gmail, accesoNoEliminado())
                .filter(u -> idExcluir == null || !u.getIdUsuario().equals(idExcluir))
                .ifPresent(u -> {
                    throw new IllegalArgumentException("Ya existe un usuario con ese correo electronico");
                });
    }

    private Docente findDocente(Integer id) {
        return docenteRepository.findByIdDocenteAndAccesoNot(id, accesoNoEliminado())
                .orElseThrow(() -> new ResourceNotFoundException("Docente no encontrado con id " + id));
    }

    private Acceso accesoNoEliminado() {
        return accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow();
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
        response.setTipoContratoId(docente.getTipoContrato() != null ? docente.getTipoContrato().getIdTipoContrato() : null);
        response.setTipoContratoNombre(docente.getTipoContrato() != null ? docente.getTipoContrato().getNombre() : null);
        response.setFechaContratacion(docente.getFechaContratacion());
        response.setGradoAcademicoId(docente.getGradoAcademico() != null ? docente.getGradoAcademico().getIdGradoAcademico() : null);
        response.setGradoAcademicoNombre(docente.getGradoAcademico() != null ? docente.getGradoAcademico().getNombre() : null);
        response.setNiveles(nivelesDe(docente));
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
