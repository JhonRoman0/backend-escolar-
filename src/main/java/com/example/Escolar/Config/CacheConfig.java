package com.example.Escolar.Config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Caché en memoria para datos que no cambian mientras corre el sistema
 * (los estados Activo/Eliminado/Inactivo). Sin esto, cada lectura de "Eliminado"
 * era una consulta a la base, a veces una por fila de un listado.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    public static final String ACCESOS = "accesos";

    @Bean
    public CacheManager cacheManager() {
        return new ConcurrentMapCacheManager(ACCESOS);
    }
}
