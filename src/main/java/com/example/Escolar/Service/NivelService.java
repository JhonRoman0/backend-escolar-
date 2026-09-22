package com.example.Escolar.Service;

import com.example.Escolar.Dto.NivelResponse;
import com.example.Escolar.Repository.AccesoRepository;
import com.example.Escolar.Repository.NivelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NivelService {

    private final NivelRepository nivelRepository;
    private final AccesoRepository accesoRepository;

    public List<NivelResponse> getAll() {
        return nivelRepository.findByAccesoNot(accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow()).stream()
                .map(nivel -> {
                    NivelResponse response = new NivelResponse();
                    response.setIdNivel(nivel.getIdNivel());
                    response.setNombre(nivel.getNombre());
                    return response;
                })
                .toList();
    }
}
