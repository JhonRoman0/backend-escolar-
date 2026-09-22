package com.example.Escolar.Service;

import com.example.Escolar.Dto.PlantillaSiagieResponse;
import com.example.Escolar.Exception.ResourceNotFoundException;
import com.example.Escolar.Model.PlantillaSiagie;
import com.example.Escolar.Repository.AccesoRepository;
import com.example.Escolar.Repository.PlantillaSiagieRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PlantillaSiagieService {

    private static final byte NO_VIGENTE = 0;
    private static final byte VIGENTE = 1;

    private final PlantillaSiagieRepository plantillaRepository;
    private final AccesoRepository accesoRepository;

    @Transactional
    public PlantillaSiagieResponse subir(String anio, String nombreArchivo, byte[] archivo) {
        plantillaRepository.findByAnioAndAccesoNot(anio, accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow()).stream()
                .filter(p -> p.getVigente() == VIGENTE)
                .forEach(p -> {
                    p.setVigente(NO_VIGENTE);
                    plantillaRepository.save(p);
                });

        PlantillaSiagie plantilla = new PlantillaSiagie();
        plantilla.setAnio(anio.trim());
        plantilla.setVigente(VIGENTE);
        plantilla.setArchivo(archivo);
        plantilla.setNombreArchivo(nombreArchivo);
        plantilla.setAcceso(accesoRepository.findById(AccesoConstants.ACTIVO).orElseThrow());

        return toResponse(plantillaRepository.save(plantilla));
    }

    @Transactional(readOnly = true)
    public List<PlantillaSiagieResponse> listar() {
        return plantillaRepository.findByAccesoNot(accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow()).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public PlantillaSiagie obtenerVigente() {
        return plantillaRepository.findByVigenteAndAccesoNot(VIGENTE, accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No hay plantilla SIAGIE vigente. Suba una plantilla desde el panel del administrador."));
    }

    private PlantillaSiagieResponse toResponse(PlantillaSiagie p) {
        PlantillaSiagieResponse r = new PlantillaSiagieResponse();
        r.setIdPlantilla(p.getIdPlantilla());
        r.setAnio(p.getAnio());
        r.setVigente(p.getVigente() == VIGENTE);
        r.setNombreArchivo(p.getNombreArchivo());
        return r;
    }
}
