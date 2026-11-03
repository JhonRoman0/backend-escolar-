package com.example.Escolar.Service;

import com.example.Escolar.Repository.AlumnoRepository;
import com.example.Escolar.Repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Objects;

/** Las escrituras llaman estas consultas dentro de su transacción SERIALIZABLE. */
@Service
@RequiredArgsConstructor
public class ExclusividadDniService {
    private final AlumnoRepository alumnoRepository;
    private final UsuarioRepository usuarioRepository;

    public void validarUsuario(String dni) {
        validarFormato(dni);
        if (alumnoRepository.findByDocumentoIdentidad(dni).isPresent()) {
            throw new IllegalArgumentException("Este DNI ya está registrado como alumno y no puede registrarse como usuario");
        }
    }

    public void validarAlumno(String dni) {
        validarFormato(dni);
        if (usuarioRepository.existsByDocumentoIdentidad(dni)) {
            throw new IllegalArgumentException("Este DNI ya está registrado como usuario y no puede registrarse como alumno");
        }
    }

    /** Solo existencia: conserva la reutilización histórica sin cargar fichas personales. */
    public void validarAlumnoInscripcion(String dni) {
        validarFormato(dni);
        if (!alumnoRepository.existsByDocumentoIdentidadAndAccesoIdAccesoNot(dni, AccesoConstants.ELIMINADO)) {
            validarAlumno(dni);
        }
    }

    public void validarCambioUsuario(String dni, String original, boolean reactivando) {
        if (reactivando || !Objects.equals(dni, original)) validarUsuario(dni);
    }

    private void validarFormato(String dni) {
        if (dni == null || !dni.matches("^[0-9]{8}$")) {
            throw new IllegalArgumentException("El DNI debe contener exactamente 8 dígitos");
        }
    }
}
