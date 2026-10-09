package com.example.Escolar.Controller;

import com.example.Escolar.Dto.GradoAcademicoResponse;
import com.example.Escolar.Service.GradoAcademicoService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/grados-academicos")
@RequiredArgsConstructor
public class GradoAcademicoController {

    private final GradoAcademicoService gradoAcademicoService;

    @GetMapping
    public List<GradoAcademicoResponse> getAll() {
        return gradoAcademicoService.getAll();
    }
}
