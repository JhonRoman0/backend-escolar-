package com.example.Escolar.Service;

import com.example.Escolar.Dto.RolAsignadoSesionResponse;
import com.example.Escolar.Dto.RolAsignadoSesionResponse.EstadoRol;
import com.example.Escolar.Model.Acceso;
import com.example.Escolar.Model.Docente;
import com.example.Escolar.Model.EstadoSuspension;
import com.example.Escolar.Model.Rol;
import com.example.Escolar.Model.SuspensionDocente;
import com.example.Escolar.Model.UsuarioRol;
import com.example.Escolar.Repository.AccesoRepository;
import com.example.Escolar.Repository.DocenteRepository;
import com.example.Escolar.Repository.SuspensionDocenteRepository;
import com.example.Escolar.Repository.UsuarioRolRepository;
import com.example.Escolar.Util.FechaColegio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RolAsignadoResolverTest {

    private static final Integer ID_USUARIO = 1;

    @Mock
    private UsuarioRolRepository usuarioRolRepository;
    @Mock
    private DocenteRepository docenteRepository;
    @Mock
    private SuspensionDocenteRepository suspensionDocenteRepository;
    @Mock
    private AccesoRepository accesoRepository;

    @InjectMocks
    private RolAsignadoResolver resolver;

    private LocalDate hoy;
    private Acceso activo;
    private Acceso inactivo;
    private Acceso eliminado;

    @BeforeEach
    void setUp() {
        hoy = FechaColegio.hoy();
        activo = acceso(AccesoConstants.ACTIVO);
        inactivo = acceso(AccesoConstants.INACTIVO);
        eliminado = acceso(AccesoConstants.ELIMINADO);
        when(accesoRepository.findById(AccesoConstants.ELIMINADO)).thenReturn(Optional.of(eliminado));
    }

    @Test
    void excluyeRolEliminadoAunqueTengaSuspensionAplicable() {
        Docente docente = docente(10, activo);
        Rol rolDocente = rol(1, "DOCENTE", eliminado);
        stub(List.of(asignacion(rolDocente)), docente,
                List.of(suspension(docente, EstadoSuspension.VIGENTE, hoy, null, activo)));

        assertEquals(List.of(), resolver.resolver(ID_USUARIO));
        verify(docenteRepository, never()).findByUsuarioIdUsuario(ID_USUARIO);
        verify(suspensionDocenteRepository, never()).findByDocenteAndAccesoNot(any(), any());
    }

    @Test
    void rolDocenteConAccesoDesconocidoQuedaInactivoSinConsultarDocente() {
        Rol rolDocente = rol(1, "DOCENTE", acceso(99L));
        stubAsignaciones(List.of(asignacion(rolDocente)));

        assertEquals(List.of(EstadoRol.INACTIVO), estados(resolver.resolver(ID_USUARIO)));
        verify(docenteRepository, never()).findByUsuarioIdUsuario(ID_USUARIO);
        verify(suspensionDocenteRepository, never()).findByDocenteAndAccesoNot(any(), any());
    }

    @Test
    void rolDocenteConAccesoNullQuedaInactivoSinConsultarDocente() {
        Rol rolDocente = rol(1, "DOCENTE", null);
        stubAsignaciones(List.of(asignacion(rolDocente)));

        assertEquals(List.of(EstadoRol.INACTIVO), estados(resolver.resolver(ID_USUARIO)));
        verify(docenteRepository, never()).findByUsuarioIdUsuario(ID_USUARIO);
        verify(suspensionDocenteRepository, never()).findByDocenteAndAccesoNot(any(), any());
    }

    @Test
    void rolNoDocenteActivoNoConsultaDocente() {
        Rol admin = rol(1, "ADMIN", activo);
        stubAsignaciones(List.of(asignacion(admin)));

        assertEquals(List.of(EstadoRol.ACTIVO), estados(resolver.resolver(ID_USUARIO)));
        verify(docenteRepository, never()).findByUsuarioIdUsuario(ID_USUARIO);
        verify(suspensionDocenteRepository, never()).findByDocenteAndAccesoNot(any(), any());
    }

    @Test
    void rolInactivoGanaSobreSuspensionVigente() {
        Docente docente = docente(10, activo);
        Rol rolDocente = rol(1, "Docente", inactivo);
        stub(List.of(asignacion(rolDocente)), docente,
                List.of(suspension(docente, EstadoSuspension.VIGENTE, hoy, null, activo)));

        assertEquals(List.of(EstadoRol.INACTIVO), estados(resolver.resolver(ID_USUARIO)));
    }

    @Test
    void rolDocenteInactivoNoConsultaDocente() {
        Rol rolDocente = rol(1, "DOCENTE", inactivo);
        stubAsignaciones(List.of(asignacion(rolDocente)));

        assertEquals(List.of(EstadoRol.INACTIVO), estados(resolver.resolver(ID_USUARIO)));
        verify(docenteRepository, never()).findByUsuarioIdUsuario(ID_USUARIO);
        verify(suspensionDocenteRepository, never()).findByDocenteAndAccesoNot(any(), any());
    }

    @Test
    void suspensionVigenteConvierteDocenteInactivoEnSuspendido() {
        Docente docente = docente(10, inactivo);
        Rol rolDocente = rol(1, "DOCENTE", activo);
        stub(List.of(asignacion(rolDocente)), docente,
                List.of(suspension(docente, EstadoSuspension.VIGENTE, hoy, null, activo)));

        assertEquals(List.of(EstadoRol.SUSPENDIDO), estados(resolver.resolver(ID_USUARIO)));
        assertEquals(inactivo, docente.getAcceso());
    }

    @Test
    void suspensionVigenteConFechaFinPasadaSigueSuspendiendo() {
        Docente docente = docente(10, activo);
        Rol rolDocente = rol(1, "DOCENTE", activo);
        stub(List.of(asignacion(rolDocente)), docente,
                List.of(suspension(docente, EstadoSuspension.VIGENTE, hoy.minusDays(10), hoy.minusDays(1), activo)));

        assertEquals(List.of(EstadoRol.SUSPENDIDO), estados(resolver.resolver(ID_USUARIO)));
    }

    @Test
    void suspensionProgramadaConFechaInicioAlcanzadaSuspende() {
        Docente docente = docente(10, activo);
        Rol rolDocente = rol(1, "DOCENTE", activo);
        stub(List.of(asignacion(rolDocente)), docente,
                List.of(suspension(docente, EstadoSuspension.PROGRAMADA, hoy, null, activo)));

        assertEquals(List.of(EstadoRol.SUSPENDIDO), estados(resolver.resolver(ID_USUARIO)));
    }

    @Test
    void suspensionProgramadaFuturaNoSuspende() {
        Docente docente = docente(10, activo);
        Rol rolDocente = rol(1, "DOCENTE", activo);
        stub(List.of(asignacion(rolDocente)), docente,
                List.of(suspension(docente, EstadoSuspension.PROGRAMADA, hoy.plusDays(1), null, activo)));

        assertEquals(List.of(EstadoRol.ACTIVO), estados(resolver.resolver(ID_USUARIO)));
    }

    @Test
    void suspensionConEstadoNullBloqueaPreventivamente() {
        Docente docente = docente(10, activo);
        Rol rolDocente = rol(1, "DOCENTE", activo);
        stub(List.of(asignacion(rolDocente)), docente,
                List.of(suspension(docente, null, hoy, null, activo)));

        assertEquals(List.of(EstadoRol.SUSPENDIDO), estados(resolver.resolver(ID_USUARIO)));
    }

    @Test
    void suspensionEliminadaConEstadoNullNoBloquea() {
        Docente docente = docente(10, activo);
        Rol rolDocente = rol(1, "DOCENTE", activo);
        stub(List.of(asignacion(rolDocente)), docente,
                List.of(suspension(docente, null, hoy, null, eliminado)));

        assertEquals(List.of(EstadoRol.ACTIVO), estados(resolver.resolver(ID_USUARIO)));
    }

    @Test
    void suspensionFinalizadaOCanceladaNoSuspende() {
        Docente docente = docente(10, activo);
        Rol rolDocente = rol(1, "DOCENTE", activo);
        stub(List.of(asignacion(rolDocente)), docente, List.of(
                suspension(docente, EstadoSuspension.FINALIZADA, hoy, hoy, activo),
                suspension(docente, EstadoSuspension.CANCELADA, hoy, hoy, activo)));

        assertEquals(List.of(EstadoRol.ACTIVO), estados(resolver.resolver(ID_USUARIO)));
    }

    @Test
    void docenteInexistenteEsInactivo() {
        Rol rolDocente = rol(1, "DOCENTE", activo);
        stub(List.of(asignacion(rolDocente)), null, List.of());

        assertEquals(List.of(EstadoRol.INACTIVO), estados(resolver.resolver(ID_USUARIO)));
        verify(docenteRepository).findByUsuarioIdUsuario(ID_USUARIO);
        verify(suspensionDocenteRepository, never()).findByDocenteAndAccesoNot(any(), any());
    }

    @Test
    void docenteInactivoSinSuspensionEsInactivo() {
        Docente docente = docente(10, inactivo);
        Rol rolDocente = rol(1, "DOCENTE", activo);
        stub(List.of(asignacion(rolDocente)), docente, List.of());

        assertEquals(List.of(EstadoRol.INACTIVO), estados(resolver.resolver(ID_USUARIO)));
    }

    @Test
    void docenteEliminadoSinSuspensionEsInactivo() {
        Docente docente = docente(10, eliminado);
        Rol rolDocente = rol(1, "DOCENTE", activo);
        stub(List.of(asignacion(rolDocente)), docente, List.of());

        assertEquals(List.of(EstadoRol.INACTIVO), estados(resolver.resolver(ID_USUARIO)));
    }

    @Test
    void docenteConAccesoNullSinSuspensionEsInactivo() {
        Docente docente = docente(10, null);
        Rol rolDocente = rol(1, "DOCENTE", activo);
        stub(List.of(asignacion(rolDocente)), docente, List.of());

        assertEquals(List.of(EstadoRol.INACTIVO), estados(resolver.resolver(ID_USUARIO)));
    }

    @Test
    void docenteConAccesoDesconocidoSinSuspensionEsInactivo() {
        Docente docente = docente(10, acceso(99L));
        Rol rolDocente = rol(1, "DOCENTE", activo);
        stub(List.of(asignacion(rolDocente)), docente, List.of());

        assertEquals(List.of(EstadoRol.INACTIVO), estados(resolver.resolver(ID_USUARIO)));
    }

    @Test
    void suspensionVigenteConDocenteAccesoDesconocidoEsSuspendido() {
        Docente docente = docente(10, acceso(99L));
        Rol rolDocente = rol(1, "DOCENTE", activo);
        stub(List.of(asignacion(rolDocente)), docente,
                List.of(suspension(docente, EstadoSuspension.VIGENTE, hoy, null, activo)));

        assertEquals(List.of(EstadoRol.SUSPENDIDO), estados(resolver.resolver(ID_USUARIO)));
    }

    @Test
    void suspensionConAccesoDesconocidoBloquea() {
        Docente docente = docente(10, activo);
        Rol rolDocente = rol(1, "DOCENTE", activo);
        stub(List.of(asignacion(rolDocente)), docente,
                List.of(suspension(docente, EstadoSuspension.VIGENTE, hoy, null, acceso(99L))));

        assertEquals(List.of(EstadoRol.SUSPENDIDO), estados(resolver.resolver(ID_USUARIO)));
    }

    @Test
    void suspensionDelSustitutoNoAfectaAlTitular() {
        Docente titular = docente(10, activo);
        Docente sustituto = docente(11, activo);
        Rol rolDocente = rol(1, "DOCENTE", activo);
        stub(List.of(asignacion(rolDocente)), titular,
                List.of(suspension(sustituto, EstadoSuspension.VIGENTE, hoy, null, activo)));

        assertEquals(List.of(EstadoRol.ACTIVO), estados(resolver.resolver(ID_USUARIO)));
    }

    @Test
    void conservaEstadosDeOtrosRolesYNoEscribe() {
        Docente docente = docente(10, activo);
        Rol admin = rol(1, "ADMIN", activo);
        Rol apoderado = rol(2, "Apoderado", inactivo);
        Rol rolDocente = rol(3, "Docente", activo);
        stub(List.of(asignacion(admin), asignacion(apoderado), asignacion(rolDocente)), docente,
                List.of(suspension(docente, EstadoSuspension.VIGENTE, hoy, null, activo)));

        List<RolAsignadoSesionResponse> respuesta = resolver.resolver(ID_USUARIO);

        assertEquals(List.of(EstadoRol.ACTIVO, EstadoRol.INACTIVO, EstadoRol.SUSPENDIDO), estados(respuesta));
        assertEquals(new RolAsignadoSesionResponse(3, "Docente", EstadoRol.SUSPENDIDO), respuesta.get(2));
        verify(usuarioRolRepository, never()).save(any());
        verify(docenteRepository, never()).save(any());
        verify(suspensionDocenteRepository, never()).save(any());
    }

    private void stub(List<UsuarioRol> asignaciones, Docente docente, List<SuspensionDocente> suspensiones) {
        stubAsignaciones(asignaciones);
        when(docenteRepository.findByUsuarioIdUsuario(ID_USUARIO)).thenReturn(Optional.ofNullable(docente));
        if (docente != null) {
            when(suspensionDocenteRepository.findByDocenteAndAccesoNot(docente, eliminado)).thenReturn(suspensiones);
        }
    }

    private void stubAsignaciones(List<UsuarioRol> asignaciones) {
        when(usuarioRolRepository.findByUsuarioIdUsuario(ID_USUARIO)).thenReturn(asignaciones);
    }

    private List<EstadoRol> estados(List<RolAsignadoSesionResponse> respuesta) {
        return respuesta.stream().map(RolAsignadoSesionResponse::estado).toList();
    }

    private Acceso acceso(Long id) {
        Acceso acceso = new Acceso();
        acceso.setIdAcceso(id);
        acceso.setNombre("Acceso " + id);
        return acceso;
    }

    private Rol rol(Integer id, String nombre, Acceso acceso) {
        Rol rol = new Rol();
        rol.setIdRol(id);
        rol.setNombre(nombre);
        rol.setAcceso(acceso);
        return rol;
    }

    private UsuarioRol asignacion(Rol rol) {
        UsuarioRol asignacion = new UsuarioRol();
        asignacion.setRol(rol);
        return asignacion;
    }

    private Docente docente(Integer id, Acceso acceso) {
        Docente docente = new Docente();
        docente.setIdDocente(id);
        docente.setAcceso(acceso);
        return docente;
    }

    private SuspensionDocente suspension(Docente docente, EstadoSuspension estado, LocalDate inicio,
                                         LocalDate fin, Acceso acceso) {
        SuspensionDocente suspension = new SuspensionDocente();
        suspension.setDocente(docente);
        suspension.setEstado(estado);
        suspension.setFechaInicio(inicio);
        suspension.setFechaFin(fin);
        suspension.setAcceso(acceso);
        return suspension;
    }
}
