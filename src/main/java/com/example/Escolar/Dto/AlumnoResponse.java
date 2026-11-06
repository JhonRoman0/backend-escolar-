package com.example.Escolar.Dto;

import com.example.Escolar.Model.Alumno;
import com.example.Escolar.Model.AlumnoApoderado;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

@Getter
@Setter
public class AlumnoResponse {
    private Integer idAlumno;
    private String codigo;
    private String codigoHash;
    private String nombre;
    private String apellidoPat;
    private String apellidoMat;
    private LocalDate fechaNacimiento;
    private String direccion;
    private String documentoIdentidad;
    private String urlFoto;
    private String sexo;
    private boolean tieneDiscapacidad;
    private String discapacidad;
    private Long accesoId;
    private List<ApoderadoResponse> apoderados;
    // Enriquecimiento vía matrícula vigente (fallback "Sin matrícula")
    private Integer idNivel;
    private String nivel;
    private Integer idGrado;
    private String grado;
    private Integer idSeccion;
    private String seccion;
    private Integer idTurno;
    private String turno;
    private Integer idGradoSeccion;
    private Integer idAnio;
    private String anio;

    public static AlumnoResponse fromEntity(Alumno alumno, List<AlumnoApoderado> vinculos) {
        AlumnoResponse response = new AlumnoResponse();
        response.setIdAlumno(alumno.getIdAlumno());
        response.setCodigo(alumno.getCodigo());
        response.setCodigoHash(alumno.getCodigoHash());
        response.setNombre(alumno.getNombre());
        response.setApellidoPat(alumno.getApellidoPat());
        response.setApellidoMat(alumno.getApellidoMat());
        response.setFechaNacimiento(alumno.getFechaNacimiento());
        response.setDireccion(alumno.getDireccion());
        response.setDocumentoIdentidad(alumno.getDocumentoIdentidad());
        response.setUrlFoto(alumno.getUrlFoto());
        response.setSexo(alumno.getSexo() == null ? null : alumno.getSexo().name());
        response.setTieneDiscapacidad(Boolean.TRUE.equals(alumno.getTieneDiscapacidad()));
        response.setDiscapacidad(alumno.getDiscapacidad());
        response.setAccesoId(alumno.getAcceso().getIdAcceso().longValue());
        if (vinculos != null) {
            response.setApoderados(vinculos.stream()
                    .sorted(Comparator.comparing(AlumnoApoderado::getApoPrincipal).reversed())
                    .filter(v -> v.getApoderado().getAcceso().getIdAcceso() != 2)
                    .map(ApoderadoResponse::fromVinculo)
                    .toList());
        }
        // Fallback académico cuando no se enriquece vía servicio
        response.setNivel("Sin matrícula");
        response.setGrado("Sin matrícula");
        response.setSeccion("Sin matrícula");
        response.setTurno("Sin matrícula");
        response.setAnio("Sin matrícula");
        return response;
    }

    public static AlumnoResponse fromEntity(Alumno alumno, List<AlumnoApoderado> vinculos, com.example.Escolar.Model.Matricula matriculaVigente) {
        AlumnoResponse response = fromEntity(alumno, vinculos);
        if (matriculaVigente != null && matriculaVigente.getGradoSeccion() != null) {
            var gs = matriculaVigente.getGradoSeccion();
            response.setIdGradoSeccion(gs.getIdGradoSeccion());
            if (gs.getGrado() != null) {
                response.setIdGrado(gs.getGrado().getIdGrado());
                response.setGrado(gs.getGrado().getNombre());
                if (gs.getGrado().getNivel() != null) {
                    response.setIdNivel(gs.getGrado().getNivel().getIdNivel());
                    response.setNivel(gs.getGrado().getNivel().getNombre());
                }
            }
            if (gs.getSeccion() != null) {
                response.setIdSeccion(gs.getSeccion().getIdSeccion());
                response.setSeccion(gs.getSeccion().getNombre());
            } else {
                response.setSeccion("Única");
            }
            if (gs.getTurno() != null) {
                response.setIdTurno(gs.getTurno().getIdTurno());
                response.setTurno(gs.getTurno().getNombre());
            }
            if (gs.getAnioEscolar() != null) {
                response.setIdAnio(gs.getAnioEscolar().getIdAnio());
                response.setAnio(gs.getAnioEscolar().getAnio());
            }
        }
        return response;
    }
}