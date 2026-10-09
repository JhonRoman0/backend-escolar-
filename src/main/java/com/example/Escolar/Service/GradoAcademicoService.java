package com.example.Escolar.Service;

import com.example.Escolar.Dto.GradoAcademicoResponse;
import com.example.Escolar.Repository.AccesoRepository;
import com.example.Escolar.Repository.GradoAcademicoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GradoAcademicoService {

    private final GradoAcademicoRepository gradoAcademicoRepository;
    private final AccesoRepository accesoRepository;

    public List<GradoAcademicoResponse> getAll() {
        return gradoAcademicoRepository.findByAccesoNot(accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow()).stream()
                .map(grado -> {
                    GradoAcademicoResponse response = new GradoAcademicoResponse();
                    response.setIdGradoAcademico(grado.getIdGradoAcademico());
                    response.setNombre(grado.getNombre());
                    return response;
                })
                .toList();
    }
}
