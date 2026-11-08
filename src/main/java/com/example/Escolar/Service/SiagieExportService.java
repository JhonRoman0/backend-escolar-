package com.example.Escolar.Service;

import com.example.Escolar.Dto.ConfiguracionAcademicaResponse;
import com.example.Escolar.Exception.ResourceNotFoundException;
import com.example.Escolar.Model.*;
import com.example.Escolar.Repository.*;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Registro de notas de una sección y periodo con el formato del RegNotas del
 * SIAGIE: Generalidades y una hoja por área. Sin la columna ID del
 * original; grado y sección van en texto. Las celdas validan según la escala.
 */
@Service
@RequiredArgsConstructor
public class SiagieExportService {

    private static final String DISENO_CURRICULAR = "CURRÍCULO NACIONAL 2017";
    private static final int PRIMERA_FILA_ALUMNOS = 2;

    private final NotaRepository notaRepository;
    private final MatriculaRepository matriculaRepository;
    private final GradoSeccionRepository gradoSeccionRepository;
    private final ColegioRepository colegioRepository;
    private final AccesoRepository accesoRepository;
    private final ConfiguracionAcademicaService configuracionService;
    private final NotaFinalService notaFinalService;
    private final MallaEvaluable mallaEvaluable;

    public record Archivo(String nombre, byte[] contenido) {
    }

    public record Pendientes(int alumnos, int esperadas, int faltantes, List<AreaPendiente> areas) {
        public record AreaPendiente(String area, int faltantes) {
        }
    }

    private record Datos(GradoSeccion gs, ConfiguracionAcademicaResponse config, int periodo, List<MallaEvaluable.Area> areas,
                         List<Matricula> alumnos, Map<String, Nota> notas) {
    }

    /** Cuántas notas finales faltan, para avisar antes de descargar. */
    @Transactional(readOnly = true)
    public Pendientes pendientes(Integer idGradoSeccion, Integer periodo, Integer idUsuario, List<String> roles) {
        Datos d = datos(idGradoSeccion, periodo, idUsuario, roles);
        List<Pendientes.AreaPendiente> areas = new ArrayList<>();
        int esperadas = 0;
        int faltantes = 0;
        for (MallaEvaluable.Area area : d.areas()) {
            int faltanArea = 0;
            for (Matricula m : d.alumnos()) {
                for (Competencia c : area.competencias()) {
                    esperadas++;
                    if (!d.notas().containsKey(clave(m, c))) {
                        faltanArea++;
                    }
                }
            }
            faltantes += faltanArea;
            if (faltanArea > 0) {
                areas.add(new Pendientes.AreaPendiente(area.curso().getNombre(), faltanArea));
            }
        }
        return new Pendientes(d.alumnos().size(), esperadas, faltantes, areas);
    }

    @Transactional(readOnly = true)
    public Archivo exportar(Integer idGradoSeccion, Integer periodo, Integer idUsuario, List<String> roles) {
        Datos d = datos(idGradoSeccion, periodo, idUsuario, roles);
        try (XSSFWorkbook libro = new XSSFWorkbook(); ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
            Estilos estilos = new Estilos(libro);
            Optional<Colegio> colegio = colegioRepository.findFirstByAccesoNot(eliminado());
            generalidades(libro, estilos, d, colegio);
            Set<String> nombres = new HashSet<>(Set.of("Generalidades"));
            for (MallaEvaluable.Area area : d.areas()) {
                hojaArea(libro, estilos, d, area, nombreHoja(area.curso(), nombres));
            }
            libro.write(salida);
            return new Archivo(nombreArchivo(d), salida.toByteArray());
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo generar el Excel", e);
        }
    }

    // ── Datos ──

