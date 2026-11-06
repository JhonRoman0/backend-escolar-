package com.example.Escolar.Service;

import com.example.Escolar.Dto.CursoCopiaRequest;
import com.example.Escolar.Dto.CursoImportResponse;
import com.example.Escolar.Dto.CursoResponse;
import com.example.Escolar.Dto.CursoVistaPreviaResponse;
import com.example.Escolar.Dto.SubcursoRequest;
import com.example.Escolar.Exception.ResourceNotFoundException;
import com.example.Escolar.Model.Acceso;
import com.example.Escolar.Model.AnioEscolar;
import com.example.Escolar.Model.Competencia;
import com.example.Escolar.Model.Curso;
import com.example.Escolar.Model.Nivel;
import com.example.Escolar.Model.TipoCurso;
import com.example.Escolar.Repository.AccesoRepository;
import com.example.Escolar.Repository.AnioEscolarRepository;
import com.example.Escolar.Repository.AsignacionRepository;
import com.example.Escolar.Repository.CompetenciaRepository;
import com.example.Escolar.Repository.CursoRepository;
import com.example.Escolar.Repository.NivelRepository;
import com.example.Escolar.Service.PlantillaCursosParser.AreaLeida;
import com.example.Escolar.Service.PlantillaCursosParser.CompetenciaLeida;
import com.example.Escolar.Service.PlantillaCursosParser.PlantillaCursos;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Malla curricular por año escolar y nivel. Las areas, transversales y
 * competencias solo entran importando el Excel del SIAGIE o copiando otro año;
 * lo unico que el colegio escribe son los subcursos.
 */
@Service
@RequiredArgsConstructor
public class CursoService {

    private final CursoRepository cursoRepository;
    private final CompetenciaRepository competenciaRepository;
    private final AccesoRepository accesoRepository;
    private final AnioEscolarRepository anioEscolarRepository;
    private final NivelRepository nivelRepository;
    private final AsignacionRepository asignacionRepository;
    private final PlantillaCursosParser parser;

    // ── Consultas ──────────────────────────────────────────────────────────

    /** Sin filtros devuelve todos los cursos, que es lo que usa Asignaciones. */
    @Transactional(readOnly = true)
    public List<CursoResponse> getAll(Integer idAnio, Integer idNivel) {
        Acceso eliminado = accesoEliminado();
        List<Curso> cursos;
        if (idAnio != null && idNivel != null) {
            cursos = cursoRepository.findByAnioEscolarIdAnioAndNivelIdNivelAndAccesoNot(idAnio, idNivel, eliminado);
        } else if (idAnio != null) {
            cursos = cursoRepository.findByAnioEscolarIdAnioAndAccesoNot(idAnio, eliminado);
        } else {
            cursos = cursoRepository.findByAccesoNot(eliminado).stream()
                    .filter(c -> idNivel == null || c.getNivel().getIdNivel().equals(idNivel))
                    .toList();
        }
        return toResponses(cursos);
    }

    @Transactional(readOnly = true)
    public CursoResponse getById(Integer id) {
        return toResponses(List.of(findCurso(id))).get(0);
    }

    // ── Importacion ────────────────────────────────────────────────────────

    /** Lee el archivo y muestra lo que pasaria al importarlo, sin guardar nada. */
    @Transactional(readOnly = true)
    public CursoVistaPreviaResponse vistaPrevia(MultipartFile archivo, Integer idAnio) {
        PlantillaCursos plantilla = parser.leer(archivo);
        Nivel nivel = resolverNivel(plantilla.nivel());
        AnioEscolar anio = findAnio(idAnio);
        validarImportable(anio, nivel);
        Map<String, Curso> existentes = areasExistentes(anio, nivel);

        Set<String> codigosArchivo = new HashSet<>();
        List<CursoVistaPreviaResponse.Area> areas = new ArrayList<>();
        for (AreaLeida leida : plantilla.areas()) {
            codigosArchivo.add(leida.codigo());
            CursoVistaPreviaResponse.Area area = new CursoVistaPreviaResponse.Area();
            area.setCodigo(leida.codigo());
            area.setNombre(leida.nombre());
            area.setTipo((leida.transversal() ? TipoCurso.TRANSVERSAL : TipoCurso.AREA).name());
            area.setAccion(existentes.containsKey(leida.codigo()) ? "ACTUALIZA" : "NUEVA");
            area.setCompetencias(leida.competencias().stream().map(c -> {
                CursoVistaPreviaResponse.Competencia competencia = new CursoVistaPreviaResponse.Competencia();
                competencia.setCodigo(c.codigo());
                competencia.setNombre(c.nombre());
                return competencia;
            }).toList());
            areas.add(area);
        }

        List<String> advertencias = new ArrayList<>(plantilla.advertencias());
        if (plantilla.anio() != null && !plantilla.anio().equals(anio.getAnio())) {
            advertencias.add("El archivo es del año " + plantilla.anio()
                    + " y se importará al año " + anio.getAnio() + ".");
        }

        CursoVistaPreviaResponse response = new CursoVistaPreviaResponse();
        response.setIdNivel(nivel.getIdNivel());
        response.setNivel(nivel.getNombre());
        response.setNivelArchivo(plantilla.nivel());
        response.setAnioArchivo(plantilla.anio());
        response.setPeriodoArchivo(plantilla.periodo());
        response.setAreas(areas);
        response.setSeInactivan(existentes.values().stream()
                .filter(c -> !codigosArchivo.contains(c.getCodigo()) && esActivo(c))
                .map(Curso::getNombre)
                .sorted()
                .toList());
        response.setAdvertencias(advertencias);
        return response;
    }

