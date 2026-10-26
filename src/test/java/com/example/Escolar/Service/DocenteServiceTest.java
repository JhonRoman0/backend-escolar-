package com.example.Escolar.Service;

import com.example.Escolar.Dto.DocenteRequest;
import com.example.Escolar.Exception.DocenteConAsignacionesException;
import com.example.Escolar.Model.Acceso;
import com.example.Escolar.Model.AnioEscolar;
import com.example.Escolar.Model.Asignacion;
import com.example.Escolar.Model.Docente;
import com.example.Escolar.Model.DocenteNivel;
import com.example.Escolar.Model.Grado;
import com.example.Escolar.Model.GradoSeccion;
import com.example.Escolar.Model.Nivel;
import com.example.Escolar.Model.Usuario;
import com.example.Escolar.Repository.AccesoRepository;
import com.example.Escolar.Repository.AsignacionRepository;
import com.example.Escolar.Repository.DocenteNivelRepository;
import com.example.Escolar.Repository.DocenteRepository;
import com.example.Escolar.Repository.GradoAcademicoRepository;
import com.example.Escolar.Repository.NivelRepository;
import com.example.Escolar.Repository.RolRepository;
import com.example.Escolar.Repository.TipoContratoRepository;
import com.example.Escolar.Repository.UsuarioRepository;
import com.example.Escolar.Repository.UsuarioRolRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DocenteServiceTest {

    @Mock
    private DocenteRepository docenteRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private RolRepository rolRepository;
    @Mock
    private UsuarioRolRepository usuarioRolRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private AccesoRepository accesoRepository;
    @Mock
    private AsignacionRepository asignacionRepository;
    @Mock
    private GradoAcademicoRepository gradoAcademicoRepository;
    @Mock
    private TipoContratoRepository tipoContratoRepository;
    @Mock
    private NivelRepository nivelRepository;
    @Mock
    private DocenteNivelRepository docenteNivelRepository;
    @InjectMocks
    private DocenteService docenteService;

    private Docente docente;
    private Nivel inicial;
    private Nivel primaria;

    private Nivel nivel(int id, String nombre) {
        Nivel nivel = new Nivel();
        nivel.setIdNivel(id);
        nivel.setNombre(nombre);
        return nivel;
    }

    private DocenteNivel docenteNivel(Nivel nivel) {
        DocenteNivel docenteNivel = new DocenteNivel();
        docenteNivel.setDocente(docente);
        docenteNivel.setNivel(nivel);
        return docenteNivel;
    }

    @BeforeEach
    void setUp() {
        Acceso eliminado = new Acceso();
        eliminado.setIdAcceso(AccesoConstants.ELIMINADO);
        when(accesoRepository.findById(AccesoConstants.ELIMINADO)).thenReturn(Optional.of(eliminado));

        Usuario usuario = new Usuario();
        usuario.setNombre("Ana");
        usuario.setApellidoPat("Ruiz");
        docente = new Docente();
        docente.setIdDocente(7);
        docente.setUsuario(usuario);
        when(docenteRepository.findByIdDocenteAndAccesoNot(eq(7), any())).thenReturn(Optional.of(docente));

        inicial = nivel(1, "Inicial");
        primaria = nivel(2, "Primaria");
        // Hoy enseña en Inicial y Primaria.
        when(docenteNivelRepository.findByDocenteAndAccesoNot(eq(docente), any()))
                .thenReturn(List.of(docenteNivel(inicial), docenteNivel(primaria)));
    }

    private Asignacion asignacionEn(String anio, byte estado) {
        return asignacionEn(anio, estado, primaria);
    }

    private Asignacion asignacionEn(String anio, byte estado, Nivel nivel) {
        Grado grado = new Grado();
        grado.setNivel(nivel);
        GradoSeccion gradoSeccion = new GradoSeccion();
        gradoSeccion.setGrado(grado);
        AnioEscolar anioEscolar = new AnioEscolar();
        anioEscolar.setAnio(anio);
        anioEscolar.setEstado(estado);
        Asignacion asignacion = new Asignacion();
        asignacion.setAnioEscolar(anioEscolar);
        asignacion.setGradoSeccion(gradoSeccion);
        return asignacion;
    }

    private void tiene(Asignacion... asignaciones) {
        when(asignacionRepository.findByDocenteIdDocenteAndAccesoNot(eq(7), any())).thenReturn(Arrays.asList(asignaciones));
    }

    @Test
    void lasAsignacionesDeUnAnioCerradoNoBloquean() {
        tiene(asignacionEn("2024", AnioEscolarService.ESTADO_CERRADO));
        assertDoesNotThrow(() -> docenteService.validarSinAsignacionesVigentes(docente, "quitar el rol Docente a"));
    }

    @Test
    void lasDeUnAnioVigenteOPorComenzarBloqueanYSeCuentanPorAnio() {
        tiene(asignacionEn("2024", AnioEscolarService.ESTADO_CERRADO),
                asignacionEn("2026", AnioEscolarService.ESTADO_VIGENTE),
                asignacionEn("2026", AnioEscolarService.ESTADO_VIGENTE),
                asignacionEn("2027", AnioEscolarService.ESTADO_POR_COMENZAR));

        DocenteConAsignacionesException ex = assertThrows(DocenteConAsignacionesException.class,
                () -> docenteService.validarSinAsignacionesVigentes(docente, "quitar el rol Docente a"));

        assertEquals("No se puede quitar el rol Docente a Ana Ruiz: tiene 3 asignaciones (2026: 2, 2027: 1). "
                + "Reasígnalas en Cambios de docente o elimínalas primero.", ex.getMessage());
    }

    private DocenteRequest soloCon(Nivel... niveles) {
        DocenteRequest request = new DocenteRequest();
        request.setNiveles(Arrays.stream(niveles).map(Nivel::getIdNivel).toList());
        return request;
    }

    @Test
    void noLeQuitaUnNivelEnElQueTieneAsignacionesVigentes() {
        tiene(asignacionEn("2026", AnioEscolarService.ESTADO_VIGENTE, inicial),
                asignacionEn("2027", AnioEscolarService.ESTADO_POR_COMENZAR, primaria));

        DocenteConAsignacionesException ex = assertThrows(DocenteConAsignacionesException.class,
                () -> docenteService.update(7, soloCon(inicial)));

        assertEquals("No se puede quitar el nivel Primaria a Ana Ruiz: tiene 1 asignación de Primaria (2027: 1). "
                + "Reasígnalas en Cambios de docente o elimínalas primero.", ex.getMessage());
        verify(docenteNivelRepository, never()).deleteByDocente(any());
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void unNivelSinAsignacionesVigentesSiSePuedeQuitar() {
        // En Primaria solo tiene historia de un año cerrado; lo vigente es de Inicial.
        tiene(asignacionEn("2024", AnioEscolarService.ESTADO_CERRADO, primaria),
                asignacionEn("2026", AnioEscolarService.ESTADO_VIGENTE, inicial));

        assertDoesNotThrow(() -> docenteService.validarSinAsignacionesVigentes(docente, primaria, "quitar el nivel Primaria a"));
    }

    @Test
    void informaQueNivelesTienenAsignacionesVigentes() {
        Acceso activo = new Acceso();
        activo.setIdAcceso(AccesoConstants.ACTIVO);
        docente.setAcceso(activo);
        tiene(asignacionEn("2024", AnioEscolarService.ESTADO_CERRADO, inicial),
                asignacionEn("2026", AnioEscolarService.ESTADO_VIGENTE, primaria),
                asignacionEn("2027", AnioEscolarService.ESTADO_POR_COMENZAR, primaria));

        assertEquals(List.of(2), docenteService.getById(7).getNivelesConAsignaciones());
    }
}
