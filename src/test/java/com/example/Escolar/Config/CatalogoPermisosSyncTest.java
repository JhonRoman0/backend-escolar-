package com.example.Escolar.Config;

import com.example.Escolar.Model.Acceso;
import com.example.Escolar.Model.Accion;
import com.example.Escolar.Model.Permiso;
import com.example.Escolar.Model.PermisoAccion;
import com.example.Escolar.Model.RolPermiso;
import com.example.Escolar.Model.RolPermisoAccion;
import com.example.Escolar.Repository.AccesoRepository;
import com.example.Escolar.Repository.AccionRepository;
import com.example.Escolar.Repository.PermisoAccionRepository;
import com.example.Escolar.Repository.PermisoRepository;
import com.example.Escolar.Repository.RolPermisoAccionRepository;
import com.example.Escolar.Repository.RolPermisoRepository;
import com.example.Escolar.Service.AccesoConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CatalogoPermisosSyncTest {

    @Mock
    private PermisoRepository permisoRepository;
    @Mock
    private AccionRepository accionRepository;
    @Mock
    private PermisoAccionRepository permisoAccionRepository;
    @Mock
    private RolPermisoRepository rolPermisoRepository;
    @Mock
    private RolPermisoAccionRepository rolPermisoAccionRepository;
    @Mock
    private AccesoRepository accesoRepository;
    @InjectMocks
    private CatalogoPermisosSync sync;

    private Acceso activo;
    private Permiso competencias;
    private final Map<String, Accion> acciones = new HashMap<>();

    @BeforeEach
    void setUp() {
        activo = acceso(AccesoConstants.ACTIVO);
        Acceso eliminado = acceso(AccesoConstants.ELIMINADO);
        when(accesoRepository.findById(AccesoConstants.ACTIVO)).thenReturn(Optional.of(activo));
        when(accesoRepository.findById(AccesoConstants.ELIMINADO)).thenReturn(Optional.of(eliminado));

        int id = 1;
        for (String codigo : List.of("CREAR", "ACTUALIZAR", "ELIMINAR", "IMPRIMIR_EXPORTAR")) {
            Accion accion = new Accion();
            accion.setIdAccion(id++);
            accion.setCodigo(codigo);
            acciones.put(codigo, accion);
            when(accionRepository.findByCodigoAndAccesoNot(eq(codigo), any())).thenReturn(Optional.of(accion));
        }
        when(permisoRepository.findByCodigoAndAccesoNot(anyString(), any())).thenReturn(Optional.empty());
        competencias = new Permiso();
        competencias.setIdPermiso(20);
        competencias.setCodigo("COMPETENCIAS");
        when(permisoRepository.findByCodigoAndAccesoNot(eq("COMPETENCIAS"), any())).thenReturn(Optional.of(competencias));
    }

    private Acceso acceso(Long id) {
        Acceso a = new Acceso();
        a.setIdAcceso(id);
        return a;
    }

    private PermisoAccion permisoAccion(String codigoAccion) {
        PermisoAccion pa = new PermisoAccion();
        pa.setPermiso(competencias);
        pa.setAccion(acciones.get(codigoAccion));
        pa.setAcceso(activo);
        when(permisoAccionRepository.findByPermisoIdPermisoAndAccionIdAccion(eq(20), eq(acciones.get(codigoAccion).getIdAccion())))
                .thenReturn(Optional.of(pa));
        return pa;
    }

    @Test
    void quitaLasAccionesQueSobranTambienDeLosRolesYEtiquetaLasQueQuedan() {
        // Competencias venia sembrado con crear, actualizar y eliminar.
        PermisoAccion crear = permisoAccion("CREAR");
        PermisoAccion actualizar = permisoAccion("ACTUALIZAR");
        PermisoAccion eliminar = permisoAccion("ELIMINAR");
        when(permisoAccionRepository.findByPermisoIdPermisoAndAccesoNot(eq(20), any()))
                .thenReturn(List.of(crear, actualizar, eliminar));
        RolPermiso secretaria = new RolPermiso();
        secretaria.setIdRolPermiso(7);
        when(rolPermisoRepository.findByPermisoIdPermiso(20)).thenReturn(List.of(secretaria));
        RolPermisoAccion crearConcedido = new RolPermisoAccion();
        crearConcedido.setAcceso(activo);
        when(rolPermisoAccionRepository.findByRolPermisoIdRolPermisoAndAccionIdAccionAndAccesoNot(
                eq(7), eq(acciones.get("CREAR").getIdAccion()), any())).thenReturn(Optional.of(crearConcedido));

        sync.sincronizar();

        assertEquals(AccesoConstants.ELIMINADO, crear.getAcceso().getIdAcceso());
        assertEquals(AccesoConstants.ELIMINADO, eliminar.getAcceso().getIdAcceso());
        assertEquals(AccesoConstants.ACTIVO, actualizar.getAcceso().getIdAcceso());
        assertEquals("Activar y desactivar", actualizar.getEtiqueta());
        assertEquals(AccesoConstants.ELIMINADO, crearConcedido.getAcceso().getIdAcceso());
    }

    @Test
    void agregaLaAccionQueFaltaConSuEtiqueta() {
        when(permisoAccionRepository.findByPermisoIdPermisoAndAccionIdAccion(anyInt(), anyInt())).thenReturn(Optional.empty());
        when(permisoAccionRepository.findByPermisoIdPermisoAndAccesoNot(eq(20), any())).thenReturn(List.of());

        sync.sincronizar();

        ArgumentCaptor<PermisoAccion> captor = ArgumentCaptor.forClass(PermisoAccion.class);
        verify(permisoAccionRepository, atLeastOnce()).save(captor.capture());
        assertTrue(captor.getAllValues().stream().anyMatch(pa -> pa.getPermiso() == competencias
                && pa.getAccion().getCodigo().equals("ACTUALIZAR")
                && "Activar y desactivar".equals(pa.getEtiqueta())
                && pa.getAcceso() == activo));
    }
}
