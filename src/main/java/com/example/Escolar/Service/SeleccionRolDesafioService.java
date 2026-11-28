package com.example.Escolar.Service;

import com.example.Escolar.Dto.RolAsignadoSesionResponse;
import com.example.Escolar.Dto.RolAsignadoSesionResponse.EstadoRol;
import com.example.Escolar.Exception.DesafioSeleccionRolInvalidoException;
import com.example.Escolar.Exception.DosFactorVencidoException;
import com.example.Escolar.Model.Usuario;
import com.example.Escolar.Service.DosFactorService.PropositoDesafio;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SeleccionRolDesafioService {

    public static final int MINUTOS_DESAFIO = 3;

    public enum Resultado {
        INGRESO_DIRECTO,
        SELECCION_REQUERIDA,
        SIN_ROLES_DISPONIBLES
    }

    public record Evaluacion(Resultado resultado, List<RolAsignadoSesionResponse> roles) {

        public List<RolAsignadoSesionResponse> rolesSeleccionables() {
            return roles.stream().filter(rol -> rol.estado() == EstadoRol.ACTIVO).toList();
        }
    }

    public record Consumido(Usuario usuario, RolAsignadoSesionResponse rol, Integer idDispositivo) {
    }

    private final DosFactorService dosFactorService;
    private final RolAsignadoResolver rolAsignadoResolver;
    private final VigenciaService vigenciaService;

    public Evaluacion evaluar(Integer idUsuario) {
        List<RolAsignadoSesionResponse> roles = rolAsignadoResolver.resolver(idUsuario);
        long activos = roles.stream().filter(rol -> rol.estado() == EstadoRol.ACTIVO).count();
        if (activos == 0) {
            return new Evaluacion(Resultado.SIN_ROLES_DISPONIBLES, roles);
        }
        if (roles.size() == 1) {
            return new Evaluacion(Resultado.INGRESO_DIRECTO, roles);
        }
        return new Evaluacion(Resultado.SELECCION_REQUERIDA, roles);
    }

    public String emitir(Usuario usuario) {
        return emitir(usuario, null);
    }

    public String emitir(Usuario usuario, Integer idDispositivo) {
        if (usuario == null || usuario.getAcceso() == null
                || !AccesoConstants.ACTIVO.equals(usuario.getAcceso().getIdAcceso())) {
            throw invalido();
        }
        if (evaluar(usuario.getIdUsuario()).resultado() != Resultado.SELECCION_REQUERIDA) {
            throw invalido();
        }
        validarVigencia(usuario);
        return dosFactorService.emitirDesafio(usuario, PropositoDesafio.SELECCION_ROL, MINUTOS_DESAFIO, idDispositivo);
    }

    @Transactional
    public Consumido consumir(String desafio, Integer idRol) {
        if (idRol == null) {
            throw invalido();
        }
        Usuario usuario = validar(desafio);
        validarVigencia(usuario);
        RolAsignadoSesionResponse rol = rolAsignadoResolver.resolver(usuario.getIdUsuario()).stream()
                .filter(vista -> idRol.equals(vista.idRol()) && vista.estado() == EstadoRol.ACTIVO)
                .findFirst()
                .orElseThrow(this::invalido);
        Integer idDispositivo;
        try {
            idDispositivo = dosFactorService.idDispositivo(desafio, PropositoDesafio.SELECCION_ROL);
            dosFactorService.consumirDesafio(usuario, desafio, PropositoDesafio.SELECCION_ROL);
        } catch (DosFactorVencidoException e) {
            throw invalido();
        }
        return new Consumido(usuario, rol, idDispositivo);
    }

    private Usuario validar(String desafio) {
        try {
            return dosFactorService.validarDesafio(desafio, PropositoDesafio.SELECCION_ROL);
        } catch (DosFactorVencidoException e) {
            throw invalido();
        }
    }

    private void validarVigencia(Usuario usuario) {
        if (vigenciaService.datosNoDisponibles(usuario.getFechaInicioVigencia(), usuario.getFechaFinVigencia())
                || !vigenciaService.esValida(usuario.getFechaInicioVigencia(), usuario.getFechaFinVigencia())) {
            throw invalido();
        }
    }

    private DesafioSeleccionRolInvalidoException invalido() {
        return new DesafioSeleccionRolInvalidoException("El desafío de selección de rol ya no es válido.");
    }
}
