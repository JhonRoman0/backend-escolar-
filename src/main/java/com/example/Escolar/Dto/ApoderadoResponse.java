package com.example.Escolar.Dto;

import com.example.Escolar.Model.AlumnoApoderado;
import com.example.Escolar.Model.Apoderado;
import com.example.Escolar.Model.Usuario;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class ApoderadoResponse {
    private Integer idApoderado;
    private Integer idUsuario;
    private String codigo;
    private String nombre;
    private String apellidoPat;
    private String apellidoMat;
    private String gmail;
    private String celular;
    private String direccion;
    private Integer idParentesco;
    private String parentesco;
    private String sexo;
    private boolean principal;
    private String documentoIdentidad;
    private LocalDate fechaNaci;
    private String urlFoto;
    private Long accesoId;

    public static ApoderadoResponse fromEntity(Apoderado apoderado) {
        ApoderadoResponse response = new ApoderadoResponse();
        response.setIdApoderado(apoderado.getIdApoderado());
        if (apoderado.getUsuario() != null) {
            poblarUsuario(response, apoderado.getUsuario());
        }
        response.setCelular(apoderado.getCelular());
        response.setDireccion(apoderado.getDireccion());
        response.setSexo(apoderado.getSexo() == null ? null : apoderado.getSexo().name());
        response.setAccesoId(apoderado.getAcceso().getIdAcceso().longValue());
        return response;
    }

    // El parentesco es del vínculo con un alumno, no del apoderado.
    public static ApoderadoResponse fromVinculo(AlumnoApoderado vinculo) {
        ApoderadoResponse response = fromEntity(vinculo.getApoderado());
        if (vinculo.getParentesco() != null) {
            response.setIdParentesco(vinculo.getParentesco().getIdParentesco());
            response.setParentesco(vinculo.getParentesco().getNombre());
        }
        response.setPrincipal(vinculo.getApoPrincipal() == 1);
        return response;
    }

    public static ApoderadoResponse fromUsuario(Usuario usuario) {
        ApoderadoResponse response = new ApoderadoResponse();
        poblarUsuario(response, usuario);
        return response;
    }

    private static void poblarUsuario(ApoderadoResponse response, Usuario usuario) {
        response.setIdUsuario(usuario.getIdUsuario());
        response.setCodigo(usuario.getCodigo());
        response.setNombre(usuario.getNombre());
        response.setApellidoPat(usuario.getApellidoPat());
        response.setApellidoMat(usuario.getApellidoMat());
        response.setGmail(usuario.getGmail());
        response.setDocumentoIdentidad(usuario.getDocumentoIdentidad());
        response.setFechaNaci(usuario.getFechaNaci());
        response.setUrlFoto(usuario.getUrlFoto());
    }
}