package com.example.Escolar.Config;

import com.example.Escolar.Model.Acceso;
import com.example.Escolar.Model.Accion;
import com.example.Escolar.Model.Permiso;
import com.example.Escolar.Model.PermisoAccion;
import com.example.Escolar.Model.RolPermiso;
import com.example.Escolar.Repository.AccesoRepository;
import com.example.Escolar.Repository.AccionRepository;
import com.example.Escolar.Repository.PermisoAccionRepository;
import com.example.Escolar.Repository.PermisoRepository;
import com.example.Escolar.Repository.RolPermisoAccionRepository;
import com.example.Escolar.Repository.RolPermisoRepository;
import com.example.Escolar.Service.AccesoConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Deja los permisos del CatalogoPermisos como dice el catalogo: agrega las
 * acciones que faltan (o reactiva las que se habian quitado), actualiza su
 * etiqueta y desactiva las que sobran, tambien en los roles que las tenian.
 * Lo demas que cada rol tiene asignado no se toca. Lo llama el DataSeeder al
 * arrancar, despues de sembrar los modulos.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CatalogoPermisosSync {

    private final PermisoRepository permisoRepository;
    private final AccionRepository accionRepository;
    private final PermisoAccionRepository permisoAccionRepository;
    private final RolPermisoRepository rolPermisoRepository;
    private final RolPermisoAccionRepository rolPermisoAccionRepository;
    private final AccesoRepository accesoRepository;

    public void sincronizar() {
        Acceso activo = accesoRepository.findById(AccesoConstants.ACTIVO).orElseThrow();
        Acceso eliminado = accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow();
        CatalogoPermisos.todos().forEach((codigoPermiso, acciones) ->
                permisoRepository.findByCodigoAndAccesoNot(codigoPermiso, eliminado).ifPresent(permiso -> {
                    acciones.forEach((codigoAccion, etiqueta) ->
                            asegurarAccion(permiso, codigoAccion, etiqueta, activo, eliminado));
                    for (PermisoAccion pa : permisoAccionRepository.findByPermisoIdPermisoAndAccesoNot(permiso.getIdPermiso(), eliminado)) {
                        if (!acciones.containsKey(pa.getAccion().getCodigo())) {
                            quitarAccion(permiso, pa, eliminado);
                        }
                    }
                }));
    }

    private void asegurarAccion(Permiso permiso, String codigoAccion, String etiqueta, Acceso activo, Acceso eliminado) {
        Accion accion = accionRepository.findByCodigoAndAccesoNot(codigoAccion, eliminado)
                .orElseThrow(() -> new IllegalStateException("Acción no sembrada: " + codigoAccion));
        PermisoAccion pa = permisoAccionRepository
                .findByPermisoIdPermisoAndAccionIdAccion(permiso.getIdPermiso(), accion.getIdAccion())
                .orElseGet(() -> {
                    PermisoAccion nueva = new PermisoAccion();
                    nueva.setPermiso(permiso);
                    nueva.setAccion(accion);
                    return nueva;
                });
        pa.setEtiqueta(etiqueta);
        pa.setAcceso(activo);
        permisoAccionRepository.save(pa);
    }

    private void quitarAccion(Permiso permiso, PermisoAccion pa, Acceso eliminado) {
        pa.setAcceso(eliminado);
        permisoAccionRepository.save(pa);
        for (RolPermiso rolPermiso : rolPermisoRepository.findByPermisoIdPermiso(permiso.getIdPermiso())) {
            rolPermisoAccionRepository.findByRolPermisoIdRolPermisoAndAccionIdAccionAndAccesoNot(
                            rolPermiso.getIdRolPermiso(), pa.getAccion().getIdAccion(), eliminado)
                    .ifPresent(concedida -> {
                        concedida.setAcceso(eliminado);
                        rolPermisoAccionRepository.save(concedida);
                    });
        }
        log.info("Catálogo de permisos: se quitó la acción {} de {}", pa.getAccion().getCodigo(), permiso.getCodigo());
    }
}
