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
    private Long accesoId;
    private List<ApoderadoResponse> apoderados;

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
        response.setAccesoId(alumno.getAcceso().getIdAcceso().longValue());
        if (vinculos != null) {
            response.setApoderados(vinculos.stream()
                    .sorted(Comparator.comparing(AlumnoApoderado::getApoPrincipal).reversed())
                    .map(AlumnoApoderado::getApoderado)
                    .filter(ap -> ap.getAcceso().getIdAcceso() != 2)
                    .map(ApoderadoResponse::fromEntity)
                    .toList());
        }
        return response;
    }
}