package com.example.Escolar.Service;

import com.example.Escolar.Dto.ColegioRequest;
import com.example.Escolar.Dto.UploadResponse;
import com.example.Escolar.Model.Acceso;
import com.example.Escolar.Model.Colegio;
import com.example.Escolar.Repository.AccesoRepository;
import com.example.Escolar.Repository.ColegioRepository;
import jakarta.validation.Validation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ColegioServiceTest {
    @Mock ColegioRepository colegioRepository;
    @Mock AccesoRepository accesoRepository;
    @Mock CloudinaryService cloudinaryService;
    @InjectMocks ColegioService service;

    private final Acceso activo = new Acceso(AccesoConstants.ACTIVO, "Activo");
    private final Acceso eliminado = new Acceso(AccesoConstants.ELIMINADO, "Eliminado");
    private final MockMultipartFile foto = new MockMultipartFile("foto", "f.png", "image/png", new byte[]{1});
    private final MockMultipartFile portada = new MockMultipartFile("portada", "p.png", "image/png", new byte[]{2});

    @BeforeEach
    void preparar() {
        when(accesoRepository.findById(AccesoConstants.ACTIVO)).thenReturn(Optional.of(activo));
        when(accesoRepository.findById(AccesoConstants.ELIMINADO)).thenReturn(Optional.of(eliminado));
        when(colegioRepository.findFirstByAccesoNot(eliminado)).thenReturn(Optional.empty());
        when(colegioRepository.save(any(Colegio.class))).thenAnswer(i -> i.getArgument(0));
    }

    private ColegioRequest request() {
        ColegioRequest r = new ColegioRequest();
        r.setNombre("IE San Martin");
        r.setCelular("987654321");
        r.setTelefono("074123456");
        r.setDireccion("Av. Principal 123");
        r.setCodigoColegio("1234567");
        return r;
    }

    private UploadResponse subida(String id) {
        UploadResponse u = new UploadResponse();
        u.setUrl("https://cdn/" + id);
        u.setPublicId(id);
        return u;
    }

    private Set<String> camposInvalidos(ColegioRequest r) {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            return factory.getValidator().validate(r).stream()
                    .map(v -> v.getPropertyPath().toString())
                    .collect(Collectors.toSet());
        }
    }

    @Test
    void losCamposSonObligatoriosSalvoElCodigo() {
        assertEquals(Set.of("nombre", "celular", "telefono", "direccion"),
                camposInvalidos(new ColegioRequest()));
        assertEquals(Set.of(), camposInvalidos(request()));
    }

    @Test
    void elCelularDebeTenerNueveDigitos() {
        ColegioRequest r = request();
        r.setCelular("12345");
        assertEquals(Set.of("celular"), camposInvalidos(r));
    }

    @Test
    void elCelularNoAceptaLetras() {
        ColegioRequest r = request();
        r.setCelular("98765432a");
        assertEquals(Set.of("celular"), camposInvalidos(r));
    }

    @Test
    void elTelefonoSoloAceptaDigitos() {
        ColegioRequest r = request();
        r.setTelefono("074abc456");
        assertEquals(Set.of("telefono"), camposInvalidos(r));
        r.setTelefono("074-123456");
        assertEquals(Set.of("telefono"), camposInvalidos(r));
        r.setTelefono("12345");
        assertEquals(Set.of("telefono"), camposInvalidos(r));
    }

    @Test
    void elCodigoVacioSeGuardaComoSinCodigo() {
        when(cloudinaryService.upload(foto, "portal/colegio/foto")).thenReturn(subida("f1"));
        ColegioRequest r = request();
        r.setCodigoColegio("   ");
        assertEquals(Set.of(), camposInvalidos(r));

        assertNull(service.create(r, foto, null).getCodigoColegio());
    }

    @Test
    void noCreaSinFoto() {
        var error = assertThrows(IllegalArgumentException.class, () -> service.create(request(), null, null));

        assertEquals("La foto del colegio es obligatoria", error.getMessage());
        verify(colegioRepository, never()).save(any());
        verifyNoInteractions(cloudinaryService);
    }

    @Test
    void creaConFotoYSinPortada() {
        when(cloudinaryService.upload(foto, "portal/colegio/foto")).thenReturn(subida("f1"));

        var creado = service.create(request(), foto, null);

        assertEquals("https://cdn/f1", creado.getUrlFoto());
        assertNull(creado.getUrlPortal());
    }

    @Test
    void siFallaLaFotoNoCreaNada() {
        when(cloudinaryService.upload(foto, "portal/colegio/foto")).thenThrow(new IllegalStateException("sin red"));

        assertThrows(IllegalStateException.class, () -> service.create(request(), foto, null));

        verify(colegioRepository, never()).save(any());
    }

    @Test
    void siFallaLaPortadaBorraLaFotoYaSubida() {
        when(cloudinaryService.upload(foto, "portal/colegio/foto")).thenReturn(subida("f1"));
        when(cloudinaryService.upload(portada, "portal/colegio/portada")).thenThrow(new IllegalStateException("sin red"));

        assertThrows(IllegalStateException.class, () -> service.create(request(), foto, portada));

        verify(cloudinaryService).delete("f1");
        verify(colegioRepository, never()).save(any());
    }

    private Colegio existente() {
        Colegio c = new Colegio();
        c.setIdColegio(1);
        c.setUrlFoto("https://cdn/vieja");
        c.setPkUrlFoto("vieja");
        c.setUrlPortal("https://cdn/portada");
        c.setPkUrlPortada("portada");
        c.setAcceso(activo);
        when(colegioRepository.findByIdColegioAndAccesoNot(1, eliminado)).thenReturn(Optional.of(c));
        return c;
    }

    @Test
    void guardarDatosConservaLasImagenes() {
        Colegio c = existente();

        service.update(1, request());

        assertEquals("https://cdn/vieja", c.getUrlFoto());
        assertEquals("https://cdn/portada", c.getUrlPortal());
    }

    @Test
    void cambiarLaFotoSubeLaNuevaAntesDeBorrarLaAnterior() {
        existente();
        when(cloudinaryService.upload(foto, "portal/colegio/foto")).thenReturn(subida("nueva"));

        assertEquals("https://cdn/nueva", service.subirFoto(1, foto).getUrlFoto());

        InOrder orden = inOrder(cloudinaryService);
        orden.verify(cloudinaryService).upload(foto, "portal/colegio/foto");
        orden.verify(cloudinaryService).delete("vieja");
    }

    @Test
    void siFallaLaFotoNuevaConservaLaAnterior() {
        Colegio c = existente();
        when(cloudinaryService.upload(foto, "portal/colegio/foto")).thenThrow(new IllegalStateException("sin red"));

        assertThrows(IllegalStateException.class, () -> service.subirFoto(1, foto));

        assertEquals("https://cdn/vieja", c.getUrlFoto());
        verify(cloudinaryService, never()).delete(any());
    }

    @Test
    void laPortadaSePuedeQuitar() {
        Colegio c = existente();

        service.eliminarPortada(1);

        assertNull(c.getUrlPortal());
        assertEquals("https://cdn/vieja", c.getUrlFoto());
    }
}
