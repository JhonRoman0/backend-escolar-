package com.example.Escolar.Config;

import com.example.Escolar.Model.Acceso;
import com.example.Escolar.Model.Permiso;
import com.example.Escolar.Repository.AccesoRepository;
import com.example.Escolar.Repository.PermisoAccionRepository;
import com.example.Escolar.Repository.PermisoRepository;
import com.example.Escolar.Repository.RolPermisoRepository;
import com.example.Escolar.Service.AccesoConstants;
import com.example.Escolar.Service.CloudinaryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Statement;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Borra de forma definitiva la página web pública que se retiró del sistema:
 * primero sus imágenes en Cloudinary, luego sus tablas, y quita sus permisos.
 * Si alguna imagen no se pudo borrar no toca las tablas (se reintenta en el
 * siguiente arranque); sin las tablas no hace nada.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RetiroPaginaWeb {

    /** En orden de borrado: primero las que apuntan a otras. */
    static final List<String> TABLAS = List.of(
            "galeriaDetalle", "galeriaImagen", "publicacion", "eventoEscolar", "contactoMensaje", "ajustePortal");
    static final List<String> TABLAS_CON_IMAGENES = List.of("publicacion", "eventoEscolar", "galeriaDetalle");
    static final List<String> PERMISOS = List.of("PUBLICACIONES", "EVENTOS", "GALERIAS", "CONTACTOS", "AJUSTES");

    private final JdbcTemplate jdbc;
    private final CloudinaryService cloudinaryService;
    private final PermisoRepository permisoRepository;
    private final PermisoAccionRepository permisoAccionRepository;
    private final RolPermisoRepository rolPermisoRepository;
    private final AccesoRepository accesoRepository;

    public void retirar() {
        try {
            quitarPermisos();
            Map<String, String> tablas = tablasExistentes();
            if (tablas.isEmpty()) {
                return;
            }
            if (!borrarImagenes(tablas)) {
                log.warn("Retiro de la página web: quedaron imágenes sin borrar; las tablas se borrarán en el próximo arranque");
                return;
            }
            borrarTablas(tablas);
            log.info("Retiro de la página web: se borraron sus tablas {}", tablas.values());
        } catch (RuntimeException e) {
            // Nunca impide el arranque: se reintenta la próxima vez.
            log.error("Retiro de la página web: no se pudo completar", e);
        }
    }

    private void quitarPermisos() {
        Acceso eliminado = accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow();
        for (String codigo : PERMISOS) {
            permisoRepository.findByCodigoAndAccesoNot(codigo, eliminado).ifPresent(permiso -> quitarPermiso(permiso, eliminado));
        }
    }

    private void quitarPermiso(Permiso permiso, Acceso eliminado) {
        permisoAccionRepository.findByPermisoIdPermisoAndAccesoNot(permiso.getIdPermiso(), eliminado).forEach(pa -> {
            pa.setAcceso(eliminado);
            permisoAccionRepository.save(pa);
        });
        rolPermisoRepository.findByPermisoIdPermiso(permiso.getIdPermiso()).forEach(rp -> {
            rp.setAcceso(eliminado);
            rolPermisoRepository.save(rp);
        });
        permiso.setAcceso(eliminado);
        permisoRepository.save(permiso);
        log.info("Retiro de la página web: se quitó el permiso {}", permiso.getCodigo());
    }

    /** Nombre en minúsculas → nombre real (en Linux MySQL distingue mayúsculas). */
    Map<String, String> tablasExistentes() {
        List<String> nombres = jdbc.queryForList(
                "SELECT table_name FROM information_schema.tables WHERE table_schema = DATABASE()", String.class);
        Map<String, String> buscadas = new HashMap<>();
        TABLAS.forEach(t -> buscadas.put(t.toLowerCase(Locale.ROOT), t));
        Map<String, String> existentes = new HashMap<>();
        for (String nombre : nombres) {
            String clave = nombre.toLowerCase(Locale.ROOT);
            if (buscadas.containsKey(clave)) {
                existentes.put(clave, nombre);
            }
        }
        return existentes;
    }

    private boolean borrarImagenes(Map<String, String> tablas) {
        int fallidas = 0;
        for (String tabla : TABLAS_CON_IMAGENES) {
            String real = tablas.get(tabla.toLowerCase(Locale.ROOT));
            if (real == null) {
                continue;
            }
            List<String> ids = jdbc.queryForList(
                    "SELECT pk_url_foto FROM `" + real + "` WHERE pk_url_foto IS NOT NULL AND pk_url_foto <> ''", String.class);
            for (String id : ids) {
                try {
                    cloudinaryService.delete(id);
                } catch (RuntimeException e) {
                    fallidas++;
                    log.warn("Retiro de la página web: no se pudo borrar la imagen {}: {}", id, e.getMessage());
                }
            }
        }
        return fallidas == 0;
    }

    private void borrarTablas(Map<String, String> tablas) {
        jdbc.execute((ConnectionCallback<Void>) conexion -> {
            try (Statement st = conexion.createStatement()) {
                st.execute("SET FOREIGN_KEY_CHECKS = 0");
                try {
                    for (String tabla : TABLAS) {
                        String real = tablas.get(tabla.toLowerCase(Locale.ROOT));
                        if (real != null) {
                            st.execute("DROP TABLE IF EXISTS `" + real + "`");
                        }
                    }
                } finally {
                    st.execute("SET FOREIGN_KEY_CHECKS = 1");
                }
            }
            return null;
        });
    }
}
