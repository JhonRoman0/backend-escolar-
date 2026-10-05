package com.example.Escolar.Security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.stereotype.Controller;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.ClassUtils;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * El RbacAuthorizationManager es fail closed: si un endpoint no aparece en el
 * ApiPermisoRegistro devuelve 403 aunque el rol tenga todos los permisos. Este
 * test recorre los controladores y falla si algún endpoint expuesto se queda
 * sin registrar, que es como se rompió el PATCH de estado del año escolar.
 *
 * Es unitario a propósito: no levanta el contexto de Spring ni toca la base de
 * datos, así que corre en cualquier máquina sin configuración previa.
 */
class ApiPermisoRegistroCoberturaTest {

    private static final String PAQUETE_BASE = "com.example.Escolar";

    private static final List<String> VERBOS =
            Arrays.stream(RequestMethod.values()).map(RequestMethod::name).toList();

    /**
     * Rutas que SecurityConfig resuelve antes de llegar al RBAC, en sus
     * .requestMatchers(...).permitAll() y .authenticated(). No pasan por el
     * ApiPermisoRegistro, así que no se exigen aquí.
     *
     * Refleja SecurityConfig.securityFilterChain. Si allí se añade o se quita una
     * ruta hay que tocar esto también: quitar una sin tocarla deja el test en
     * verde mientras el endpoint vuelve a dar 403.
     */
    private static final List<String> RESUELTAS_POR_CONFIG = List.of(
            "/auth/login",
            "/auth/logout",
            "/auth/forgot-password",
            "/auth/reset-password",
            "/auth/me",
            "/consulta/dni/**",
            "/portal/**",
            "/swagger-ui.html",
            "/swagger-ui/**",
            "/v3/api-docs/**");

    private static final AntPathMatcher MATCHER = new AntPathMatcher();

    private static boolean resueltaPorConfig(String ruta) {
        return RESUELTAS_POR_CONFIG.stream().anyMatch(patron -> MATCHER.match(patron, ruta));
    }

    @Test
    void todoEndpointExpuestoEstaRegistradoEnElCatalogo() throws ClassNotFoundException {
        ApiPermisoRegistro registro = new ApiPermisoRegistro();
        List<Class<?>> controladores = controllers();
        List<String> faltantes = new ArrayList<>();

        for (Class<?> controlador : controladores) {
            String base = normaliza(rutaDe(AnnotatedElementUtils
                    .findMergedAnnotation(controlador, RequestMapping.class)));
            for (Method metodo : controlador.getDeclaredMethods()) {
                if (metodo.isSynthetic() || metodo.isBridge()) {
                    continue;
                }
                // findMergedAnnotation también resuelve @GetMapping, @PatchMapping y
                // compañía: todas son @RequestMapping con método fijo.
                RequestMapping mapping =
                        AnnotatedElementUtils.findMergedAnnotation(metodo, RequestMapping.class);
                if (mapping == null) {
                    continue;
                }
                String ruta = normaliza(base + primeraRuta(mapping));
                if (resueltaPorConfig(ruta)) {
                    continue;
                }
                String[] verbos = verbosDe(mapping);
                for (String verbo : verbos) {
                    if (registro.buscar(verbo, ruta).isEmpty()) {
                        faltantes.add(verbo + " " + ruta + "  (" + controlador.getSimpleName()
                                + "." + metodo.getName() + ")");
                    }
                }
            }
        }

        assertFalse(controladores.isEmpty(),
                "No se encontró ningún controlador en " + PAQUETE_BASE
                        + ": el escaneo está roto y el test passing no significaría nada");

        assertTrue(faltantes.isEmpty(), () -> "Endpoints sin registrar en ApiPermisoRegistro; "
                + "el RbacAuthorizationManager los deniega con 403 aunque el rol tenga permisos:\n  "
                + String.join("\n  ", faltantes));
    }

    private static List<Class<?>> controllers() throws ClassNotFoundException {
        ClassPathScanningCandidateComponentProvider escaner =
                new ClassPathScanningCandidateComponentProvider(false);
        escaner.addIncludeFilter(new AnnotationTypeFilter(RestController.class));
        escaner.addIncludeFilter(new AnnotationTypeFilter(Controller.class));

        List<Class<?>> encontrados = new ArrayList<>();
        for (BeanDefinition definicion : escaner.findCandidateComponents(PAQUETE_BASE)) {
            Class<?> clase = ClassUtils.forName(definicion.getBeanClassName(),
                    ClassUtils.getDefaultClassLoader());
            // @RestControllerAdvice está anotado con @Controller, pero responde a
            // excepciones: sus métodos son @ExceptionHandler, no rutas HTTP.
            if (AnnotatedElementUtils.hasAnnotation(clase, ControllerAdvice.class)) {
                continue;
            }
            encontrados.add(clase);
        }
        return encontrados;
    }

    private static String[] verbosDe(RequestMapping mapping) {
        if (mapping.method().length == 0) {
            // Sin verbo fijo responde a todos: hay que comprobar cada uno.
            return VERBOS.toArray(String[]::new);
        }
        return Arrays.stream(mapping.method()).map(RequestMethod::name).toArray(String[]::new);
    }

    private static String primeraRuta(RequestMapping mapping) {
        String[] rutas = mapping.value().length > 0 ? mapping.value() : mapping.path();
        return rutas.length > 0 ? rutas[0] : "";
    }

    private static String rutaDe(RequestMapping mapping) {
        return mapping == null ? "" : primeraRuta(mapping);
    }

    private static String normaliza(String ruta) {
        String limpia = ruta.replaceAll("\\{[^}]*}", "1").replaceAll("/{2,}", "/");
        if (limpia.isEmpty()) {
            return "/";
        }
        if (!limpia.startsWith("/")) {
            limpia = "/" + limpia;
        }
        return limpia.length() > 1 ? limpia.replaceAll("/+$", "") : limpia;
    }
}
