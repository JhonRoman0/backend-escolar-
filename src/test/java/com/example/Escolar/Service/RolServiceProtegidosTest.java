package com.example.Escolar.Service;

import com.example.Escolar.Dto.RolRequest;
import com.example.Escolar.Model.Acceso;
import com.example.Escolar.Model.Rol;
import com.example.Escolar.Repository.AccesoRepository;
import com.example.Escolar.Repository.RolRepository;
import com.example.Escolar.Repository.UsuarioRolRepository;
import com.example.Escolar.Security.ConcesionesService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/** El rol Apoderado (lo asigna la matrícula) está protegido igual que ADMIN. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RolServiceProtegidosTest {
    @Mock RolRepository rolRepository;
    @Mock AccesoRepository accesoRepository;
    @Mock UsuarioRolRepository usuarioRolRepository;
    @Mock ConcesionesService concesionesService;
    @InjectMocks RolService service;

    private final Acceso activo = new Acceso(AccesoConstants.ACTIVO, "Activo");
    private final Acceso eliminado = new Acceso(AccesoConstants.ELIMINADO, "Eliminado");
    private final Acceso inactivo = new Acceso(AccesoConstants.INACTIVO, "Inactivo");

    @BeforeEach
    void preparar() {
        when(accesoRepository.findById(AccesoConstants.ACTIVO)).thenReturn(Optional.of(activo));
        when(accesoRepository.findById(AccesoConstants.ELIMINADO)).thenReturn(Optional.of(eliminado));
        when(accesoRepository.findById(AccesoConstants.INACTIVO)).thenReturn(Optional.of(inactivo));
        when(usuarioRolRepository.findByRolIdRol(any())).thenReturn(List.of());
        when(rolRepository.save(any())).thenAnswer(i -> i.getArgument(0));
    }

    private Rol rol(int id, String nombre) {
        Rol rol = new Rol();
        rol.setIdRol(id);
        rol.setNombre(nombre);
        rol.setAcceso(activo);
        when(rolRepository.findByIdRolAndAccesoNot(eq(id), any())).thenReturn(Optional.of(rol));
        return rol;
    }

    private static RolRequest pedido(String nombre, Long accesoId) {
        RolRequest r = new RolRequest();
        r.setNombre(nombre);
        r.setAccesoId(accesoId);
        return r;
    }

    @Test
    void apoderadoNoSeEliminaAunqueNoTengaUsuarios() {
        rol(5, "Apoderado");
        assertThrows(IllegalArgumentException.class, () -> service.delete(5));
        verify(rolRepository, never()).save(any());
    }

    @Test
    void apoderadoNoSeRenombraNiDesactiva() {
        rol(5, "Apoderado");
        assertThrows(IllegalArgumentException.class, () -> service.update(5, pedido("Padres", null)));
        assertThrows(IllegalArgumentException.class, () -> service.update(5, pedido("Apoderado", AccesoConstants.INACTIVO)));
        verify(rolRepository, never()).save(any());
    }

    @Test
    void nadieMasPuedeLlamarseApoderado() {
        assertThrows(IllegalArgumentException.class, () -> service.create(pedido(" apoderado ", null)));
        rol(7, "Secretaria");
        assertThrows(IllegalArgumentException.class, () -> service.update(7, pedido("Apoderado", null)));
        verify(rolRepository, never()).save(any());
    }

    @Test
    void losDemasRolesSinUsuariosSiSeEliminan() {
        Rol secretaria = rol(7, "Secretaria");
        service.delete(7);
        assertEquals(AccesoConstants.ELIMINADO, secretaria.getAcceso().getIdAcceso());
    }
}
