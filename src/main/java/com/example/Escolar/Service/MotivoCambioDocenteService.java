package com.example.Escolar.Service;

import com.example.Escolar.Dto.MotivoCambioDocenteRequest;
import com.example.Escolar.Dto.MotivoCambioDocenteResponse;
import com.example.Escolar.Exception.ResourceNotFoundException;
import com.example.Escolar.Model.MotivoCambioDocente;
import com.example.Escolar.Model.TipoCambioDocente;
import com.example.Escolar.Repository.AccesoRepository;
import com.example.Escolar.Repository.MotivoCambioDocenteRepository;
import com.example.Escolar.Util.FechaColegio;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MotivoCambioDocenteService {

    private final MotivoCambioDocenteRepository motivoRepository;
    private final AccesoRepository accesoRepository;

    public List<MotivoCambioDocenteResponse> getAll() {
        return motivoRepository.findByAccesoNot(eliminado()).stream().map(this::toResponse).toList();
    }

    public List<MotivoCambioDocenteResponse> getByTipo(TipoCambioDocente tipo) {
        return activos().stream()
                .filter(m -> tipo == TipoCambioDocente.TEMPORAL
                        ? Boolean.TRUE.equals(m.getAplicaTemporal())
                        : Boolean.TRUE.equals(m.getAplicaDefinitivo()))
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public MotivoCambioDocenteResponse create(MotivoCambioDocenteRequest request) {
        String nombre = request.nombre().trim();
        validarNombre(nombre, null);
        MotivoCambioDocente motivo = new MotivoCambioDocente();
        motivo.setNombre(nombre);
        motivo.setAplicaTemporal(request.aplicaTemporal());
        motivo.setAplicaDefinitivo(request.aplicaDefinitivo());
        motivo.setFechaRegistro(FechaColegio.hoy());
        motivo.setAcceso(activo());
        return toResponse(motivoRepository.save(motivo));
    }

    @Transactional
    public MotivoCambioDocenteResponse update(Integer id, MotivoCambioDocenteRequest request) {
        MotivoCambioDocente motivo = find(id);
        String nombre = request.nombre().trim();
        validarNombre(nombre, id);
        motivo.setNombre(nombre);
        motivo.setAplicaTemporal(request.aplicaTemporal());
        motivo.setAplicaDefinitivo(request.aplicaDefinitivo());
        return toResponse(motivoRepository.save(motivo));
    }

    @Transactional
    public void delete(Integer id) {
        MotivoCambioDocente motivo = find(id);
        motivo.setAcceso(eliminado());
        motivoRepository.save(motivo);
    }

    private List<MotivoCambioDocente> activos() {
        return motivoRepository.findByAccesoNot(eliminado());
    }

    private MotivoCambioDocente find(Integer id) {
        return motivoRepository.findByIdMotivoCambioDocenteAndAccesoNot(id, eliminado())
                .orElseThrow(() -> new ResourceNotFoundException("Motivo de cambio no encontrado con id " + id));
    }

    private void validarNombre(String nombre, Integer idExcluir) {
        if (nombre.isEmpty()) {
            throw new IllegalArgumentException("El nombre del motivo es obligatorio");
        }
        if (motivoRepository.existsByNombreIgnoreCaseAndAccesoNot(nombre, eliminado())
                && (idExcluir == null || !motivoRepository.findById(idExcluir)
                        .map(m -> m.getNombre().equalsIgnoreCase(nombre)).orElse(false))) {
            throw new IllegalArgumentException("Ya existe un motivo de cambio con ese nombre");
        }
    }

    private com.example.Escolar.Model.Acceso activo() {
        return accesoRepository.findById(AccesoConstants.ACTIVO).orElseThrow();
    }

    private com.example.Escolar.Model.Acceso eliminado() {
        return accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow();
    }

    private MotivoCambioDocenteResponse toResponse(MotivoCambioDocente motivo) {
        return new MotivoCambioDocenteResponse(
                motivo.getIdMotivoCambioDocente(),
                motivo.getNombre(),
                motivo.getAplicaTemporal(),
                motivo.getAplicaDefinitivo(),
                motivo.getAcceso() != null && AccesoConstants.ACTIVO.equals(motivo.getAcceso().getIdAcceso())
        );
    }
}
