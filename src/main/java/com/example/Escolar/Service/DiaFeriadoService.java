package com.example.Escolar.Service;

import com.example.Escolar.Dto.DiaFeriadoRequest;
import com.example.Escolar.Dto.DiaFeriadoResponse;
import com.example.Escolar.Exception.ResourceNotFoundException;
import com.example.Escolar.Model.AnioEscolar;
import com.example.Escolar.Model.DiaFeriado;
import com.example.Escolar.Repository.AnioEscolarRepository;
import com.example.Escolar.Repository.AccesoRepository;
import com.example.Escolar.Repository.DiaFeriadoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DiaFeriadoService {

    private final DiaFeriadoRepository diaFeriadoRepository;
    private final AnioEscolarRepository anioEscolarRepository;
    private final AccesoRepository accesoRepository;

    public List<DiaFeriadoResponse> getAll() {
        return diaFeriadoRepository.findByAccesoNot(accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow()).stream()
                .map(this::toResponse)
                .toList();
    }

    public DiaFeriadoResponse getById(Integer id) {
        return toResponse(findDiaFeriado(id));
    }

    @Transactional
    public DiaFeriadoResponse create(DiaFeriadoRequest request) {
        DiaFeriado diaFeriado = new DiaFeriado();
        diaFeriado.setFecha(request.getFecha());
        diaFeriado.setMotivo(request.getMotivo().trim());
        diaFeriado.setAnioEscolar(resolverAnio(request.getIdAnioEscolar()));
        diaFeriado.setAcceso(accesoRepository.findById(request.getAccesoId() == null ? AccesoConstants.ACTIVO : request.getAccesoId()).orElseThrow());
        return toResponse(diaFeriadoRepository.save(diaFeriado));
    }

    @Transactional
    public DiaFeriadoResponse update(Integer id, DiaFeriadoRequest request) {
        DiaFeriado diaFeriado = findDiaFeriado(id);
        diaFeriado.setFecha(request.getFecha());
        diaFeriado.setMotivo(request.getMotivo().trim());
        diaFeriado.setAnioEscolar(resolverAnio(request.getIdAnioEscolar()));
        if (request.getAccesoId() != null) {
            diaFeriado.setAcceso(accesoRepository.findById(request.getAccesoId()).orElseThrow());
        }
        return toResponse(diaFeriadoRepository.save(diaFeriado));
    }

    @Transactional
    public void delete(Integer id) {
        DiaFeriado diaFeriado = findDiaFeriado(id);
        diaFeriado.setAcceso(accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow());
        diaFeriadoRepository.save(diaFeriado);
    }

    private AnioEscolar resolverAnio(Integer idAnioEscolar) {
        if (idAnioEscolar == null) {
            return null;
        }
        return anioEscolarRepository.findByIdAnioAndAccesoNot(idAnioEscolar, accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow())
                .orElseThrow(() -> new ResourceNotFoundException("Año escolar no encontrado con id " + idAnioEscolar));
    }

    private DiaFeriado findDiaFeriado(Integer id) {
        return diaFeriadoRepository.findByIdDiaFeriadoAndAccesoNot(id, accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow())
                .orElseThrow(() -> new ResourceNotFoundException("Feriado no encontrado con id " + id));
    }

    private DiaFeriadoResponse toResponse(DiaFeriado diaFeriado) {
        DiaFeriadoResponse response = new DiaFeriadoResponse();
        response.setIdDiaFeriado(diaFeriado.getIdDiaFeriado());
        response.setFecha(diaFeriado.getFecha());
        response.setMotivo(diaFeriado.getMotivo());
        response.setIdAnioEscolar(diaFeriado.getAnioEscolar() != null ? diaFeriado.getAnioEscolar().getIdAnio() : null);
        response.setAnio(diaFeriado.getAnioEscolar() != null ? diaFeriado.getAnioEscolar().getAnio() : null);
        response.setAccesoId(diaFeriado.getAcceso().getIdAcceso().longValue());
        return response;
    }
}