    /**
     * Crea o actualiza las areas por codigo dentro del año y nivel, y sus
     * competencias por numero dentro de cada area. Lo que ya no viene en el
     * archivo se inactiva, no se borra. Lo que ya existia conserva su estado:
     * si el colegio inactivo un area (por ejemplo Castellano como segunda
     * lengua), volver a importar no la reactiva.
     */
    @Transactional
    public CursoImportResponse importar(MultipartFile archivo, Integer idAnio) {
        PlantillaCursos plantilla = parser.leer(archivo);
        Nivel nivel = resolverNivel(plantilla.nivel());
        AnioEscolar anio = findAnio(idAnio);
        validarImportable(anio, nivel);
        Map<String, Curso> existentes = areasExistentes(anio, nivel);

        int creados = 0;
        int actualizados = 0;
        Set<String> codigosArchivo = new HashSet<>();
        for (AreaLeida leida : plantilla.areas()) {
            codigosArchivo.add(leida.codigo());
            Curso curso = existentes.get(leida.codigo());
            if (curso == null) {
                curso = new Curso();
                curso.setCodigo(leida.codigo());
                curso.setAnioEscolar(anio);
                curso.setNivel(nivel);
                curso.setAcceso(accesoActivo());
                creados++;
            } else {
                actualizados++;
            }
            curso.setNombre(leida.nombre());
            curso.setTipo(leida.transversal() ? TipoCurso.TRANSVERSAL : TipoCurso.AREA);
            // Una transversal no admite subcursos.
            if (leida.transversal() && curso.isTieneSubcursos()) {
                curso.setTieneSubcursos(false);
                cambiarAccesoSubcursos(curso, accesoEliminado());
            }
            curso = cursoRepository.save(curso);
            sincronizarCompetencias(curso, leida.competencias());
        }

        int inactivados = 0;
        for (Curso curso : existentes.values()) {
            if (!codigosArchivo.contains(curso.getCodigo()) && esActivo(curso)) {
                curso.setAcceso(accesoInactivo());
                cursoRepository.save(curso);
                cambiarAccesoSubcursos(curso, accesoInactivo());
                inactivados++;
            }
        }

        return CursoImportResponse.builder()
                .totalDetectados(plantilla.areas().size())
                .creados(creados)
                .actualizados(actualizados)
                .inactivados(inactivados)
                .cursos(getAll(anio.getIdAnio(), nivel.getIdNivel()))
                .build();
    }

    private void sincronizarCompetencias(Curso curso, List<CompetenciaLeida> leidas) {
        Map<String, Competencia> existentes = curso.getIdCurso() == null ? Map.of()
                : competenciaRepository.findByCursoIdCursoAndAccesoNot(curso.getIdCurso(), accesoEliminado())
                .stream()
                .collect(Collectors.toMap(Competencia::getCodigo, Function.identity(), (a, b) -> a));
        Set<String> codigosArchivo = new HashSet<>();
        for (CompetenciaLeida leida : leidas) {
            codigosArchivo.add(leida.codigo());
            Competencia competencia = existentes.get(leida.codigo());
            if (competencia == null) {
                competencia = new Competencia();
                competencia.setCurso(curso);
                competencia.setCodigo(leida.codigo());
                competencia.setAcceso(accesoActivo());
            }
            competencia.setNombre(leida.nombre());
            competencia.setOrden(leida.orden());
            competenciaRepository.save(competencia);
        }
        for (Competencia competencia : existentes.values()) {
            if (!codigosArchivo.contains(competencia.getCodigo())
                    && AccesoConstants.ACTIVO.equals(competencia.getAcceso().getIdAcceso())) {
                competencia.setAcceso(accesoInactivo());
                competenciaRepository.save(competencia);
            }
        }
    }

