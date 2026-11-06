package com.example.Escolar.Service;

import com.example.Escolar.Service.PlantillaCursosParser.AreaLeida;
import com.example.Escolar.Service.PlantillaCursosParser.PlantillaCursos;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Usa copias reales del RegNotas del SIAGIE (inicial y secundaria) sin las
 * filas de alumnos ni los datos del colegio.
 */
class PlantillaCursosParserTest {

    private final PlantillaCursosParser parser = new PlantillaCursosParser();

    static MockMultipartFile excel(String recurso) throws IOException {
        try (InputStream in = PlantillaCursosParserTest.class.getResourceAsStream("/siagie/" + recurso)) {
            return new MockMultipartFile("archivo", recurso,
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", in);
        }
    }

    private AreaLeida area(PlantillaCursos plantilla, String codigo) {
        return plantilla.areas().stream().filter(a -> a.codigo().equals(codigo)).findFirst().orElseThrow();
    }

    @Test
    void leeSecundariaConSusAreasCompetenciasYTransversales() throws IOException {
        PlantillaCursos plantilla = parser.leer(excel("regnotas-secundaria.xlsx"));

        assertEquals("SECUNDARIA", plantilla.nivel());
        assertEquals("2026", plantilla.anio());
        assertEquals("PRIMER BIMESTRE", plantilla.periodo());
        assertEquals(13, plantilla.areas().size());
        assertTrue(plantilla.advertencias().isEmpty());

        AreaLeida matematica = area(plantilla, "063: MATE");
        assertEquals("MATEMÁTICA", matematica.nombre());
        assertFalse(matematica.transversal());
        assertEquals(List.of("01", "02", "03", "04"),
                matematica.competencias().stream().map(c -> c.codigo()).toList());
        assertEquals("Resuelve problemas de cantidad", matematica.competencias().get(0).nombre());
        assertEquals(4, matematica.competencias().get(3).orden());

        List<String> transversales = plantilla.areas().stream()
                .filter(AreaLeida::transversal).map(AreaLeida::codigo).toList();
        assertEquals(List.of("0006: DESEN TIC", "0007: GEST AUTO"), transversales);
    }

    @Test
    void leeInicialAunqueLaLeyendaEsteEnOtraFila() throws IOException {
        PlantillaCursos plantilla = parser.leer(excel("regnotas-inicial.xlsx"));

        assertEquals("INICIAL - JARDÍN", plantilla.nivel());
        assertEquals("2025", plantilla.anio());
        assertEquals("SEGUNDO TRIMESTRE", plantilla.periodo());
        assertEquals(List.of("0004: CIENC TEC", "0005: COMU", "0008: PSICO", "063: MATE", "067: PPSS"),
                plantilla.areas().stream().map(AreaLeida::codigo).toList());
        assertEquals(4, area(plantilla, "0005: COMU").competencias().size());
        assertEquals(3, area(plantilla, "067: PPSS").competencias().size());

        // Una sola competencia, pero con otro nombre que el area: no es transversal.
        AreaLeida psicomotriz = area(plantilla, "0008: PSICO");
        assertEquals(1, psicomotriz.competencias().size());
        assertFalse(psicomotriz.transversal());
        assertTrue(plantilla.areas().stream().noneMatch(AreaLeida::transversal));
    }

    @Test
    void rechazaUnArchivoQueNoEsExcel() {
        MockMultipartFile pdf = new MockMultipartFile("archivo", "notas.pdf", "application/pdf", new byte[]{1});
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> parser.leer(pdf));
        assertTrue(ex.getMessage().contains(".xlsx"));
    }

    @Test
    void rechazaUnExcelSinLaHojaGeneralidades() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            workbook.createSheet("Hoja1").createRow(0).createCell(0).setCellValue("Cualquier cosa");
            workbook.write(out);
        }
        MockMultipartFile archivo = new MockMultipartFile("archivo", "otro.xlsx", "application/octet-stream",
                out.toByteArray());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> parser.leer(archivo));
        assertTrue(ex.getMessage().contains("Generalidades"));
    }
}
