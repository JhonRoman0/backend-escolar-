package com.example.Escolar.Service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Component
@RequiredArgsConstructor
public class SuspensionDocenteProcesador {

    private final SuspensionDocenteService suspensionDocenteService;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void activar(Integer idSuspension, LocalDate hoy) {
        suspensionDocenteService.activarProgramado(idSuspension, hoy);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void finalizar(Integer idSuspension, LocalDate hoy) {
        suspensionDocenteService.finalizarAutomatico(idSuspension, hoy);
    }
}
