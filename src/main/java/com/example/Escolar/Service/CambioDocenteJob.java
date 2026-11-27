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
public class CambioDocenteJob {

    private static final Logger log = LoggerFactory.getLogger(CambioDocenteJob.class);

    private final CambioDocenteService cambioDocenteService;
    private final CambioDocenteProcesador procesador;

    @Scheduled(fixedDelayString = "${app.cambios-docente.cada-ms:60000}", initialDelay = 60000)
    public void procesar() {
        procesar(FechaColegio.hoy());
    }

    void procesar(LocalDate hoy) {
        for (Integer id : cambioDocenteService.idsProgramadosActivables(hoy)) {
            try {
                procesador.activar(id, hoy);
            } catch (RuntimeException ex) {
                log.warn("No se pudo activar el cambio programado {}: {}", id, ex.getMessage());
            }
        }
        // Los programados se evalúan primero porque uno puede volverse vigente hoy.
        for (Integer id : cambioDocenteService.idsTemporalesFinalizables(hoy)) {
            try {
                procesador.finalizar(id, hoy);
            } catch (RuntimeException ex) {
                log.warn("No se pudo finalizar el cambio temporal {}: {}", id, ex.getMessage());
            }
        }
    }
}
