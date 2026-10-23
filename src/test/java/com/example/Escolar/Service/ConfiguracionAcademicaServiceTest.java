package com.example.Escolar.Service;

import com.example.Escolar.Dto.ConfiguracionAcademicaRequest;
import com.example.Escolar.Dto.ConfiguracionAcademicaResponse;
import com.example.Escolar.Model.Acceso;
import com.example.Escolar.Model.AnioEscolar;
import com.example.Escolar.Model.ConfiguracionAcademica;
import com.example.Escolar.Model.Nivel;
import com.example.Escolar.Repository.AccesoRepository;
import com.example.Escolar.Repository.AnioEscolarRepository;
import com.example.Escolar.Repository.AsignacionRepository;
import com.example.Escolar.Repository.ConfiguracionAcademicaRepository;
import com.example.Escolar.Repository.NivelRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ConfiguracionAcademicaServiceTest {

    @Mock
    private ConfiguracionAcademicaRepository configuracionRepository;
    @Mock
    private AnioEscolarRepository anioEscolarRepository;
    @Mock
    private NivelRepository nivelRepository;
    @Mock
    private AsignacionRepository asignacionRepository;
    @Mock
    private AccesoRepository accesoRepository;
    @InjectMocks
    private ConfiguracionAcademicaService service;

    private Acceso activo;
    private AnioEscolar anio2026;
    private AnioEscolar anio2027;
    private Nivel inicial;
    private Nivel primaria;

    @BeforeEach
    void setUp() {
        activo = acceso(AccesoConstants.ACTIVO);
        when(accesoRepository.findById(AccesoConstants.ACTIVO)).thenReturn(Optional.of(activo));
        when(accesoRepository.findById(AccesoConstants.ELIMINADO)).thenReturn(Optional.of(acceso(AccesoConstants.ELIMINADO)));

        anio2026 = anio(1, "2026", AnioEscolarService.ESTADO_VIGENTE);
        anio2027 = anio(2, "2027", AnioEscolarService.ESTADO_POR_COMENZAR);
        inicial = nivel(1, "Inicial");
        primaria = nivel(2, "Primaria");
        when(nivelRepository.findByAccesoNot(any())).thenReturn(List.of(inicial, primaria));
        when(configuracionRepository.findByAnioEscolarIdAnioAndNivelIdNivelAndAccesoNot(anyInt(), anyInt(), any()))
                .thenReturn(Optional.empty());
        when(configuracionRepository.findByNivelIdNivelAndAccesoNot(anyInt(), any())).thenReturn(List.of());
    }

    private Acceso acceso(Long id) {
        Acceso a = new Acceso();
        a.setIdAcceso(id);
        return a;
    }

    private AnioEscolar anio(int id, String valor, byte estado) {
        AnioEscolar a = new AnioEscolar();
        a.setIdAnio(id);
        a.setAnio(valor);
        a.setEstado(estado);
        when(anioEscolarRepository.findByIdAnioAndAccesoNot(eq(id), any())).thenReturn(Optional.of(a));
        return a;
    }

    private Nivel nivel(int id, String nombre) {
        Nivel n = new Nivel();
        n.setIdNivel(id);
        n.setNombre(nombre);
        when(nivelRepository.findByIdNivelAndAccesoNot(eq(id), any())).thenReturn(Optional.of(n));
        return n;
    }

    private ConfiguracionAcademica guardada(AnioEscolar anio, Nivel nivel, Integer minutos) {
        ConfiguracionAcademica c = new ConfiguracionAcademica();
        c.setAnioEscolar(anio);
        c.setNivel(nivel);
        c.setMinutosHoraAcademica(minutos);
        c.setAcceso(activo);
        return c;
    }

    private ConfiguracionAcademicaRequest request(int idAnio, int idNivel, Integer minutos) {
        ConfiguracionAcademicaRequest r = new ConfiguracionAcademicaRequest();
        r.setIdAnio(idAnio);
        r.setIdNivel(idNivel);
        r.setMinutosHoraAcademica(minutos);
        return r;
    }

    @Test
    void sinNadaGuardadoInicialEsLibreYPrimariaDura45() {
        List<ConfiguracionAcademicaResponse> res = service.listar(2);

        assertNull(res.get(0).getMinutosHoraAcademica());
        assertEquals(45, res.get(1).getMinutosHoraAcademica());
        assertTrue(res.stream().allMatch(r -> r.getOrigen().equals("DEFECTO")));
    }

    @Test
    void unAnioSinConfiguracionHeredaLaDelAnioAnterior() {
        when(configuracionRepository.findByNivelIdNivelAndAccesoNot(eq(2), any()))
                .thenReturn(List.of(guardada(anio2026, primaria, 40)));

        ConfiguracionAcademicaResponse res = service.listar(2).get(1);

        assertEquals(40, res.getMinutosHoraAcademica());
        assertEquals("HEREDADO", res.getOrigen());
        assertEquals("2026", res.getAnioOrigen());
    }

    @Test
    void laDelPropioAnioGanaALaHeredada() {
        when(configuracionRepository.findByNivelIdNivelAndAccesoNot(eq(2), any()))
                .thenReturn(List.of(guardada(anio2026, primaria, 40)));
        when(configuracionRepository.findByAnioEscolarIdAnioAndNivelIdNivelAndAccesoNot(eq(2), eq(2), any()))
                .thenReturn(Optional.of(guardada(anio2027, primaria, 50)));

        assertEquals(50, service.minutosHoraAcademica(anio2027, primaria));
    }

    @Test
    void guardaLaDuracionEnUnAnioPorComenzar() {
        service.guardar(request(2, 2, 40));

        ArgumentCaptor<ConfiguracionAcademica> captor = ArgumentCaptor.forClass(ConfiguracionAcademica.class);
        verify(configuracionRepository).save(captor.capture());
        assertEquals(40, captor.getValue().getMinutosHoraAcademica());
        assertEquals(anio2027, captor.getValue().getAnioEscolar());
    }

    @Test
    void noSeCambiaEnUnAnioVigente() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service.guardar(request(1, 2, 40)));
        assertTrue(ex.getMessage().contains("vigente"));
        verify(configuracionRepository, never()).save(any());
    }

    @Test
    void soloDe30A60MinutosDe5En5() {
        for (Integer invalido : List.of(25, 65, 42)) {
            assertThrows(IllegalArgumentException.class, () -> service.guardar(request(2, 2, invalido)));
        }
        verify(configuracionRepository, never()).save(any());
    }

    @Test
    void noSeCambiaSiElNivelYaTieneAsignaciones() {
        when(asignacionRepository.countByAnioEscolarIdAnioAndGradoSeccionGradoNivelIdNivelAndAccesoNot(eq(2), eq(2), any()))
                .thenReturn(3L);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service.guardar(request(2, 2, 40)));
        assertTrue(ex.getMessage().contains("3 asignaciones"));
        verify(configuracionRepository, never()).save(any());
    }
}
