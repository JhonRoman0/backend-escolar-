package com.example.Escolar.Config;

import com.example.Escolar.Model.Acceso;
import com.example.Escolar.Model.Rol;
import com.example.Escolar.Repository.AccesoRepository;
import com.example.Escolar.Repository.RolRepository;
import com.example.Escolar.Service.AccesoConstants;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/** Al arrancar solo se crean los roles base que faltan, sin acciones. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DataSeederRolesTest {
    @Mock RolRepository roles;
    @Mock AccesoRepository accesos;
    @InjectMocks DataSeeder seeder;

    @Test
    void creaLosRolesQueFaltanYNoTocaLosExistentes() {
        when(accesos.findById(AccesoConstants.ACTIVO)).thenReturn(Optional.of(new Acceso(AccesoConstants.ACTIVO, "Activo")));
        Rol borrado = new Rol();
        borrado.setNombre("Auxiliar");
        borrado.setAcceso(new Acceso(AccesoConstants.ELIMINADO, "Eliminado"));
        when(roles.findByNombre(anyString())).thenReturn(Optional.empty());
        when(roles.findByNombre("Auxiliar")).thenReturn(Optional.of(borrado));
        when(roles.save(any())).thenAnswer(i -> i.getArgument(0));

        ReflectionTestUtils.invokeMethod(seeder, "seedRoles");

        ArgumentCaptor<Rol> creados = ArgumentCaptor.forClass(Rol.class);
        verify(roles, times(4)).save(creados.capture());
        assertEquals(List.of("Administrador", "Secretaria", "Docente", "Apoderado"),
                creados.getAllValues().stream().map(Rol::getNombre).toList());
        // Un rol borrado no vuelve.
        assertEquals(AccesoConstants.ELIMINADO, borrado.getAcceso().getIdAcceso());
    }

    @Test
    void elArranqueYaNoAsignaAccionesARoles() {
        assertTrue(Arrays.stream(DataSeeder.class.getDeclaredMethods())
                .noneMatch(m -> m.getName().equals("seedMatrizRbac") || m.getName().equals("asignarPermiso")));
        assertTrue(Arrays.stream(DataSeeder.class.getDeclaredFields())
                .noneMatch(f -> f.getType().getSimpleName().startsWith("RolPermiso")));
    }
}
