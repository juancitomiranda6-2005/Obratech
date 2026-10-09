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
import com.obratech.util.PerfilDisplayName;

@RestController
@RequestMapping("/api/client/projects/{projectId}/applications")
public class ClientApplicationsApiController {

    private final ProyectoRepository proyectoRepository;
    private final PostulacionRepository postulacionRepository;
    private final PerfilRepository perfilRepository;
    private final UsuarioRepository usuarioRepository;

    public ClientApplicationsApiController(
            ProyectoRepository proyectoRepository,
            PostulacionRepository postulacionRepository,
            PerfilRepository perfilRepository,
            UsuarioRepository usuarioRepository) {
        this.proyectoRepository = proyectoRepository;
        this.postulacionRepository = postulacionRepository;
        this.perfilRepository = perfilRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @GetMapping
    public ResponseEntity<?> list(
            @PathVariable String projectId,
            Authentication authentication) {
        Proyecto proyecto = ownedProject(projectId, authentication.getName());
        if (proyecto == null) {
            return ResponseEntity.notFound().build();
        }
        List<ApplicationItem> applications = postulacionRepository.findByProyectoId(projectId)
                .stream().map(this::toItem).toList();
        return ResponseEntity.ok(new ApplicationsResponse(
                proyecto.getId(), proyecto.getTitulo(), proyecto.isPostulacionesAbiertas(),
                proyecto.getContratistaAsignado() != null, applications));
    }

    @PatchMapping("/{applicationId}")
    public ResponseEntity<?> update(
            @PathVariable String projectId,
            @PathVariable String applicationId,
            @RequestBody ApplicationAction request,
            Authentication authentication) {
        if (request == null || (!"ACCEPTED".equals(request.estado()) && !"REJECTED".equals(request.estado()))) {
            return ResponseEntity.badRequest().body(new ApiError("Acción de postulación no válida."));
        }

        Proyecto proyecto = ownedProject(projectId, authentication.getName());
        if (proyecto == null) {
            return ResponseEntity.notFound().build();
        }
        Postulacion postulacion = postulacionRepository.findById(applicationId).orElse(null);
        if (postulacion == null || postulacion.getProyecto() == null
                || !projectId.equals(postulacion.getProyecto().getId())) {
            return ResponseEntity.notFound().build();
        }
        if (!EstadoPostulacion.PENDING.equals(postulacion.getEstado())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new ApiError("Esta postulación ya fue respondida."));
        }

        EstadoPostulacion nextState = "ACCEPTED".equals(request.estado())
                ? EstadoPostulacion.ACCEPTED : EstadoPostulacion.REJECTED;
        if (nextState == EstadoPostulacion.ACCEPTED && proyecto.getContratistaAsignado() != null) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new ApiError("El proyecto ya tiene un contratista seleccionado."));
        }

        if (nextState == EstadoPostulacion.ACCEPTED) {
            postulacionRepository.findByProyectoId(projectId).stream()
                    .filter(candidate -> !candidate.getId().equals(applicationId))
                    .forEach(candidate -> {
                        candidate.setEstado(EstadoPostulacion.REJECTED);
                        postulacionRepository.save(candidate);
                    });
        }
        postulacion.setEstado(nextState);
        postulacionRepository.save(postulacion);

        String applicantUsername = postulacion.getUsuario() == null ? null : postulacion.getUsuario().getUsername();
        if (nextState == EstadoPostulacion.ACCEPTED && applicantUsername != null) {
            Perfil perfil = perfilRepository.findByUsernameIgnoreCase(applicantUsername).orElse(null);
            if (perfil != null && perfil.getRoles() != null && perfil.getRoles().contains("ROLE_CONTRACTOR")) {
                proyecto.setContratistaAsignado(perfil);
                proyecto.setEstadoAsignacion(EstadoAsignacion.SELECCIONADO_PENDIENTE_CONTRATACION);
                proyecto.setPostulacionesAbiertas(false);
                proyectoRepository.save(proyecto);
            }
        }

        return ResponseEntity.ok(new ActionResponse(applicationId, nextState.name()));
    }

    @org.springframework.web.bind.annotation.DeleteMapping("/{applicationId}")
    public ResponseEntity<?> delete(
            @PathVariable String projectId,
            @PathVariable String applicationId,
            Authentication authentication) {
        Proyecto proyecto = ownedProject(projectId, authentication.getName());
        if (proyecto == null) {
            return ResponseEntity.notFound().build();
        }
        Postulacion postulacion = postulacionRepository.findById(applicationId).orElse(null);
        if (postulacion == null || postulacion.getProyecto() == null
                || !projectId.equals(postulacion.getProyecto().getId())) {
            return ResponseEntity.notFound().build();
        }

        Perfil contratistaAsignado = proyecto.getContratistaAsignado();
        String applicantUsername = postulacion.getUsuario() == null ? null : postulacion.getUsuario().getUsername();
        if (contratistaAsignado != null && applicantUsername != null
                && applicantUsername.equalsIgnoreCase(contratistaAsignado.getUsername())) {
            proyecto.setContratistaAsignado(null);
            proyecto.setEstadoAsignacion(EstadoAsignacion.SIN_ASIGNAR);
            proyecto.setPostulacionesAbiertas(true);
            proyectoRepository.save(proyecto);
            postulacionRepository.findByProyectoId(projectId).forEach(postulacionRepository::delete);
        } else {
            postulacionRepository.delete(postulacion);
        }

        return ResponseEntity.ok(new ActionResponse(applicationId, "DELETED"));
    }

    private Proyecto ownedProject(String projectId, String username) {
        Usuario owner = usuarioRepository.findByUsernameIgnoreCase(username).orElse(null);
        Proyecto proyecto = proyectoRepository.findById(projectId).orElse(null);
        boolean administrator = owner != null && owner.getRoles() != null && owner.getRoles().contains("ROLE_ADMIN");
        if (administrator && proyecto != null) {
            return proyecto;
        }
        if (owner == null || proyecto == null || proyecto.getCliente() == null
                || proyecto.getCliente().getUsername() == null
                || !proyecto.getCliente().getUsername().equalsIgnoreCase(owner.getUsername())) {
            return null;
        }
        return proyecto;
    }

    private ApplicationItem toItem(Postulacion postulacion) {
        Usuario usuario = postulacion.getUsuario();
        Perfil perfil = usuario == null ? null : perfilRepository.findByUsernameIgnoreCase(usuario.getUsername()).orElse(null);
        String username = usuario == null ? "Usuario" : usuario.getUsername();
        String nombre = perfil == null || perfil.getNombre() == null || perfil.getNombre().isBlank()
            ? username : PerfilDisplayName.of(perfil);
        String role = perfil == null || perfil.getRoles() == null ? ""
                : perfil.getRoles().stream().filter(value -> !"ROLE_USER".equals(value)).findFirst().orElse("");
        return new ApplicationItem(
            postulacion.getId(), perfil == null ? null : perfil.getId(),
            usuario == null ? "" : usuario.getUsername(), nombre, role,
                usuario != null && usuario.isVerificado(), postulacion.getMensaje(),
                postulacion.getEstado() == null ? "PENDING" : postulacion.getEstado().name(),
                postulacion.getFechaPostulacion());
    }

    public record ApplicationsResponse(
            String projectId,
            String projectTitle,
            boolean applicationsOpen,
            boolean contractorAssigned,
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

    public record ApplicationAction(String estado) {}

    public record ActionResponse(String id, String estado) {}

    public record ApiError(String error) {}
}