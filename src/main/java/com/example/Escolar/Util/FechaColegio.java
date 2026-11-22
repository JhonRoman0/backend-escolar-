package com.example.Escolar.Util;

import java.time.LocalDate;
import java.time.ZoneId;

public final class FechaColegio {

    public static final ZoneId ZONA = ZoneId.of("America/Lima");

    private FechaColegio() {
    }

    public static LocalDate hoy() {
        return LocalDate.now(ZONA);
    }
}
