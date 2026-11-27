package com.example.Escolar.Service;

import com.example.Escolar.Util.FechaColegio;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@RequiredArgsConstructor
public class SuspensionDocenteJob {

    private static final Logger log = LoggerFactory.getLogger(SuspensionDocenteJob.class);

    private final SuspensionDocenteService suspensionDocenteService;
    private final SuspensionDocenteProcesador procesador;

    @Scheduled(fixedDelayString = "${app.suspensiones-docente.cada-ms:60000}", initialDelay = 60000)
    public void procesar() {
        procesar(FechaColegio.hoy());
    }

    void procesar(LocalDate hoy) {
        for (Integer id : suspensionDocenteService.idsProgramadosActivables(hoy)) {
            try {
                procesador.activar(id, hoy);
            } catch (RuntimeException ex) {
                log.warn("No se pudo activar la suspensión programada {}: {}", id, ex.getMessage());
            }
        }
        for (Integer id : suspensionDocenteService.idsVigentesFinalizables(hoy)) {
            try {
                procesador.finalizar(id, hoy);
            } catch (RuntimeException ex) {
                log.warn("No se pudo finalizar la suspensión vigente {}: {}", id, ex.getMessage());
            }
        }
    }
}
