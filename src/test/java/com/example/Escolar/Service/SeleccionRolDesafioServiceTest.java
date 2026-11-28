package com.example.Escolar.Service;

import com.example.Escolar.Dto.RolAsignadoSesionResponse;
import com.example.Escolar.Dto.RolAsignadoSesionResponse.EstadoRol;
import com.example.Escolar.Exception.DesafioSeleccionRolInvalidoException;
import com.example.Escolar.Exception.DosFactorVencidoException;
import com.example.Escolar.Model.Acceso;
import com.example.Escolar.Model.Usuario;
import com.example.Escolar.Service.DosFactorService.PropositoDesafio;
import com.example.Escolar.Service.SeleccionRolDesafioService.Resultado;
import com.example.Escolar.Util.FechaColegio;
import com.example.Escolar.Util.HashSeguro;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SeleccionRolDesafioServiceTest {

    private static final String DESAFIO = "SELROL:0:0:abc";

    @Mock
    private DosFactorService dosFactorService;
    @Mock
    private RolAsignadoResolver rolAsignadoResolver;
    @Mock
    private VigenciaService vigenciaService;

    @InjectMocks
    private SeleccionRolDesafioService servicio;

    private Usuario usuario;

    @BeforeEach
    void preparar() {
        usuario = new Usuario();
        usuario.setIdUsuario(7);
        usuario.setVersionToken(0);
        Acceso activo = new Acceso();
        activo.setIdAcceso(AccesoConstants.ACTIVO);
        usuario.setAcceso(activo);
        usuario.setFechaInicioVigencia(FechaColegio.hoy().minusDays(1));
        when(vigenciaService.datosNoDisponibles(any(), any())).thenReturn(false);
        when(vigenciaService.esValida(any(), any())).thenReturn(true);
    }

    @Test
    void dosRolesNoEliminadosConUnoActivoRequierenSeleccion() {
        when(rolAsignadoResolver.resolver(7)).thenReturn(List.of(rol(1, EstadoRol.SUSPENDIDO), rol(2, EstadoRol.ACTIVO)));

        SeleccionRolDesafioService.Evaluacion evaluacion = servicio.evaluar(7);

        assertEquals(Resultado.SELECCION_REQUERIDA, evaluacion.resultado());
        assertEquals(List.of(rol(1, EstadoRol.SUSPENDIDO), rol(2, EstadoRol.ACTIVO)), evaluacion.roles());
        assertEquals(List.of(rol(2, EstadoRol.ACTIVO)), evaluacion.rolesSeleccionables());
    }

    @Test
    void unicoRolActivoPermiteIngresoDirecto() {
        when(rolAsignadoResolver.resolver(7)).thenReturn(List.of(rol(2, EstadoRol.ACTIVO)));

        SeleccionRolDesafioService.Evaluacion evaluacion = servicio.evaluar(7);

        assertEquals(Resultado.INGRESO_DIRECTO, evaluacion.resultado());
        assertEquals(List.of(rol(2, EstadoRol.ACTIVO)), evaluacion.roles());
    }

    @Test
    void sinRolesActivosImpideIngreso() {
        when(rolAsignadoResolver.resolver(7)).thenReturn(List.of(rol(1, EstadoRol.INACTIVO), rol(2, EstadoRol.SUSPENDIDO)));

        SeleccionRolDesafioService.Evaluacion evaluacion = servicio.evaluar(7);

        assertEquals(Resultado.SIN_ROLES_DISPONIBLES, evaluacion.resultado());
        assertEquals(List.of(rol(1, EstadoRol.INACTIVO), rol(2, EstadoRol.SUSPENDIDO)), evaluacion.roles());
        assertEquals(List.of(), evaluacion.rolesSeleccionables());
    }

    @Test
    void sinRolesImpideIngreso() {
        when(rolAsignadoResolver.resolver(7)).thenReturn(List.of());

        assertEquals(Resultado.SIN_ROLES_DISPONIBLES, servicio.evaluar(7).resultado());
    }

    @Test
    void emitirGuardaHashYExpiracionDeTresMinutos() {
        when(rolAsignadoResolver.resolver(7)).thenReturn(List.of(rol(1, EstadoRol.INACTIVO), rol(2, EstadoRol.ACTIVO)));
        when(dosFactorService.emitirDesafio(usuario, PropositoDesafio.SELECCION_ROL, 3, null)).thenAnswer(i -> {
            usuario.setDosFactorDesafio(HashSeguro.sha256(DESAFIO));
            usuario.setDosFactorDesafioExpiracion(LocalDateTime.now().plusMinutes(3));
            return DESAFIO;
        });

        String token = servicio.emitir(usuario);

        assertEquals(DESAFIO, token);
        assertEquals(HashSeguro.sha256(token), usuario.getDosFactorDesafio());
        assertTrue(usuario.getDosFactorDesafioExpiracion().isAfter(LocalDateTime.now().plusMinutes(2)));
        assertTrue(usuario.getDosFactorDesafioExpiracion().isBefore(LocalDateTime.now().plusMinutes(4)));
        verify(dosFactorService).emitirDesafio(usuario, PropositoDesafio.SELECCION_ROL, 3, null);
    }

    @Test
    void emitirVinculaElDispositivoQueOriginoElDesafio() {
        when(rolAsignadoResolver.resolver(7)).thenReturn(List.of(rol(1, EstadoRol.INACTIVO), rol(2, EstadoRol.ACTIVO)));
        when(dosFactorService.emitirDesafio(usuario, PropositoDesafio.SELECCION_ROL, 3, 40))
                .thenReturn("SELROL:0:40:abc");

        assertEquals("SELROL:0:40:abc", servicio.emitir(usuario, 40));
        verify(dosFactorService).emitirDesafio(usuario, PropositoDesafio.SELECCION_ROL, 3, 40);
    }

    @Test
    void emitirConIngresoDirectoNoGeneraDesafio() {
        when(rolAsignadoResolver.resolver(7)).thenReturn(List.of(rol(2, EstadoRol.ACTIVO)));

        assertThrows(DesafioSeleccionRolInvalidoException.class, () -> servicio.emitir(usuario));
        verify(dosFactorService, never()).emitirDesafio(any(), any(), anyInt(), any());
    }

    @Test
    void emitirConCuentaInactivaNoGeneraDesafio() {
        Acceso inactivo = new Acceso();
        inactivo.setIdAcceso(AccesoConstants.INACTIVO);
        usuario.setAcceso(inactivo);

        assertThrows(DesafioSeleccionRolInvalidoException.class, () -> servicio.emitir(usuario));
        verifyNoInteractions(dosFactorService, rolAsignadoResolver);
    }

    @Test
    void consumirConRolActivoEjecutaConsumoAtomico() {
        RolAsignadoSesionResponse suspendido = rol(1, EstadoRol.SUSPENDIDO);
        RolAsignadoSesionResponse activo = rol(2, EstadoRol.ACTIVO);
        when(dosFactorService.validarDesafio(DESAFIO, PropositoDesafio.SELECCION_ROL)).thenReturn(usuario);
        when(dosFactorService.idDispositivo(DESAFIO, PropositoDesafio.SELECCION_ROL)).thenReturn(40);
        when(rolAsignadoResolver.resolver(7)).thenReturn(List.of(suspendido, activo));

        SeleccionRolDesafioService.Consumido consumido = servicio.consumir(DESAFIO, 2);

        assertSame(usuario, consumido.usuario());
        assertEquals(activo, consumido.rol());
        assertEquals(40, consumido.idDispositivo());
        verify(dosFactorService).consumirDesafio(usuario, DESAFIO, PropositoDesafio.SELECCION_ROL);
    }

    @Test
    void docenteSuspendidoNoImpideIngresarConOtroRolDisponible() {
        RolAsignadoSesionResponse docente = new RolAsignadoSesionResponse(1, "Docente", EstadoRol.SUSPENDIDO);
        RolAsignadoSesionResponse apoderado = new RolAsignadoSesionResponse(2, "Apoderado", EstadoRol.ACTIVO);
        when(dosFactorService.validarDesafio(DESAFIO, PropositoDesafio.SELECCION_ROL)).thenReturn(usuario);
        when(dosFactorService.idDispositivo(DESAFIO, PropositoDesafio.SELECCION_ROL)).thenReturn(40);
        when(rolAsignadoResolver.resolver(7)).thenReturn(List.of(docente, apoderado));

        SeleccionRolDesafioService.Consumido consumido = servicio.consumir(DESAFIO, 2);

        assertEquals(apoderado, consumido.rol());
        verify(dosFactorService).consumirDesafio(usuario, DESAFIO, PropositoDesafio.SELECCION_ROL);
    }

    @Test
    void consumirExigeIdRol() {
        assertThrows(DesafioSeleccionRolInvalidoException.class, () -> servicio.consumir(DESAFIO, null));
        verifyNoInteractions(dosFactorService);
    }

    @Test
    void consumirRechazaRolesNoActivos() {
        when(dosFactorService.validarDesafio(DESAFIO, PropositoDesafio.SELECCION_ROL)).thenReturn(usuario);
        when(rolAsignadoResolver.resolver(7)).thenReturn(List.of(rol(2, EstadoRol.INACTIVO), rol(3, EstadoRol.SUSPENDIDO)));

        assertThrows(DesafioSeleccionRolInvalidoException.class, () -> servicio.consumir(DESAFIO, 2));
        assertThrows(DesafioSeleccionRolInvalidoException.class, () -> servicio.consumir(DESAFIO, 3));
        verify(dosFactorService, never()).consumirDesafio(any(), anyString(), any());
    }

    @Test
    void consumirRechazaDesafioDe2FA() {
        when(dosFactorService.validarDesafio("2FA:abc", PropositoDesafio.SELECCION_ROL))
                .thenThrow(new DosFactorVencidoException("invalido"));

        assertThrows(DesafioSeleccionRolInvalidoException.class, () -> servicio.consumir("2FA:abc", 2));
        when(dosFactorService.validarDesafio(DESAFIO, PropositoDesafio.SELECCION_ROL))
                .thenThrow(new DosFactorVencidoException("vencido"));
        assertThrows(DesafioSeleccionRolInvalidoException.class, () -> servicio.consumir(DESAFIO, 2));
        verify(dosFactorService, never()).consumirDesafio(any(), anyString(), any());
    }

    @Test
    void consumirRechazaSiElConsumoAtomicoNoAfectaFilas() {
        when(dosFactorService.validarDesafio(DESAFIO, PropositoDesafio.SELECCION_ROL)).thenReturn(usuario);
        when(rolAsignadoResolver.resolver(7)).thenReturn(List.of(rol(2, EstadoRol.ACTIVO)));
        doThrow(new DosFactorVencidoException("invalido"))
                .when(dosFactorService).consumirDesafio(usuario, DESAFIO, PropositoDesafio.SELECCION_ROL);

        assertThrows(DesafioSeleccionRolInvalidoException.class, () -> servicio.consumir(DESAFIO, 2));
    }

    @Test
    void consumirRechazaVigenciaVencida() {
        when(dosFactorService.validarDesafio(DESAFIO, PropositoDesafio.SELECCION_ROL)).thenReturn(usuario);
        when(vigenciaService.datosNoDisponibles(any(), any())).thenReturn(true);

        assertThrows(DesafioSeleccionRolInvalidoException.class, () -> servicio.consumir(DESAFIO, 2));
        verify(dosFactorService, never()).consumirDesafio(any(), anyString(), any());
    }

    private RolAsignadoSesionResponse rol(Integer idRol, EstadoRol estado) {
        return new RolAsignadoSesionResponse(idRol, "Rol" + idRol, estado);
    }
}
