package com.example.Escolar.Service;

import com.example.Escolar.Dto.MetodoPagoRequest;
import com.example.Escolar.Dto.MetodoPagoResponse;
import com.example.Escolar.Dto.MontoMatriculaRequest;
import com.example.Escolar.Dto.MontoMatriculaResponse;
import com.example.Escolar.Dto.OpcionResponse;
import com.example.Escolar.Dto.ParentescoResponse;
import com.example.Escolar.Exception.ResourceNotFoundException;
import com.example.Escolar.Model.Acceso;
import com.example.Escolar.Model.AnioEscolar;
import com.example.Escolar.Model.MetodoPago;
import com.example.Escolar.Model.MontoMatricula;
import com.example.Escolar.Model.Nivel;
import com.example.Escolar.Model.Parentesco;
import com.example.Escolar.Model.Sexo;
import com.example.Escolar.Repository.AccesoRepository;
import com.example.Escolar.Repository.AnioEscolarRepository;
import com.example.Escolar.Repository.MetodoPagoRepository;
import com.example.Escolar.Repository.MontoMatriculaRepository;
import com.example.Escolar.Repository.NivelRepository;
import com.example.Escolar.Repository.ParentescoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/** Configuración del módulo Pagos (métodos y montos) y listas fijas del formulario de matrícula. */
@Service
@RequiredArgsConstructor
public class PagoConfigService {

    private final MetodoPagoRepository metodoPagoRepository;
    private final MontoMatriculaRepository montoMatriculaRepository;
    private final NivelRepository nivelRepository;
    private final AnioEscolarRepository anioEscolarRepository;
    private final ParentescoRepository parentescoRepository;
    private final AccesoRepository accesoRepository;

    public List<ParentescoResponse> parentescos() {
        return parentescoRepository.findByAccesoNotOrderByIdParentesco(acceso(AccesoConstants.ELIMINADO)).stream()
                .map(p -> new ParentescoResponse(p.getIdParentesco(), p.getNombre()))
                .toList();
    }

    public Parentesco parentesco(Integer id) {
        if (id == null) {
            throw new IllegalArgumentException("El parentesco es obligatorio");
        }
        return parentescoRepository.findByIdParentescoAndAccesoNot(id, acceso(AccesoConstants.ELIMINADO))
                .orElseThrow(() -> new ResourceNotFoundException("Parentesco no encontrado con id " + id));
    }

    public List<OpcionResponse> sexos() {
        return Arrays.stream(Sexo.values()).map(s -> new OpcionResponse(s.name(), s.getEtiqueta())).toList();
    }