    private Datos datos(Integer idGradoSeccion, Integer periodo, Integer idUsuario, List<String> roles) {
        GradoSeccion gs = gradoSeccionRepository.findById(idGradoSeccion)
                .filter(g -> !AccesoConstants.ELIMINADO.equals(g.getAcceso().getIdAcceso()))
                .orElseThrow(() -> new ResourceNotFoundException("Sección no encontrada"));
        if (!notaFinalService.puedeVerSeccion(gs, idUsuario, roles)) {
            throw new ResourceNotFoundException("Sección no encontrada o fuera de tus asignaciones");
        }
        AnioEscolar anio = gs.getAnioEscolar();
        Nivel nivel = gs.getGrado().getNivel();
        boolean existe = configuracionService.periodos(anio, nivel).stream().anyMatch(p -> periodo != null && p.getNumero() == periodo);
        if (!existe) {
            throw new IllegalArgumentException("El periodo no existe: configúralo en Configuración académica");
        }
        Acceso eliminado = eliminado();
        List<MallaEvaluable.Area> areas = mallaEvaluable.areas(anio, nivel);
        List<Matricula> alumnos = matriculaRepository.findByGradoSeccionIdGradoSeccionAndAccesoNot(idGradoSeccion, eliminado).stream()
                .filter(m -> AccesoConstants.ACTIVO.equals(m.getAcceso().getIdAcceso()))
                .sorted(MatriculaService.ORDEN_REPORTE)
                .toList();
        Map<String, Nota> notas = notaRepository
                .findByMatriculaGradoSeccionIdGradoSeccionAndBimestreAndAccesoNot(idGradoSeccion, periodo.byteValue(), eliminado)
                .stream()
                .collect(Collectors.toMap(n -> clave(n.getMatricula(), n.getCompetencia()), Function.identity(), (a, b) -> a));
        return new Datos(gs, configuracionService.obtener(anio.getIdAnio(), nivel.getIdNivel()), periodo, areas, alumnos, notas);
    }

    // ── Hojas ──

    private void generalidades(Workbook libro, Estilos e, Datos d, Optional<Colegio> colegio) {
        Sheet hoja = libro.createSheet("Generalidades");
        hoja.setDisplayGridlines(false);
        texto(hoja, 1, 1, "DATOS GENERALES :", e.titulo);
        barra(hoja, 2, "Institución educativa :", e);
        texto(hoja, 3, 1, "Código modular - Anexo :", e.etiqueta);
        valorCombinado(hoja, 3, 2, 3, colegio.map(Colegio::getCodigoColegio).orElse(""), e);
        texto(hoja, 3, 5, "Nivel :", e.etiqueta);
        valorCombinado(hoja, 3, 6, 9, nivel(d).toUpperCase(), e);
        texto(hoja, 4, 1, "Nombre :", e.etiqueta);
        valorCombinado(hoja, 4, 2, 9, colegio.map(Colegio::getNombre).orElse(""), e);
        barra(hoja, 6, "Datos referentes al registro de notas :", e);
        texto(hoja, 7, 1, "Año académico :", e.etiqueta);
        valorCombinado(hoja, 7, 2, 3, d.gs().getAnioEscolar().getAnio(), e);
        texto(hoja, 8, 1, "Diseño curricular :", e.etiqueta);
        valorCombinado(hoja, 8, 2, 9, DISENO_CURRICULAR, e);
        texto(hoja, 9, 1, "Período de evaluación :", e.etiqueta);
        valorCombinado(hoja, 9, 2, 3, nombrePeriodo(d), e);
        texto(hoja, 9, 4, "Grado :", e.etiqueta);
        valorCombinado(hoja, 9, 5, 6, grado(d), e);
        texto(hoja, 9, 7, "Sección :", e.etiqueta);
        valorCombinado(hoja, 9, 8, 9, seccion(d), e);
        barra(hoja, 11, "ÁREAS", e);
        int fila = 12;
        for (MallaEvaluable.Area area : d.areas()) {
            texto(hoja, fila, 1, area.curso().getCodigo() != null ? area.curso().getCodigo() : "", e.dato);
            valorCombinado(hoja, fila, 2, 9, area.curso().getNombre(), e);
            fila++;
        }
        int[] anchos = {2, 26, 14, 14, 9, 12, 10, 10, 12, 12};
        for (int c = 0; c < anchos.length; c++) {
            hoja.setColumnWidth(c, anchos[c] * 256);
        }
    }

