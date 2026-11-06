package com.example.Escolar.Service;

import com.example.Escolar.Dto.ReniecResponse;
import com.example.Escolar.Exception.ResourceNotFoundException;
import com.example.Escolar.Repository.AccesoRepository;
import com.example.Escolar.Repository.UsuarioRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class ReniecService {

    private final RestClient restClient;
    private final UsuarioRepository usuarioRepository;
    private final AccesoRepository accesoRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${api.dni.url:https://apiperu.dev/api/dni}")
    private String dniBaseUrl;

    @Value("${api.dni.token:}")
    private String dniToken;

    // Caché en memoria: DNI -> entrada con TTL 24h, max 100 entradas (plan gratuito)
    private static final long TTL_MILLIS = 24 * 60 * 60 * 1000L;
    private static final int MAX_CACHE_SIZE = 100;
    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();

    private record CacheEntry(ReniecResponse data, long expiraEn) {}

    public ReniecService(RestClient restClient, UsuarioRepository usuarioRepository,
                         AccesoRepository accesoRepository) {
        this.restClient = restClient;
        this.usuarioRepository = usuarioRepository;
        this.accesoRepository = accesoRepository;
    }

    public ReniecResponse consultar(String dni) {
        if (dni == null || !dni.matches("^[0-9]{8}$")) {
            throw new IllegalArgumentException("El DNI debe contener exactamente 8 dígitos");
        }

        // 1. DB primero: si ya existe como usuario/alumno, no gastar API
        var accesoEliminado = accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow();
        var usuarioOpt = usuarioRepository.findByDocumentoIdentidadAndAccesoNot(dni, accesoEliminado);
        if (usuarioOpt.isPresent()) {
            var u = usuarioOpt.get();
            ReniecResponse r = new ReniecResponse();
            r.setDni(dni);
            r.setNombres(u.getNombre());
            r.setApellidoPaterno(u.getApellidoPat());
            r.setApellidoMaterno(u.getApellidoMat());
            r.setOrigen("LOCAL");
            return r;
        }

        // 2. Caché en memoria
        CacheEntry entry = cache.get(dni);
        if (entry != null && Instant.now().toEpochMilli() < entry.expiraEn()) {
            entry.data().setOrigen("CACHE");
            return entry.data();
        } else if (entry != null) {
            cache.remove(dni);
        }

        // 3. Llamada a apisperu.dev
        if (dniToken == null || dniToken.isBlank()) {
            throw new IllegalStateException("API de RENIEC no configurada. Configure API_TOKEN en .env");
        }

        try {
            String body = restClient.get()
                    .uri(dniBaseUrl + "/" + dni)
                    .header("Authorization", "Bearer " + dniToken)
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(body);
            // apisperu.dev responde: { success:true, data:{ numero, nombres, apellido_paterno, apellido_materno, codigo_verificacion } }
            JsonNode data = root.has("data") ? root.get("data") : root;

            if (data == null || data.isNull() || (!data.has("numero") && !data.has("dni"))) {
                throw new ResourceNotFoundException("DNI no encontrado en RENIEC");
            }

            ReniecResponse r = new ReniecResponse();
            // apisperu.dev usa "numero", fallback a "dni" por compatibilidad
            String dniValor = data.has("numero") ? data.path("numero").asText(dni) : data.path("dni").asText(dni);
            r.setDni(dniValor);
            r.setNombres(data.path("nombres").asText(null));
            r.setApellidoPaterno(data.path("apellido_paterno").asText(
                    data.path("apellidoPaterno").asText(null)));
            r.setApellidoMaterno(data.path("apellido_materno").asText(
                    data.path("apellidoMaterno").asText(null)));
            // apisperu.dev usa codigo_verificacion (snake) o codigoVerificacion
            String cv = data.has("codigo_verificacion") ? data.path("codigo_verificacion").asText(null)
                    : data.path("codigoVerificacion").asText(data.path("codVerifica").asText(null));
            r.setCodVerifica(cv);
            r.setOrigen("RENIEC");

            // Guardar en caché con evicción si supera 100
            if (cache.size() >= MAX_CACHE_SIZE) {
                // Evicción simple: remover el más antiguo
                cache.keySet().stream().findFirst().ifPresent(cache::remove);
            }
            cache.put(dni, new CacheEntry(r, Instant.now().toEpochMilli() + TTL_MILLIS));

            log.info("RENIEC consultado para DNI {} (origen RENIEC)", dni);
            return r;

        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            String msg = e.getMessage() != null ? e.getMessage() : "";
            if (msg.contains("404") || msg.contains("Not Found")) {
                throw new ResourceNotFoundException("DNI no encontrado en RENIEC");
            }
            log.error("Error consultando RENIEC para DNI {}: {}", dni, e.getMessage());
            throw new RuntimeException("Error consultando servicio RENIEC: " + e.getMessage());
        }
    }
}
