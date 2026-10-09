package com.example.Escolar.Service;

import com.example.Escolar.Dto.TipoContratoResponse;
import com.example.Escolar.Repository.AccesoRepository;
import com.example.Escolar.Repository.TipoContratoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TipoContratoService {

    private final TipoContratoRepository tipoContratoRepository;
    private final AccesoRepository accesoRepository;

    public List<TipoContratoResponse> getAll() {
        return tipoContratoRepository.findByAccesoNot(accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow()).stream()
                .map(tipo -> {
                    TipoContratoResponse response = new TipoContratoResponse();
                    response.setIdTipoContrato(tipo.getIdTipoContrato());
                    response.setNombre(tipo.getNombre());
                    return response;
                })
                .toList();
    }
}
