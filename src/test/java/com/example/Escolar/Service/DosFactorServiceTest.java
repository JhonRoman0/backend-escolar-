package com.example.Escolar.Service;

import com.example.Escolar.Exception.CuentaBloqueadaException;
import com.example.Escolar.Exception.DosFactorVencidoException;
import com.example.Escolar.Exception.EnvioCodigoFallidoException;
import com.example.Escolar.Exception.EsperaReenvioCodigoException;
import com.example.Escolar.Exception.LimiteEnviosCodigoException;
import com.example.Escolar.Exception.ResourceNotFoundException;
import com.example.Escolar.Model.Acceso;
import com.example.Escolar.Model.DispositivoUsuario;
import com.example.Escolar.Model.Usuario;
import com.example.Escolar.Repository.DispositivoUsuarioRepository;
import com.example.Escolar.Repository.UsuarioRepository;
import com.example.Escolar.Util.HashSeguro;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DosFactorServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private DispositivoUsuarioRepository dispositivoRepository;
    @Mock
    private EmailService emailService;
    @Mock
    private EventosService eventosService;

    @InjectMocks
    private DosFactorService servicio;

    private final DosFactorService.Contexto contexto = new DosFactorService.Contexto("Chrome en Windows", "190.1.2.3");
    /** Último código enviado por "correo". */
    private final AtomicReference<String> codigoEnviado = new AtomicReference<>();
    private Usuario usuario;

    @BeforeEach
    void preparar() {
        ReflectionTestUtils.setField(servicio, "activo", true);
        usuario = new Usuario();
        usuario.setIdUsuario(7);
        usuario.setNombre("Ana");
        usuario.setGmail("ana.perez@gmail.com");
        usuario.setContraseña("$2a$hash");
        usuario.setVersionToken(0);
        usuario.setIntentosFallidos(0);
        Acceso activo = new Acceso();
        activo.setIdAcceso(AccesoConstants.ACTIVO);
        usuario.setAcceso(activo);
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));
        // Como la base: al guardar, el dispositivo recibe su id.
        when(dispositivoRepository.save(any(DispositivoUsuario.class))).thenAnswer(i -> {
            DispositivoUsuario d = i.getArgument(0);
            if (d.getIdDispositivo() == null) d.setIdDispositivo(1);
            return d;
        });
        when(emailService.enviarCodigoDosFactor(anyString(), any(), anyString(), anyInt(), any(), any()))
                .thenAnswer(i -> {
                    codigoEnviado.set(i.getArgument(2));
                    return true;
                });
    }

    /** Simula la búsqueda por desafío tal como la haría la base. */
    private void buscarPorDesafio() {
        when(usuarioRepository.findByDosFactorDesafio(anyString())).thenAnswer(i ->
                i.getArgument(0).equals(usuario.getDosFactorDesafio()) ? Optional.of(usuario) : Optional.empty());
    }

    @Test
    void iniciarEnviaUnCodigoYSoloGuardaHashes() {
        DosFactorService.Desafio d = servicio.iniciar(usuario, contexto);

        assertEquals("an***@gmail.com", d.correoEnmascarado());
        assertEquals(5, d.minutosCodigo());
        assertNotNull(codigoEnviado.get());
        assertEquals(HashSeguro.sha256(codigoEnviado.get()), usuario.getDosFactorCodigo());
        assertEquals(HashSeguro.sha256(d.desafio()), usuario.getDosFactorDesafio());
        assertNotEquals(d.desafio(), usuario.getDosFactorDesafio());
        assertTrue(usuario.getDosFactorCodigoExpiracion().isBefore(LocalDateTime.now().plusMinutes(6)));
    }

    @Test
    void siElCorreoFallaElUsuarioSeEntera() {
        when(emailService.enviarCodigoDosFactor(anyString(), any(), anyString(), anyInt(), any(), any())).thenReturn(false);
        assertThrows(EnvioCodigoFallidoException.class, () -> servicio.iniciar(usuario, contexto));
    }

    @Test
    void codigoCorrectoCompletaYLimpiaElDesafio() {
        buscarPorDesafio();
        DosFactorService.Desafio d = servicio.iniciar(usuario, contexto);

        DosFactorService.Verificado v = servicio.verificar(d.desafio(), codigoEnviado.get(), false, null, contexto);

        assertSame(usuario, v.usuario());
        assertNotNull(v.tokenDispositivo());
        assertNull(usuario.getDosFactorDesafio());
        assertNull(usuario.getDosFactorCodigo());
        // El desafío no se puede reutilizar.
        assertThrows(DosFactorVencidoException.class,
                () -> servicio.verificar(d.desafio(), codigoEnviado.get(), false, null, contexto));
    }

    @Test
    void codigoIncorrectoDescuentaIntentosYAlTopeBloquea() {
        buscarPorDesafio();
        DosFactorService.Desafio d = servicio.iniciar(usuario, contexto);
        String malo = codigoEnviado.get().equals("111111") ? "222222" : "111111";

        ResourceNotFoundException e = assertThrows(ResourceNotFoundException.class,
                () -> servicio.verificar(d.desafio(), malo, false, null, contexto));
        assertTrue(e.getMessage().contains("4 intentos"));

        usuario.setIntentosFallidos(4);
        assertThrows(CuentaBloqueadaException.class, () -> servicio.verificar(d.desafio(), malo, false, null, contexto));
        assertNotNull(usuario.getFechaBloqueo());
        assertNull(usuario.getDosFactorDesafio());
    }

    @Test
    void codigoVencidoNoGastaIntentos() {
        buscarPorDesafio();
        DosFactorService.Desafio d = servicio.iniciar(usuario, contexto);
        usuario.setDosFactorCodigoExpiracion(LocalDateTime.now().minusSeconds(1));

        ResourceNotFoundException e = assertThrows(ResourceNotFoundException.class,
                () -> servicio.verificar(d.desafio(), codigoEnviado.get(), false, null, contexto));
        assertTrue(e.getMessage().contains("expirado"));
        assertEquals(0, usuario.getIntentosFallidos());
    }

    @Test
    void desafioDesconocidoOVencidoPideVolverALaContrasena() {
        buscarPorDesafio();
        assertThrows(DosFactorVencidoException.class, () -> servicio.verificar("inventado", "123456", false, null, contexto));

        DosFactorService.Desafio d = servicio.iniciar(usuario, contexto);
        usuario.setDosFactorDesafioExpiracion(LocalDateTime.now().minusSeconds(1));
        assertThrows(DosFactorVencidoException.class, () -> servicio.reenviar(d.desafio(), contexto));
    }

    @Test
    void reenviarRespetaLaEsperaYAnulaElCodigoAnterior() {
        buscarPorDesafio();
        DosFactorService.Desafio d = servicio.iniciar(usuario, contexto);
        assertThrows(EsperaReenvioCodigoException.class, () -> servicio.reenviar(d.desafio(), contexto));

        String anterior = usuario.getDosFactorCodigo();
        usuario.setDosFactorEnviado(LocalDateTime.now().minusSeconds(46));
        servicio.reenviar(d.desafio(), contexto);
        assertEquals(HashSeguro.sha256(codigoEnviado.get()), usuario.getDosFactorCodigo());
        // Mismo código por azar es posible pero improbable; lo importante es el hash nuevo guardado.
        assertNotNull(anterior);
    }

    @Test
    void maximoCincoEnviosPorHora() {
        for (int i = 0; i < DosFactorService.MAX_ENVIOS_POR_HORA; i++) {
            servicio.iniciar(usuario, contexto);
        }
        assertThrows(LimiteEnviosCodigoException.class, () -> servicio.iniciar(usuario, contexto));

        // Pasada la hora, se puede de nuevo.
        usuario.setDosFactorVentana(LocalDateTime.now().minusMinutes(61));
        assertDoesNotThrow(() -> servicio.iniciar(usuario, contexto));
    }

    @Test
    void entrarYSalirVariasVecesUsandoElCodigoNoBloquea() {
        buscarPorDesafio();
        for (int i = 0; i < DosFactorService.MAX_ENVIOS_POR_HORA + 3; i++) {
            DosFactorService.Desafio d = servicio.iniciar(usuario, contexto);
            servicio.verificar(d.desafio(), codigoEnviado.get(), false, null, contexto);
        }
        assertEquals(0, usuario.getDosFactorEnvios());
    }

    @Test
    void recordarDejaElNavegadorDeConfianzaSieteDias() {
        buscarPorDesafio();
        DosFactorService.Desafio d = servicio.iniciar(usuario, contexto);
        ArgumentCaptor<DispositivoUsuario> guardado = ArgumentCaptor.forClass(DispositivoUsuario.class);

        DosFactorService.Verificado v = servicio.verificar(d.desafio(), codigoEnviado.get(), true, null, contexto);

        verify(dispositivoRepository).save(guardado.capture());
        DispositivoUsuario dispositivo = guardado.getValue();
        assertEquals(HashSeguro.sha256(v.tokenDispositivo()), dispositivo.getTokenHash());
        assertTrue(dispositivo.getConfiableHasta().isAfter(LocalDateTime.now().plusDays(6)));
        assertEquals("Chrome en Windows", dispositivo.getNavegador());

        when(dispositivoRepository.findByUsuarioIdUsuarioAndTokenHash(7, dispositivo.getTokenHash()))
                .thenReturn(Optional.of(dispositivo));
        assertTrue(servicio.esDeConfianza(usuario, v.tokenDispositivo()));
        assertFalse(servicio.esDeConfianza(usuario, "otro-navegador"));
    }

    @Test
    void cambiarContrasenaCorreoOVersionDeSesionQuitaLaConfianza() {
        buscarPorDesafio();
        DosFactorService.Desafio d = servicio.iniciar(usuario, contexto);
        ArgumentCaptor<DispositivoUsuario> guardado = ArgumentCaptor.forClass(DispositivoUsuario.class);
        DosFactorService.Verificado v = servicio.verificar(d.desafio(), codigoEnviado.get(), true, null, contexto);
        verify(dispositivoRepository).save(guardado.capture());
        when(dispositivoRepository.findByUsuarioIdUsuarioAndTokenHash(eq(7), anyString()))
                .thenReturn(Optional.of(guardado.getValue()));

        usuario.setContraseña("$2a$otra");
        assertFalse(servicio.esDeConfianza(usuario, v.tokenDispositivo()));
        usuario.setContraseña("$2a$hash");
        usuario.setGmail("nuevo@gmail.com");
        assertFalse(servicio.esDeConfianza(usuario, v.tokenDispositivo()));
        usuario.setGmail("ana.perez@gmail.com");
        usuario.setVersionToken(1);
        assertFalse(servicio.esDeConfianza(usuario, v.tokenDispositivo()));
        usuario.setVersionToken(0);
        assertTrue(servicio.esDeConfianza(usuario, v.tokenDispositivo()));
    }

    @Test
    void avisaSoloCuandoEsUnNavegadorNuevoYNoElPrimero() {
        buscarPorDesafio();
        // Primer navegador de la cuenta: sin aviso.
        when(dispositivoRepository.existsByUsuarioIdUsuario(7)).thenReturn(false);
        DosFactorService.Desafio d1 = servicio.iniciar(usuario, contexto);
        servicio.verificar(d1.desafio(), codigoEnviado.get(), false, null, contexto);
        verify(emailService, never()).enviarAvisoNuevoDispositivo(any(), any(), any(), any(), any());

        // Otro navegador cuando ya tiene uno: aviso.
        when(dispositivoRepository.existsByUsuarioIdUsuario(7)).thenReturn(true);
        DosFactorService.Desafio d2 = servicio.iniciar(usuario, contexto);
        servicio.verificar(d2.desafio(), codigoEnviado.get(), false, null, contexto);
        verify(emailService).enviarAvisoNuevoDispositivo(eq("ana.perez@gmail.com"), eq("Ana"),
                eq("Chrome en Windows"), eq("190.1.2.3"), anyString());

        // Navegador ya conocido: sin aviso nuevo.
        DispositivoUsuario conocido = new DispositivoUsuario();
        when(dispositivoRepository.findByUsuarioIdUsuarioAndTokenHash(eq(7), anyString())).thenReturn(Optional.of(conocido));
        DosFactorService.Desafio d3 = servicio.iniciar(usuario, contexto);
        servicio.verificar(d3.desafio(), codigoEnviado.get(), false, "token-conocido", contexto);
        verify(emailService, times(1)).enviarAvisoNuevoDispositivo(any(), any(), any(), any(), any());
    }

    @Test
    void quitarUnDispositivoLoBorraYAvisaALasPestanasDelUsuario() {
        DispositivoUsuario d = new DispositivoUsuario();
        d.setIdDispositivo(40);
        when(dispositivoRepository.findByIdDispositivoAndUsuarioIdUsuario(40, 7)).thenReturn(Optional.of(d));

        servicio.olvidar(7, 40);

        verify(dispositivoRepository).delete(d);
        verify(eventosService).avisarCuenta(7);
        // Un dispositivo de otro usuario no se puede quitar.
        assertThrows(ResourceNotFoundException.class, () -> servicio.olvidar(8, 40));
    }

    @Test
    void iniciarSesionAvisaALasPestanasDelUsuario() {
        buscarPorDesafio();
        DosFactorService.Desafio d = servicio.iniciar(usuario, contexto);
        verify(eventosService, never()).avisarCuenta(any());

        DosFactorService.Verificado v = servicio.verificar(d.desafio(), codigoEnviado.get(), true, null, contexto);
        verify(eventosService, times(1)).avisarCuenta(7);

        // Entrar por un navegador de confianza también lo avisa (cambia su último uso).
        ArgumentCaptor<DispositivoUsuario> guardado = ArgumentCaptor.forClass(DispositivoUsuario.class);
        verify(dispositivoRepository).save(guardado.capture());
        when(dispositivoRepository.findByUsuarioIdUsuarioAndTokenHash(eq(7), anyString()))
                .thenReturn(Optional.of(guardado.getValue()));
        assertTrue(servicio.dispositivoDeConfianza(usuario, v.tokenDispositivo()).isPresent());
        verify(eventosService, times(2)).avisarCuenta(7);
    }

    @Test
    void verificarDevuelveElDispositivoParaAtarLaSesion() {
        buscarPorDesafio();
        when(dispositivoRepository.save(any(DispositivoUsuario.class))).thenAnswer(i -> {
            DispositivoUsuario d = i.getArgument(0);
            d.setIdDispositivo(55);
            return d;
        });
        DosFactorService.Desafio d = servicio.iniciar(usuario, contexto);
        assertEquals(55, servicio.verificar(d.desafio(), codigoEnviado.get(), false, null, contexto).idDispositivo());
    }

    @Test
    void noArrancaSiUnAdministradorNoTieneCorreo() {
        when(usuarioRepository.administradoresSinCorreo(AccesoConstants.ACTIVO, AccesoConstants.ELIMINADO))
                .thenReturn(List.of("ADM0001"));
        IllegalStateException e = assertThrows(IllegalStateException.class, servicio::verificarAdministradores);
        assertTrue(e.getMessage().contains("ADM0001"));

        ReflectionTestUtils.setField(servicio, "activo", false);
        assertDoesNotThrow(servicio::verificarAdministradores);
    }

    @Test
    void enmascaraElCorreo() {
        assertEquals("ja***@gmail.com", DosFactorService.enmascarar("jairo@gmail.com"));
        assertEquals("a***@x.pe", DosFactorService.enmascarar("a@x.pe"));
    }
}
