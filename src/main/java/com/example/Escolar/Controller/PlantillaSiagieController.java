package com.example.Escolar.Controller;

import com.example.Escolar.Dto.PlantillaSiagieResponse;
import com.example.Escolar.Service.PlantillaSiagieService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/plantillas-siagie")
@RequiredArgsConstructor
public class PlantillaSiagieController {

    private final PlantillaSiagieService plantillaService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public PlantillaSiagieResponse subir(
            @RequestParam String anio,
            @RequestPart MultipartFile archivo) throws IOException {
        return plantillaService.subir(anio, archivo.getOriginalFilename(), archivo.getBytes());
    }

    @GetMapping
    public List<PlantillaSiagieResponse> listar() {
        return plantillaService.listar();
    }

    @GetMapping("/vigente")
    public ResponseEntity<byte[]> descargarVigente() {
        com.example.Escolar.Model.PlantillaSiagie p = plantillaService.obtenerVigente();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData("attachment", p.getNombreArchivo());
        headers.setContentLength(p.getArchivo().length);
        return new ResponseEntity<>(p.getArchivo(), headers, HttpStatus.OK);
    }
}
