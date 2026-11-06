package com.example.Escolar.Controller;

import com.example.Escolar.Dto.MetodoPagoRequest;
import com.example.Escolar.Dto.MetodoPagoResponse;
import com.example.Escolar.Dto.MontoMatriculaRequest;
import com.example.Escolar.Dto.MontoMatriculaResponse;
import com.example.Escolar.Dto.OpcionResponse;
import com.example.Escolar.Dto.ParentescoResponse;
import com.example.Escolar.Service.PagoConfigService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class PagoConfigController {

    private final PagoConfigService pagoConfigService;

    @GetMapping("/parentescos")
    public List<ParentescoResponse> parentescos() {
        return pagoConfigService.parentescos();
    }

    @GetMapping("/sexos")
    public List<OpcionResponse> sexos() {
        return pagoConfigService.sexos();
    }

    @GetMapping("/metodos-pago")
    public List<MetodoPagoResponse> metodos(@RequestParam(defaultValue = "false") boolean soloActivos) {
        return pagoConfigService.metodos(soloActivos);
    }

    @PostMapping("/metodos-pago")
    @ResponseStatus(HttpStatus.CREATED)
    public MetodoPagoResponse crearMetodo(@Valid @RequestBody MetodoPagoRequest request) {
        return pagoConfigService.crearMetodo(request);
    }

    @PutMapping("/metodos-pago/{id}")
    public MetodoPagoResponse actualizarMetodo(@PathVariable Integer id, @Valid @RequestBody MetodoPagoRequest request) {
        return pagoConfigService.actualizarMetodo(id, request);
    }

    @DeleteMapping("/metodos-pago/{id}")
    public ResponseEntity<Void> eliminarMetodo(@PathVariable Integer id) {
        pagoConfigService.eliminarMetodo(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/montos-matricula")
    public List<MontoMatriculaResponse> montos(@RequestParam Integer idAnio) {
        return pagoConfigService.montos(idAnio);
    }

    @PutMapping("/montos-matricula")
    public List<MontoMatriculaResponse> guardarMontos(@Valid @RequestBody MontoMatriculaRequest request) {
        return pagoConfigService.guardarMontos(request);
    }
}
