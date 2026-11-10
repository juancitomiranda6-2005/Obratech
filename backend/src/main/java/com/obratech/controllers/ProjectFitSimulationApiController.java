package com.obratech.controllers;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.obratech.entity.Perfil;
import com.obratech.entity.Proyecto;
import com.obratech.entity.Usuario;
import com.obratech.repository.PerfilRepository;
import com.obratech.repository.ProyectoRepository;
import com.obratech.repository.UsuarioRepository;
import com.obratech.service.ProjectFitSimulationService;

@RestController
@RequestMapping("/api/projects")
public class ProjectFitSimulationApiController {

    private final ProyectoRepository proyectoRepository;
    private final UsuarioRepository usuarioRepository;
    private final PerfilRepository perfilRepository;
    private final ProjectFitSimulationService simulationService;

    public ProjectFitSimulationApiController(
            ProyectoRepository proyectoRepository,
            UsuarioRepository usuarioRepository,
            PerfilRepository perfilRepository,
            ProjectFitSimulationService simulationService) {
        this.proyectoRepository = proyectoRepository;
        this.usuarioRepository = usuarioRepository;
        this.perfilRepository = perfilRepository;
        this.simulationService = simulationService;
    }

    @GetMapping("/{projectId}/simulate-fit")
    public ResponseEntity<?> simulate(
            @PathVariable String projectId,
            @RequestParam(required = false) String contratistaId,
            Authentication authentication) {
        Usuario user = usuarioRepository.findByUsernameIgnoreCase(authentication.getName()).orElse(null);
        Perfil contractor = user == null ? null
                : perfilRepository.findByUsernameIgnoreCase(user.getUsername()).orElse(null);
        if (user == null || contractor == null || contractor.getRoles() == null
                || !contractor.getRoles().contains("ROLE_CONTRACTOR")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Solo un contratista puede consultar esta simulación."));
        }
        if (contratistaId != null && !contratistaId.equals(contractor.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "El contratista solicitado no coincide con la sesión actual."));
        }
        Proyecto project = proyectoRepository.findById(projectId).orElse(null);
        if (project == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(simulationService.simulate(project, contractor));
    }
}
