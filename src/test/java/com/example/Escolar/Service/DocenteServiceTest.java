package com.example.Escolar.Service;

import com.example.Escolar.Exception.DocenteConAsignacionesException;
import com.example.Escolar.Model.Acceso;
import com.example.Escolar.Model.AnioEscolar;
import com.example.Escolar.Model.Asignacion;
import com.example.Escolar.Model.Docente;
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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
    }

    private Asignacion asignacionEn(String anio, byte estado) {
        AnioEscolar anioEscolar = new AnioEscolar();
        anioEscolar.setAnio(anio);
        anioEscolar.setEstado(estado);
        Asignacion asignacion = new Asignacion();
        asignacion.setAnioEscolar(anioEscolar);
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
}
