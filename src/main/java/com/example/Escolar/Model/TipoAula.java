package com.example.Escolar.Model;

/**
 * SALON: el aula fija de una seccion (una por seccion, por año y turno).
 * AMBIENTE: laboratorio, computo... lo usan todas las secciones, una clase a la vez.
 * COMPARTIDO: patio, losa, auditorio... admite varias clases a la misma hora.
 */
public enum TipoAula {
    SALON,
    AMBIENTE,
    COMPARTIDO
}
