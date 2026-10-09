package com.example.Escolar.Service;

import com.example.Escolar.Dto.UsuarioRequest;
import com.example.Escolar.Dto.UsuarioResponse;
import com.example.Escolar.Exception.UsuarioReactivacionException;
import com.example.Escolar.Model.Acceso;
import com.example.Escolar.Model.Docente;
import com.example.Escolar.Model.DocenteNivel;
import com.example.Escolar.Model.GradoAcademico;
import com.example.Escolar.Model.Nivel;
import com.example.Escolar.Model.Rol;
import com.example.Escolar.Model.TipoContrato;
import com.example.Escolar.Model.Usuario;
import com.example.Escolar.Model.UsuarioRol;
import com.example.Escolar.Repository.AccesoRepository;
import com.example.Escolar.Repository.DocenteNivelRepository;
import com.example.Escolar.Repository.DocenteRepository;
import com.example.Escolar.Repository.GradoAcademicoRepository;
import com.example.Escolar.Repository.NivelRepository;
import com.example.Escolar.Repository.RolRepository;
import com.example.Escolar.Repository.TipoContratoRepository;
import com.example.Escolar.Repository.UsuarioRepository;
import com.example.Escolar.Repository.UsuarioRolRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UsuarioServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private AccesoRepository accesoRepository;
    @Mock
    private RolRepository rolRepository;
    @Mock
    private UsuarioRolRepository usuarioRolRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private CloudinaryService cloudinaryService;
    @Mock
    private DocenteRepository docenteRepository;
    @Mock
    private DocenteNivelRepository docenteNivelRepository;
    @Mock
    private NivelRepository nivelRepository;
    @Mock
    private GradoAcademicoRepository gradoAcademicoRepository;
    @Mock
    private TipoContratoRepository tipoContratoRepository;
    @Mock
    private EmailService emailService;

    @InjectMocks
    private UsuarioService service;

    private Acceso acceso(Long id) {
        Acceso acceso = new Acceso();
        acceso.setIdAcceso(id);
        return acceso;
    }

    private void stubAccesoRepository() {
        when(accesoRepository.findById(AccesoConstants.ACTIVO)).thenReturn(Optional.of(acceso(AccesoConstants.ACTIVO)));
        when(accesoRepository.findById(AccesoConstants.ELIMINADO)).thenReturn(Optional.of(acceso(AccesoConstants.ELIMINADO)));
    }

    private void stubValidacionesUnicas() {
        when(usuarioRepository.findByDocumentoIdentidadAndAccesoNot(anyString(), any(Acceso.class)))
                .thenReturn(Optional.empty());
        when(usuarioRepository.findByGmailAndAccesoNot(anyString(), any(Acceso.class)))
                .thenReturn(Optional.empty());
    }

    private void stubSaveYRolesActivos(Usuario usuario, Rol rol) {
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));
        when(usuarioRepository.findByIdUsuarioAndAccesoNot(any(Integer.class), any(Acceso.class)))
                .thenReturn(Optional.of(usuario));
        when(usuarioRolRepository.findByUsuarioIdUsuario(usuario.getIdUsuario()))
                .thenReturn(List.of(usuarioRol(usuario, rol)));
    }

    private Usuario usuario(Integer id, Long accesoId) {
        Usuario u = new Usuario();
        u.setIdUsuario(id);
        u.setNombre("JHONATAN");
        u.setApellidoPat("GUEVARA");
        u.setApellidoMat("VASQUEZ");
        u.setCodigo("A20260001");
        u.setDocumentoIdentidad("76988609");
        u.setContraseña("hashPrevIa");
        u.setGmail("jhoanatanguevaravasquez@gmail.com");
        u.setCelular("999999999");
        u.setFechaNaci(LocalDate.of(2004, 11, 16));
        u.setFechaCreacion(LocalDate.of(2026, 1, 5));
        u.setAcceso(acceso(accesoId));
        u.setIntentosFallidos(0);
        return u;
    }

    private Rol rol(Integer id, String nombre) {
        Rol rol = new Rol();
        rol.setIdRol(id);
        rol.setNombre(nombre);
        rol.setAcceso(acceso(AccesoConstants.ACTIVO));
        return rol;
    }

    private Docente docente(Integer id, Long accesoId) {
        Docente docente = new Docente();
        docente.setIdDocente(id);
        docente.setAcceso(acceso(accesoId));
        return docente;
    }

    private UsuarioRol usuarioRol(Usuario usuario, Rol rol) {
        UsuarioRol ur = new UsuarioRol();
        ur.setUsuario(usuario);
        ur.setRol(rol);
        ur.setFechaAsignacion(LocalDateTime.now());
        return ur;
    }

    private UsuarioRequest requestSinDocente() {
        UsuarioRequest r = new UsuarioRequest();
        r.setNombre("JHONATAN");
        r.setApellidoPat("GUEVARA");
        r.setApellidoMat("VASQUEZ");
        r.setDocumentoIdentidad("76988609");
        r.setContraseña("NuevaClave1");
        r.setGmail("jhoanatanguevaravasquez@gmail.com");
        r.setCelular("999999999");
        r.setFechaNaci(LocalDate.of(2004, 11, 16));
        r.setRolIds(List.of(1));
        return r;
    }

    @Test
    void reactivarUsuariosEliminadosReutilizaFilaYCodigo() {
        stubAccesoRepository();
        stubValidacionesUnicas();
        Usuario eliminado = usuario(2, AccesoConstants.ELIMINADO);
        when(usuarioRepository.findById(2)).thenReturn(Optional.of(eliminado));
        when(passwordEncoder.encode(anyString())).thenReturn("hashNueva");
        when(rolRepository.findByIdRolAndAccesoNot(any(Integer.class), any(Acceso.class)))
                .thenReturn(Optional.of(rol(1, "Administrador")));
        when(docenteRepository.findByUsuarioIdUsuario(2)).thenReturn(Optional.empty());
        stubSaveYRolesActivos(eliminado, rol(1, "Administrador"));

        UsuarioResponse response = service.reactivar(2, requestSinDocente());

        assertEquals(2, response.getIdUsuario());
        assertEquals("A20260001", response.getCodigo());
        assertEquals(AccesoConstants.ACTIVO, response.getAccesoId());
        assertEquals("Administrador", response.getNombreRol());
        assertEquals(AccesoConstants.ACTIVO, eliminado.getAcceso().getIdAcceso());
        assertEquals("hashNueva", eliminado.getContraseña());
    }

    @Test
    void reactivarSinContrasenaLanzaIllegalArgument() {
        UsuarioRequest request = requestSinDocente();
        request.setContraseña(null);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service.reactivar(2, request));
        assertEquals("La contraseña es obligatoria para reactivar el usuario", ex.getMessage());
    }

    @Test
    void reactivarUsuarioActivoLanzaIllegalArgument() {
        stubAccesoRepository();
        Usuario activo = usuario(1, AccesoConstants.ACTIVO);
        when(usuarioRepository.findById(1)).thenReturn(Optional.of(activo));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service.reactivar(1, requestSinDocente()));
        assertEquals("El usuario no esta eliminado y no requiere reactivacion", ex.getMessage());
    }

    @Test
    void crearConDniDeEliminadoLanzaUsuarioReactivacionExceptionConDatos() {
        stubAccesoRepository();
        stubValidacionesUnicas();
        Usuario eliminado = usuario(2, AccesoConstants.ELIMINADO);
        when(usuarioRepository.findByDocumentoIdentidad("76988609")).thenReturn(Optional.of(eliminado));

        UsuarioReactivacionException ex = assertThrows(
                UsuarioReactivacionException.class,
                () -> service.create(requestSinDocente()));

        assertEquals(2, ex.getIdUsuario());
        assertEquals("JHONATAN GUEVARA VASQUEZ", ex.getNombre());
        assertEquals("A20260001", ex.getCodigo());
    }

    @Test
    void reactivarConDocenteActivoNoLoToca() {
        stubAccesoRepository();
        stubValidacionesUnicas();
        Usuario eliminado = usuario(2, AccesoConstants.ELIMINADO);
        when(usuarioRepository.findById(2)).thenReturn(Optional.of(eliminado));
        when(passwordEncoder.encode(anyString())).thenReturn("hashNueva");
        when(rolRepository.findByIdRolAndAccesoNot(any(Integer.class), any(Acceso.class)))
                .thenReturn(Optional.of(rol(3, "Docente")));
        Docente docente = docente(1, AccesoConstants.ACTIVO);
        docente.setUsuario(eliminado);
        when(docenteRepository.findByUsuarioIdUsuario(2)).thenReturn(Optional.of(docente));
        stubSaveYRolesActivos(eliminado, rol(3, "Docente"));

        UsuarioRequest request = requestSinDocente();
        request.setRolIds(List.of(3));

        service.reactivar(2, request);

        verify(docenteRepository, never()).save(any(Docente.class));
        assertEquals(AccesoConstants.ACTIVO, docente.getAcceso().getIdAcceso());
    }

    @Test
    void reactivarConDocenteEliminadoLoReaplicaYSincronizaNiveles() {
        stubAccesoRepository();
        stubValidacionesUnicas();
        Usuario eliminado = usuario(2, AccesoConstants.ELIMINADO);
        when(usuarioRepository.findById(2)).thenReturn(Optional.of(eliminado));
        when(passwordEncoder.encode(anyString())).thenReturn("hashNueva");
        when(rolRepository.findByIdRolAndAccesoNot(any(Integer.class), any(Acceso.class)))
                .thenReturn(Optional.of(rol(3, "Docente")));
        Docente docente = docente(1, AccesoConstants.ELIMINADO);
        docente.setUsuario(eliminado);
        when(docenteRepository.findByUsuarioIdUsuario(2)).thenReturn(Optional.of(docente));
        when(docenteRepository.save(any(Docente.class))).thenAnswer(inv -> inv.getArgument(0));
        stubSaveYRolesActivos(eliminado, rol(3, "Docente"));

        GradoAcademico grado = new GradoAcademico();
        grado.setIdGradoAcademico(2);
        TipoContrato tipoContrato = new TipoContrato();
        tipoContrato.setIdTipoContrato(1);
        Nivel nivel = new Nivel();
        nivel.setIdNivel(1);
        when(gradoAcademicoRepository.findByIdGradoAcademicoAndAccesoNot(any(Integer.class), any(Acceso.class)))
                .thenReturn(Optional.of(grado));
        when(tipoContratoRepository.findByIdTipoContratoAndAccesoNot(any(Integer.class), any(Acceso.class)))
                .thenReturn(Optional.of(tipoContrato));
        when(nivelRepository.findByIdNivelAndAccesoNot(any(Integer.class), any(Acceso.class)))
                .thenReturn(Optional.of(nivel));

        UsuarioRequest request = requestSinDocente();
        request.setRolIds(List.of(3));
        request.setGradoAcademicoId(2);
        request.setTipoContratoId(1);
        request.setFechaContratacion(LocalDate.of(2026, 3, 1));
        request.setNiveles(List.of(1, 1));

        service.reactivar(2, request);

        assertEquals(AccesoConstants.ACTIVO, docente.getAcceso().getIdAcceso());
        assertEquals(LocalDate.of(2026, 3, 1), docente.getFechaContratacion());
        ArgumentCaptor<DocenteNivel> captor = ArgumentCaptor.forClass(DocenteNivel.class);
        verify(docenteNivelRepository, times(1)).save(captor.capture());
        assertEquals(nivel, captor.getValue().getNivel());
    }

    @Test
    void reactivarSinRolDocenteDejaDocenteEliminado() {
        stubAccesoRepository();
        stubValidacionesUnicas();
        Usuario eliminado = usuario(2, AccesoConstants.ELIMINADO);
        when(usuarioRepository.findById(2)).thenReturn(Optional.of(eliminado));
        when(passwordEncoder.encode(anyString())).thenReturn("hashNueva");
        when(rolRepository.findByIdRolAndAccesoNot(any(Integer.class), any(Acceso.class)))
                .thenReturn(Optional.of(rol(99, "Administrador")));
        Docente docente = docente(1, AccesoConstants.ELIMINADO);
        docente.setUsuario(eliminado);
        when(docenteRepository.findByUsuarioIdUsuario(2)).thenReturn(Optional.of(docente));
        stubSaveYRolesActivos(eliminado, rol(99, "Administrador"));

        UsuarioRequest request = requestSinDocente();
        request.setRolIds(List.of(99));

        service.reactivar(2, request);

        verify(docenteRepository, never()).save(any(Docente.class));
        assertEquals(AccesoConstants.ELIMINADO, docente.getAcceso().getIdAcceso());
    }

    @Test
    void actualizarConservaFotoCuandoElRequestNoLaEnvia() {
        stubAccesoRepository();
        stubValidacionesUnicas();
        Usuario activo = usuario(1, AccesoConstants.ACTIVO);
        activo.setUrlFoto("http://cloud/usuarios/foto.jpg");
        activo.setPkUrlFoto("pk123");
        stubSaveYRolesActivos(activo, rol(1, "Administrador"));

        UsuarioRequest request = requestSinDocente();
        request.setRolIds(null);

        service.update(1, request);

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(captor.capture());
        assertEquals("http://cloud/usuarios/foto.jpg", captor.getValue().getUrlFoto());
        assertEquals("pk123", captor.getValue().getPkUrlFoto());
    }

    private void stubCreacionBasica() {
        stubAccesoRepository();
        stubValidacionesUnicas();
        when(passwordEncoder.encode(anyString())).thenReturn("hashNueva");
        when(rolRepository.findByIdRolAndAccesoNot(any(Integer.class), any(Acceso.class)))
                .thenReturn(Optional.of(rol(1, "Administrador")));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> {
            Usuario saved = inv.getArgument(0);
            saved.setIdUsuario(99);
            return saved;
        });
        when(usuarioRepository.findByIdUsuarioAndAccesoNot(any(Integer.class), any(Acceso.class)))
                .thenReturn(Optional.of(usuario(99, AccesoConstants.ACTIVO)));
        when(usuarioRolRepository.findByUsuarioIdUsuario(99))
                .thenReturn(List.of(usuarioRol(usuario(99, AccesoConstants.ACTIVO), rol(1, "Administrador"))));
    }

    @Test
    void crearConGmailEnviaCredencialesYStampaTrue() {
        stubCreacionBasica();
        when(emailService.enviarCredencialesAcceso(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(true);

        UsuarioResponse response = service.create(requestSinDocente());

        assertEquals("A20260001", response.getCodigo());
        assertEquals(Boolean.TRUE, response.getCredencialesEnviadas());
        verify(emailService).enviarCredencialesAcceso(
                eq("jhoanatanguevaravasquez@gmail.com"), eq("JHONATAN GUEVARA"), eq("A20260001"), eq("NuevaClave1"));
    }

    @Test
    void crearConCorreoFallidoNoRompeLaCreacion() {
        stubCreacionBasica();
        when(emailService.enviarCredencialesAcceso(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(false);

        UsuarioResponse response = service.create(requestSinDocente());

        assertEquals(99, response.getIdUsuario());
        assertEquals(Boolean.FALSE, response.getCredencialesEnviadas());
    }

    @Test
    void crearSinGmailNoEnviaCredenciales() {
        stubCreacionBasica();
        UsuarioRequest request = requestSinDocente();
        request.setGmail("   ");

        UsuarioResponse response = service.create(request);

        assertEquals(Boolean.FALSE, response.getCredencialesEnviadas());
        verify(emailService, never())
                .enviarCredencialesAcceso(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void reactivarEnviaCredencialesYStampaTrue() {
        stubAccesoRepository();
        stubValidacionesUnicas();
        Usuario eliminado = usuario(2, AccesoConstants.ELIMINADO);
        when(usuarioRepository.findById(2)).thenReturn(Optional.of(eliminado));
        when(passwordEncoder.encode(anyString())).thenReturn("hashNueva");
        when(rolRepository.findByIdRolAndAccesoNot(any(Integer.class), any(Acceso.class)))
                .thenReturn(Optional.of(rol(1, "Administrador")));
        when(docenteRepository.findByUsuarioIdUsuario(2)).thenReturn(Optional.empty());
        stubSaveYRolesActivos(eliminado, rol(1, "Administrador"));
        when(emailService.enviarCredencialesAcceso(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(true);

        UsuarioResponse response = service.reactivar(2, requestSinDocente());

        assertEquals(Boolean.TRUE, response.getCredencialesEnviadas());
        verify(emailService).enviarCredencialesAcceso(
                eq("jhoanatanguevaravasquez@gmail.com"), eq("JHONATAN GUEVARA"), eq("A20260001"), eq("NuevaClave1"));
    }
}