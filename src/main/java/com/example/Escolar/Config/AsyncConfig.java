package com.example.Escolar.Config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Envíos en segundo plano y tareas programadas (avisos de falta). */
@Configuration
@EnableAsync
@EnableScheduling
public class AsyncConfig {
}
