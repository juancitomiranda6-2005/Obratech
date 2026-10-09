package com.obratech.controllers;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.obratech.entity.Calificacion;
import com.obratech.entity.Perfil;
import com.obratech.entity.Proyecto;
import com.obratech.entity.enums.EstadoEjecucion;
import com.obratech.repository.CalificacionRepository;
import com.obratech.repository.PerfilRepository;
import com.obratech.repository.ProyectoRepository;
import com.obratech.util.PerfilDisplayName;

@RestController
@RequestMapping("/api/public/profiles")
public class PublicProfileApiController {

    private final PerfilRepository perfilRepository;
    private final ProyectoRepository proyectoRepository;
    private final CalificacionRepository calificacionRepository;

    public PublicProfileApiController(
            PerfilRepository perfilRepository,
            ProyectoRepository proyectoRepository,
            CalificacionRepository calificacionRepository) {
        this.perfilRepository = perfilRepository;
        this.proyectoRepository = proyectoRepository;
        this.calificacionRepository = calificacionRepository;
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getPublicProfile(@PathVariable String id) {
        Perfil perfil = perfilRepository.findById(id)
            .orElseGet(() -> perfilRepository.findByUsernameIgnoreCase(id).orElse(null));
        if (perfil == null) return ResponseEntity.notFound().build();

        List<Proyecto> projects;
        if (hasRole(perfil, "ROLE_CONTRACTOR")) {
            projects = proyectoRepository.findByContratistaAsignadoId(perfil.getId());
        } else if (hasRole(perfil, "ROLE_WORKER")) {
            projects = proyectoRepository.findByEquipoTrabajoContaining(perfil);
        } else {
            projects = List.of();
        }
        List<ReviewCard> reviews = hasRole(perfil, "ROLE_CONTRACTOR")
                ? calificacionRepository.findByContratistaId(perfil.getId()).stream().map(this::toReviewCard).toList()
                : List.of();

        return ResponseEntity.ok(new PublicProfileResponse(
                perfil.getId(),
            PerfilDisplayName.of(perfil),
                perfil.getUsername(),
                perfil.getEmail(),
                perfil.getTelefono(),
                perfil.getRoles() == null ? List.of() : perfil.getRoles().stream().sorted().toList(),
                Boolean.TRUE.equals(perfil.getVerificado()),
                Boolean.TRUE.equals(perfil.getActivo()),
                perfil.getEspecialidad(),
                perfil.getOficio(),
                perfil.getExperiencia(),
                perfil.getUbicacion(),
                perfil.getDescripcion(),
                perfil.getCalificacionPromedio() == null ? 0.0 : perfil.getCalificacionPromedio(),
                perfil.getDisponibilidad(),
                perfil.getCvUrl() != null && !perfil.getCvUrl().isBlank(),
                projects.stream().map(this::toProjectCard).toList(),
                reviews));
    }

    private boolean hasRole(Perfil perfil, String role) {
        return perfil.getRoles() != null && perfil.getRoles().contains(role);
    }

    private ProjectCard toProjectCard(Proyecto proyecto) {
        return new ProjectCard(
                proyecto.getId(),
                proyecto.getTitulo(),
                proyecto.getUbicacion(),
                proyecto.getEstadoEjecucion() == null ? "PENDIENTE" : proyecto.getEstadoEjecucion().name());
    }

    private ReviewCard toReviewCard(Calificacion review) {
        return new ReviewCard(
                review.getId(),
                review.getPuntuacion(),
                review.getComentario(),
                review.getFecha(),
                review.getProyecto() == null ? "Proyecto" : review.getProyecto().getTitulo());
    }

    public record PublicProfileResponse(
            String id,
            String nombre,
            String username,
            String email,
            String telefono,
            List<String> roles,
            boolean verificado,
            boolean activo,
            String especialidad,
            String oficio,
            Integer experiencia,
            String ubicacion,
            String descripcion,
            double calificacionPromedio,
            Boolean disponible,
            boolean cvDisponible,
            List<ProjectCard> proyectos,
            List<ReviewCard> calificaciones) {}

    public record ProjectCard(String id, String titulo, String ubicacion, String estado) {}

    public record ReviewCard(String id, int puntuacion, String comentario, LocalDateTime fecha, String proyecto) {}
}
