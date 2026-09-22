package com.example.Escolar.Service;

import com.example.Escolar.Dto.CambioDocenteResponse;
import com.example.Escolar.Exception.ResourceNotFoundException;
import com.example.Escolar.Model.CambioDocente;
import com.example.Escolar.Model.Docente;
import com.example.Escolar.Repository.AccesoRepository;
import com.example.Escolar.Repository.CambioDocenteRepository;
import com.example.Escolar.Repository.DocenteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CambioDocenteService {

    private final CambioDocenteRepository cambioDocenteRepository;
    private final DocenteRepository docenteRepository;
    private final AccesoRepository accesoRepository;

    public List<CambioDocenteResponse> getAll() {
        return cambioDocenteRepository.findByAccesoNot(accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public CambioDocenteResponse getById(Integer id) {
        return toResponse(findCambio(id));
    }

    public List<CambioDocenteResponse> getByDocente(Integer idDocente) {
        Docente docente = findDocente(idDocente);
        return cambioDocenteRepository.findByDocenteAnteriorAndAccesoNot(docente, accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private CambioDocente findCambio(Integer id) {
        return cambioDocenteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cambio de docente no encontrado con id " + id));
    }

    private Docente findDocente(Integer id) {
        return docenteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Docente no encontrado con id " + id));
    }

    private CambioDocenteResponse toResponse(CambioDocente cambio) {
        CambioDocenteResponse response = new CambioDocenteResponse();
        response.setIdCambio(cambio.getIdCambio());
        response.setIdAsignacion(cambio.getAsignacion().getIdAsignacion());
        response.setIdDocenteAnterior(cambio.getDocenteAnterior().getIdDocente());
        response.setDocenteAnterior(cambio.getDocenteAnterior().getUsuario().getNombre() + " " + cambio.getDocenteAnterior().getUsuario().getApellidoPat());
        response.setIdDocenteNuevo(cambio.getDocenteNuevo().getIdDocente());
        response.setDocenteNuevo(cambio.getDocenteNuevo().getUsuario().getNombre() + " " + cambio.getDocenteNuevo().getUsuario().getApellidoPat());
        response.setMotivo(cambio.getMotivo());
        response.setMotivoDetalle(cambio.getMotivoDetalle());
        response.setFechaCambio(cambio.getFechaCambio());
        if (cambio.getUsuarioRegistro() != null) {
            response.setIdUsuarioRegistro(cambio.getUsuarioRegistro().getIdUsuario());
            response.setUsuarioRegistro(cambio.getUsuarioRegistro().getNombre() + " " + cambio.getUsuarioRegistro().getApellidoPat());
        }
        response.setAccesoId(cambio.getAcceso().getIdAcceso().longValue());
        return response;
    }
}
