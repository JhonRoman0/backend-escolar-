package com.example.Escolar.Security;

import com.example.Escolar.Dto.RolAsignadoSesionResponse;
import com.example.Escolar.Dto.RolAsignadoSesionResponse.EstadoRol;
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
import com.example.Escolar.Service.RolAsignadoResolver;
import org.junit.jupiter.api.BeforeEach;
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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
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
    @Mock
    private RolAsignadoResolver rolAsignadoResolver;
    @InjectMocks
    private ConcesionesService service;

    @BeforeEach
    void sinRolesActivosPorDefecto() {
        lenient().when(rolAsignadoResolver.resolver(eq(1), anyList())).thenReturn(List.of());
    }

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

    private Accion accion(String codigo) {
        Accion accion = new Accion();
        accion.setCodigo(codigo);
        accion.setAcceso(acceso(AccesoConstants.ACTIVO));
        return accion;
    }

    private RolPermisoAccion concedida(Accion accion) {
        RolPermisoAccion concedida = new RolPermisoAccion();
        concedida.setAcceso(acceso(AccesoConstants.ACTIVO));
        concedida.setAccion(accion);
        return concedida;
    }

    private RolAsignadoSesionResponse estado(Rol rol, EstadoRol estado) {
        return new RolAsignadoSesionResponse(rol.getIdRol(), rol.getNombre(), estado);
    }

    @Test
    void rolInactivoNoAportaPermisos() {
        Rol inactivo = rol(10, AccesoConstants.INACTIVO);
        Rol activo = rol(11, AccesoConstants.ACTIVO);
        when(usuarioRolRepository.findByUsuarioIdUsuario(1))
                .thenReturn(List.of(usuarioRol(inactivo), usuarioRol(activo)));
        when(rolPermisoRepository.findByRolIdRol(11))
                .thenReturn(List.of(rolPermiso(2, activo, permiso("NOTAS", AccesoConstants.ACTIVO))));
        when(rolAsignadoResolver.resolver(eq(1), anyList()))
                .thenReturn(List.of(estado(activo, EstadoRol.ACTIVO)));

        Map<String, Set<String>> resultado = service.accionesPorPermiso(1, 11);

        assertTrue(resultado.containsKey("NOTAS"));
        assertFalse(resultado.containsKey("USUARIOS"));
        assertFalse(service.rolActivoValido(1, 10));
    }

    @Test
    void permisoInactivoNoAportaPermisos() {
        Rol activo = rol(11, AccesoConstants.ACTIVO);
        when(usuarioRolRepository.findByUsuarioIdUsuario(1)).thenReturn(List.of(usuarioRol(activo)));
        when(rolPermisoRepository.findByRolIdRol(11))
                .thenReturn(List.of(rolPermiso(2, activo, permiso("NOTAS", AccesoConstants.INACTIVO))));
        when(rolAsignadoResolver.resolver(eq(1), anyList()))
                .thenReturn(List.of(estado(activo, EstadoRol.ACTIVO)));

        Map<String, Set<String>> resultado = service.accionesPorPermiso(1, 11);

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
        when(rolAsignadoResolver.resolver(eq(1), anyList()))
                .thenReturn(List.of(estado(activo, EstadoRol.ACTIVO)));

        Map<String, Set<String>> resultado = service.accionesPorPermiso(1, 11);

        assertEquals(Set.of(), resultado.get("NOTAS"));
    }

    @Test
    void rolPermisoInactivoNoAportaPermisos() {
        Rol activo = rol(11, AccesoConstants.ACTIVO);
        RolPermiso rolPermiso = rolPermiso(2, activo, permiso("NOTAS", AccesoConstants.ACTIVO));
        rolPermiso.setAcceso(acceso(AccesoConstants.INACTIVO));
        when(usuarioRolRepository.findByUsuarioIdUsuario(1)).thenReturn(List.of(usuarioRol(activo)));
        when(rolPermisoRepository.findByRolIdRol(11)).thenReturn(List.of(rolPermiso));
        when(rolAsignadoResolver.resolver(eq(1), anyList()))
                .thenReturn(List.of(estado(activo, EstadoRol.ACTIVO)));

        Map<String, Set<String>> resultado = service.accionesPorPermiso(1, 11);

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
        when(rolAsignadoResolver.resolver(eq(1), anyList()))
                .thenReturn(List.of(estado(activo, EstadoRol.ACTIVO)));

        Map<String, Set<String>> resultado = service.accionesPorPermiso(1, 11);

        assertEquals(Set.of(), resultado.get("NOTAS"));
    }

    @Test
    void invalidarCambioRolesSubeVersionYRompeCache() {
        when(usuarioRolRepository.findByUsuarioIdUsuario(1)).thenReturn(List.of());

        service.accionesPorPermiso(1, 11);
        service.invalidarCambioRoles(1);
        service.accionesPorPermiso(1, 11);

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

    @Test
    void concesionesCaducanPorTiempoSinInvalidacionManual() throws InterruptedException {
        Rol activo = rol(11, AccesoConstants.ACTIVO);
        when(usuarioRolRepository.findByUsuarioIdUsuario(1)).thenReturn(List.of(usuarioRol(activo)));
        when(rolAsignadoResolver.resolver(eq(1), anyList()))
                .thenReturn(List.of(estado(activo, EstadoRol.ACTIVO)),
                        List.of(estado(activo, EstadoRol.SUSPENDIDO)));

        assertTrue(service.rolActivoValido(1, 11));

        Thread.sleep(ConcesionesService.VIGENCIA_MS + 200);

        assertFalse(service.rolActivoValido(1, 11));
        verify(rolAsignadoResolver, times(2)).resolver(eq(1), anyList());
        verify(usuarioRepository, never()).incrementarVersionToken(any(), any());
    }

    @Test
    void accionesPorPermisoDelRolActivoNoMezclaRoles() {
        Rol activo = rol(11, AccesoConstants.ACTIVO);
        Rol otro = rol(12, AccesoConstants.ACTIVO);
        when(usuarioRolRepository.findByUsuarioIdUsuario(1)).thenReturn(List.of(usuarioRol(activo), usuarioRol(otro)));
        when(rolPermisoRepository.findByRolIdRol(11))
                .thenReturn(List.of(rolPermiso(2, activo, permiso("NOTAS", AccesoConstants.ACTIVO))));
        when(rolPermisoRepository.findByRolIdRol(12))
                .thenReturn(List.of(rolPermiso(3, otro, permiso("NOTAS", AccesoConstants.ACTIVO))));
        when(rolPermisoAccionRepository.findByRolPermisoIdRolPermiso(2))
                .thenReturn(List.of(concedida(accion("ACTUALIZAR"))));
        when(rolPermisoAccionRepository.findByRolPermisoIdRolPermiso(3))
                .thenReturn(List.of(concedida(accion("ELIMINAR"))));
        when(rolAsignadoResolver.resolver(eq(1), anyList())).thenReturn(List.of(
                estado(activo, EstadoRol.ACTIVO), estado(otro, EstadoRol.ACTIVO)));

        assertEquals(Set.of("ACTUALIZAR"), service.accionesPorPermiso(1, 11).get("NOTAS"));
        assertEquals(Set.of("ELIMINAR"), service.accionesPorPermiso(1, 12).get("NOTAS"));
        assertTrue(service.rolActivoValido(1, 11));
    }

    @Test
    void sinRolActivoORolNoAsignadoDevuelveVacio() {
        Rol activo = rol(11, AccesoConstants.ACTIVO);
        when(usuarioRolRepository.findByUsuarioIdUsuario(1)).thenReturn(List.of(usuarioRol(activo)));

        assertEquals(Map.of(), service.accionesPorPermiso(1, null));
        assertEquals(Map.of(), service.accionesPorPermiso(1, 99));
        assertFalse(service.rolActivoValido(1, 99));
        assertFalse(service.esAdminActivo(1, 99));
    }

    @Test
    void rolSuspendidoNoAportaPermisos() {
        Rol docente = rol(11, AccesoConstants.ACTIVO);
        docente.setNombre("Docente");
        when(usuarioRolRepository.findByUsuarioIdUsuario(1)).thenReturn(List.of(usuarioRol(docente)));
        when(rolAsignadoResolver.resolver(eq(1), anyList()))
                .thenReturn(List.of(estado(docente, EstadoRol.SUSPENDIDO)));

        assertTrue(service.accionesPorPermiso(1, 11).isEmpty());
        assertFalse(service.rolActivoValido(1, 11));
        assertNull(service.nombreRolActivo(1, 11));
    }

    @Test
    void suspensionProgramadaYaAplicadaSeRechazaTrasRomperCacheYSubeVersionGlobal() {
        Rol docente = rol(11, AccesoConstants.ACTIVO);
        docente.setNombre("Docente");
        when(usuarioRolRepository.findByUsuarioIdUsuario(1)).thenReturn(List.of(usuarioRol(docente)));
        when(rolAsignadoResolver.resolver(eq(1), anyList()))
                .thenReturn(List.of(estado(docente, EstadoRol.ACTIVO)),
                        List.of(estado(docente, EstadoRol.SUSPENDIDO)));

        assertTrue(service.rolActivoValido(1, 11));
        service.invalidarCambioRoles(1);

        assertFalse(service.rolActivoValido(1, 11));
        verify(usuarioRepository).incrementarVersionToken(Set.of(1), AccesoConstants.ELIMINADO);
        verify(rolAsignadoResolver, times(2)).resolver(eq(1), anyList());
    }

    @Test
    void esAdminActivoSoloConRolAdminAsignadoYActivo() {
        Rol admin = rol(11, AccesoConstants.ACTIVO);
        admin.setNombre("ADMIN");
        Rol portero = rol(12, AccesoConstants.ACTIVO);
        portero.setNombre("Portero");
        when(usuarioRolRepository.findByUsuarioIdUsuario(1)).thenReturn(List.of(usuarioRol(admin), usuarioRol(portero)));
        when(rolAsignadoResolver.resolver(eq(1), anyList())).thenReturn(List.of(
                estado(admin, EstadoRol.ACTIVO), estado(portero, EstadoRol.ACTIVO)));

        assertTrue(service.esAdminActivo(1, 11));
        assertFalse(service.esAdminActivo(1, 12));
        assertFalse(service.esAdminActivo(1, null));
    }

    @Test
    void nombreRolActivoSoloDelRolAsignadoYVigente() {
        Rol docente = rol(11, AccesoConstants.ACTIVO);
        docente.setNombre("DOCENTE");
        Rol apoderado = rol(12, AccesoConstants.ACTIVO);
        apoderado.setNombre("Apoderado");
        Rol inactivo = rol(13, AccesoConstants.INACTIVO);
        inactivo.setNombre("Portero");
        when(usuarioRolRepository.findByUsuarioIdUsuario(1)).thenReturn(List.of(
                usuarioRol(docente), usuarioRol(apoderado), usuarioRol(inactivo)));
        when(rolAsignadoResolver.resolver(eq(1), anyList())).thenReturn(List.of(
                estado(docente, EstadoRol.ACTIVO),
                estado(apoderado, EstadoRol.ACTIVO),
                estado(inactivo, EstadoRol.INACTIVO)));

        assertEquals("DOCENTE", service.nombreRolActivo(1, 11));
        assertEquals("Apoderado", service.nombreRolActivo(1, 12));
        assertNull(service.nombreRolActivo(1, 13));
        assertNull(service.nombreRolActivo(1, 99));
        assertNull(service.nombreRolActivo(1, null));
    }

    @Test
    void invalidarPermisosDelRolActualizaConsultaDelRolActivo() {
        Rol activo = rol(11, AccesoConstants.ACTIVO);
        when(usuarioRolRepository.findByUsuarioIdUsuario(1)).thenReturn(List.of(usuarioRol(activo)));
        when(usuarioRolRepository.findByRolIdRol(11)).thenReturn(List.of(usuarioRol(activo, 1, AccesoConstants.ACTIVO)));
        when(rolPermisoRepository.findByRolIdRol(11))
                .thenReturn(List.of(rolPermiso(2, activo, permiso("NOTAS", AccesoConstants.ACTIVO))));
        when(rolPermisoAccionRepository.findByRolPermisoIdRolPermiso(2))
                .thenReturn(List.of(concedida(accion("ACTUALIZAR"))));
        when(rolAsignadoResolver.resolver(eq(1), anyList()))
                .thenReturn(List.of(estado(activo, EstadoRol.ACTIVO)));

        service.accionesPorPermiso(1, 11);
        service.invalidarPermisosDelRol(11);
        service.accionesPorPermiso(1, 11);

        verify(usuarioRolRepository, times(2)).findByUsuarioIdUsuario(1);
    }
}
