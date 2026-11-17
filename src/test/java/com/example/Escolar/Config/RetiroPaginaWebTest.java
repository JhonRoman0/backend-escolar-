package com.example.Escolar.Config;

import com.example.Escolar.Model.Acceso;
import com.example.Escolar.Model.Permiso;
import com.example.Escolar.Model.RolPermiso;
import com.example.Escolar.Repository.AccesoRepository;
import com.example.Escolar.Repository.PermisoAccionRepository;
import com.example.Escolar.Repository.PermisoRepository;
import com.example.Escolar.Repository.RolPermisoRepository;
import com.example.Escolar.Service.AccesoConstants;
import com.example.Escolar.Service.CloudinaryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RetiroPaginaWebTest {
    @Mock JdbcTemplate jdbc;
    @Mock CloudinaryService cloudinaryService;
    @Mock PermisoRepository permisoRepository;
    @Mock PermisoAccionRepository permisoAccionRepository;
    @Mock RolPermisoRepository rolPermisoRepository;
    @Mock AccesoRepository accesoRepository;
    @InjectMocks RetiroPaginaWeb retiro;

    private final Acceso activo = new Acceso(AccesoConstants.ACTIVO, "Activo");
    private final Acceso eliminado = new Acceso(AccesoConstants.ELIMINADO, "Eliminado");

    @BeforeEach
    void preparar() {
        when(accesoRepository.findById(AccesoConstants.ELIMINADO)).thenReturn(Optional.of(eliminado));
        when(permisoRepository.findByCodigoAndAccesoNot(anyString(), eq(eliminado))).thenReturn(Optional.empty());
    }

    private void conTablas(String... nombres) {
        when(jdbc.queryForList(startsWith("SELECT table_name"), eq(String.class))).thenReturn(List.of(nombres));
    }

    private void conImagenes(String tabla, String... ids) {
        when(jdbc.queryForList(contains("FROM `" + tabla + "`"), eq(String.class))).thenReturn(List.of(ids));
    }

    @Test
    void borraImagenesYLuegoLasTablas() {
        // En Windows MySQL devuelve los nombres en minúsculas.
        conTablas("publicacion", "eventoescolar", "galeriadetalle", "galeriaimagen", "contactomensaje", "ajusteportal", "alumno");
        conImagenes("publicacion", "web/p1");
        conImagenes("eventoescolar", "web/e1");
        conImagenes("galeriadetalle", "web/g1", "web/g2");

        retiro.retirar();

        verify(cloudinaryService).delete("web/p1");
        verify(cloudinaryService).delete("web/e1");
        verify(cloudinaryService).delete("web/g1");
        verify(cloudinaryService).delete("web/g2");
        verify(jdbc).execute(any(ConnectionCallback.class));
    }

    @Test
    void siUnaImagenFallaNoBorraLasTablas() {
        conTablas("publicacion");
        conImagenes("publicacion", "web/p1", "web/p2");
        doThrow(new IllegalArgumentException("sin red")).when(cloudinaryService).delete("web/p1");

        retiro.retirar();

        verify(cloudinaryService).delete("web/p2");
        verify(jdbc, never()).execute(any(ConnectionCallback.class));
    }

    @Test
    void sinLasTablasNoHaceNada() {
        conTablas("alumno", "matricula");

        retiro.retirar();

        verifyNoInteractions(cloudinaryService);
        verify(jdbc, never()).execute(any(ConnectionCallback.class));
    }

    @Test
    void quitaLosPermisosDeLaWebYDeLosRoles() {
        conTablas();
        Permiso publicaciones = new Permiso();
        publicaciones.setIdPermiso(40);
        publicaciones.setCodigo("PUBLICACIONES");
        publicaciones.setAcceso(activo);
        RolPermiso asignado = new RolPermiso();
        asignado.setAcceso(activo);
        when(permisoRepository.findByCodigoAndAccesoNot("PUBLICACIONES", eliminado)).thenReturn(Optional.of(publicaciones));
        when(rolPermisoRepository.findByPermisoIdPermiso(40)).thenReturn(List.of(asignado));

        retiro.retirar();

        assertEquals(AccesoConstants.ELIMINADO, publicaciones.getAcceso().getIdAcceso());
        assertEquals(AccesoConstants.ELIMINADO, asignado.getAcceso().getIdAcceso());
    }

    @Test
    void unErrorNoImpideElArranque() {
        when(jdbc.queryForList(anyString(), eq(String.class))).thenThrow(new RuntimeException("sin base"));
        assertDoesNotThrow(() -> retiro.retirar());
    }
}
