package com.example.Escolar.Service;

import com.cloudinary.Cloudinary;
import com.example.Escolar.Dto.UploadResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CloudinaryService {

    private final Cloudinary cloudinary;

    public UploadResponse upload(MultipartFile file, String carpeta) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("El archivo no puede estar vacío");
        }
        try {
            Map<?, ?> result = cloudinary.uploader().upload(
                    file.getBytes(),
                    Map.of("folder", carpeta)
            );
            UploadResponse response = new UploadResponse();
            response.setUrl((String) result.get("secure_url"));
            response.setPublicId((String) result.get("public_id"));
            return response;
        } catch (IOException e) {
            throw new IllegalArgumentException("Error al subir la imagen a Cloudinary: " + e.getMessage());
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("No se pudo subir la imagen: verifique las credenciales de Cloudinary o intente de nuevo");
        }
    }

    public void delete(String publicId) {
        try {
            cloudinary.uploader().destroy(publicId, Map.of("invalidate", true));
        } catch (IOException e) {
            throw new IllegalArgumentException("Error al eliminar la imagen de Cloudinary: " + e.getMessage());
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("No se pudo eliminar la imagen: verifique las credenciales de Cloudinary");
        }
    }
}
