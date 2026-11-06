package com.example.Escolar.Controller;

import com.example.Escolar.Dto.NivelResponse;
import com.example.Escolar.Service.NivelService;
import com.example.Escolar.Service.OrdenAcademicoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/niveles")
@RequiredArgsConstructor
public class NivelController {

    private final NivelService nivelService;
    private final OrdenAcademicoService ordenAcademicoService;

    @GetMapping
    public List<NivelResponse> getAll() {
        return nivelService.getAll();
    }

    @PutMapping("/{id}/orden")
    public ResponseEntity<Void> mover(@PathVariable Integer id, @RequestParam boolean subir) {
        ordenAcademicoService.moverNivel(id, subir);
        return ResponseEntity.noContent().build();
    }
}
