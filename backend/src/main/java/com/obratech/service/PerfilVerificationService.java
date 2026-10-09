package com.obratech.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.obratech.entity.Perfil;

@Service
public class PerfilVerificationService {

    private final ContratistaProfileValidator contratistaProfileValidator;

    public PerfilVerificationService(ContratistaProfileValidator contratistaProfileValidator) {
        this.contratistaProfileValidator = contratistaProfileValidator;
    }

    public List<String> getMissingRequirements(Perfil perfil) {
        List<String> missing = new ArrayList<>();
        if (perfil == null) {
            missing.add("perfil");
            return missing;
        }

        require(missing, perfil.getNombre(), "nombre completo");
        require(missing, perfil.getApellido(), "apellido");
        require(missing, perfil.getTelefono(), "teléfono");

        if (hasRole(perfil, "ROLE_CLIENT")) {
            require(missing, perfil.getEmpresa(), "empresa");
        }
        if (hasRole(perfil, "ROLE_CONTRACTOR")) {
            require(missing, perfil.getEmail(), "correo electrónico");
            require(missing, perfil.getEspecialidad(), "especialidad");
            require(missing, perfil.getUbicacion(), "ubicación");
            require(missing, perfil.getDescripcion(), "descripción profesional");
            requireNonNegative(missing, perfil.getExperiencia(), "años de experiencia");
            missing.addAll(contratistaProfileValidator.validate(perfil));
        }
        if (hasRole(perfil, "ROLE_WORKER")) {
            require(missing, perfil.getOficio(), "oficio");
            requireNonNegative(missing, perfil.getExperiencia(), "años de experiencia");
        }

        return missing;
    }

    public boolean isComplete(Perfil perfil) {
        return getMissingRequirements(perfil).isEmpty();
    }

    private boolean hasRole(Perfil perfil, String role) {
        return perfil.getRoles() != null && perfil.getRoles().contains(role);
    }

    private void require(List<String> missing, String value, String label) {
        if (value == null || value.isBlank()) {
            missing.add(label);
        }
    }

    private void requireNonNegative(List<String> missing, Integer value, String label) {
        if (value == null || value < 0) {
            missing.add(label);
        }
    }
}
