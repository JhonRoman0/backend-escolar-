package com.example.Escolar.Controller;

import com.example.Escolar.Dto.TipoContratoResponse;
import com.example.Escolar.Service.TipoContratoService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/tipos-contrato")
@RequiredArgsConstructor
public class TipoContratoController {

    private final TipoContratoService tipoContratoService;

    @GetMapping
    public List<TipoContratoResponse> getAll() {
        return tipoContratoService.getAll();
    }
}
