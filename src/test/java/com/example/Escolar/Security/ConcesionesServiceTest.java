package com.example.Escolar.Security;

import com.example.Escolar.Model.Accion;
import com.example.Escolar.Model.Acceso;
import com.example.Escolar.Model.Modulo;
import com.example.Escolar.Model.Permiso;
import com.example.Escolar.Model.Rol;
import com.example.Escolar.Model.RolPermiso;
import com.example.Escolar.Model.RolPermisoAccion;
import com.example.Escolar.Model.Usuario;
import com.example.Escolar.Model.UsuarioRol;
import com.example.Escolar.Repository.RolPermisoAccionRepository;
import com.example.Escolar.Repository.RolPermisoRepository;
import com.example.Escolar.Repository.UsuarioRepository;
import com.example.Escolar.Repository.UsuarioRolRepository;
import com.example.Escolar.Service.AccesoConstants;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConcesionesServiceTest {

    @Mock
    private UsuarioRolRepository usuarioRolRepository;
    @Mock
    private RolPermisoRepository rolPermisoRepository;
    @Mock
    private RolPermisoAccionRepository rolPermisoAccionRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @InjectMocks
    private ConcesionesService service;

    private Acceso acceso(Long id) {
        return new Acceso(id, id.equals(AccesoConstants.ACTIVO) ? "Activo" : "Inactivo");
    }

    private Rol rol(Integer id, Long accesoId) {
        Rol rol = new Rol();
        rol.setIdRol(id);
        rol.setNombre("ROL " + id);
        rol.setAcceso(acceso(accesoId));
        return rol;
    }

    private Permiso permiso(String codigo, Long accesoId) {
        Modulo modulo = new Modulo();
        modulo.setAcceso(acceso(AccesoConstants.ACTIVO));
        Permiso permiso = new Permiso();
        permiso.setCodigo(codigo);
        permiso.setAcceso(acceso(accesoId));
        permiso.setModulo(modulo);
        return permiso;
    }

    private RolPermiso rolPermiso(Integer id, Rol rol, Permiso permiso) {
        RolPermiso rolPermiso = new RolPermiso();
        rolPermiso.setIdRolPermiso(id);
        rolPermiso.setRol(rol);
        rolPermiso.setPermiso(permiso);
        rolPermiso.setAcceso(acceso(AccesoConstants.ACTIVO));
        return rolPermiso;
    }

    private UsuarioRol usuarioRol(Rol rol) {
        UsuarioRol usuarioRol = new UsuarioRol();
        usuarioRol.setRol(rol);
        return usuarioRol;
    }

    private UsuarioRol usuarioRol(Rol rol, Integer idUsuario, Long accesoId) {
        Usuario usuario = new Usuario();
        usuario.setIdUsuario(idUsuario);
        usuario.setAcceso(acceso(accesoId));
        UsuarioRol usuarioRol = new UsuarioRol();
        usuarioRol.setRol(rol);
        usuarioRol.setUsuario(usuario);
        return usuarioRol;
    }

    @Test
    void rolInactivoNoAportaPermisos() {
        Rol inactivo = rol(10, AccesoConstants.INACTIVO);
        Rol activo = rol(11, AccesoConstants.ACTIVO);
        when(usuarioRolRepository.findByUsuarioIdUsuario(1))
                .thenReturn(List.of(usuarioRol(inactivo), usuarioRol(activo)));
        when(rolPermisoRepository.findByRolIdRol(11))
                .thenReturn(List.of(rolPermiso(2, activo, permiso("NOTAS", AccesoConstants.ACTIVO))));

        Map<String, Set<String>> resultado = service.accionesPorPermiso(1);

        assertTrue(resultado.containsKey("NOTAS"));
        assertFalse(resultado.containsKey("USUARIOS"));
    }

    @Test
    void permisoInactivoNoAportaPermisos() {
        Rol activo = rol(11, AccesoConstants.ACTIVO);
        when(usuarioRolRepository.findByUsuarioIdUsuario(1)).thenReturn(List.of(usuarioRol(activo)));
        when(rolPermisoRepository.findByRolIdRol(11))
                .thenReturn(List.of(rolPermiso(2, activo, permiso("NOTAS", AccesoConstants.INACTIVO))));

        Map<String, Set<String>> resultado = service.accionesPorPermiso(1);

        assertTrue(resultado.isEmpty());
    }

    @Test
    void accionInactivaNoAportaAcciones() {
        Rol activo = rol(11, AccesoConstants.ACTIVO);
        RolPermiso rolPermiso = rolPermiso(2, activo, permiso("NOTAS", AccesoConstants.ACTIVO));
        Accion accion = new Accion();
        accion.setCodigo("ACTUALIZAR");
        accion.setAcceso(acceso(AccesoConstants.ACTIVO));
        RolPermisoAccion concedida = new RolPermisoAccion();
        concedida.setAcceso(acceso(AccesoConstants.INACTIVO));
        concedida.setAccion(accion);
        when(usuarioRolRepository.findByUsuarioIdUsuario(1)).thenReturn(List.of(usuarioRol(activo)));
        when(rolPermisoRepository.findByRolIdRol(11)).thenReturn(List.of(rolPermiso));
        when(rolPermisoAccionRepository.findByRolPermisoIdRolPermiso(2)).thenReturn(List.of(concedida));

        Map<String, Set<String>> resultado = service.accionesPorPermiso(1);

        assertEquals(Set.of(), resultado.get("NOTAS"));
    }

    @Test
    void rolPermisoInactivoNoAportaPermisos() {
        Rol activo = rol(11, AccesoConstants.ACTIVO);
        RolPermiso rolPermiso = rolPermiso(2, activo, permiso("NOTAS", AccesoConstants.ACTIVO));
        rolPermiso.setAcceso(acceso(AccesoConstants.INACTIVO));
        when(usuarioRolRepository.findByUsuarioIdUsuario(1)).thenReturn(List.of(usuarioRol(activo)));
        when(rolPermisoRepository.findByRolIdRol(11)).thenReturn(List.of(rolPermiso));

        Map<String, Set<String>> resultado = service.accionesPorPermiso(1);

        assertTrue(resultado.isEmpty());
    }

    @Test
    void accionConAccesoInactivoNoAportaAcciones() {
        Rol activo = rol(11, AccesoConstants.ACTIVO);
        RolPermiso rolPermiso = rolPermiso(2, activo, permiso("NOTAS", AccesoConstants.ACTIVO));
        Accion accion = new Accion();
        accion.setCodigo("ACTUALIZAR");
        accion.setAcceso(acceso(AccesoConstants.INACTIVO));
        RolPermisoAccion concedida = new RolPermisoAccion();
        concedida.setAcceso(acceso(AccesoConstants.ACTIVO));
        concedida.setAccion(accion);
        when(usuarioRolRepository.findByUsuarioIdUsuario(1)).thenReturn(List.of(usuarioRol(activo)));
        when(rolPermisoRepository.findByRolIdRol(11)).thenReturn(List.of(rolPermiso));
        when(rolPermisoAccionRepository.findByRolPermisoIdRolPermiso(2)).thenReturn(List.of(concedida));

        Map<String, Set<String>> resultado = service.accionesPorPermiso(1);

        assertEquals(Set.of(), resultado.get("NOTAS"));
    }

    @Test
    void invalidarCambioRolesSubeVersionYRompeCache() {
        when(usuarioRolRepository.findByUsuarioIdUsuario(1)).thenReturn(List.of());

        service.accionesPorPermiso(1);
        service.invalidarCambioRoles(1);
        service.accionesPorPermiso(1);

        verify(usuarioRepository).incrementarVersionToken(Set.of(1), AccesoConstants.ELIMINADO);
        verify(usuarioRolRepository, org.mockito.Mockito.times(2)).findByUsuarioIdUsuario(1);
    }

    @Test
    void invalidarPermisosDelRolActualizaSoloSusUsuariosEnLote() {
        Rol rol = rol(11, AccesoConstants.ACTIVO);
        when(usuarioRolRepository.findByRolIdRol(11)).thenReturn(List.of(
                usuarioRol(rol, 1, AccesoConstants.ACTIVO),
                usuarioRol(rol, 1, AccesoConstants.ACTIVO),
                usuarioRol(rol, 2, AccesoConstants.ELIMINADO)));

        service.invalidarPermisosDelRol(11);

        verify(usuarioRepository).incrementarVersionToken(eq(Set.of(1)), eq(AccesoConstants.ELIMINADO));
        verify(usuarioRepository, never()).findAll();
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void invalidarPermisosDelPermisoActualizaSoloUsuariosDeSusRoles() {
        Rol rol = rol(11, AccesoConstants.ACTIVO);
        RolPermiso rolPermiso = rolPermiso(2, rol, permiso("NOTAS", AccesoConstants.ACTIVO));
        when(rolPermisoRepository.findByPermisoIdPermiso(3)).thenReturn(List.of(rolPermiso));
        when(usuarioRolRepository.findByRolIdRol(11)).thenReturn(List.of(
                usuarioRol(rol, 4, AccesoConstants.ACTIVO)));

        service.invalidarPermisosDelPermiso(3);

        verify(usuarioRepository).incrementarVersionToken(eq(Set.of(4)), eq(AccesoConstants.ELIMINADO));
        verify(usuarioRepository, never()).findAll();
    }
}
