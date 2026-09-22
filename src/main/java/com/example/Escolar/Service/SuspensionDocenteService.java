package com.example.Escolar.Service;

import com.example.Escolar.Dto.SuspensionDocenteRequest;
import com.example.Escolar.Dto.SuspensionDocenteResponse;
import com.example.Escolar.Exception.ResourceNotFoundException;
import com.example.Escolar.Model.Acceso;
import com.example.Escolar.Model.Asignacion;
import com.example.Escolar.Model.CambioDocente;
import com.example.Escolar.Model.Docente;
import com.example.Escolar.Model.SuspensionDocente;
import com.example.Escolar.Repository.AccesoRepository;
import com.example.Escolar.Repository.AsignacionRepository;
import com.example.Escolar.Repository.CambioDocenteRepository;
import com.example.Escolar.Repository.DocenteRepository;
import com.example.Escolar.Repository.SuspensionDocenteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SuspensionDocenteService {

    private final SuspensionDocenteRepository suspensionDocenteRepository;
    private final DocenteRepository docenteRepository;
    private final AsignacionRepository asignacionRepository;
    private final CambioDocenteRepository cambioDocenteRepository;
    private final AccesoRepository accesoRepository;

    public List<SuspensionDocenteResponse> getAll() {
        return suspensionDocenteRepository.findByAccesoNot(accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public SuspensionDocenteResponse getById(Integer id) {
        return toResponse(findSuspension(id));
    }

    public List<SuspensionDocenteResponse> getByDocente(Integer idDocente) {
        Docente docente = findDocente(idDocente);
        return suspensionDocenteRepository.findByDocenteAndAccesoNot(docente, accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public SuspensionDocenteResponse create(SuspensionDocenteRequest request) {
        Docente docente = findDocente(request.getIdDocente());

        // Validar que el docente este ACTIVO
        if (!docente.getAcceso().getIdAcceso().equals(AccesoConstants.ACTIVO)) {
            throw new IllegalArgumentException("Solo se pueden suspender docentes en estado ACTIVO");
        }

        // Validar que no tenga una suspension activa
        if (suspensionDocenteRepository.existsByDocenteAndAcceso(docente, accesoRepository.findById(AccesoConstants.ACTIVO).orElseThrow())) {
            throw new IllegalArgumentException("El docente ya tiene una suspension activa");
        }

        Docente sustituto = null;
        if (request.getIdSustituto() != null) {
            sustituto = findDocente(request.getIdSustituto());
            if (!sustituto.getAcceso().getIdAcceso().equals(AccesoConstants.ACTIVO)) {
                throw new IllegalArgumentException("El sustituto debe estar en estado ACTIVO");
            }
        }

        SuspensionDocente suspension = new SuspensionDocente();
        suspension.setDocente(docente);
        suspension.setSustituto(sustituto);
        suspension.setMotivo(request.getMotivo());
        suspension.setMotivoDetalle(request.getMotivoDetalle());
        suspension.setFechaInicio(request.getFechaInicio());
        suspension.setFechaFin(request.getFechaFin());
        suspension.setAcceso(accesoRepository.findById(AccesoConstants.ACTIVO).orElseThrow());

        SuspensionDocente saved = suspensionDocenteRepository.save(suspension);

        // Si hay sustituto, transferir asignaciones
        if (sustituto != null) {
            transferirAsignaciones(docente.getIdDocente(), sustituto.getIdDocente(), "SUSPENSION");
        }

        // Marcar docente como INACTIVO
        docente.setAcceso(accesoRepository.findById(AccesoConstants.INACTIVO).orElseThrow());
        docenteRepository.save(docente);

        return toResponse(saved);
    }

    @Transactional
    public SuspensionDocenteResponse finalizar(Integer id) {
        SuspensionDocente suspension = findSuspension(id);

        // Transferir asignaciones de vuelta al docente original
        if (suspension.getSustituto() != null) {
            transferirAsignaciones(suspension.getSustituto().getIdDocente(), suspension.getDocente().getIdDocente(), "FIN_SUSPENSION");
        }

        // Reactivar docente original
        Docente docente = suspension.getDocente();
        docente.setAcceso(accesoRepository.findById(AccesoConstants.ACTIVO).orElseThrow());
        docenteRepository.save(docente);

        // Eliminar suspension (marcar como ELIMINADO)
        suspension.setAcceso(accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow());
        suspensionDocenteRepository.save(suspension);

        return toResponse(suspension);
    }

    @Transactional
    public void delete(Integer id) {
        SuspensionDocente suspension = findSuspension(id);
        suspension.setAcceso(accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow());
        suspensionDocenteRepository.save(suspension);
    }

    private void transferirAsignaciones(Integer idDocenteOrigen, Integer idDocenteNuevo, String motivo) {
        Docente docenteNuevo = findDocente(idDocenteNuevo);
        Acceso accesoActivo = accesoRepository.findById(AccesoConstants.ACTIVO).orElseThrow();

        List<Asignacion> asignaciones = asignacionRepository
                .findByDocenteIdDocenteAndAcceso(idDocenteOrigen, accesoActivo);

        for (Asignacion asignacion : asignaciones) {
            Docente docenteAnterior = asignacion.getDocente();
            asignacion.setDocente(docenteNuevo);
            asignacionRepository.save(asignacion);

            // Registrar en cambio_docente
            CambioDocente cambio = new CambioDocente();
            cambio.setAsignacion(asignacion);
            cambio.setDocenteAnterior(docenteAnterior);
            cambio.setDocenteNuevo(docenteNuevo);
            cambio.setMotivo(motivo);
            cambio.setFechaCambio(LocalDate.now());
            cambio.setUsuarioRegistro(null); // Se debe pasar el usuario actual desde el controller
            cambio.setAcceso(accesoActivo);
            cambioDocenteRepository.save(cambio);
        }
    }

    private SuspensionDocente findSuspension(Integer id) {
        return suspensionDocenteRepository.findByIdSuspensionAndAccesoNot(id, accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow())
                .orElseThrow(() -> new ResourceNotFoundException("Suspension no encontrada con id " + id));
    }

    private Docente findDocente(Integer id) {
        return docenteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Docente no encontrado con id " + id));
    }

    private SuspensionDocenteResponse toResponse(SuspensionDocente suspension) {
        SuspensionDocenteResponse response = new SuspensionDocenteResponse();
        response.setIdSuspension(suspension.getIdSuspension());
        response.setIdDocente(suspension.getDocente().getIdDocente());
        response.setDocente(suspension.getDocente().getUsuario().getNombre() + " " + suspension.getDocente().getUsuario().getApellidoPat());
        if (suspension.getSustituto() != null) {
            response.setIdSustituto(suspension.getSustituto().getIdDocente());
            response.setSustituto(suspension.getSustituto().getUsuario().getNombre() + " " + suspension.getSustituto().getUsuario().getApellidoPat());
        }
        response.setMotivo(suspension.getMotivo());
        response.setMotivoDetalle(suspension.getMotivoDetalle());
        response.setFechaInicio(suspension.getFechaInicio());
        response.setFechaFin(suspension.getFechaFin());
        response.setAccesoId(suspension.getAcceso().getIdAcceso().longValue());
        return response;
    }
}
