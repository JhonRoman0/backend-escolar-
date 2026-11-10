package com.example.Escolar.Service;

import com.example.Escolar.Model.*;
import jakarta.persistence.*;
import org.springframework.stereotype.Component;

/** Orden de escritura: matriculas por ID, asignaciones por ID, evaluaciones por ID. */
@Component
public class BloqueoCalificaciones {
    @PersistenceContext private EntityManager em;

    public void matricula(Integer id) { refrescar(Matricula.class, id); }
    public void asignacion(Integer id) { refrescar(Asignacion.class, id); }
    public void evaluacion(Integer id) { refrescar(Evaluacion.class, id); }
    public void nota(Integer id) { refrescar(NotaEvaluacion.class, id); }
    public void reconocimiento(Integer id) { refrescar(ReconocimientoCalificacion.class, id); }

    private <T> void refrescar(Class<T> tipo, Integer id) {
        T fila = em.find(tipo, id, LockModeType.PESSIMISTIC_WRITE);
        if (fila == null) throw new IllegalArgumentException("Registro no encontrado");
        em.refresh(fila, LockModeType.PESSIMISTIC_WRITE);
    }
}
