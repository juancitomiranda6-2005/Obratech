package com.obratech.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.obratech.entity.Perfil;

class PerfilVerificationServiceTest {

    private final PerfilVerificationService service = new PerfilVerificationService(new ContratistaProfileValidator());

    @Test
    void clientProfileRequiresContactDetailsAndCompany() {
        Perfil perfil = new Perfil();
        perfil.setUsername("cliente@demo.com");
        perfil.setRoles(Set.of("ROLE_CLIENT"));

        List<String> missing = service.getMissingRequirements(perfil);

        assertTrue(missing.containsAll(List.of("nombre completo", "apellido", "teléfono", "empresa")));
        assertFalse(service.isComplete(perfil));

        perfil.setNombre("Ana");
        perfil.setApellido("Gómez");
        perfil.setTelefono("3001234567");
        perfil.setEmpresa("Constructora Demo");
        assertTrue(service.isComplete(perfil));
    }

    @Test
    void workerProfileRequiresOccupationAndNonNegativeExperience() {
        Perfil perfil = new Perfil();
        perfil.setUsername("trabajador@demo.com");
        perfil.setRoles(Set.of("ROLE_WORKER"));
        perfil.setNombre("Luis");
        perfil.setApellido("Pérez");
        perfil.setTelefono("3001234567");

        List<String> missing = service.getMissingRequirements(perfil);

        assertTrue(missing.containsAll(List.of("oficio", "años de experiencia")));
        perfil.setOficio("Electricista");
        perfil.setExperiencia(5);
        assertTrue(service.isComplete(perfil));
    }
}