    /** Franja verde de título, de la columna B a la J. */
    private static void barra(Sheet hoja, int fila, String texto, Estilos e) {
        Row row = hoja.getRow(fila) != null ? hoja.getRow(fila) : hoja.createRow(fila);
        row.setHeightInPoints(16);
        for (int c = 1; c <= 9; c++) {
            celda(row, c, c == 1 ? texto : "", e.barra);
        }
        hoja.addMergedRegion(new CellRangeAddress(fila, fila, 1, 9));
    }

    /** Valor con borde, combinado en las columnas indicadas. */
    private static void valorCombinado(Sheet hoja, int fila, int desde, int hasta, String valor, Estilos e) {
        Row row = hoja.getRow(fila) != null ? hoja.getRow(fila) : hoja.createRow(fila);
        for (int c = desde; c <= hasta; c++) {
            celda(row, c, c == desde ? valor : "", e.dato);
        }
        if (hasta > desde) {
            hoja.addMergedRegion(new CellRangeAddress(fila, fila, desde, hasta));
        }
    }

    private void hojaArea(Workbook libro, Estilos e, Datos d, MallaEvaluable.Area area, String nombre) {
        Sheet hoja = libro.createSheet(nombre);
        boolean literal = d.config().getEscala() == EscalaCalificacion.LITERAL;
        Row codigos = hoja.createRow(0);
        Row titulos = hoja.createRow(1);
        celda(codigos, 0, "Cód. Estudiante", e.encabezado);
        celda(titulos, 0, "Cód. Estudiante", e.encabezado);
        celda(codigos, 1, "Nombres", e.encabezado);
        celda(titulos, 1, "Nombres", e.encabezado);
        hoja.addMergedRegion(new CellRangeAddress(0, 1, 0, 0));
        hoja.addMergedRegion(new CellRangeAddress(0, 1, 1, 1));
        int col = 2;
        for (Competencia c : area.competencias()) {
            celda(codigos, col, c.getCodigo(), e.encabezado);
            celda(codigos, col + 1, c.getCodigo(), e.encabezado);
            hoja.addMergedRegion(new CellRangeAddress(0, 0, col, col + 1));
            celda(titulos, col, literal ? "NL" : "Nota", e.encabezado);
            celda(titulos, col + 1, "Conclusión descriptiva de la competencia", e.encabezado);
            hoja.setColumnWidth(col, 12 * 256);
            hoja.setColumnWidth(col + 1, 45 * 256);
            col += 2;
        }

        int fila = PRIMERA_FILA_ALUMNOS;
        for (Matricula m : d.alumnos()) {
            Alumno a = m.getAlumnoApoderado().getAlumno();
            Row row = hoja.createRow(fila++);
            celda(row, 0, a.getCodigo(), e.dato);
            celda(row, 1, a.getApellidoPat() + " " + a.getApellidoMat() + ", " + a.getNombre(), e.dato);
            int c = 2;
            for (Competencia competencia : area.competencias()) {
                Nota nota = d.notas().get(clave(m, competencia));
                Cell celdaNota = row.createCell(c);
                celdaNota.setCellStyle(e.nota);
                if (nota != null) {
                    if (literal) {
                        celdaNota.setCellValue(nota.getCalificacion());
                    } else {
                        celdaNota.setCellValue(Integer.parseInt(nota.getCalificacion()));
                    }
                }
                celda(row, c + 1, nota != null && nota.getConclusionDescriptiva() != null ? nota.getConclusionDescriptiva() : "", e.conclusion);
                c += 2;
            }
        }
        validaciones(hoja, literal, area.competencias().size(), Math.max(d.alumnos().size(), 1));

        int leyenda = fila + 2;
        texto(hoja, leyenda++, 0, "LEYENDA", e.titulo);
        texto(hoja, leyenda++, 0, literal ? "NL = Nivel de logro alcanzado" : "Nota = Calificación de 0 a 20", e.valor);
        for (Competencia c : area.competencias()) {
            texto(hoja, leyenda++, 0, c.getCodigo() + " = " + c.getNombre(), e.valor);
        }
        hoja.setDisplayGridlines(true);
        hoja.setColumnWidth(0, 16 * 256);
        hoja.setColumnWidth(1, 44 * 256);
        codigos.setHeightInPoints(18);
        titulos.setHeightInPoints(30);
        hoja.createFreezePane(2, PRIMERA_FILA_ALUMNOS);
    }

