package com.obratech.controllers;

import java.util.Comparator;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.obratech.entity.Perfil;
import com.obratech.entity.Postulacion;
import com.obratech.entity.Proyecto;
import com.obratech.entity.Usuario;
import com.obratech.repository.PerfilRepository;
import com.obratech.repository.PostulacionRepository;
import com.obratech.repository.ProyectoRepository;
import com.obratech.repository.UsuarioRepository;
import com.obratech.util.PerfilDisplayName;

@RestController
@RequestMapping("/api/client/applications")
public class ClientApplicationInboxApiController {

    private final UsuarioRepository usuarioRepository;
    private final ProyectoRepository proyectoRepository;
    private final PostulacionRepository postulacionRepository;
    private final PerfilRepository perfilRepository;

    public ClientApplicationInboxApiController(
            UsuarioRepository usuarioRepository,
            ProyectoRepository proyectoRepository,
            PostulacionRepository postulacionRepository,
            PerfilRepository perfilRepository) {
        this.usuarioRepository = usuarioRepository;
        this.proyectoRepository = proyectoRepository;
        this.postulacionRepository = postulacionRepository;
        this.perfilRepository = perfilRepository;
    }

    @GetMapping
    public ResponseEntity<?> listReceivedApplications(Authentication authentication) {
        Usuario owner = usuarioRepository.findByUsernameIgnoreCase(authentication.getName()).orElse(null);
        if (owner == null) {
            return ResponseEntity.notFound().build();
        }

        List<ProjectApplications> projects = proyectoRepository.findByClienteId(owner.getId()).stream()
                .sorted(Comparator.comparing(Proyecto::getFechaCreacion,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .map(project -> new ProjectApplications(
                        project.getId(), project.getTitulo(), project.getUbicacion(), project.getPresupuesto(),
                        postulacionRepository.findByProyectoId(project.getId()).stream()
                                .map(this::toItem).toList()))
                .toList();
        return ResponseEntity.ok(projects);
    }

    private ApplicationItem toItem(Postulacion postulacion) {
        Usuario applicant = postulacion.getUsuario();
        Perfil profile = applicant == null ? null
                : perfilRepository.findByUsernameIgnoreCase(applicant.getUsername()).orElse(null);
        String username = applicant == null ? "Usuario" : applicant.getUsername();
        String name = profile == null || profile.getNombre() == null || profile.getNombre().isBlank()
                ? username : PerfilDisplayName.of(profile);
        String role = profile == null || profile.getRoles() == null ? ""
                : profile.getRoles().stream().filter(value -> !"ROLE_USER".equals(value)).findFirst().orElse("");
        return new ApplicationItem(
                postulacion.getId(), profile == null ? null : profile.getId(), username, name, role,
                applicant != null && applicant.isVerificado(), postulacion.getMensaje(),
                postulacion.getEstado() == null ? "PENDING" : postulacion.getEstado().name(),
                postulacion.getFechaPostulacion());
    }

    public record ProjectApplications(
            String projectId,
            String projectTitle,
            String ubicacion,
            Double presupuesto,
            List<ApplicationItem> applications) {}

    public record ApplicationItem(
            String id,
            String perfilId,
            String username,
            String nombre,
            String rol,
            boolean usuarioVerificado,
            String mensaje,
            String estado,
            java.time.LocalDateTime fechaPostulacion) {}
}