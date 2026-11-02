package com.example.Escolar.Service;

import com.example.Escolar.Dto.ApoderadoEdicionRequest;
import com.example.Escolar.Dto.ApoderadoResponse;
import com.example.Escolar.Exception.ResourceNotFoundException;
import com.example.Escolar.Model.Acceso;
import com.example.Escolar.Model.AlumnoApoderado;
import com.example.Escolar.Model.Apoderado;
import com.example.Escolar.Model.Matricula;
import com.example.Escolar.Model.Usuario;
import com.example.Escolar.Repository.AccesoRepository;
import com.example.Escolar.Repository.AlumnoApoderadoRepository;
import com.example.Escolar.Repository.ApoderadoRepository;
import com.example.Escolar.Repository.UsuarioRepository;
import com.example.Escolar.Service.AccesoContextoService.Alcance;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ApoderadoService {

    private static final String PERMISO = "APODERADOS";
    private final ApoderadoRepository apoderadoRepository;
    private final UsuarioRepository usuarioRepository;
    private final AccesoContextoService accesoContextoService;
    private final AccesoRepository accesoRepository;
    private final AlumnoApoderadoRepository alumnoApoderadoRepository;
    private final PagoConfigService pagoConfigService;

    @Transactional(readOnly = true)
    public List<ApoderadoResponse> getAll(Integer idUsuario, List<String> roles) {
        Set<Integer> visibles = idsApoderadosVisibles(idUsuario, roles);
        return apoderadoRepository.findByAccesoNot(accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow()).stream()
                .filter(a -> visibles == null || visibles.contains(a.getIdApoderado()))
                .map(ApoderadoResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public ApoderadoResponse getById(Integer id, Integer idUsuario, List<String> roles) {
        Apoderado apoderado = apoderadoRepository.findByIdApoderadoAndAccesoNot(id, accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow())
                .orElseThrow(() -> new ResourceNotFoundException("Apoderado no encontrado con id " + id));
        if (!esVisible(apoderado.getIdApoderado(), idUsuario, roles)) {
            throw new ResourceNotFoundException("Apoderado no encontrado con id " + id);
        }
        return ApoderadoResponse.fromEntity(apoderado);
    }

    @Transactional(readOnly = true)
    public ApoderadoResponse getByDocumento(String documento, Integer idUsuario, List<String> roles) {
        if (documento == null || documento.isBlank()) {
            throw new IllegalArgumentException("El documento de identidad es obligatorio");
        }
        ApoderadoResponse response = apoderadoRepository
                .findByUsuarioDocumentoIdentidadAndAccesoNot(documento, accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow())
                .map(ApoderadoResponse::fromEntity)
                .orElseGet(() -> ApoderadoResponse.fromUsuario(
                        usuarioRepository.findByDocumentoIdentidadAndAccesoNot(documento, accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow())
                                .orElseThrow(() -> new ResourceNotFoundException("No existe un usuario con el documento " + documento))));
        if (!esVisible(response.getIdApoderado(), idUsuario, roles)) {
            throw new ResourceNotFoundException("No existe un usuario con el documento " + documento);
        }
        return response;
    }

    /** Edición de la misma persona desde Estudiantes; otro DNI es un reemplazo, no una edición. */
    @Transactional
    public ApoderadoResponse actualizar(Integer id, ApoderadoEdicionRequest request) {
        Acceso eliminado = accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow();
        Apoderado apoderado = apoderadoRepository.findByIdApoderadoAndAccesoNot(id, eliminado)
                .orElseThrow(() -> new ResourceNotFoundException("Apoderado no encontrado con id " + id));
        Usuario usuario = apoderado.getUsuario();
        String dni = request.getDocumentoIdentidad().trim();
        String gmail = request.getGmail().trim();
        if (usuario.getDocumentoIdentidad() != null && !usuario.getDocumentoIdentidad().equals(dni)) {
            throw new IllegalArgumentException("Para poner a otra persona hay que reemplazar al apoderado");
        }
        usuarioRepository.findByGmailAndAccesoNot(gmail, eliminado)
                .filter(u -> !u.getIdUsuario().equals(usuario.getIdUsuario()))
                .ifPresent(u -> {
                    throw new IllegalArgumentException("El correo " + gmail + " ya lo usa otro usuario");
                });
        AlumnoApoderado vinculo = alumnoApoderadoRepository.findByApoderado(apoderado).stream()
                .filter(v -> v.getAlumno().getIdAlumno().equals(request.getIdAlumno()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("El apoderado no está vinculado a ese alumno"));

        usuario.setDocumentoIdentidad(dni);
        usuario.setNombre(request.getNombre().trim());
        usuario.setApellidoPat(request.getApellidoPat().trim());
        usuario.setApellidoMat(request.getApellidoMat().trim());
        usuario.setFechaNaci(request.getFechaNaci());
        usuario.setGmail(gmail);
        usuario.setCelular(request.getCelular());
        usuarioRepository.save(usuario);
        apoderado.setSexo(request.getSexo());
        apoderado.setDireccion(request.getDireccion().trim());
        apoderado.setCelular(request.getCelular());
        apoderadoRepository.save(apoderado);
        vinculo.setParentesco(pagoConfigService.parentesco(request.getIdParentesco()));
        alumnoApoderadoRepository.save(vinculo);

        return ApoderadoResponse.fromVinculo(vinculo);
    }

    private boolean esVisible(Integer idApoderado, Integer idUsuario, List<String> roles) {
        if (idApoderado == null) {
            return accesoContextoService.alcance(PERMISO, idUsuario, roles) == Alcance.TODO;
        }
        Set<Integer> visibles = idsApoderadosVisibles(idUsuario, roles);
        return visibles == null || visibles.contains(idApoderado);
    }

    private Set<Integer> idsApoderadosVisibles(Integer idUsuario, List<String> roles) {
        Alcance alcance = accesoContextoService.alcance(PERMISO, idUsuario, roles);
        if (alcance == Alcance.TODO) {
            return null;
        }
        if (alcance == Alcance.DOCENTE) {
            Set<Integer> ids = new HashSet<>();
            for (Matricula matricula : accesoContextoService.matriculasDeGradoSecciones(
                    accesoContextoService.gradoSeccionIdsDeDocente(idUsuario))) {
                alumnoApoderadoRepository.findByAlumno(matricula.getAlumnoApoderado().getAlumno())
                        .forEach(v -> ids.add(v.getApoderado().getIdApoderado()));
            }
            return ids;
        }
        if (alcance == Alcance.APODERADO) {
            Set<Integer> ids = new HashSet<>();
            Apoderado apoderado = accesoContextoService.apoderadoDeUsuario(idUsuario).orElse(null);
            if (apoderado != null) {
                ids.add(apoderado.getIdApoderado());
            }
            return ids;
        }
        return new HashSet<>();
    }
}
