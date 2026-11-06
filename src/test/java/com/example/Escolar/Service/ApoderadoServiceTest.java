package com.example.Escolar.Service;

import com.example.Escolar.Dto.ApoderadoEdicionRequest;
import com.example.Escolar.Model.Acceso;
import com.example.Escolar.Model.Apoderado;
import com.example.Escolar.Model.Alumno;
import com.example.Escolar.Model.AlumnoApoderado;
import com.example.Escolar.Model.Usuario;
import com.example.Escolar.Repository.AccesoRepository;
import com.example.Escolar.Repository.AlumnoApoderadoRepository;
import com.example.Escolar.Repository.ApoderadoRepository;
import com.example.Escolar.Repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.List;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApoderadoServiceTest {

    @Mock
    private ExclusividadDniService exclusividadDni;
    @Mock
    private ApoderadoRepository apoderadoRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private AccesoRepository accesoRepository;
    @Mock
    private AlumnoApoderadoRepository alumnoApoderadoRepository;
    @InjectMocks
    private ApoderadoService apoderadoService;

    @Test
    void editarApoderadoRechazaFechaNuevaInvalidaSinModificarLaPersona() {
        Usuario usuario = new Usuario();
        usuario.setDocumentoIdentidad("41111111");
        usuario.setFechaNaci(LocalDate.of(1980, 1, 1));
        Apoderado apoderado = new Apoderado();
        apoderado.setUsuario(usuario);
        Alumno alumno = new Alumno();
        alumno.setIdAlumno(9);
        AlumnoApoderado vinculo = new AlumnoApoderado();
        vinculo.setAlumno(alumno);
        when(accesoRepository.findById(AccesoConstants.ELIMINADO)).thenReturn(Optional.of(new Acceso()));
        when(apoderadoRepository.findByIdApoderadoAndAccesoNot(eq(50), any())).thenReturn(Optional.of(apoderado));
        when(alumnoApoderadoRepository.findByApoderado(apoderado)).thenReturn(List.of(vinculo));
        ApoderadoEdicionRequest request = new ApoderadoEdicionRequest();
        request.setDocumentoIdentidad("41111111");
        request.setGmail("ana@mail.com");
        request.setIdAlumno(9);
        request.setFechaNaci(FechaNacimiento.hoy());

        assertThrows(IllegalArgumentException.class, () -> apoderadoService.actualizar(50, request));
        assertEquals(LocalDate.of(1980, 1, 1), usuario.getFechaNaci());
        verify(usuarioRepository, never()).save(any());
        verify(apoderadoRepository, never()).save(any());
    }

    @Test
    void editarNoCambiaElDniNiReescribeAOtraPersona() {
        Usuario usuario = new Usuario();
        usuario.setDocumentoIdentidad("41111111");
        Apoderado apoderado = new Apoderado();
        apoderado.setUsuario(usuario);
        when(accesoRepository.findById(AccesoConstants.ELIMINADO)).thenReturn(Optional.of(new Acceso()));
        when(apoderadoRepository.findByIdApoderadoAndAccesoNot(eq(50), any())).thenReturn(Optional.of(apoderado));
        ApoderadoEdicionRequest request = new ApoderadoEdicionRequest();
        request.setDocumentoIdentidad("40000001");
        request.setGmail("ana@mail.com");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> apoderadoService.actualizar(50, request));

        assertEquals("Para poner a otra persona hay que reemplazar al apoderado", ex.getMessage());
        assertEquals("41111111", usuario.getDocumentoIdentidad());
        verify(usuarioRepository, never()).save(any());
    }
}