    public List<MetodoPagoResponse> metodos(boolean soloActivos) {
        return metodoPagoRepository.findByAccesoNotOrderByNombre(acceso(AccesoConstants.ELIMINADO)).stream()
                .filter(m -> !soloActivos || m.getAcceso().getIdAcceso().equals(AccesoConstants.ACTIVO))
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public MetodoPagoResponse crearMetodo(MetodoPagoRequest request) {
        validarNombreUnico(request.getNombre(), null);
        MetodoPago metodo = new MetodoPago();
        metodo.setAcceso(acceso(AccesoConstants.ACTIVO));
        aplicar(metodo, request);
        return toResponse(metodoPagoRepository.save(metodo));
    }

    @Transactional
    public MetodoPagoResponse actualizarMetodo(Integer id, MetodoPagoRequest request) {
        MetodoPago metodo = findMetodo(id);
        validarNombreUnico(request.getNombre(), id);
        aplicar(metodo, request);
        return toResponse(metodoPagoRepository.save(metodo));
    }

    // Borrado lógico: los pagos ya registrados conservan su método.
    @Transactional
    public void eliminarMetodo(Integer id) {
        MetodoPago metodo = findMetodo(id);
        metodo.setAcceso(acceso(AccesoConstants.ELIMINADO));
        metodoPagoRepository.save(metodo);
    }

    public MetodoPago metodoActivo(Integer id) {
        if (id == null) {
            throw new IllegalArgumentException("El método de pago es obligatorio");
        }
        MetodoPago metodo = findMetodo(id);
        if (!metodo.getAcceso().getIdAcceso().equals(AccesoConstants.ACTIVO)) {
            throw new IllegalArgumentException("El método de pago " + metodo.getNombre() + " está desactivado");
        }
        return metodo;
    }

    /** Todos los niveles con el monto del año; si el año no tiene ninguno, sugiere los del año anterior. */
    public List<MontoMatriculaResponse> montos(Integer idAnio) {
        AnioEscolar anio = findAnio(idAnio);
        Map<Integer, BigDecimal> actuales = montosDe(anio.getIdAnio());
        Map<Integer, BigDecimal> sugeridos = actuales.isEmpty()
                ? anioAnteriorConMontos(anio).map(a -> montosDe(a.getIdAnio())).orElse(Map.of())
                : Map.of();
        return nivelRepository.findByAccesoNot(acceso(AccesoConstants.ELIMINADO)).stream()
                .sorted(OrdenAcademicoService.POR_ORDEN_NIVEL)
                .map(nivel -> {
                    MontoMatriculaResponse response = new MontoMatriculaResponse();
                    response.setIdNivel(nivel.getIdNivel());
                    response.setNivel(nivel.getNombre());
                    response.setMonto(actuales.get(nivel.getIdNivel()));
                    response.setSugerido(sugeridos.get(nivel.getIdNivel()));
                    return response;
                })
                .toList();
    }

    @Transactional
    public List<MontoMatriculaResponse> guardarMontos(MontoMatriculaRequest request) {
        AnioEscolar anio = findAnio(request.getIdAnio());
        for (MontoMatriculaRequest.Item item : request.getMontos()) {
            Nivel nivel = nivelRepository.findByIdNivelAndAccesoNot(item.getIdNivel(), acceso(AccesoConstants.ELIMINADO))
                    .orElseThrow(() -> new ResourceNotFoundException("Nivel no encontrado con id " + item.getIdNivel()));
            MontoMatricula monto = montoMatriculaRepository
                    .findByNivelIdNivelAndAnioEscolarIdAnio(nivel.getIdNivel(), anio.getIdAnio())
                    .orElseGet(() -> {
                        MontoMatricula nuevo = new MontoMatricula();
                        nuevo.setNivel(nivel);
                        nuevo.setAnioEscolar(anio);
                        return nuevo;
                    });
            monto.setMonto(item.getMonto());
            monto.setAcceso(acceso(AccesoConstants.ACTIVO));
            montoMatriculaRepository.save(monto);
        }
        return montos(anio.getIdAnio());
    }

    /** Precio de matrícula del nivel en ese año; sin él no se puede matricular. */
    public BigDecimal montoDe(Nivel nivel, AnioEscolar anio) {
        return montoMatriculaRepository.findByNivelIdNivelAndAnioEscolarIdAnio(nivel.getIdNivel(), anio.getIdAnio())
                .filter(m -> !m.getAcceso().getIdAcceso().equals(AccesoConstants.ELIMINADO))
                .map(MontoMatricula::getMonto)
                .orElseThrow(() -> new IllegalArgumentException("Configura el monto de matrícula de "
                        + nivel.getNombre() + " " + anio.getAnio() + " en el módulo Pagos"));
    }

    private Map<Integer, BigDecimal> montosDe(Integer idAnio) {
        return montoMatriculaRepository.findByAnioEscolarIdAnio(idAnio).stream()
                .filter(m -> !m.getAcceso().getIdAcceso().equals(AccesoConstants.ELIMINADO))
                .collect(Collectors.toMap(m -> m.getNivel().getIdNivel(), MontoMatricula::getMonto));
    }

    private Optional<AnioEscolar> anioAnteriorConMontos(AnioEscolar anio) {
        return anioEscolarRepository.findByAccesoNot(acceso(AccesoConstants.ELIMINADO)).stream()
                .filter(a -> a.getAnio().compareTo(anio.getAnio()) < 0)
                .sorted(Comparator.comparing(AnioEscolar::getAnio).reversed())
                .filter(a -> !montosDe(a.getIdAnio()).isEmpty())
                .findFirst();
    }

    private void aplicar(MetodoPago metodo, MetodoPagoRequest request) {
        metodo.setNombre(request.getNombre().trim());
        metodo.setRequiereCodigo(request.isRequiereCodigo());
        metodo.setRequiereComprobante(request.isRequiereComprobante());
        if (request.getActivo() != null) {
            metodo.setAcceso(acceso(request.getActivo() ? AccesoConstants.ACTIVO : AccesoConstants.INACTIVO));
        }
    }

    private void validarNombreUnico(String nombre, Integer idExcluir) {
        metodoPagoRepository.findByNombreIgnoreCaseAndAccesoNot(nombre.trim(), acceso(AccesoConstants.ELIMINADO))
                .filter(m -> !m.getIdMetodoPago().equals(idExcluir))
                .ifPresent(m -> {
                    throw new IllegalArgumentException("Ya existe el método de pago " + m.getNombre());
                });
    }

    private MetodoPagoResponse toResponse(MetodoPago metodo) {
        MetodoPagoResponse response = new MetodoPagoResponse();
        response.setIdMetodoPago(metodo.getIdMetodoPago());
        response.setNombre(metodo.getNombre());
        response.setRequiereCodigo(metodo.isRequiereCodigo());
        response.setRequiereComprobante(metodo.isRequiereComprobante());
        response.setActivo(metodo.getAcceso().getIdAcceso().equals(AccesoConstants.ACTIVO));
        return response;
    }

    private MetodoPago findMetodo(Integer id) {
        return metodoPagoRepository.findByIdMetodoPagoAndAccesoNot(id, acceso(AccesoConstants.ELIMINADO))
                .orElseThrow(() -> new ResourceNotFoundException("Método de pago no encontrado con id " + id));
    }

    private AnioEscolar findAnio(Integer id) {
        return anioEscolarRepository.findByIdAnioAndAccesoNot(id, acceso(AccesoConstants.ELIMINADO))
                .orElseThrow(() -> new ResourceNotFoundException("Año escolar no encontrado con id " + id));
    }

    private Acceso acceso(Long id) {
        return accesoRepository.findById(id).orElseThrow();
    }
}
