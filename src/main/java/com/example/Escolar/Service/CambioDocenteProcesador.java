package com.example.Escolar.Service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Component
@RequiredArgsConstructor
public class CambioDocenteProcesador {

    private final CambioDocenteService cambioDocenteService;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void activar(Integer idCambio, LocalDate hoy) {
        cambioDocenteService.activarProgramado(idCambio, hoy);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void finalizar(Integer idCambio, LocalDate hoy) {
        cambioDocenteService.finalizarAutomatico(idCambio, hoy);
    }
}
