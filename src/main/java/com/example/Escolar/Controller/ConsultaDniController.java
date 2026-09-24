package com.example.Escolar.Controller;

import com.example.Escolar.Dto.ReniecResponse;
import com.example.Escolar.Service.ReniecService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/consulta/dni")
@RequiredArgsConstructor
public class ConsultaDniController {

    private final ReniecService reniecService;

    @GetMapping("/{numero}")
    public ReniecResponse consultar(@PathVariable String numero) {
        return reniecService.consultar(numero);
    }
}
