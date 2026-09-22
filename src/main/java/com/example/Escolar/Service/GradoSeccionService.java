package com.example.Escolar.Service;

import com.example.Escolar.Dto.GradoSeccionResponse;
import com.example.Escolar.Exception.ResourceNotFoundException;
import com.example.Escolar.Model.Grado;
import com.example.Escolar.Model.GradoSeccion;
import com.example.Escolar.Repository.AccesoRepository;
import com.example.Escolar.Repository.GradoRepository;
import com.example.Escolar.Repository.GradoSeccionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GradoSeccionService {

    private final GradoSeccionRepository gradoSeccionRepository;
    private final GradoRepository gradoRepository;
    private final AccesoRepository accesoRepository;

    public List<GradoSeccionResponse> getSeccionesByGrado(Integer idGrado) {
        Grado grado = gradoRepository.findByIdGradoAndAccesoNot(idGrado, accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow())
                .orElseThrow(() -> new ResourceNotFoundException("Grado no encontrado con id " + idGrado));
        return gradoSeccionRepository.findByGradoAndAccesoNot(grado, accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow()).stream()
                .filter(gs -> gs.getSeccion() != null)
                .map(this::toResponse)
                .toList();
    }

    private GradoSeccionResponse toResponse(GradoSeccion gs) {
        GradoSeccionResponse response = new GradoSeccionResponse();
        response.setIdGradoSeccion(gs.getIdGradoSeccion());
        if (gs.getSeccion() != null) {
            response.setIdSeccion(gs.getSeccion().getIdSeccion());
            response.setNombre(gs.getSeccion().getNombre());
        }
        response.setIdTurno(gs.getTurno().getIdTurno());
        response.setTurno(gs.getTurno().getNombre());
        return response;
    }
}
