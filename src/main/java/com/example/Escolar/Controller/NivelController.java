package com.example.Escolar.Controller;

import com.example.Escolar.Dto.NivelResponse;
import com.example.Escolar.Service.NivelService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/niveles")
@RequiredArgsConstructor
public class NivelController {

    private final NivelService nivelService;

    @GetMapping
    public List<NivelResponse> getAll() {
        return nivelService.getAll();
    }
}
