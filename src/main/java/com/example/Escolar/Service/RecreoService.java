package com.example.Escolar.Service;

import com.example.Escolar.Dto.RecreoRequest;
import com.example.Escolar.Dto.RecreoResponse;
import com.example.Escolar.Exception.ResourceNotFoundException;
import com.example.Escolar.Model.GradoSeccion;
import com.example.Escolar.Model.Nivel;
import com.example.Escolar.Model.Recreo;
import com.example.Escolar.Repository.AccesoRepository;
import com.example.Escolar.Repository.GradoSeccionRepository;
import com.example.Escolar.Repository.NivelRepository;
import com.example.Escolar.Repository.RecreoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RecreoService {

    private final RecreoRepository recreoRepository;
    private final NivelRepository nivelRepository;
    private final GradoSeccionRepository gradoSeccionRepository;
    private final AccesoRepository accesoRepository;

    public List<RecreoResponse> getAll() {
        return recreoRepository.findByAccesoNot(accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public RecreoResponse getById(Integer id) {
        return toResponse(findRecreo(id));
    }

    public List<RecreoResponse> getByNivel(Integer idNivel) {
        Nivel nivel = findNivel(idNivel);
        return recreoRepository.findByNivelAndAccesoNot(nivel, accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public List<RecreoResponse> getByNivelAndDia(Integer idNivel, Byte diaSemana) {
        Nivel nivel = findNivel(idNivel);
        return recreoRepository.findByNivelAndDiaSemanaAndAccesoNot(nivel, diaSemana, accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public RecreoResponse create(RecreoRequest request) {
        Nivel nivel = findNivel(request.getIdNivel());
        GradoSeccion gradoSeccion = null;
        if (request.getIdGradoSeccion() != null) {
            gradoSeccion = findGradoSeccion(request.getIdGradoSeccion());
        }

        Recreo recreo = new Recreo();
        recreo.setNivel(nivel);
        recreo.setGradoSeccion(gradoSeccion);
        recreo.setDiaSemana(request.getDiaSemana());
        recreo.setHoraInicio(request.getHoraInicio());
        recreo.setHoraFin(request.getHoraFin());
        recreo.setAcceso(accesoRepository.findById(AccesoConstants.ACTIVO).orElseThrow());

        return toResponse(recreoRepository.save(recreo));
    }

    @Transactional
    public RecreoResponse update(Integer id, RecreoRequest request) {
        Recreo recreo = findRecreo(id);
        if (request.getIdNivel() != null) {
            recreo.setNivel(findNivel(request.getIdNivel()));
        }
        if (request.getIdGradoSeccion() != null) {
            recreo.setGradoSeccion(findGradoSeccion(request.getIdGradoSeccion()));
        }
        if (request.getDiaSemana() != null) {
            recreo.setDiaSemana(request.getDiaSemana());
        }
        if (request.getHoraInicio() != null) {
            recreo.setHoraInicio(request.getHoraInicio());
        }
        if (request.getHoraFin() != null) {
            recreo.setHoraFin(request.getHoraFin());
        }
        return toResponse(recreoRepository.save(recreo));
    }

    @Transactional
    public void delete(Integer id) {
        Recreo recreo = findRecreo(id);
        recreo.setAcceso(accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow());
        recreoRepository.save(recreo);
    }

    private Recreo findRecreo(Integer id) {
        return recreoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Recreo no encontrado con id " + id));
    }

    private Nivel findNivel(Integer id) {
        return nivelRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Nivel no encontrado con id " + id));
    }

    private GradoSeccion findGradoSeccion(Integer id) {
        return gradoSeccionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Grado-Seccion no encontrado con id " + id));
    }

    private RecreoResponse toResponse(Recreo recreo) {
        RecreoResponse response = new RecreoResponse();
        response.setIdRecreo(recreo.getIdRecreo());
        response.setIdNivel(recreo.getNivel().getIdNivel());
        response.setNivel(recreo.getNivel().getNombre());
        if (recreo.getGradoSeccion() != null) {
            response.setIdGradoSeccion(recreo.getGradoSeccion().getIdGradoSeccion());
            response.setGradoSeccion(recreo.getGradoSeccion().getGrado().getNombre() + " " + recreo.getGradoSeccion().getSeccion().getNombre());
        }
        response.setDiaSemana(recreo.getDiaSemana());
        response.setHoraInicio(recreo.getHoraInicio());
        response.setHoraFin(recreo.getHoraFin());
        response.setAccesoId(recreo.getAcceso().getIdAcceso().longValue());
        return response;
    }
}
