package com.example.Escolar.Security;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.ClassUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ApiPermisoRegistroCoberturaTest {

    private static final String PAQUETE_CONTROLLERS = "com.example.Escolar.Controller";

    private static final List<String> VERBOS = List.of("GET", "POST", "PUT", "DELETE");

    private final ApiPermisoRegistro registro = new ApiPermisoRegistro();

    private final AntPathMatcher matcher = new AntPathMatcher();

    @Test
    void todoEndpointSujetoARbacTienePermisoRegistrado() {
        List<String> faltantes = new ArrayList<>();

        for (Class<?> controller : controllers()) {
            String prefijo = prefijoDe(controller);
            for (Method metodo : controller.getDeclaredMethods()) {
                if (metodo.isSynthetic() || metodo.isBridge()) {
                    continue;
                }
                RequestMapping mapeo = AnnotatedElementUtils.findMergedAnnotation(metodo, RequestMapping.class);
                if (mapeo == null) {
                    continue;
                }
                for (String patron : patrones(mapeo)) {
                    String ruta = prefijo + patron;
                    if (exento(ruta)) {
                        continue;
                    }
                    for (String verbo : verbos(mapeo)) {
                        if (registro.buscar(verbo, ruta).isEmpty()) {
                            faltantes.add(verbo + " " + ruta + "  (" + controller.getSimpleName() + ")");
                        }
                    }
                }
            }
        }

        faltantes.sort(Comparator.naturalOrder());

        assertTrue(faltantes.isEmpty(), () -> "Endpoints sin registro en ApiPermisoRegistro (respondiendo 403 "
                + "para todos los roles, incluido ADMIN). Registralos o marcalos como RUTAS_PUBLICAS/"
                + "RUTAS_SOLO_AUTENTICADAS en SecurityConfig:\n  " + String.join("\n  ", faltantes));
    }

    @Test
    void elLoteDeSeccionesHeredaElMismoPermisoQueLaCreacionIndividual() {
        var individual = registro.buscar("POST", "/secciones");
        var lote = registro.buscar("POST", "/secciones/lote");

        assertTrue(individual.isPresent(), "POST /secciones debe seguir registrado");
        assertTrue(lote.isPresent(), "POST /secciones/lote debe estar registrado");
        assertTrue(individual.get().equals(lote.get()),
                "El lote debe usar el mismo permiso/accion que la creacion individual: individual="
                        + individual.get() + ", lote=" + lote.get());
    }

    private Set<Class<?>> controllers() {
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(RestController.class));

        Set<Class<?>> encontrados = new LinkedHashSet<>();
        scanner.findCandidateComponents(PAQUETE_CONTROLLERS)
                .forEach(definicion -> {
                    try {
                        encontrados.add(ClassUtils.forName(definicion.getBeanClassName(), getClass().getClassLoader()));
                    } catch (ClassNotFoundException | LinkageError ignorada) {
                    }
                });
        return encontrados;
    }

    private String prefijoDe(Class<?> controller) {
        RequestMapping mapeo = AnnotatedElementUtils.findMergedAnnotation(controller, RequestMapping.class);
        if (mapeo == null || patrones(mapeo).isEmpty()) {
            return "";
        }
        StringBuilder prefijo = new StringBuilder();
        for (String patron : patrones(mapeo)) {
            prefijo.append(patron);
        }
        return prefijo.toString();
    }

    private List<String> patrones(RequestMapping mapeo) {
        Set<String> valores = new LinkedHashSet<>(List.of(mapeo.path()));
        valores.addAll(List.of(mapeo.value()));
        return List.copyOf(valores);
    }

    private List<String> verbos(RequestMapping mapeo) {
        if (mapeo.method().length == 0) {
            return VERBOS;
        }
        return List.of(mapeo.method()).stream().map(Enum::name).toList();
    }

    private boolean exento(String ruta) {
        List<String> excepciones = new ArrayList<>(SecurityConfig.RUTAS_PUBLICAS);
        excepciones.addAll(SecurityConfig.RUTAS_SOLO_AUTENTICADAS);
        return excepciones.stream().anyMatch(patron -> matcher.match(patron, ruta));
    }
}