package com.example.Escolar.Service;

import com.example.Escolar.Model.Acceso;
import com.example.Escolar.Model.Alumno;
import com.example.Escolar.Model.Usuario;
import com.example.Escolar.Repository.AlumnoRepository;
import com.example.Escolar.Repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.List;
import java.util.Optional;
import java.util.Map;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExclusividadDniServiceTest {
    @Mock private AlumnoRepository alumnos;
    @Mock private UsuarioRepository usuarios;
    @InjectMocks private ExclusividadDniService service;

    @Test
    void alumnoReservaSuDniEnCualquierEstado() {
        for (Long estado : List.of(AccesoConstants.ACTIVO, AccesoConstants.INACTIVO, AccesoConstants.ELIMINADO)) {
            Alumno alumno = new Alumno();
            Acceso acceso = new Acceso();
            acceso.setIdAcceso(estado);
            alumno.setAcceso(acceso);
            when(alumnos.findByDocumentoIdentidad("12345678")).thenReturn(Optional.of(alumno));
            assertTrue(assertThrows(IllegalArgumentException.class,
                    () -> service.validarUsuario("12345678")).getMessage().contains("como alumno"));
        }
    }

    @Test
    void usuarioReservaSuDniEnCualquierEstadoSinDependerDeSuRol() {
        for (Long estado : List.of(AccesoConstants.ACTIVO, AccesoConstants.INACTIVO, AccesoConstants.ELIMINADO)) {
            Usuario usuario = new Usuario();
            Acceso acceso = new Acceso();
            acceso.setIdAcceso(estado);
            usuario.setAcceso(acceso);
            when(usuarios.findByDocumentoIdentidad("12345678")).thenReturn(Optional.of(usuario));
            assertTrue(assertThrows(IllegalArgumentException.class,
                    () -> service.validarAlumno("12345678")).getMessage().contains("como usuario"));
        }
    }

    @Test
    void dniDisponibleNoRequiereEstadoNiRolEspecial() {
        assertDoesNotThrow(() -> service.validarUsuario("12345678"));
        assertDoesNotThrow(() -> service.validarAlumno("12345678"));
    }

    @Test
    void dniIntactoConservaConflictosHistoricosPeroNoHabilitaReactivacion() {
        service.validarCambioUsuario("12345678", "12345678", false);
        verifyNoInteractions(alumnos);
        when(alumnos.findByDocumentoIdentidad("12345678")).thenReturn(Optional.of(new Alumno()));
        assertThrows(IllegalArgumentException.class,
                () -> service.validarCambioUsuario("12345678", "12345678", true));
        assertThrows(IllegalArgumentException.class,
                () -> service.validarCambioUsuario("12345678", "87654321", false));
    }

    @Test
    void noConsultaLaBaseConDniInvalido() {
        for (String dni : List.of("", "123", "1234567A", " 12345678")) {
            assertThrows(IllegalArgumentException.class, () -> service.validarUsuario(dni));
            assertThrows(IllegalArgumentException.class, () -> service.validarAlumno(dni));
        }
        assertThrows(IllegalArgumentException.class, () -> service.validarUsuario(null));
        verifyNoInteractions(alumnos, usuarios);
    }

    @Test
    void lasEscriturasMantienenLaComprobacionDentroDeUnaTransaccionSerializable() {
        Map<Class<?>, List<String>> escrituras = Map.of(
                UsuarioService.class, List.of("create", "update", "reactivar"),
                DocenteService.class, List.of("update"),
                ApoderadoService.class, List.of("actualizar"),
                InscripcionService.class, List.of("inscribir", "agregarApoderado", "reemplazarApoderado"));
        escrituras.forEach((tipo, nombres) -> {
            for (String nombre : nombres) {
                var metodo = java.util.Arrays.stream(tipo.getDeclaredMethods())
                        .filter(m -> m.getName().equals(nombre)).findFirst().orElseThrow();
                Transactional transaccion = metodo.getAnnotation(Transactional.class);
                assertNotNull(transaccion, tipo.getSimpleName() + "." + nombre);
                assertEquals(Isolation.SERIALIZABLE, transaccion.isolation());
            }
        });
    }
}
