package com.obratech.controllers;

import java.util.Comparator;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.obratech.entity.Perfil;
import com.obratech.repository.PerfilRepository;
import com.obratech.util.PerfilDisplayName;

@RestController
@RequestMapping("/api/directory")
public class ProfileDirectoryApiController {

    private final PerfilRepository perfilRepository;

    public ProfileDirectoryApiController(PerfilRepository perfilRepository) {
        this.perfilRepository = perfilRepository;
    }

    @GetMapping("/contractors")
    public ResponseEntity<List<DirectoryProfile>> contractors(
            @RequestParam(required = false) String especialidad) {
        List<Perfil> profiles = especialidad == null || especialidad.isBlank()
                ? perfilRepository.findByRolesAndActivoTrue("ROLE_CONTRACTOR")
                : perfilRepository.findByEspecialidadContainingIgnoreCaseAndRoles(especialidad, "ROLE_CONTRACTOR")
                        .stream().filter(profile -> !Boolean.FALSE.equals(profile.getActivo())).toList();
        return ResponseEntity.ok(toDirectory(profiles));
    }

    @GetMapping("/clients")
    public ResponseEntity<List<DirectoryProfile>> clients() {
        return ResponseEntity.ok(toDirectory(perfilRepository.findByRolesAndActivoTrue("ROLE_CLIENT")));
    }

    @GetMapping("/workers")
    public ResponseEntity<List<DirectoryProfile>> workers(
            @RequestParam(defaultValue = "false") boolean available) {
        List<Perfil> profiles = available
                ? perfilRepository.findByDisponibilidadTrueAndRoles("ROLE_WORKER")
                : perfilRepository.findByRolesAndActivoTrue("ROLE_WORKER");
        return ResponseEntity.ok(toDirectory(profiles.stream()
                .filter(profile -> !Boolean.FALSE.equals(profile.getActivo())).toList()));
    }

    private List<DirectoryProfile> toDirectory(List<Perfil> profiles) {
        return profiles.stream()
                .map(profile -> new DirectoryProfile(
                        profile.getId(),
                        PerfilDisplayName.of(profile),
                        profile.getUsername(),
                        profile.getEmail(),
                        profile.getTelefono(),
                        profile.getEmpresa(),
                        profile.getEspecialidad(),
                        profile.getOficio(),
                        profile.getExperiencia(),
                        Boolean.TRUE.equals(profile.getDisponibilidad()),
                        profile.getCalificacionPromedio() == null ? 0.0 : profile.getCalificacionPromedio(),
                        Boolean.TRUE.equals(profile.getVerificado())))
                .sorted(Comparator.comparing(DirectoryProfile::nombre, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    public record DirectoryProfile(
            String id,
            String nombre,
            String username,
            String email,
            String telefono,
            String empresa,
            String especialidad,
            String oficio,
            Integer experiencia,
            boolean disponible,
            double calificacionPromedio,
            boolean verificado) {}
}