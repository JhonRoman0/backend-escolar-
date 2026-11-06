package com.example.Escolar.Service;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Lee el registro de notas (RegNotas) que descarga el SIAGIE. Todo se busca por
 * su texto y nunca por una fila fija, porque las posiciones cambian entre
 * niveles: la LEYENDA de inicial esta mas abajo que la de secundaria.
 *
 * - Hoja Generalidades: nivel, año, periodo y la lista de areas bajo "ÁREAS".
 * - Una hoja por area (por ejemplo "063-MATE"): sus competencias en la LEYENDA.
 */
@Component
public class PlantillaCursosParser {

    public record CompetenciaLeida(String codigo, String nombre, byte orden) {
    }

    public record AreaLeida(String codigo, String nombre, boolean transversal, List<CompetenciaLeida> competencias) {
    }

    public record PlantillaCursos(String nivel, String anio, String periodo, List<AreaLeida> areas,
                                  List<String> advertencias) {
    }

    // "063: MATE" o "0001: ART Y CULT"
    private static final Pattern CODIGO_AREA = Pattern.compile("^(\\d{3,4})\\s*:\\s*(.+)$");
    // "01 = Resuelve problemas de cantidad"
    private static final Pattern COMPETENCIA = Pattern.compile("^(\\d{1,2})\\s*=\\s*(.+)$");

    private final DataFormatter formato = new DataFormatter();

    public PlantillaCursos leer(MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) {
            throw new IllegalArgumentException("Debe seleccionar un archivo Excel para importar");
        }
        String nombreArchivo = archivo.getOriginalFilename() == null ? "" : archivo.getOriginalFilename().toLowerCase();
        if (!nombreArchivo.endsWith(".xlsx") && !nombreArchivo.endsWith(".xls")) {
            throw new IllegalArgumentException("El archivo debe ser un documento Excel válido (.xlsx)");
        }

        try (Workbook workbook = WorkbookFactory.create(archivo.getInputStream())) {
            Sheet generalidades = workbook.getSheet("Generalidades");
            if (generalidades == null) {
                throw new IllegalArgumentException(
                        "El archivo no tiene la hoja Generalidades del registro de notas del SIAGIE");
            }
            String nivel = valorDeEtiqueta(generalidades, "NIVEL");
            if (nivel == null) {
                throw new IllegalArgumentException("No se encontró el nivel en la hoja Generalidades");
            }
            String anio = valorDeEtiqueta(generalidades, "ANOACADEMICO");
            String periodo = valorDeEtiqueta(generalidades, "PERIODODEEVALUACION");

            List<String> advertencias = new ArrayList<>();
            List<AreaLeida> areas = leerAreas(workbook, generalidades, advertencias);
            if (areas.isEmpty()) {
                throw new IllegalArgumentException(
                        "No se encontraron áreas en la sección ÁREAS de la hoja Generalidades");
            }
            return new PlantillaCursos(nivel, anio, periodo, areas, advertencias);
        } catch (IOException e) {
            throw new IllegalArgumentException("No se pudo leer el archivo Excel: " + e.getMessage());
        }
    }

    private List<AreaLeida> leerAreas(Workbook workbook, Sheet generalidades, List<String> advertencias) {
        List<AreaLeida> areas = new ArrayList<>();
        Set<String> codigosVistos = new HashSet<>();
        boolean enAreas = false;
        for (Row row : generalidades) {
            List<String> valores = textos(row);
            if (!enAreas) {
                enAreas = valores.stream().anyMatch(v -> clave(v).equals("AREAS"));
                continue;
            }
            // En la fila el codigo va primero y el nombre en la celda siguiente.
            String codigo = null;
            String nombre = null;
            for (String valor : valores) {
                Matcher m = CODIGO_AREA.matcher(valor);
                if (codigo == null && m.matches()) {
                    codigo = m.group(1) + ": " + m.group(2).trim().replaceAll("\\s+", " ");
                } else if (codigo != null) {
                    nombre = valor;
                    break;
                }
            }
            if (codigo == null || nombre == null || !codigosVistos.add(codigo)) {
                continue;
            }

            Sheet hoja = hojaDelArea(workbook, codigo);
            if (hoja == null) {
                advertencias.add("El área " + nombre + " (" + codigo + ") no tiene hoja de notas y se omitió.");
                continue;
            }
            List<CompetenciaLeida> competencias = leerLeyenda(hoja);
            if (competencias.isEmpty()) {
                advertencias.add("El área " + nombre + " no trae competencias en su LEYENDA.");
            }
            // Una transversal (TIC, Gestion autonoma) tiene una sola competencia que
            // se llama igual que el area. Psicomotriz tambien tiene una sola, pero
            // con otro nombre, asi que sigue siendo un area normal.
            boolean transversal = competencias.size() == 1
                    && clave(competencias.get(0).nombre()).equals(clave(nombre));
            areas.add(new AreaLeida(codigo, nombre, transversal, competencias));
        }
        return areas;
    }

    // La hoja se llama como el codigo con guion: "063: MATE" -> "063-MATE".
    private Sheet hojaDelArea(Workbook workbook, String codigo) {
        String buscada = clave(codigo);
        String numero = codigo.substring(0, codigo.indexOf(':'));
        Sheet porPrefijo = null;
        for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
            String nombre = workbook.getSheetName(i);
            if (clave(nombre).equals(buscada)) {
                return workbook.getSheetAt(i);
            }
            if (porPrefijo == null && nombre.startsWith(numero + "-")) {
                porPrefijo = workbook.getSheetAt(i);
            }
        }
        return porPrefijo;
    }

    private List<CompetenciaLeida> leerLeyenda(Sheet hoja) {
        List<CompetenciaLeida> competencias = new ArrayList<>();
        Set<String> codigos = new HashSet<>();
        boolean enLeyenda = false;
        for (Row row : hoja) {
            for (String valor : textos(row)) {
                if (!enLeyenda) {
                    enLeyenda = clave(valor).equals("LEYENDA");
                    continue;
                }
                Matcher m = COMPETENCIA.matcher(valor);
                if (!m.matches()) {
                    continue;
                }
                int numero = Integer.parseInt(m.group(1));
                String codigo = String.format("%02d", numero);
                if (codigos.add(codigo)) {
                    competencias.add(new CompetenciaLeida(codigo, m.group(2).trim(), (byte) numero));
                }
            }
        }
        return competencias;
    }

    /** Valor de la primera celda con texto a la derecha de la etiqueta ("Nivel :" -> "SECUNDARIA"). */
    private String valorDeEtiqueta(Sheet hoja, String etiqueta) {
        for (Row row : hoja) {
            boolean encontrada = false;
            for (Cell cell : row) {
                String valor = formato.formatCellValue(cell).trim();
                if (valor.isEmpty()) {
                    continue;
                }
                if (encontrada) {
                    return valor;
                }
                encontrada = clave(valor).equals(etiqueta);
            }
        }
        return null;
    }

    private List<String> textos(Row row) {
        List<String> valores = new ArrayList<>();
        for (Cell cell : row) {
            String valor = formato.formatCellValue(cell).trim();
            if (!valor.isEmpty()) {
                valores.add(valor);
            }
        }
        return valores;
    }

    /** Mayusculas sin tildes ni signos: "Año académico :" -> "ANOACADEMICO". */
    public static String clave(String texto) {
        if (texto == null) {
            return "";
        }
        return Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replaceAll("[^A-Za-z0-9]", "")
                .toUpperCase();
    }
}
