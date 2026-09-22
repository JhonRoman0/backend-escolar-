package com.example.Escolar.Service;

import com.example.Escolar.Dto.ApoderadoResponse;
import com.example.Escolar.Exception.ResourceNotFoundException;
import com.example.Escolar.Model.Apoderado;
import com.example.Escolar.Model.Matricula;
import com.example.Escolar.Repository.AccesoRepository;
import com.example.Escolar.Repository.ApoderadoRepository;
import com.example.Escolar.Repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ApoderadoService {

    private final ApoderadoRepository apoderadoRepository;
    private final UsuarioRepository usuarioRepository;
    private final AccesoContextoService accesoContextoService;
    private final AccesoRepository accesoRepository;

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

    private boolean esVisible(Integer idApoderado, Integer idUsuario, List<String> roles) {
        if (idApoderado == null) {
            return accesoContextoService.esGestion(roles);
        }
        Set<Integer> visibles = idsApoderadosVisibles(idUsuario, roles);
        return visibles == null || visibles.contains(idApoderado);
    }

    private Set<Integer> idsApoderadosVisibles(Integer idUsuario, List<String> roles) {
        if (accesoContextoService.esGestion(roles)) {
            return null;
        }
        if (accesoContextoService.esPersonalDocente(roles)) {
            Set<Integer> ids = new HashSet<>();
            for (Matricula matricula : accesoContextoService.matriculasDeGradoSecciones(
                    accesoContextoService.gradoSeccionIdsDeDocente(idUsuario))) {
                ids.add(matricula.getAlumnoApoderado().getApoderado().getIdApoderado());
            }
            return ids;
        }
        if (accesoContextoService.esApoderado(roles)) {
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
