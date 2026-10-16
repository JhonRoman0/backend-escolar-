package com.example.Escolar.Service;

import com.example.Escolar.Dto.CursoImportResponse;
import com.example.Escolar.Dto.CursoRequest;
import com.example.Escolar.Dto.CursoResponse;
import com.example.Escolar.Exception.ResourceNotFoundException;
import com.example.Escolar.Model.Acceso;
import com.example.Escolar.Model.Curso;
import com.example.Escolar.Repository.AccesoRepository;
import com.example.Escolar.Repository.AnioEscolarRepository;
import com.example.Escolar.Repository.AsignacionRepository;
import com.example.Escolar.Repository.CursoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class CursoService {

    public static final byte ESTADO_ANIO_VIGENTE = 1;

    private static final Pattern PATRON_CODIGO_SIAGIE =
            Pattern.compile("^\\d{3,4}\\s*[:\\-]\\s*[A-ZÁÉÍÓÚÑa-záéíóúñ\\s]+$");

    private final CursoRepository cursoRepository;
    private final AccesoRepository accesoRepository;
    private final AnioEscolarRepository anioEscolarRepository;
    private final AsignacionRepository asignacionRepository;

    public List<CursoResponse> getAll() {
        return cursoRepository.findByAccesoNot(accesoEliminado()).stream()
                .map(this::toResponse)
                .toList();
    }

    public CursoResponse getById(Integer id) {
        return toResponse(findCurso(id));
    }

    @Transactional
    public CursoResponse create(CursoRequest request) {
        String nombre = request.getNombre().trim();
        String codigo = (request.getCodigo() != null && !request.getCodigo().isBlank()) ? request.getCodigo().trim() : null;

        validarNombreUnico(nombre, null);
        if (codigo != null) {
            validarCodigoUnico(codigo, null);
        }

        // Si ya existía un curso previamente eliminado lógicamente (acceso = ELIMINADO),
        // se reactiva y actualiza para evitar el "limbo" y errores de clave duplicada en BD
        Optional<Curso> eliminadoOpt = Optional.empty();
        if (codigo != null) {
            eliminadoOpt = cursoRepository.findByCodigo(codigo);
        }
        if (eliminadoOpt.isEmpty()) {
            eliminadoOpt = cursoRepository.findByNombreIgnoreCase(nombre);
        }

        Curso curso;
        Long idAcceso = request.getAccesoId() == null ? AccesoConstants.ACTIVO : request.getAccesoId();
        if (eliminadoOpt.isPresent() && eliminadoOpt.get().getAcceso().getIdAcceso().equals(AccesoConstants.ELIMINADO)) {
            curso = eliminadoOpt.get();
            curso.setNombre(nombre);
            curso.setCodigo(codigo);
            curso.setAcceso(accesoRepository.findById(idAcceso).orElseThrow());
        } else {
            curso = new Curso();
            curso.setNombre(nombre);
            curso.setCodigo(codigo);
            curso.setAcceso(accesoRepository.findById(idAcceso).orElseThrow());
        }
        return toResponse(cursoRepository.save(curso));
    }


    @Transactional
    public CursoResponse update(Integer id, CursoRequest request) {
        validarModificacionPermitida(id);
        Curso curso = findCurso(id);

        if (request.getNombre() != null && !request.getNombre().isBlank()) {
            validarNombreUnico(request.getNombre(), id);
            curso.setNombre(request.getNombre().trim());
        }
        if (request.getCodigo() != null && !request.getCodigo().isBlank()) {
            validarCodigoUnico(request.getCodigo(), id);
            curso.setCodigo(request.getCodigo().trim());
        }
        if (request.getAccesoId() != null) {
            curso.setAcceso(accesoRepository.findById(request.getAccesoId()).orElseThrow());
        }
        return toResponse(cursoRepository.save(curso));
    }

    @Transactional
    public CursoResponse cambiarEstado(Integer id, Long nuevoAccesoId) {
        validarModificacionPermitida(id);
        Curso curso = findCurso(id);
        if (nuevoAccesoId == null) {
            throw new IllegalArgumentException("El estado de acceso es obligatorio");
        }
        curso.setAcceso(accesoRepository.findById(nuevoAccesoId).orElseThrow(
                () -> new ResourceNotFoundException("Acceso no encontrado con id " + nuevoAccesoId)));
        return toResponse(cursoRepository.save(curso));
    }

    @Transactional
    public void delete(Integer id) {
        validarModificacionPermitida(id);
        Curso curso = findCurso(id);

        if (asignacionRepository.existsByCursoIdCursoAndAccesoNot(id, accesoEliminado())) {
            throw new IllegalArgumentException("El curso no puede eliminarse porque tiene asignaciones académicas asociadas.");
        }

        curso.setAcceso(accesoEliminado());
        cursoRepository.save(curso);
    }

    /**
     * Importación resiliente de cursos desde la plantilla oficial de registro de notas del SIAGIE (.xlsx).
     *
     * 1. Ignora la posición fija de celdas: busca la sección "ÁREAS" dinámicamente.
     * 2. Detecta el código mediante el patrón oficial (3-4 dígitos con abreviatura).
     * 3. Aplica validación cruzada: confirma que exista una pestaña de notas con ese código en el libro.
     * 4. Es idempotente: si el curso ya existe, actualiza sus datos sin duplicar.
     */
    @Transactional
    public CursoImportResponse importarDesdeExcel(MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) {
            throw new IllegalArgumentException("Debe seleccionar un archivo Excel para importar");
        }

        String nombreArchivo = archivo.getOriginalFilename() != null ? archivo.getOriginalFilename().toLowerCase() : "";
        if (!nombreArchivo.endsWith(".xlsx") && !nombreArchivo.endsWith(".xls")) {
            throw new IllegalArgumentException("El archivo debe ser un documento Excel válido (.xlsx)");
        }

        try (Workbook workbook = WorkbookFactory.create(archivo.getInputStream())) {
            Set<String> nombresHojas = new HashSet<>();
            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                nombresHojas.add(normalizarClave(workbook.getSheetName(i)));
            }

            Sheet sheet = workbook.getSheet("Generalidades");
            if (sheet == null) {
                // Si la plantilla cambió de nombre, usa la primera hoja como fallback
                sheet = workbook.getSheetAt(0);
            }

            boolean seccionAreasIniciada = false;
            List<CursoResponse> respuestas = new ArrayList<>();
            int creados = 0;
            int actualizados = 0;

            for (Row row : sheet) {
                if (row == null) {
                    continue;
                }

                List<String> valoresFila = new ArrayList<>();
                for (Cell cell : row) {
                    String val = obtenerTextoCelda(cell);
                    if (!val.isBlank()) {
                        valoresFila.add(val.trim());
                    }
                }

                // 1. Detección del punto de anclaje de inicio
                if (!seccionAreasIniciada) {
                    if (valoresFila.stream().anyMatch(v -> v.toUpperCase().contains("ÁREAS") || v.toUpperCase().contains("AREAS"))) {
                        seccionAreasIniciada = true;
                    }
                    continue;
                }

                // Si encontramos fila vacía después de haber iniciado la sección de áreas, continuamos o detenemos
                if (valoresFila.isEmpty()) {
                    continue;
                }

                // 2. Extraer candidato a código y candidato a nombre
                String codigoCand = null;
                String nombreCand = null;

                for (String val : valoresFila) {
                    if (PATRON_CODIGO_SIAGIE.matcher(val).matches()) {
                        codigoCand = val;
                    } else if (val.length() > 3 && !val.equalsIgnoreCase("ÁREAS") && !val.equalsIgnoreCase("AREAS")) {
                        nombreCand = val;
                    }
                }

                if (codigoCand != null && nombreCand != null) {
                    // 3. Validación cruzada con pestañas del libro
                    String claveHojaEquiv = normalizarClave(codigoCand.replace(':', '-'));
                    boolean existePestaña = nombresHojas.stream().anyMatch(h -> h.contains(claveHojaEquiv) || claveHojaEquiv.contains(h));

                    if (!existePestaña) {
                        log.debug("Se descarta candidato [{}] por no tener pestaña en el Excel", codigoCand);
                        continue;
                    }

                    // 4. Upsert (Guardar, Actualizar o Reactivar)
                    Optional<Curso> cursoOpt = cursoRepository.findByCodigoAndAccesoNot(codigoCand, accesoEliminado());
                    if (cursoOpt.isEmpty()) {
                        cursoOpt = cursoRepository.findByNombreAndAccesoNot(nombreCand, accesoEliminado());
                    }

                    Curso curso;
                    if (cursoOpt.isPresent()) {
                        curso = cursoOpt.get();
                        curso.setCodigo(codigoCand);
                        curso.setNombre(nombreCand);
                        actualizados++;
                    } else {
                        // Si no está activo ni inactivo, verificar si existía como ELIMINADO para reactivarlo
                        Optional<Curso> eliminadoOpt = cursoRepository.findByCodigo(codigoCand);
                        if (eliminadoOpt.isEmpty()) {
                            eliminadoOpt = cursoRepository.findByNombreIgnoreCase(nombreCand);
                        }

                        if (eliminadoOpt.isPresent()) {
                            curso = eliminadoOpt.get();
                            curso.setCodigo(codigoCand);
                            curso.setNombre(nombreCand);
                            curso.setAcceso(accesoActivo());
                            actualizados++;
                        } else {
                            curso = new Curso();
                            curso.setCodigo(codigoCand);
                            curso.setNombre(nombreCand);
                            curso.setAcceso(accesoActivo());
                            creados++;
                        }
                    }

                    Curso guardado = cursoRepository.save(curso);
                    respuestas.add(toResponse(guardado));
                }
            }

            if (respuestas.isEmpty()) {
                throw new IllegalArgumentException("No se encontraron áreas curriculares válidas en el archivo Excel");
            }

            return CursoImportResponse.builder()
                    .totalDetectados(respuestas.size())
                    .creados(creados)
                    .actualizados(actualizados)
                    .cursos(respuestas)
                    .build();

        } catch (IOException e) {
            log.error("Error al procesar el archivo Excel del SIAGIE", e);
            throw new IllegalArgumentException("No se pudo leer el archivo Excel: " + e.getMessage());
        }
    }

    /**
     * Regla de negocio: si el año escolar ya ha iniciado y está en curso (ESTADO_VIGENTE),
     * la malla y cursos no pueden ser alterados, inactivados ni eliminados.
     */
    public void validarModificacionPermitida(Integer idCurso) {
        boolean hayAnioVigente = anioEscolarRepository
                .findByEstadoAndAccesoNot(ESTADO_ANIO_VIGENTE, accesoEliminado())
                .isPresent();

        if (hayAnioVigente) {
            throw new IllegalArgumentException(
                    "No se puede inactivar, modificar ni eliminar el curso porque el año escolar ya ha iniciado y se encuentra vigente.");
        }
    }

    private void validarNombreUnico(String nombre, Integer idExcluir) {
        if (nombre == null || nombre.isBlank()) {
            return;
        }
        cursoRepository.findByNombreAndAccesoNot(nombre.trim(), accesoEliminado())
                .filter(c -> idExcluir == null || !c.getIdCurso().equals(idExcluir))
                .ifPresent(c -> {
                    throw new IllegalArgumentException("Ya existe un curso con ese nombre");
                });
    }

    private void validarCodigoUnico(String codigo, Integer idExcluir) {
        if (codigo == null || codigo.isBlank()) {
            return;
        }
        cursoRepository.findByCodigoAndAccesoNot(codigo.trim(), accesoEliminado())
                .filter(c -> idExcluir == null || !c.getIdCurso().equals(idExcluir))
                .ifPresent(c -> {
                    throw new IllegalArgumentException("Ya existe un curso con ese código");
                });
    }

    private Curso findCurso(Integer id) {
        return cursoRepository.findByIdCursoAndAccesoNot(id, accesoEliminado())
                .orElseThrow(() -> new ResourceNotFoundException("Curso no encontrado con id " + id));
    }

    private Acceso accesoEliminado() {
        return accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow();
    }

    private Acceso accesoActivo() {
        return accesoRepository.findById(AccesoConstants.ACTIVO).orElseThrow();
    }


    private String obtenerTextoCelda(Cell cell) {
        if (cell == null) {
            return "";
        }
        if (cell.getCellType() == CellType.STRING) {
            return cell.getStringCellValue();
        } else if (cell.getCellType() == CellType.NUMERIC) {
            return String.valueOf((long) cell.getNumericCellValue());
        }
        return "";
    }

    private String normalizarClave(String texto) {
        if (texto == null) return "";
        return Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("[^\\p{ASCII}]", "")
                .replaceAll("[^a-zA-Z0-9]", "")
                .toUpperCase();
    }

    private CursoResponse toResponse(Curso curso) {
        CursoResponse response = new CursoResponse();
        response.setIdCurso(curso.getIdCurso());
        response.setCodigo(curso.getCodigo());
        response.setNombre(curso.getNombre());
        response.setAccesoId(curso.getAcceso().getIdAcceso().longValue());
        return response;
    }
}
