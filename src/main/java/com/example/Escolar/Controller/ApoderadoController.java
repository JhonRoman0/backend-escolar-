package com.example.Escolar.Controller;

import com.example.Escolar.Dto.ApoderadoResponse;
import com.example.Escolar.Service.AccesoContextoService;
import com.example.Escolar.Service.ApoderadoService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/apoderados")
@RequiredArgsConstructor
public class ApoderadoController {

    private final ApoderadoService apoderadoService;
    private final AccesoContextoService accesoContextoService;

    @GetMapping
    public List<ApoderadoResponse> getAll() {
        return apoderadoService.getAll(
                accesoContextoService.idUsuarioAutenticado(),
                accesoContextoService.rolesAutenticados());
    }

    @GetMapping("/{id}")
    public ApoderadoResponse getById(@PathVariable Integer id) {
        return apoderadoService.getById(id,
                accesoContextoService.idUsuarioAutenticado(),
                accesoContextoService.rolesAutenticados());
    }

    @GetMapping("/dni/{documento}")
    public ApoderadoResponse getByDocumento(@PathVariable String documento) {
        return apoderadoService.getByDocumento(documento,
                accesoContextoService.idUsuarioAutenticado(),
                accesoContextoService.rolesAutenticados());
    }
}
