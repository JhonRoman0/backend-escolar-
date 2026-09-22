package com.example.Escolar.Service;

import com.example.Escolar.Dto.AutorizacionRegistroResponse;
import com.example.Escolar.Dto.AutorizacionRequest;
import com.example.Escolar.Dto.AutorizacionResponse;
import com.example.Escolar.Dto.NotaBatchRequest;
import com.example.Escolar.Dto.NotaConsolidadoResponse;
import com.example.Escolar.Dto.NotaReporteResponse;
import com.example.Escolar.Dto.NotaRequest;
import com.example.Escolar.Dto.NotaResponse;
import com.example.Escolar.Dto.PromedioResponse;
import com.example.Escolar.Dto.ValidarAutorizacionRequest;
import com.example.Escolar.Exception.AutorizacionRequeridaException;
import com.example.Escolar.Exception.ResourceNotFoundException;
import com.example.Escolar.Model.Competencia;
import com.example.Escolar.Model.Curso;
import com.example.Escolar.Model.Docente;
import com.example.Escolar.Model.Matricula;
import com.example.Escolar.Model.Nota;
import com.example.Escolar.Model.NotaAutorizacion;
import com.example.Escolar.Model.Usuario;
import com.example.Escolar.Repository.AccesoRepository;
import com.example.Escolar.Repository.CompetenciaRepository;
import com.example.Escolar.Repository.DocenteRepository;
import com.example.Escolar.Repository.MatriculaRepository;
import com.example.Escolar.Repository.NotaAutorizacionRepository;
import com.example.Escolar.Repository.NotaRepository;
import com.example.Escolar.Repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class NotaService {

    public static final byte NO_USADO = 0;
    public static final byte USADO = 1;

    private static final long MINUTOS_VALIDEZ = 10;
    private static final Set<String> CALIFICACIONES_VALIDAS = Set.of("AD", "A", "B", "C");

    private final NotaRepository notaRepository;
    private final CompetenciaRepository competenciaRepository;
    private final MatriculaRepository matriculaRepository;
    private final AccesoContextoService accesoContextoService;
    private final NotaAutorizacionRepository notaAutorizacionRepository;
    private final DocenteRepository docenteRepository;
    private final UsuarioRepository usuarioRepository;
    private final AccesoRepository accesoRepository;

    public List<NotaResponse> getPorCompetencia(Integer idCompetencia, Byte bimestre, Integer idUsuario, List<String> roles) {
        Competencia competencia = findCompetencia(idCompetencia);
        List<Nota> notas;
        if (bimestre != null) {
            notas = notaRepository.findByCompetenciaIdCompetenciaAndBimestreAndAccesoNot(idCompetencia, bimestre, accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow());
        } else {
            notas = notaRepository.findByCompetenciaIdCompetenciaAndBimestreAndAccesoNot(idCompetencia, (byte) 0, accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow());
            if (notas.isEmpty()) {
                notas = notaRepository.findByCompetenciaIdCompetenciaAndBimestreAndAccesoNot(idCompetencia, (byte) 1, accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow());
            }
        }
        return notas.stream().map(this::toResponse).toList();
    }

    public List<NotaResponse> getPorMatricula(Integer idMatricula, Integer idUsuario, List<String> roles) {
        Matricula matricula = findMatricula(idMatricula);
        if (!esVisibleMatricula(matricula, idUsuario, roles)) {
            throw new ResourceNotFoundException("Matrícula no encontrada con id " + idMatricula);
        }
        return notaRepository.findByMatriculaIdMatriculaAndAccesoNot(idMatricula, accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow()).stream()
                .map(this::toResponse)
                .toList();
    }

    public List<NotaConsolidadoResponse> getConsolidado(Integer idGradoSeccion, Byte bimestre, Integer idCurso, Integer idUsuario, List<String> roles) {
        List<Nota> notas = notaRepository.findByMatriculaGradoSeccionIdGradoSeccionAndBimestreAndAccesoNot(
                idGradoSeccion, bimestre, accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow());

        Map<Integer, NotaConsolidadoResponse> porMatricula = new LinkedHashMap<>();
        for (Nota nota : notas) {
            Competencia comp = nota.getCompetencia();
            Curso curso = comp.getCurso();
            if (idCurso != null && !curso.getIdCurso().equals(idCurso)) {
                continue;
            }
            Matricula mat = nota.getMatricula();
            NotaConsolidadoResponse consolidado = porMatricula.computeIfAbsent(mat.getIdMatricula(), k -> {
                NotaConsolidadoResponse r = new NotaConsolidadoResponse();
                r.setIdMatricula(mat.getIdMatricula());
                r.setIdAlumno(mat.getAlumnoApoderado().getAlumno().getIdAlumno());
                r.setCodigoAlumno(mat.getAlumnoApoderado().getAlumno().getCodigo());
                r.setAlumno(mat.getAlumnoApoderado().getAlumno().getNombre() + " "
                        + mat.getAlumnoApoderado().getAlumno().getApellidoPat());
                r.setIdCurso(curso.getIdCurso());
                r.setCurso(curso.getNombre());
                r.setBimestre(bimestre);
                r.setCompetencias(new ArrayList<>());
                return r;
            });

            NotaConsolidadoResponse.CompetenciaNota cn = new NotaConsolidadoResponse.CompetenciaNota();
            cn.setIdCompetencia(comp.getIdCompetencia());
            cn.setCompetencia(comp.getNombre());
            cn.setOrden(comp.getOrden());
            cn.setIdNota(nota.getIdNota());
            cn.setCalificacion(nota.getCalificacion());
            cn.setConclusionDescriptiva(nota.getConclusionDescriptiva());
            cn.setFechaRegistro(nota.getFechaRegistro());
            consolidado.getCompetencias().add(cn);
        }
        return new ArrayList<>(porMatricula.values());
    }

    public PromedioResponse promedioPorBimestre(Integer idMatricula, Integer idUsuario, List<String> roles) {
        Matricula matricula = findMatricula(idMatricula);
        if (!esVisibleMatricula(matricula, idUsuario, roles)) {
            throw new ResourceNotFoundException("Matrícula no encontrada con id " + idMatricula);
        }
        List<Nota> notas = notaRepository.findByMatriculaIdMatriculaAndAccesoNot(idMatricula, accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow());

        Map<Byte, List<String>> notasPorBimestre = new LinkedHashMap<>();
        for (Nota nota : notas) {
            notasPorBimestre.computeIfAbsent(nota.getBimestre(), k -> new ArrayList<>()).add(nota.getCalificacion());
        }

        PromedioResponse response = new PromedioResponse();
        response.setIdMatricula(matricula.getIdMatricula());
        response.setIdAlumno(matricula.getAlumnoApoderado().getAlumno().getIdAlumno());
        response.setCodigoAlumno(matricula.getAlumnoApoderado().getAlumno().getCodigo());
        response.setAlumno(matricula.getAlumnoApoderado().getAlumno().getNombre() + " "
                + matricula.getAlumnoApoderado().getAlumno().getApellidoPat());

        List<PromedioResponse.PorBimestre> promedios = new ArrayList<>();
        for (Map.Entry<Byte, List<String>> entry : notasPorBimestre.entrySet()) {
            PromedioResponse.PorBimestre item = new PromedioResponse.PorBimestre();
            item.setBimestre(entry.getKey());
            item.setPromedioLiteral(calcularPromedioLiteral(entry.getValue()));
            promedios.add(item);
        }
        response.setPromedios(promedios);
        return response;
    }

    @Transactional
    public NotaResponse registrar(NotaRequest request, Integer idUsuario, List<String> roles) {
        Matricula matricula = findMatricula(request.getIdMatricula());
        Competencia competencia = findCompetencia(request.getIdCompetencia());
        validarCalificacion(request.getCalificacion());
        if ("C".equals(request.getCalificacion())) {
            validarConclusionDescriptiva(request.getConclusionDescriptiva());
        }

        Nota existente = notaRepository
                .findByMatriculaIdMatriculaAndCompetenciaIdCompetenciaAndBimestreAndAccesoNot(
                        matricula.getIdMatricula(), competencia.getIdCompetencia(), request.getBimestre(), accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow())
                .orElse(null);

        if (existente != null) {
            validarYConsumirAutorizacion(request.getCodigoAutorizacion(), idUsuario);
            existente.setCalificacion(request.getCalificacion());
            existente.setConclusionDescriptiva(request.getConclusionDescriptiva());
            existente.setFechaRegistro(LocalDate.now());
            return toResponse(notaRepository.save(existente));
        }
        return toResponse(crearNota(matricula, competencia, request.getBimestre(),
                request.getCalificacion(), request.getConclusionDescriptiva(), idUsuario));
    }

    @Transactional
    public List<NotaResponse> registrarPorLote(NotaBatchRequest request, Integer idUsuario, List<String> roles) {
        Competencia competencia = findCompetencia(request.getIdCompetencia());
        List<NotaResponse> result = new ArrayList<>();

        boolean algunRequiereAutorizacion = false;
        for (NotaBatchRequest.Item item : request.getNotas()) {
            Matricula matricula = findMatricula(item.getIdMatricula());
            validarCalificacion(item.getCalificacion());
            if ("C".equals(item.getCalificacion())) {
                validarConclusionDescriptiva(item.getConclusionDescriptiva());
            }
            boolean existe = notaRepository
                    .findByMatriculaIdMatriculaAndCompetenciaIdCompetenciaAndBimestreAndAccesoNot(
                            matricula.getIdMatricula(), competencia.getIdCompetencia(), request.getBimestre(), accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow())
                    .isPresent();
            if (existe) {
                algunRequiereAutorizacion = true;
            }
        }
        if (algunRequiereAutorizacion) {
            validarYConsumirAutorizacion(request.getCodigoAutorizacion(), idUsuario);
        }

        for (NotaBatchRequest.Item item : request.getNotas()) {
            Matricula matricula = findMatricula(item.getIdMatricula());
            Nota existente = notaRepository
                    .findByMatriculaIdMatriculaAndCompetenciaIdCompetenciaAndBimestreAndAccesoNot(
                            matricula.getIdMatricula(), competencia.getIdCompetencia(), request.getBimestre(), accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow())
                    .orElse(null);
            if (existente != null) {
                existente.setCalificacion(item.getCalificacion());
                existente.setConclusionDescriptiva(item.getConclusionDescriptiva());
                existente.setFechaRegistro(LocalDate.now());
                result.add(toResponse(notaRepository.save(existente)));
            } else {
                result.add(toResponse(crearNota(matricula, competencia, request.getBimestre(),
                        item.getCalificacion(), item.getConclusionDescriptiva(), idUsuario)));
            }
        }
        return result;
    }

    private Nota crearNota(Matricula matricula, Competencia competencia, Byte bimestre,
                           String calificacion, String conclusionDescriptiva, Integer idUsuarioRegistro) {
        Nota nota = new Nota();
        nota.setMatricula(matricula);
        nota.setCompetencia(competencia);
        nota.setBimestre(bimestre);
        nota.setCalificacion(calificacion);
        nota.setConclusionDescriptiva(conclusionDescriptiva);
        nota.setFechaRegistro(LocalDate.now());
        nota.setUsuarioRegistro(findUsuario(idUsuarioRegistro));
        nota.setAcceso(accesoRepository.findById(AccesoConstants.ACTIVO).orElseThrow());
        return notaRepository.save(nota);
    }

    private void validarCalificacion(String calificacion) {
        if (calificacion == null || !CALIFICACIONES_VALIDAS.contains(calificacion.toUpperCase())) {
            throw new IllegalArgumentException("La calificación debe ser AD, A, B o C");
        }
    }

    private void validarConclusionDescriptiva(String conclusion) {
        if (conclusion == null || conclusion.isBlank()) {
            throw new IllegalArgumentException("La calificación 'C' requiere una conclusión descriptiva obligatoria");
        }
    }

    @Transactional
    public AutorizacionResponse generarAutorizacion(Integer idUsuario, List<String> roles, AutorizacionRequest request) {
        boolean esAutorizador = roles != null && roles.stream()
                .anyMatch(r -> "ADMIN".equalsIgnoreCase(r) || "Administrador".equalsIgnoreCase(r));
        if (!esAutorizador) {
            throw new AutorizacionRequeridaException("Solo un director o usuario con rol ADMIN puede generar códigos de autorización");
        }
        Usuario emisor = findUsuario(idUsuario);
        if (request == null || request.getIdUsuarioDestinatario() == null) {
            throw new AutorizacionRequeridaException("Debe indicar el docente al que se asigna el código");
        }
        Usuario destinatario = findUsuario(request.getIdUsuarioDestinatario());

        String codigo = generarCodigo();
        NotaAutorizacion autorizacion = new NotaAutorizacion();
        autorizacion.setCodigo(codigo);
        autorizacion.setCodigoHash(generarCodigoHash(codigo));
        autorizacion.setUsuarioEmisor(emisor);
        autorizacion.setUsuarioDestinatario(destinatario);
        autorizacion.setFechaAsignacion(LocalDateTime.now());
        autorizacion.setFechaGeneracion(LocalDateTime.now());
        autorizacion.setFechaExpiracion(LocalDateTime.now().plusMinutes(MINUTOS_VALIDEZ));
        autorizacion.setUsado(NO_USADO);
        autorizacion.setAcceso(accesoRepository.findById(AccesoConstants.ACTIVO).orElseThrow());
        notaAutorizacionRepository.save(autorizacion);

        AutorizacionResponse response = new AutorizacionResponse();
        response.setCodigo(codigo);
        response.setDestinatario(nombreUsuario(destinatario));
        response.setFechaGeneracion(autorizacion.getFechaGeneracion());
        response.setFechaExpiracion(autorizacion.getFechaExpiracion());
        return response;
    }

    @Transactional(readOnly = true)
    public List<AutorizacionRegistroResponse> listarAutorizaciones(Integer idUsuario, List<String> roles) {
        boolean esAutorizador = roles != null && roles.stream()
                .anyMatch(r -> "ADMIN".equalsIgnoreCase(r) || "Administrador".equalsIgnoreCase(r));
        if (!esAutorizador) {
            throw new AutorizacionRequeridaException("Solo el administrador puede consultar los códigos de autorización");
        }
        LocalDateTime ahora = LocalDateTime.now();
        return notaAutorizacionRepository.findAll().stream()
                .filter(a -> !a.getAcceso().getIdAcceso().equals(AccesoConstants.ELIMINADO))
                .map(a -> {
                    AutorizacionRegistroResponse r = new AutorizacionRegistroResponse();
                    r.setIdNotaAutorizacion(a.getIdNotaAutorizacion());
                    r.setCodigo(a.getCodigo());
                    r.setEmisor(nombreUsuario(a.getUsuarioEmisor()));
                    r.setDestinatario(a.getUsuarioDestinatario() != null ? nombreUsuario(a.getUsuarioDestinatario()) : null);
                    r.setFechaAsignacion(a.getFechaAsignacion());
                    r.setFechaGeneracion(a.getFechaGeneracion());
                    r.setFechaExpiracion(a.getFechaExpiracion());
                    r.setEstado(estadoAutorizacion(a, ahora));
                    r.setConsumidor(a.getUsuarioConsumidor() != null ? nombreUsuario(a.getUsuarioConsumidor()) : null);
                    r.setFechaUso(a.getFechaUso());
                    return r;
                })
                .sorted(Comparator.comparing(AutorizacionRegistroResponse::getFechaGeneracion).reversed())
                .toList();
    }

    @Transactional(readOnly = true)
    public List<NotaReporteResponse> reporte(LocalDate inicio, LocalDate fin, Integer idGradoSeccion, Integer bimestre) {
        return notaRepository.findByFechaRegistroBetweenAndAccesoNot(inicio, fin, accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow()).stream()
                .filter(nota -> {
                    if (idGradoSeccion != null) {
                        Integer gsId = nota.getMatricula().getGradoSeccion().getIdGradoSeccion();
                        if (!idGradoSeccion.equals(gsId)) return false;
                    }
                    if (bimestre != null) {
                        if (bimestre != (int) nota.getBimestre()) return false;
                    }
                    return true;
                })
                .map(nota -> {
                    NotaReporteResponse r = new NotaReporteResponse();
                    r.setFecha(nota.getFechaRegistro());
                    r.setAlumno(nota.getMatricula().getAlumnoApoderado().getAlumno().getNombre() + " "
                            + nota.getMatricula().getAlumnoApoderado().getAlumno().getApellidoPat());
                    r.setCodigo(nota.getMatricula().getAlumnoApoderado().getAlumno().getCodigo());
                    r.setCurso(nota.getCompetencia().getCurso().getNombre());
                    r.setCompetencia(nota.getCompetencia().getNombre());
                    r.setBimestre((int) nota.getBimestre());
                    r.setCalificacion(nota.getCalificacion());
                    r.setDocente(nombreUsuario(nota.getUsuarioRegistro()));
                    return r;
                })
                .toList();
    }

    private String calcularPromedioLiteral(List<String> calificaciones) {
        Map<String, Integer> valores = Map.of("AD" , 4, "A" , 3, "B" , 2, "C" , 1);
        Map<Integer, String> inversa = Map.of(4, "AD", 3, "A", 2, "B", 1, "C");
        if (calificaciones.isEmpty()) return "N/A";
        double promedio = calificaciones.stream()
                .mapToInt(c -> valores.getOrDefault(c.toUpperCase(), 0))
                .average()
                .orElse(0);
        int redondeado = (int) Math.round(promedio);
        return inversa.getOrDefault(redondeado, "N/A");
    }

    private String nombreUsuario(Usuario usuario) {
        return usuario.getNombre() + " " + usuario.getApellidoPat();
    }

    private String estadoAutorizacion(NotaAutorizacion autorizacion, LocalDateTime ahora) {
        if (autorizacion.getUsado() == USADO) return "USADA";
        if (autorizacion.getFechaExpiracion().isBefore(ahora)) return "EXPIRADA";
        return "ACTIVA";
    }

    private void validarYConsumirAutorizacion(String codigo, Integer idUsuarioConsumidor) {
        NotaAutorizacion autorizacion = obtenerAutorizacionValida(codigo);
        autorizacion.setUsado(USADO);
        autorizacion.setUsuarioConsumidor(findUsuario(idUsuarioConsumidor));
        autorizacion.setFechaUso(LocalDateTime.now());
        notaAutorizacionRepository.save(autorizacion);
    }

    public void validarAutorizacion(String codigo) {
        obtenerAutorizacionValida(codigo);
    }

    private NotaAutorizacion obtenerAutorizacionValida(String codigo) {
        if (codigo == null || codigo.isBlank()) {
            throw new AutorizacionRequeridaException(
                    "La nota ya existe: se requiere un código de autorización del director para modificarla");
        }
        String hash = generarCodigoHash(codigo);
        NotaAutorizacion autorizacion = notaAutorizacionRepository.findByCodigoHashAndAccesoNot(hash, accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow())
                .orElseThrow(() -> new AutorizacionRequeridaException("El código de autorización no es válido"));
        if (autorizacion.getUsado() == USADO) {
            throw new AutorizacionRequeridaException("El código de autorización ya fue utilizado");
        }
        if (autorizacion.getFechaExpiracion().isBefore(LocalDateTime.now())) {
            throw new AutorizacionRequeridaException("El código de autorización ha expirado");
        }
        return autorizacion;
    }

    private boolean esVisibleMatricula(Matricula matricula, Integer idUsuario, List<String> roles) {
        if (accesoContextoService.esGestion(roles)) {
            return true;
        }
        if (accesoContextoService.esPersonalDocente(roles)) {
            return accesoContextoService.docenteDeUsuario(idUsuario)
                    .map(d -> matricula.getGradoSeccion().getIdGradoSeccion()
                            .equals(matricula.getGradoSeccion().getIdGradoSeccion()))
                    .orElse(false);
        }
        if (accesoContextoService.esApoderado(roles)) {
            return accesoContextoService.apoderadoDeUsuario(idUsuario)
                    .map(a -> a.getIdApoderado().equals(
                            matricula.getAlumnoApoderado().getApoderado().getIdApoderado()))
                    .orElse(false);
        }
        return false;
    }

    private String generarCodigo() {
        String caracteres = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder(8);
        for (int i = 0; i < 8; i++) {
            sb.append(caracteres.charAt(random.nextInt(caracteres.length())));
        }
        return sb.toString();
    }

    private String generarCodigoHash(String codigo) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(codigo.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalArgumentException("No se pudo generar el hash del código");
        }
    }

    private Matricula findMatricula(Integer id) {
        return matriculaRepository.findByIdMatriculaAndAccesoNot(id, accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow())
                .orElseThrow(() -> new ResourceNotFoundException("Matrícula no encontrada con id " + id));
    }

    private Competencia findCompetencia(Integer id) {
        return competenciaRepository.findByIdCompetenciaAndAccesoNot(id, accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow())
                .orElseThrow(() -> new ResourceNotFoundException("Competencia no encontrada con id " + id));
    }

    private Usuario findUsuario(Integer id) {
        return usuarioRepository.findByIdUsuarioAndAccesoNot(id, accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con id " + id));
    }

    private NotaResponse toResponse(Nota nota) {
        Matricula matricula = nota.getMatricula();
        Competencia competencia = nota.getCompetencia();
        NotaResponse response = new NotaResponse();
        response.setIdNota(nota.getIdNota());
        response.setIdMatricula(matricula.getIdMatricula());
        response.setIdAlumno(matricula.getAlumnoApoderado().getAlumno().getIdAlumno());
        response.setCodigoAlumno(matricula.getAlumnoApoderado().getAlumno().getCodigo());
        response.setAlumno(matricula.getAlumnoApoderado().getAlumno().getNombre() + " "
                + matricula.getAlumnoApoderado().getAlumno().getApellidoPat());
        response.setIdCompetencia(competencia.getIdCompetencia());
        response.setCompetencia(competencia.getNombre());
        response.setIdCurso(competencia.getCurso().getIdCurso());
        response.setCurso(competencia.getCurso().getNombre());
        response.setBimestre(nota.getBimestre());
        response.setCalificacion(nota.getCalificacion());
        response.setConclusionDescriptiva(nota.getConclusionDescriptiva());
        response.setFechaRegistro(nota.getFechaRegistro());
        response.setDocenteRegistro(nombreUsuario(nota.getUsuarioRegistro()));
        response.setAccesoId(nota.getAcceso().getIdAcceso().longValue());
        return response;
    }
}
