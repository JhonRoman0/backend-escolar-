package com.example.Escolar.Service;

import com.example.Escolar.Dto.NotificacionAsistenciaDto;

/** Avisos de asistencia a los apoderados (correo, SMS o simulado). */
public interface NotificacionService {

    void notificar(NotificacionAsistenciaDto evento);
}
