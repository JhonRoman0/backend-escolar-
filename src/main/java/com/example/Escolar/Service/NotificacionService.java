package com.example.Escolar.Service;

import com.example.Escolar.Dto.NotificacionAsistenciaDto;

public interface NotificacionService {

    void notificarMarcado(NotificacionAsistenciaDto evento);
}