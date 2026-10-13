package com.obratech.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.obratech.entity.Perfil;
import com.obratech.entity.Usuario;

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

    @Test
    void completeProfileAutomaticallyVerifiesProfileAndUser() {
        Perfil perfil = completeClientProfile();
        Usuario usuario = new Usuario();
        usuario.setActivo(true);

        boolean verified = service.synchronizeVerification(perfil, usuario);

        assertTrue(verified);
        assertTrue(perfil.getVerificado());
        assertTrue(usuario.isVerificado());
        assertTrue(usuario.isActivo());
    }

    @Test
    void incompleteProfileStaysActiveButUnverified() {
        Perfil perfil = new Perfil();
        perfil.setRoles(Set.of("ROLE_CLIENT"));
        Usuario usuario = new Usuario();
        usuario.setActivo(true);
        usuario.setVerificado(true);

        boolean verified = service.synchronizeVerification(perfil, usuario);

        assertFalse(verified);
        assertFalse(perfil.getVerificado());
        assertFalse(usuario.isVerificado());
        assertTrue(usuario.isActivo());
    }

    @Test
    void contractorMustUploadResumeBeforeAutomaticVerification() {
        Perfil perfil = new Perfil();
        perfil.setRoles(Set.of("ROLE_CONTRACTOR"));
        perfil.setNombre("Luis");
        perfil.setApellido("Pérez");
        perfil.setEmail("luis@demo.com");
        perfil.setTelefono("3001234567");
        perfil.setEspecialidad("Electricidad");
        perfil.setUbicacion("Bogotá, Cundinamarca");
        perfil.setCiudad("Bogotá");
        perfil.setDepartamento("Cundinamarca");
        perfil.setDescripcion("Experiencia profesional en instalaciones eléctricas y mantenimiento de redes.".repeat(3));
        perfil.setExperiencia(5);
        perfil.setMatriculaProfesional("TP-123");
        perfil.setMatriculaDocumentoUrl("license-file");
        perfil.setEstatutosUrl("rut-file");
        perfil.setSubespecialidades(Set.of("RETIE", "Cableado"));

        assertTrue(service.getMissingRequirements(perfil).contains("Debes cargar tu hoja de vida."));
        assertFalse(service.isComplete(perfil));

        perfil.setCvUrl("resume-file");
        assertTrue(service.isComplete(perfil));
    }

    private Perfil completeClientProfile() {
        Perfil perfil = new Perfil();
        perfil.setRoles(Set.of("ROLE_CLIENT"));
        perfil.setNombre("Ana");
        perfil.setApellido("Gómez");
        perfil.setTelefono("3001234567");
        perfil.setEmpresa("Constructora Demo");
        return perfil;
    }
}
