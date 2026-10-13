package com.obratech.controllers;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.obratech.entity.Perfil;
import com.obratech.entity.Postulacion;
import com.obratech.entity.Proyecto;
import com.obratech.entity.Usuario;
import com.obratech.entity.enums.EstadoAsignacion;
import com.obratech.entity.enums.EstadoPostulacion;
import com.obratech.repository.PerfilRepository;
import com.obratech.repository.PostulacionRepository;
import com.obratech.repository.ProyectoRepository;
import com.obratech.repository.UsuarioRepository;

@RestController
@RequestMapping("/api/contractor/applications")
public class ContractorProjectOfferApiController {

    private final PerfilRepository perfilRepository;
    private final UsuarioRepository usuarioRepository;
    private final ProyectoRepository proyectoRepository;
    private final PostulacionRepository postulacionRepository;

    public ContractorProjectOfferApiController(
            PerfilRepository perfilRepository,
            UsuarioRepository usuarioRepository,
            ProyectoRepository proyectoRepository,
            PostulacionRepository postulacionRepository) {
        this.perfilRepository = perfilRepository;
        this.usuarioRepository = usuarioRepository;
        this.proyectoRepository = proyectoRepository;
        this.postulacionRepository = postulacionRepository;
    }

    @GetMapping("/offers")
    public ResponseEntity<?> listOffers(Authentication authentication) {
        Perfil contractor = contractorProfile(authentication);
        Usuario user = authentication == null ? null
                : usuarioRepository.findByUsernameIgnoreCase(authentication.getName()).orElse(null);
        if (contractor == null || user == null) return ResponseEntity.notFound().build();

        List<OfferItem> offers = postulacionRepository.findByUsuarioId(user.getId()).stream()
                .filter(application -> EstadoPostulacion.OFFERED.equals(application.getEstado()))
                .map(application -> toOffer(application, contractor))
                .filter(java.util.Objects::nonNull)
                .toList();
        return ResponseEntity.ok(offers);
    }

    @PatchMapping("/{applicationId}/decision")
    public ResponseEntity<?> decideOffer(
            @PathVariable String applicationId,
            @RequestBody OfferDecision request,
            Authentication authentication) {
        if (request == null || (!"ACCEPTED".equals(request.estado()) && !"REJECTED".equals(request.estado()))) {
            return ResponseEntity.badRequest().body(new ApiError("Decisión de oferta no válida."));
        }
        Perfil contractor = contractorProfile(authentication);
        Usuario user = authentication == null ? null
                : usuarioRepository.findByUsernameIgnoreCase(authentication.getName()).orElse(null);
        if (contractor == null || user == null) return ResponseEntity.notFound().build();

        Postulacion application = postulacionRepository.findById(applicationId).orElse(null);
        Proyecto project = application == null ? null : application.getProyecto();
        if (application == null || application.getUsuario() == null
                || !user.getId().equals(application.getUsuario().getId())
                || !EstadoPostulacion.OFFERED.equals(application.getEstado())
                || project == null || project.getContratistaAsignado() == null
                || !contractor.getId().equals(project.getContratistaAsignado().getId())
                || !EstadoAsignacion.SELECCIONADO_PENDIENTE_CONTRATACION.equals(project.getEstadoAsignacion())) {
            return ResponseEntity.notFound().build();
        }

        if ("ACCEPTED".equals(request.estado())) {
            application.setEstado(EstadoPostulacion.ACCEPTED);
            postulacionRepository.save(application);
            postulacionRepository.findByProyectoId(project.getId()).stream()
                    .filter(candidate -> !candidate.getId().equals(applicationId)
                            && EstadoPostulacion.PENDING.equals(candidate.getEstado()))
                    .forEach(candidate -> {
                        candidate.setEstado(EstadoPostulacion.REJECTED);
                        postulacionRepository.save(candidate);
                    });
            project.setEstadoAsignacion(EstadoAsignacion.ASIGNADO);
        } else {
            application.setEstado(EstadoPostulacion.REJECTED);
            postulacionRepository.save(application);
            project.setContratistaAsignado(null);
            project.setEstadoAsignacion(EstadoAsignacion.SIN_ASIGNAR);
            project.setPostulacionesAbiertas(true);
        }
        proyectoRepository.save(project);
        return ResponseEntity.ok(new DecisionResponse(applicationId, request.estado()));
    }

    private Perfil contractorProfile(Authentication authentication) {
        if (authentication == null || authentication.getAuthorities().stream()
                .noneMatch(authority -> "ROLE_CONTRACTOR".equals(authority.getAuthority()))) {
            return null;
        }
        return perfilRepository.findByUsernameIgnoreCase(authentication.getName())
                .filter(profile -> profile.getRoles() != null && profile.getRoles().contains("ROLE_CONTRACTOR"))
                .orElse(null);
    }

    private OfferItem toOffer(Postulacion application, Perfil contractor) {
        Proyecto project = application.getProyecto();
        if (project == null || project.getContratistaAsignado() == null
                || !contractor.getId().equals(project.getContratistaAsignado().getId())
                || !EstadoAsignacion.SELECCIONADO_PENDIENTE_CONTRATACION.equals(project.getEstadoAsignacion())) {
            return null;
        }
        return new OfferItem(application.getId(), project.getId(), project.getTitulo(),
                project.getUbicacion(), project.getPresupuesto(), application.getFechaPostulacion());
    }

    public record OfferItem(
            String applicationId,
            String projectId,
            String projectTitle,
            String location,
            Double budget,
            java.time.LocalDateTime appliedAt) {}

    public record OfferDecision(String estado) {}

    public record DecisionResponse(String applicationId, String estado) {}

    public record ApiError(String error) {}
}