    /** Lista AD, A, B, C o entero de 0 a 20 en la nota; de 10 a 500 caracteres en la conclusión. */
    private void validaciones(Sheet hoja, boolean literal, int competencias, int alumnos) {
        DataValidationHelper ayuda = hoja.getDataValidationHelper();
        int ultima = PRIMERA_FILA_ALUMNOS + alumnos - 1;
        CellRangeAddressList notas = new CellRangeAddressList();
        CellRangeAddressList conclusiones = new CellRangeAddressList();
        for (int i = 0; i < competencias; i++) {
            notas.addCellRangeAddress(PRIMERA_FILA_ALUMNOS, 2 + i * 2, ultima, 2 + i * 2);
            conclusiones.addCellRangeAddress(PRIMERA_FILA_ALUMNOS, 3 + i * 2, ultima, 3 + i * 2);
        }
        DataValidationConstraint reglaNota = literal
                ? ayuda.createExplicitListConstraint(Calificaciones.LITERALES.toArray(String[]::new))
                : ayuda.createIntegerConstraint(DataValidationConstraint.OperatorType.BETWEEN,
                String.valueOf(Calificaciones.MINIMA), String.valueOf(Calificaciones.MAXIMA));
        agregar(hoja, ayuda.createValidation(reglaNota, notas), "Inconsistencia",
                literal ? "La nota debe ser AD, A, B o C" : "La nota debe ser un entero de 0 a 20");
        DataValidationConstraint reglaConclusion = ayuda.createTextLengthConstraint(DataValidationConstraint.OperatorType.BETWEEN,
                String.valueOf(Calificaciones.CONCLUSION_MINIMA), String.valueOf(Calificaciones.CONCLUSION_MAXIMA));
        agregar(hoja, ayuda.createValidation(reglaConclusion, conclusiones), "Inconsistencia",
                "Se requiere ingresar sólo un mínimo de 10 y un máximo de 500 caracteres");
    }

    private static void agregar(Sheet hoja, DataValidation validacion, String titulo, String mensaje) {
        validacion.setShowErrorBox(true);
        validacion.setEmptyCellAllowed(true);
        validacion.createErrorBox(titulo, mensaje);
        hoja.addValidationData(validacion);
    }

    // ── Textos ──

    private static String nivel(Datos d) {
        return d.gs().getGrado().getNivel().getNombre();
    }

    private static String grado(Datos d) {
        return d.gs().getGrado().getNombre().toUpperCase();
    }

    private static String seccion(Datos d) {
        return d.gs().getSeccion() != null ? d.gs().getSeccion().getNombre().toUpperCase() : "UNICA";
    }

    private static String nombrePeriodo(Datos d) {
        return d.config().getTipoPeriodo().nombre(d.periodo()).toUpperCase();
    }

    /** "063: MATE" → "063-MATE", como en el original; sin caracteres prohibidos y único. */
    static String nombreHoja(Curso curso, Set<String> usados) {
        String base = curso.getCodigo() != null ? curso.getCodigo().replaceFirst("\\s*:\\s*", "-") : curso.getNombre();
        base = base.replaceAll("[\\\\/?*\\[\\]:]", " ").trim();
        base = base.length() > 31 ? base.substring(0, 31) : base;
        String nombre = base;
        for (int i = 2; usados.contains(nombre.toLowerCase()); i++) {
            String sufijo = " (" + i + ")";
            nombre = base.substring(0, Math.min(base.length(), 31 - sufijo.length())) + sufijo;
        }
        usados.add(nombre.toLowerCase());
        return nombre;
    }