    // ── Copia ──────────────────────────────────────────────────────────────

    /**
     * Copia la malla de un nivel (areas, transversales, competencias y
     * subcursos con sus competencias) de un año vigente o cerrado a uno por
     * comenzar que aun no tiene cursos en ese nivel.
     */
    @Transactional
    public List<CursoResponse> copiar(CursoCopiaRequest request) {
        AnioEscolar origen = findAnio(request.getIdAnioOrigen());
        AnioEscolar destino = findAnio(request.getIdAnioDestino());
        Nivel nivel = findNivel(request.getIdNivel());
        Acceso eliminado = accesoEliminado();

        validarConfigurable(destino);
        if (origen.getEstado() == AnioEscolarService.ESTADO_POR_COMENZAR) {
            throw new IllegalArgumentException("Solo se pueden copiar cursos de un año vigente o cerrado");
        }
        if (cursoRepository.existsByAnioEscolarIdAnioAndNivelIdNivelAndAccesoNot(
                destino.getIdAnio(), nivel.getIdNivel(), eliminado)) {
            throw new IllegalArgumentException("El año " + destino.getAnio() + " ya tiene cursos en el nivel "
                    + nivel.getNombre() + ". Para cambiarlos, vuelve a importar el Excel.");
        }
        List<Curso> cursosOrigen = cursoRepository.findByAnioEscolarIdAnioAndNivelIdNivelAndAccesoNot(
                origen.getIdAnio(), nivel.getIdNivel(), eliminado);
        if (cursosOrigen.isEmpty()) {
            throw new IllegalArgumentException("El año " + origen.getAnio() + " no tiene cursos en el nivel "
                    + nivel.getNombre());
        }

        // Primero las areas y transversales con sus competencias, para que los
        // subcursos encuentren a su padre y a las competencias ya copiadas.
        Map<Integer, Curso> cursosCopiados = new HashMap<>();
        Map<Integer, Competencia> competenciasCopiadas = new HashMap<>();
        for (Curso original : cursosOrigen) {
            if (original.getTipo() == TipoCurso.SUBCURSO) {
                continue;
            }
            Curso copia = copiaBase(original, destino);
            copia.setCodigo(original.getCodigo());
            copia.setTieneSubcursos(original.isTieneSubcursos());
            copia = cursoRepository.save(copia);
            cursosCopiados.put(original.getIdCurso(), copia);
            for (Competencia competencia : competenciaRepository.findByCursoIdCursoAndAccesoNot(
                    original.getIdCurso(), eliminado)) {
                Competencia nueva = new Competencia();
                nueva.setCurso(copia);
                nueva.setCodigo(competencia.getCodigo());
                nueva.setNombre(competencia.getNombre());
                nueva.setOrden(competencia.getOrden());
                nueva.setAcceso(competencia.getAcceso());
                competenciasCopiadas.put(competencia.getIdCompetencia(), competenciaRepository.save(nueva));
            }
        }
        for (Curso original : cursosOrigen) {
            Curso padre = original.getCursoPadre() == null ? null
                    : cursosCopiados.get(original.getCursoPadre().getIdCurso());
            if (original.getTipo() != TipoCurso.SUBCURSO || padre == null) {
                continue;
            }
            Curso copia = copiaBase(original, destino);
            copia.setCursoPadre(padre);
            copia.setCompetencias(original.getCompetencias().stream()
                    .map(c -> competenciasCopiadas.get(c.getIdCompetencia()))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toCollection(HashSet::new)));
            cursoRepository.save(copia);
        }
        return getAll(destino.getIdAnio(), nivel.getIdNivel());
    }

    private Curso copiaBase(Curso original, AnioEscolar destino) {
        Curso copia = new Curso();
        copia.setNombre(original.getNombre());
        copia.setTipo(original.getTipo());
        copia.setAnioEscolar(destino);
        copia.setNivel(original.getNivel());
        copia.setAcceso(original.getAcceso());
        return copia;
    }

    // ── Subcursos ──────────────────────────────────────────────────────────

    /** Al apagarlo, los subcursos del area se eliminan. */
    @Transactional
    public CursoResponse cambiarSubcursos(Integer idArea, boolean tieneSubcursos) {
        Curso area = findCurso(idArea);
        if (area.getTipo() != TipoCurso.AREA) {
            throw new IllegalArgumentException("Solo las áreas pueden tener subcursos");
        }
        validarConfigurable(area.getAnioEscolar());
        if (area.isTieneSubcursos() == tieneSubcursos) {
            return getById(idArea);
        }
        if (tieneSubcursos) {
            if (asignacionRepository.existsByCursoIdCursoAndAccesoNot(idArea, accesoEliminado())) {
                throw new IllegalArgumentException(
                        "El área tiene asignaciones de docentes: quítalas antes de dividirla en subcursos");
            }
        } else {
            validarSinAsignaciones(subcursos(area));
            cambiarAccesoSubcursos(area, accesoEliminado());
        }
        area.setTieneSubcursos(tieneSubcursos);
        cursoRepository.save(area);
        return getById(idArea);
    }

    @Transactional
    public CursoResponse crearSubcurso(Integer idArea, SubcursoRequest request) {
        Curso area = findCurso(idArea);
        if (area.getTipo() != TipoCurso.AREA || !area.isTieneSubcursos()) {
            throw new IllegalArgumentException("Activa los subcursos del área antes de agregar uno");
        }
        validarConfigurable(area.getAnioEscolar());
        String nombre = request.getNombre().trim();
        validarNombreSubcurso(area, nombre, null);

        Curso subcurso = new Curso();
        subcurso.setNombre(nombre);
        subcurso.setTipo(TipoCurso.SUBCURSO);
        subcurso.setAnioEscolar(area.getAnioEscolar());
        subcurso.setNivel(area.getNivel());
        subcurso.setCursoPadre(area);
        subcurso.setAcceso(accesoActivo());
        subcurso.setCompetencias(competenciasDelArea(area, request.getIdCompetencias()));
        return getById(cursoRepository.save(subcurso).getIdCurso());
    }

    /** Solo se editan los subcursos: las areas se actualizan volviendo a importar. */
    @Transactional
    public CursoResponse actualizarSubcurso(Integer id, SubcursoRequest request) {
        Curso subcurso = findCurso(id);
        if (subcurso.getTipo() != TipoCurso.SUBCURSO) {
            throw new IllegalArgumentException(
                    "Las áreas no se editan a mano: vuelve a importar el Excel del SIAGIE para actualizarlas");
        }
        validarConfigurable(subcurso.getAnioEscolar());
        String nombre = request.getNombre().trim();
        validarNombreSubcurso(subcurso.getCursoPadre(), nombre, id);
        subcurso.setNombre(nombre);
        subcurso.setCompetencias(competenciasDelArea(subcurso.getCursoPadre(), request.getIdCompetencias()));
        cursoRepository.save(subcurso);
        return getById(id);
    }

    private void validarNombreSubcurso(Curso area, String nombre, Integer idExcluir) {
        boolean repetido = subcursos(area).stream()
                .anyMatch(s -> !s.getIdCurso().equals(idExcluir) && s.getNombre().equalsIgnoreCase(nombre));
        if (repetido) {
            throw new IllegalArgumentException("El área ya tiene un subcurso llamado " + nombre);
        }
    }

    private Set<Competencia> competenciasDelArea(Curso area, List<Integer> idCompetencias) {
        Map<Integer, Competencia> delArea = competenciaRepository
                .findByCursoIdCursoAndAccesoNot(area.getIdCurso(), accesoEliminado()).stream()
                .collect(Collectors.toMap(Competencia::getIdCompetencia, Function.identity()));
        Set<Competencia> elegidas = new HashSet<>();
        for (Integer idCompetencia : idCompetencias) {
            Competencia competencia = delArea.get(idCompetencia);
            if (competencia == null) {
                throw new IllegalArgumentException("Solo se pueden elegir competencias del área " + area.getNombre());
            }
            elegidas.add(competencia);
        }
        return elegidas;
    }

    // ── Estado y eliminacion ───────────────────────────────────────────────

    @Transactional
    public CursoResponse cambiarEstado(Integer id, Long nuevoAccesoId) {
        Curso curso = findCurso(id);
        validarConfigurable(curso.getAnioEscolar());
        if (!AccesoConstants.ACTIVO.equals(nuevoAccesoId) && !AccesoConstants.INACTIVO.equals(nuevoAccesoId)) {
            throw new IllegalArgumentException("El estado debe ser activo o inactivo");
        }
        curso.setAcceso(accesoRepository.findById(nuevoAccesoId).orElseThrow());
        cursoRepository.save(curso);
        return getById(id);
    }

    /** Eliminar un area tambien elimina sus subcursos. */
    @Transactional
    public void delete(Integer id) {
        Curso curso = findCurso(id);
        validarConfigurable(curso.getAnioEscolar());
        List<Curso> afectados = new ArrayList<>(subcursos(curso));
        afectados.add(curso);
        validarSinAsignaciones(afectados);
        cambiarAccesoSubcursos(curso, accesoEliminado());
        curso.setAcceso(accesoEliminado());
        cursoRepository.save(curso);
    }

    /**
     * El año escolar se borra fisicamente y sus cursos lo referencian, asi que
     * antes se quitan los que ya estaban eliminados (logicamente) junto con sus
     * competencias. Lo llama AnioEscolarService.delete cuando el año ya no
     * tiene cursos vigentes.
     */
    @Transactional
    public void borrarEliminadosDelAnio(Integer idAnio) {
        List<Curso> eliminados = new ArrayList<>(
                cursoRepository.findByAnioEscolarIdAnioAndAcceso(idAnio, accesoEliminado()));
        // Los subcursos primero: apuntan a su area y a sus competencias.
        eliminados.sort(Comparator.comparing(c -> c.getTipo() == TipoCurso.SUBCURSO ? 0 : 1));
        for (Curso curso : eliminados) {
            curso.getCompetencias().clear();
            competenciaRepository.deleteAll(competenciaRepository.findByCursoIdCurso(curso.getIdCurso()));
            cursoRepository.delete(curso);
        }
    }

    // ── Reglas comunes ─────────────────────────────────────────────────────

    /**
     * Solo la malla de un año por comenzar se configura. La de un año vigente o
     * cerrado es de solo lectura: ya se esta dictando o ya se dicto.
     */
    public void validarConfigurable(AnioEscolar anio) {
        if (anio.getEstado() != AnioEscolarService.ESTADO_POR_COMENZAR) {
            throw new IllegalArgumentException("El año " + anio.getAnio() + " está "
                    + (anio.getEstado() == AnioEscolarService.ESTADO_VIGENTE ? "vigente" : "cerrado")
                    + ": sus cursos y competencias solo se pueden consultar");
        }
    }

    // Excepcion: un vigente sin ningun curso en ese nivel admite una importacion
    // inicial, para el colegio que empieza a usar el sistema a mitad de año.
    private void validarImportable(AnioEscolar anio, Nivel nivel) {
        boolean vigenteVacio = anio.getEstado() == AnioEscolarService.ESTADO_VIGENTE
                && !cursoRepository.existsByAnioEscolarIdAnioAndNivelIdNivelAndAccesoNot(
                anio.getIdAnio(), nivel.getIdNivel(), accesoEliminado());
        if (!vigenteVacio) {
            validarConfigurable(anio);
        }
    }

    private void validarSinAsignaciones(List<Curso> cursos) {
        for (Curso curso : cursos) {
            if (asignacionRepository.existsByCursoIdCursoAndAccesoNot(curso.getIdCurso(), accesoEliminado())) {
                throw new IllegalArgumentException("El curso " + curso.getNombre()
                        + " tiene asignaciones académicas asociadas");
            }
        }
    }

    /** "INICIAL - JARDÍN" -> Inicial, "SECUNDARIA" -> Secundaria. */
    private Nivel resolverNivel(String nivelArchivo) {
        String primeraPalabra = PlantillaCursosParser.clave(nivelArchivo.trim().split("[\\s\\-]+")[0]);
        return nivelRepository.findByAccesoNot(accesoEliminado()).stream()
                .filter(n -> PlantillaCursosParser.clave(n.getNombre()).equals(primeraPalabra))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "No se reconoce el nivel \"" + nivelArchivo + "\" del archivo"));
    }

    /** Areas y transversales del año y nivel, por codigo. */
    private Map<String, Curso> areasExistentes(AnioEscolar anio, Nivel nivel) {
        Map<String, Curso> porCodigo = new LinkedHashMap<>();
        cursoRepository.findByAnioEscolarIdAnioAndNivelIdNivelAndAccesoNot(
                        anio.getIdAnio(), nivel.getIdNivel(), accesoEliminado()).stream()
                .filter(c -> c.getTipo() != TipoCurso.SUBCURSO && c.getCodigo() != null)
                .forEach(c -> porCodigo.putIfAbsent(c.getCodigo(), c));
        return porCodigo;
    }

    private List<Curso> subcursos(Curso area) {
        if (area.getTipo() != TipoCurso.AREA || area.getIdCurso() == null) {
            return List.of();
        }
        return cursoRepository.findByCursoPadreIdCursoAndAccesoNot(area.getIdCurso(), accesoEliminado());
    }

    private void cambiarAccesoSubcursos(Curso area, Acceso acceso) {
        for (Curso subcurso : subcursos(area)) {
            subcurso.setAcceso(acceso);
            cursoRepository.save(subcurso);
        }
    }

    private boolean esActivo(Curso curso) {
        return AccesoConstants.ACTIVO.equals(curso.getAcceso().getIdAcceso());
    }

    private Curso findCurso(Integer id) {
        return cursoRepository.findByIdCursoAndAccesoNot(id, accesoEliminado())
                .orElseThrow(() -> new ResourceNotFoundException("Curso no encontrado con id " + id));
    }

    private AnioEscolar findAnio(Integer idAnio) {
        if (idAnio == null) {
            throw new IllegalArgumentException("Selecciona el año escolar");
        }
        return anioEscolarRepository.findByIdAnioAndAccesoNot(idAnio, accesoEliminado())
                .orElseThrow(() -> new ResourceNotFoundException("Año escolar no encontrado con id " + idAnio));
    }

    private Nivel findNivel(Integer idNivel) {
        return nivelRepository.findByIdNivelAndAccesoNot(idNivel, accesoEliminado())
                .orElseThrow(() -> new ResourceNotFoundException("Nivel no encontrado con id " + idNivel));
    }

    private Acceso accesoEliminado() {
        return accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow();
    }

    private Acceso accesoActivo() {
        return accesoRepository.findById(AccesoConstants.ACTIVO).orElseThrow();
    }

    private Acceso accesoInactivo() {
        return accesoRepository.findById(AccesoConstants.INACTIVO).orElseThrow();
    }

    // ── Respuesta ──────────────────────────────────────────────────────────

    private List<CursoResponse> toResponses(List<Curso> cursos) {
        // Una sola consulta para contar las competencias activas de todas las areas.
        List<Integer> ids = cursos.stream()
                .filter(c -> c.getTipo() != TipoCurso.SUBCURSO)
                .map(Curso::getIdCurso)
                .toList();
        Map<Integer, Long> activasPorCurso = ids.isEmpty() ? Map.of()
                : competenciaRepository.findByCursoIdCursoInAndAccesoNot(ids, accesoEliminado()).stream()
                .filter(c -> AccesoConstants.ACTIVO.equals(c.getAcceso().getIdAcceso()))
                .collect(Collectors.groupingBy(c -> c.getCurso().getIdCurso(), Collectors.counting()));

        return cursos.stream()
                .sorted(Comparator.comparing((Curso c) -> c.getCodigo() == null ? "~" : c.getCodigo())
                        .thenComparing(Curso::getNombre))
                .map(curso -> {
                    CursoResponse response = new CursoResponse();
                    response.setIdCurso(curso.getIdCurso());
                    response.setCodigo(curso.getCodigo());
                    response.setNombre(curso.getNombre());
                    response.setAccesoId(curso.getAcceso().getIdAcceso());
                    response.setIdAnio(curso.getAnioEscolar().getIdAnio());
                    response.setAnio(curso.getAnioEscolar().getAnio());
                    response.setIdNivel(curso.getNivel().getIdNivel());
                    response.setNivel(curso.getNivel().getNombre());
                    response.setTipo(curso.getTipo().name());
                    response.setTieneSubcursos(curso.isTieneSubcursos());
                    response.setIdCursoPadre(curso.getCursoPadre() == null ? null : curso.getCursoPadre().getIdCurso());
                    response.setCantidadCompetencias(activasPorCurso.getOrDefault(curso.getIdCurso(), 0L).intValue());
                    response.setIdCompetencias(curso.getTipo() != TipoCurso.SUBCURSO ? List.of()
                            : curso.getCompetencias().stream()
                            .filter(c -> !AccesoConstants.ELIMINADO.equals(c.getAcceso().getIdAcceso()))
                            .map(Competencia::getIdCompetencia)
                            .sorted()
                            .toList());
                    return response;
                })
                .toList();
    }
}
