package com.example.Escolar.Service;

import com.example.Escolar.Dto.RolPermisoRequest;
import com.example.Escolar.Dto.RolPermisoResponse;
import com.example.Escolar.Model.Acceso;
import com.example.Escolar.Model.Modulo;
import com.example.Escolar.Model.Permiso;
import com.example.Escolar.Model.Rol;
import com.example.Escolar.Model.RolPermiso;
import com.example.Escolar.Repository.AccionRepository;
import com.example.Escolar.Repository.AccesoRepository;
import com.example.Escolar.Repository.PermisoAccionRepository;
import com.example.Escolar.Repository.PermisoRepository;
import com.example.Escolar.Repository.RolPermisoAccionRepository;
import com.example.Escolar.Repository.RolPermisoRepository;
import com.example.Escolar.Repository.RolRepository;
import com.example.Escolar.Repository.UsuarioRolRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RolPermisoServiceTest {

    @Mock
    private RolPermisoRepository rolPermisoRepository;
    @Mock
    private RolPermisoAccionRepository rolPermisoAccionRepository;
    @Mock
    private RolRepository rolRepository;
    @Mock
    private PermisoRepository permisoRepository;
    @Mock
    private AccionRepository accionRepository;
    @Mock
    private PermisoAccionRepository permisoAccionRepository;
    @Mock
    private UsuarioRolRepository usuarioRolRepository;
    @Mock
    private AccesoRepository accesoRepository;

    @InjectMocks
    private RolPermisoService service;

    private Acceso mockAcceso(Long id) {
        Acceso acceso = new Acceso();
        acceso.setIdAcceso(id);
        acceso.setNombre(id.equals(1L) ? "Activo" : "Eliminado");
        return acceso;
    }

    private void stubAccesoRepository() {
        when(accesoRepository.findById(AccesoConstants.ELIMINADO)).thenReturn(Optional.of(mockAcceso(AccesoConstants.ELIMINADO)));
        when(accesoRepository.findById(AccesoConstants.ACTIVO)).thenReturn(Optional.of(mockAcceso(AccesoConstants.ACTIVO)));
    }

    private Rol rol() {
        Rol rol = new Rol();
        rol.setIdRol(1);
        rol.setNombre("Administrador");
        rol.setAcceso(mockAcceso(AccesoConstants.ACTIVO));
        return rol;
    }

    private Permiso permiso() {
        Modulo modulo = new Modulo();
        modulo.setIdModulo(1);
        modulo.setModulo("Usuarios");
        modulo.setIcono("users");
        modulo.setAcceso(mockAcceso(AccesoConstants.ACTIVO));

        Permiso permiso = new Permiso();
        permiso.setIdPermiso(1);
        permiso.setCodigo("USUARIOS");
        permiso.setNombre("Usuarios");
        permiso.setModulo(modulo);
        permiso.setAcceso(mockAcceso(AccesoConstants.ACTIVO));
        return permiso;
    }

    private RolPermiso rolPermiso(Integer id, Long accesoId) {
        RolPermiso rp = new RolPermiso();
        rp.setIdRolPermiso(id);
        rp.setRol(rol());
        rp.setPermiso(permiso());
        rp.setAcceso(mockAcceso(accesoId));
        return rp;
    }

    private RolPermisoRequest request() {
        RolPermisoRequest request = new RolPermisoRequest();
        request.setIdRol(1);
        request.setIdPermiso(1);
        return request;
    }

    @Test
    void crearNuevoRolPermisoDevuelveResponse() {
        stubAccesoRepository();
        when(rolRepository.findByIdRolAndAccesoNot(any(Integer.class), any(Acceso.class))).thenReturn(Optional.of(rol()));
        when(permisoRepository.findByIdPermisoAndAccesoNot(any(Integer.class), any(Acceso.class))).thenReturn(Optional.of(permiso()));
        when(rolPermisoRepository.findByRolIdRolAndPermisoIdPermiso(1, 1)).thenReturn(Optional.empty());
        when(rolPermisoRepository.save(any(RolPermiso.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        RolPermisoResponse response = service.create(request());

        assertNotNull(response);
        assertEquals(AccesoConstants.ACTIVO, response.getAccesoId());
        assertEquals("Administrador", response.getRol().getNombre());
        assertEquals("Usuarios", response.getPermiso().getNombre());
    }

    @Test
    void crearConDuplicadoActivoLanzaExcepcion() {
        stubAccesoRepository();
        when(rolRepository.findByIdRolAndAccesoNot(any(Integer.class), any(Acceso.class))).thenReturn(Optional.of(rol()));
        when(permisoRepository.findByIdPermisoAndAccesoNot(any(Integer.class), any(Acceso.class))).thenReturn(Optional.of(permiso()));
        when(rolPermisoRepository.findByRolIdRolAndPermisoIdPermiso(1, 1))
                .thenReturn(Optional.of(rolPermiso(9, AccesoConstants.ACTIVO)));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service.create(request()));
        assertEquals("El permiso ya está asignado a este rol", ex.getMessage());
    }

    @Test
    void crearCuandoExisteEliminadoReactivarEnVezDeDuplicar() {
        stubAccesoRepository();
        RolPermiso eliminado = rolPermiso(9, AccesoConstants.ELIMINADO);

        when(rolRepository.findByIdRolAndAccesoNot(any(Integer.class), any(Acceso.class))).thenReturn(Optional.of(rol()));
        when(permisoRepository.findByIdPermisoAndAccesoNot(any(Integer.class), any(Acceso.class))).thenReturn(Optional.of(permiso()));
        when(rolPermisoRepository.findByRolIdRolAndPermisoIdPermiso(1, 1)).thenReturn(Optional.of(eliminado));
        when(rolPermisoRepository.save(any(RolPermiso.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        RolPermisoResponse response = service.create(request());

        assertEquals(9, response.getIdRolPermiso());
        assertEquals(AccesoConstants.ACTIVO, response.getAccesoId());
        verify(rolPermisoRepository).save(eliminado);
    }
}
