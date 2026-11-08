package com.example.Escolar.Controller;

import com.example.Escolar.Service.AccesoContextoService;
import com.example.Escolar.Service.SiagieExportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/notas/siagie")
@RequiredArgsConstructor
public class SiagieExportController {

    private final SiagieExportService siagieExportService;
    private final AccesoContextoService accesoContextoService;

    @GetMapping("/pendientes")
    public SiagieExportService.Pendientes pendientes(@RequestParam Integer idGradoSeccion, @RequestParam Integer periodo) {
        return siagieExportService.pendientes(idGradoSeccion, periodo,
                accesoContextoService.idUsuarioAutenticado(), accesoContextoService.rolesAutenticados());
    }

    @GetMapping
    public ResponseEntity<byte[]> exportar(@RequestParam Integer idGradoSeccion, @RequestParam Integer periodo) {
        SiagieExportService.Archivo archivo = siagieExportService.exportar(idGradoSeccion, periodo,
                accesoContextoService.idUsuarioAutenticado(), accesoContextoService.rolesAutenticados());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(archivo.nombre()).build().toString())
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(archivo.contenido());
    }
}
