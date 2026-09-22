package com.example.Escolar.Service;

import com.example.Escolar.Dto.CursoRequest;
import com.example.Escolar.Dto.CursoResponse;
import com.example.Escolar.Exception.ResourceNotFoundException;
import com.example.Escolar.Model.Curso;
import com.example.Escolar.Repository.AccesoRepository;
import com.example.Escolar.Repository.CursoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CursoService {

    private final CursoRepository cursoRepository;
    private final AccesoRepository accesoRepository;

    public List<CursoResponse> getAll() {
        return cursoRepository.findByAccesoNot(accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow()).stream()
                .map(this::toResponse)
                .toList();
    }

    public CursoResponse getById(Integer id) {
        return toResponse(findCurso(id));
    }

    @Transactional
    public CursoResponse create(CursoRequest request) {
        validarNombreUnico(request.getNombre(), null);
        Curso curso = new Curso();
        curso.setNombre(request.getNombre().trim());
        curso.setAcceso(accesoRepository.findById(request.getAccesoId() == null ? AccesoConstants.ACTIVO : request.getAccesoId()).orElseThrow());
        return toResponse(cursoRepository.save(curso));
    }

    @Transactional
    public CursoResponse update(Integer id, CursoRequest request) {
        Curso curso = findCurso(id);
        if (request.getNombre() != null && !request.getNombre().isBlank()) {
            validarNombreUnico(request.getNombre(), id);
            curso.setNombre(request.getNombre().trim());
        }
        if (request.getAccesoId() != null) {
            curso.setAcceso(accesoRepository.findById(request.getAccesoId()).orElseThrow());
        }
        return toResponse(cursoRepository.save(curso));
    }

    @Transactional
    public void delete(Integer id) {
        Curso curso = findCurso(id);
        curso.setAcceso(accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow());
        cursoRepository.save(curso);
    }

    private void validarNombreUnico(String nombre, Integer idExcluir) {
        if (nombre == null || nombre.isBlank()) {
            return;
        }
        cursoRepository.findByNombreAndAccesoNot(nombre.trim(), accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow())
                .filter(c -> idExcluir == null || !c.getIdCurso().equals(idExcluir))
                .ifPresent(c -> {
                    throw new IllegalArgumentException("Ya existe un curso con ese nombre");
                });
    }

    private Curso findCurso(Integer id) {
        return cursoRepository.findByIdCursoAndAccesoNot(id, accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow())
                .orElseThrow(() -> new ResourceNotFoundException("Curso no encontrado con id " + id));
    }

    private CursoResponse toResponse(Curso curso) {
        CursoResponse response = new CursoResponse();
        response.setIdCurso(curso.getIdCurso());
        response.setNombre(curso.getNombre());
        response.setAccesoId(curso.getAcceso().getIdAcceso().longValue());
        return response;
    }
}
