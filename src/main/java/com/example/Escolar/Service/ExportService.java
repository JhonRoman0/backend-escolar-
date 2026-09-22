package com.example.Escolar.Service;

import com.example.Escolar.Exception.ResourceNotFoundException;
import com.example.Escolar.Model.Colegio;
import com.example.Escolar.Model.Competencia;
import com.example.Escolar.Model.GradoSeccion;
import com.example.Escolar.Model.Matricula;
import com.example.Escolar.Model.Nota;
import com.example.Escolar.Model.PlantillaSiagie;
import com.example.Escolar.Repository.AccesoRepository;
import com.example.Escolar.Repository.ColegioRepository;
import com.example.Escolar.Repository.CompetenciaRepository;
import com.example.Escolar.Repository.GradoSeccionRepository;
import com.example.Escolar.Repository.MatriculaRepository;
import com.example.Escolar.Repository.NotaRepository;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class ExportService {

    private static final Logger log = LoggerFactory.getLogger(ExportService.class);

    private final PlantillaSiagieService plantillaService;
    private final NotaRepository notaRepository;
    private final MatriculaRepository matriculaRepository;
    private final CompetenciaRepository competenciaRepository;
    private final GradoSeccionRepository gradoSeccionRepository;
    private final ColegioRepository colegioRepository;
    private final AccesoRepository accesoRepository;

    public String nombreArchivoSiagie(Integer idGradoSeccion, Byte bimestre) {
        return "SIAGIE_GS" + idGradoSeccion + "_B" + bimestre + ".xlsx";
    }

    public byte[] exportarSiagie(Integer idGradoSeccion, Byte bimestre) {
        PlantillaSiagie plantilla = plantillaService.obtenerVigente();
        GradoSeccion gs = findGradoSeccion(idGradoSeccion);
        Colegio colegio = findColegioOrNull();

        List<Matricula> matriculas = matriculaRepository.findByGradoSeccionIdGradoSeccionAndAccesoNot(
                idGradoSeccion, accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow());

        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(plantilla.getArchivo()))) {
            Sheet sheet = workbook.getSheetAt(0);

            Map<String, int[]> competenciaColMap = detectarCompetencias(sheet);
            int dataStartRow = detectarFilaDatos(sheet);
            String[] bimestreNombres = {"", "I Bimestre", "II Bimestre", "III Bimestre", "IV Bimestre"};

            rellenarMetadatos(sheet, gs, colegio, bimestre <= bimestreNombres.length
                    ? bimestreNombres[bimestre] : bimestre + "° Bimestre");

            Map<String, Map<Integer, Nota>> notasMap = cargarNotas(matriculas, bimestre);

            int lastDataRow = dataStartRow;
            int fila = dataStartRow;
            for (Matricula mat : matriculas) {
                Row row = sheet.getRow(fila);
                if (row == null) row = sheet.createRow(fila);

                String codigo = mat.getAlumnoApoderado().getAlumno().getCodigo();
                String nombre = mat.getAlumnoApoderado().getAlumno().getApellidoPat() + ", "
                        + mat.getAlumnoApoderado().getAlumno().getNombre();

                writeOrCreateCell(row, 0, codigo);
                writeOrCreateCell(row, 1, nombre);

                Map<Integer, Nota> notasAlumno = notasMap.getOrDefault(mat.getIdMatricula(), new LinkedHashMap<>());
                for (Map.Entry<String, int[]> entry : competenciaColMap.entrySet()) {
                    String nombreCompetencia = entry.getKey();
                    int[] cols = entry.getValue();

                    Optional<Nota> notaOpt = notasAlumno.values().stream()
                            .filter(n -> n.getCompetencia().getNombre().equals(nombreCompetencia))
                            .findFirst();

                    Cell califCell = row.getCell(cols[0]);
                    if (califCell == null) califCell = row.createCell(cols[0]);
                    Cell concCell = row.getCell(cols[1]);
                    if (concCell == null) concCell = row.createCell(cols[1]);

                    if (notaOpt.isPresent()) {
                        Nota nota = notaOpt.get();
                        califCell.setCellValue(nota.getCalificacion() != null ? nota.getCalificacion() : "");
                        concCell.setCellValue(nota.getConclusionDescriptiva() != null
                                ? nota.getConclusionDescriptiva() : "");
                    } else {
                        califCell.setBlank();
                        concCell.setBlank();
                    }
                }
                lastDataRow = fila;
                fila++;
            }

            limpiarFilasRestantes(sheet, lastDataRow + 1, 50);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();

        } catch (IOException e) {
            throw new RuntimeException("Error al generar el archivo SIAGIE", e);
        }
    }

    private void rellenarMetadatos(Sheet sheet, GradoSeccion gs, Colegio colegio, String bimestreTexto) {
        for (Row row : sheet) {
            if (row == null) continue;
            for (Cell cell : row) {
                if (cell == null || cell.getCellType() != CellType.STRING) continue;
                String val = cell.getStringCellValue().trim();
                if (val.equals("Institución Educativa:")) {
                    Cell target = findAdjacentValueCell(row, cell.getColumnIndex());
                    if (target != null && colegio != null) {
                        target.setCellValue(colegio.getNombre());
                    }
                } else if (val.equals("Nivel / Grado / Sección:")) {
                    Cell target = findAdjacentValueCell(row, cell.getColumnIndex());
                    if (target != null) {
                        String nivel = gs.getGrado().getNivel() != null
                                ? gs.getGrado().getNivel().getNombre() : "";
                        String grado = gs.getGrado().getNombre();
                        String seccion = gs.getSeccion() != null ? gs.getSeccion().getNombre() : "Única";
                        target.setCellValue(nivel + " / " + grado + " / " + seccion);
                    }
                } else if (val.equals("Periodo Evaluado:")) {
                    Cell target = findAdjacentValueCell(row, cell.getColumnIndex());
                    if (target != null) {
                        target.setCellValue(bimestreTexto);
                    }
                }
            }
        }
    }

    private Cell findAdjacentValueCell(Row row, int labelCol) {
        for (int c = labelCol + 1; c <= Math.min(labelCol + 5, row.getLastCellNum()); c++) {
            Cell cell = row.getCell(c);
            if (cell != null && (cell.getCellType() == CellType.STRING || cell.getCellType() == CellType.BLANK)) {
                return cell;
            }
        }
        return row.createCell(labelCol + 2);
    }

    private Map<String, int[]> detectarCompetencias(Sheet sheet) {
        Pattern compPattern = Pattern.compile("Comp\\s+\\d+:\\s*(.+)", Pattern.CASE_INSENSITIVE);
        Map<String, int[]> result = new LinkedHashMap<>();

        for (Row row : sheet) {
            if (row == null) continue;
            for (Cell cell : row) {
                if (cell == null || cell.getCellType() != CellType.STRING) continue;
                Matcher m = compPattern.matcher(cell.getStringCellValue().trim());
                if (m.matches()) {
                    String nombre = m.group(1).trim();
                    int colInicio = cell.getColumnIndex();
                    int colFin = colInicio + 1;
                    result.put(nombre, new int[]{colInicio, colFin});
                }
            }
        }
        return result;
    }

    private int detectarFilaDatos(Sheet sheet) {
        for (Row row : sheet) {
            if (row == null) continue;
            for (Cell cell : row) {
                if (cell == null || cell.getCellType() != CellType.STRING) continue;
                String val = cell.getStringCellValue().trim();
                if (val.equalsIgnoreCase("Código SIAGIE") || val.equalsIgnoreCase("Código SIAGIE")) {
                    return row.getRowNum() + 2;
                }
            }
        }
        return 10;
    }

    private Map<String, Map<Integer, Nota>> cargarNotas(List<Matricula> matriculas, Byte bimestre) {
        Map<String, Map<Integer, Nota>> map = new LinkedHashMap<>();
        for (Matricula mat : matriculas) {
            List<Nota> notas = notaRepository
                    .findByMatriculaIdMatriculaAndBimestreAndAccesoNot(mat.getIdMatricula(), bimestre, accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow());
            Map<Integer, Nota> notasPorComp = new LinkedHashMap<>();
            for (Nota n : notas) {
                notasPorComp.put(n.getCompetencia().getIdCompetencia(), n);
            }
            map.put(mat.getAlumnoApoderado().getAlumno().getCodigo(), notasPorComp);
        }
        return map;
    }

    private void writeOrCreateCell(Row row, int col, String value) {
        Cell cell = row.getCell(col);
        if (cell == null) cell = row.createCell(col);
        cell.setCellValue(value);
    }

    private void limpiarFilasRestantes(Sheet sheet, int desde, int maxFilasLimpiar) {
        for (int i = desde; i < Math.min(desde + maxFilasLimpiar, sheet.getLastRowNum() + 1); i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;
            boolean hasData = false;
            for (Cell c : row) {
                if (c != null && c.getCellType() != CellType.BLANK
                        && !(c.getCellType() == CellType.STRING && c.getStringCellValue().trim().isEmpty())) {
                    hasData = true;
                    break;
                }
            }
            if (hasData) {
                int lastCol = row.getLastCellNum();
                for (int col = 0; col < lastCol; col++) {
                    Cell c = row.getCell(col);
                    if (c != null) c.setBlank();
                }
            }
        }
    }

    private GradoSeccion findGradoSeccion(Integer id) {
        return gradoSeccionRepository.findByIdGradoSeccionAndAccesoNot(id, accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow())
                .orElseThrow(() -> new ResourceNotFoundException("Grado-Sección no encontrado con id " + id));
    }

    private Colegio findColegioOrNull() {
        return colegioRepository.findFirstByAccesoNot(accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow()).orElse(null);
    }
}