    /** "RegNotas_Secundaria_1ro_A_B1_2027.xlsx" */
    private static String nombreArchivo(Datos d) {
        String archivo = "RegNotas_" + nivel(d) + "_" + d.gs().getGrado().getNombre() + "_" + seccion(d) + "_"
                + (d.config().getTipoPeriodo() == TipoPeriodo.TRIMESTRE ? "T" : "B") + d.periodo() + "_"
                + d.gs().getAnioEscolar().getAnio() + ".xlsx";
        return archivo.replaceAll("[^A-Za-z0-9_.-]", "");
    }

    private static String clave(Matricula m, Competencia c) {
        return m.getIdMatricula() + "-" + c.getIdCompetencia();
    }

    private static void texto(Sheet hoja, int fila, int columna, String valor, CellStyle estilo) {
        Row row = hoja.getRow(fila) != null ? hoja.getRow(fila) : hoja.createRow(fila);
        celda(row, columna, valor, estilo);
    }

    private static void celda(Row row, int columna, String valor, CellStyle estilo) {
        Cell cell = row.createCell(columna);
        cell.setCellValue(valor);
        cell.setCellStyle(estilo);
    }

    private Acceso eliminado() {
        return accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow();
    }

    private static final class Estilos {
        private static final byte[] VERDE = {(byte) 0x02, (byte) 0x67, (byte) 0x49};
        private static final byte[] GRIS = {(byte) 0x80, (byte) 0x80, (byte) 0x80};

        final CellStyle titulo;
        final CellStyle barra;
        final CellStyle etiqueta;
        final CellStyle valor;
        final CellStyle encabezado;
        final CellStyle dato;
        final CellStyle nota;
        final CellStyle conclusion;

        Estilos(Workbook libro) {
            Font normal = fuente(libro, false, false);
            Font negrita = fuente(libro, true, false);
            Font blanca = fuente(libro, true, true);

            titulo = estilo(libro, negrita);
            barra = relleno(libro, blanca, VERDE);
            barra.setVerticalAlignment(VerticalAlignment.CENTER);
            etiqueta = estilo(libro, negrita);
            valor = estilo(libro, normal);

            encabezado = relleno(libro, blanca, GRIS);
            bordes(encabezado);
            encabezado.setAlignment(HorizontalAlignment.CENTER);
            encabezado.setVerticalAlignment(VerticalAlignment.CENTER);
            encabezado.setWrapText(true);

            dato = estilo(libro, normal);
            bordes(dato);
            dato.setVerticalAlignment(VerticalAlignment.CENTER);
            nota = estilo(libro, negrita);
            bordes(nota);
            nota.setAlignment(HorizontalAlignment.CENTER);
            nota.setVerticalAlignment(VerticalAlignment.CENTER);
            conclusion = estilo(libro, normal);
            bordes(conclusion);
            conclusion.setWrapText(true);
            conclusion.setVerticalAlignment(VerticalAlignment.TOP);
        }

        private static Font fuente(Workbook libro, boolean negrita, boolean blanca) {
            Font fuente = libro.createFont();
            fuente.setFontName("Arial");
            fuente.setFontHeightInPoints((short) 9);
            fuente.setBold(negrita);
            if (blanca) {
                fuente.setColor(IndexedColors.WHITE.getIndex());
            }
            return fuente;
        }

        private static CellStyle estilo(Workbook libro, Font fuente) {
            CellStyle estilo = libro.createCellStyle();
            estilo.setFont(fuente);
            return estilo;
        }

        private static CellStyle relleno(Workbook libro, Font fuente, byte[] rgb) {
            org.apache.poi.xssf.usermodel.XSSFCellStyle estilo = (org.apache.poi.xssf.usermodel.XSSFCellStyle) estilo(libro, fuente);
            estilo.setFillForegroundColor(new org.apache.poi.xssf.usermodel.XSSFColor(rgb, null));
            estilo.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            return estilo;
        }

        private static void bordes(CellStyle estilo) {
            estilo.setBorderTop(BorderStyle.THIN);
            estilo.setBorderBottom(BorderStyle.THIN);
            estilo.setBorderLeft(BorderStyle.THIN);
            estilo.setBorderRight(BorderStyle.THIN);
        }
    }
}
