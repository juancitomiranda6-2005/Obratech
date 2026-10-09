package com.obratech.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.obratech.entity.Perfil;

@Service
public class ContratistaProfileValidator {

    private static final long MAX_IMAGE_BYTES = 5L * 1024 * 1024;
    private static final long MAX_DOCUMENT_BYTES = 10L * 1024 * 1024;

    public List<String> validate(Perfil perfil) {
        List<String> errors = new ArrayList<>();
        if (isBlank(perfil.getFotoPerfilUrl()))
            errors.add("Debes subir una foto de perfil JPG o PNG.");
        if (isBlank(perfil.getDescripcion()) || perfil.getDescripcion().trim().length() < 150) {
            errors.add("La presentación profesional debe tener al menos 150 caracteres.");
        }
        if (perfil.getSubespecialidades() == null || perfil.getSubespecialidades().size() < 2) {
            errors.add("Selecciona al menos 2 subespecialidades técnicas.");
        }
        if (isBlank(perfil.getMatriculaProfesional()))
            errors.add("La matrícula o tarjeta profesional es obligatoria.");
        if (isBlank(perfil.getMatriculaDocumentoUrl()))
            errors.add("Debes adjuntar el PDF de la matrícula profesional.");
        if (isBlank(perfil.getTelefono()) || !perfil.getTelefono().matches("^\\+?[0-9][0-9 ()-]{7,19}$")) {
            errors.add("Ingresa un número telefónico válido.");
        }
        if (isBlank(perfil.getCiudad()))
            errors.add("La ciudad de cobertura es obligatoria.");
        if (isBlank(perfil.getDepartamento()))
            errors.add("El departamento o región de cobertura es obligatorio.");
        if (isBlank(perfil.getCvUrl()))
            errors.add("Debes cargar la Hoja de Vida.");
        if (isBlank(perfil.getEstatutosUrl()))
            errors.add("Debes cargar los Estatutos o RUT.");
        return errors;
    }

    public String validateImage(MultipartFile file) {
        if (file == null || file.isEmpty())
            return "La foto de perfil es obligatoria.";
        if (file.getSize() > MAX_IMAGE_BYTES)
            return "La foto de perfil no puede superar 5 MB.";
        if (!hasExtension(file, ".jpg", ".jpeg", ".png"))
            return "La foto debe estar en formato JPG o PNG.";
        String type = file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT);
        if (!type.equals("image/jpeg") && !type.equals("image/png"))
            return "El archivo de foto no es una imagen válida.";
        return null;
    }

    public String validatePdf(MultipartFile file, String label) {
        if (file == null || file.isEmpty())
            return "El PDF de " + label + " es obligatorio.";
        if (file.getSize() > MAX_DOCUMENT_BYTES)
            return "El PDF de " + label + " no puede superar 10 MB.";
        if (!hasExtension(file, ".pdf"))
            return "El documento de " + label + " debe estar en formato PDF.";
        String type = file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT);
        if (!type.equals("application/pdf"))
            return "El archivo de " + label + " no es un PDF válido.";
        return null;
    }

    private boolean hasExtension(MultipartFile file, String... extensions) {
        String name = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase(Locale.ROOT);
        for (String extension : extensions)
            if (name.endsWith(extension))
                return true;
        return false;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
