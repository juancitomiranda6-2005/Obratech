package com.obratech.controllers;

import java.net.URI;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.obratech.entity.Postulacion;
import com.obratech.entity.Proyecto;
import com.obratech.entity.Usuario;
import com.obratech.entity.enums.EstadoValidacion;
import com.obratech.repository.PerfilRepository;
import com.obratech.repository.PostulacionRepository;
import com.obratech.repository.ProyectoRepository;
import com.obratech.repository.UsuarioRepository;
import com.obratech.service.ContratistaProfileValidator;

@RestController
@RequestMapping("/api/participant/projects")
public class ParticipantProjectsApiController {

    private final ProyectoRepository proyectoRepository;
    private final PostulacionRepository postulacionRepository;
    private final UsuarioRepository usuarioRepository;
    private final PerfilRepository perfilRepository;
    private final ContratistaProfileValidator contratistaProfileValidator;

    public ParticipantProjectsApiController(
            ProyectoRepository proyectoRepository,
            PostulacionRepository postulacionRepository,
            UsuarioRepository usuarioRepository,
            PerfilRepository perfilRepository,
            ContratistaProfileValidator contratistaProfileValidator) {
        this.proyectoRepository = proyectoRepository;
        this.postulacionRepository = postulacionRepository;
        this.usuarioRepository = usuarioRepository;
        this.perfilRepository = perfilRepository;
        this.contratistaProfileValidator = contratistaProfileValidator;
    }

    @GetMapping
        public ResponseEntity<?> list(Authentication authentication) {
        Usuario usuario = usuarioRepository.findByUsernameIgnoreCase(authentication.getName()).orElse(null);
        if (usuario == null) return ResponseEntity.notFound().build();

        boolean isContractor = hasAuthority(authentication, "ROLE_CONTRACTOR");
        var perfil = perfilRepository.findByUsernameIgnoreCase(usuario.getUsername()).orElse(null);
        List<String> requisitosFaltantes = isContractor && perfil != null
            ? contratistaProfileValidator.validate(perfil)
            : List.of();
        boolean perfilCompleto = !isContractor || (perfil != null && requisitosFaltantes.isEmpty());

        List<ProjectItem> projects = proyectoRepository.findByFechaLimitePostulacionIsNullOrFechaLimitePostulacionGreaterThanEqual(LocalDate.now())
                .stream()
                .filter(proyecto -> EstadoValidacion.APROBADO.equals(proyecto.getEstadoValidacion()))
                .filter(proyecto -> proyecto.getContratistaAsignado() == null && proyecto.isPostulacionesAbiertas())
                .map(proyecto -> toProjectItem(proyecto, usuario))
                .toList();
        return ResponseEntity.ok(new ProjectMarketResponse(
            usuario.isVerificado(), perfilCompleto, requisitosFaltantes, projects));
    }

    @GetMapping("/applications")
    public ResponseEntity<?> getMyApplications(Authentication authentication) {
        Usuario usuario = usuarioRepository.findByUsernameIgnoreCase(authentication.getName()).orElse(null);
        if (usuario == null) return ResponseEntity.notFound().build();
        List<MyApplicationItem> applications = postulacionRepository.findByUsuarioId(usuario.getId())
                .stream()
                .map(postulacion -> {
                    Proyecto proyecto = postulacion.getProyecto();
                    return new MyApplicationItem(
                            postulacion.getId(),
                            proyecto == null ? null : proyecto.getId(),
                            proyecto == null ? "Proyecto no disponible" : proyecto.getTitulo(),
                            proyecto == null ? null : proyecto.getUbicacion(),
                            proyecto == null ? null : proyecto.getPresupuesto(),
                            postulacion.getEstado() == null ? "PENDING" : postulacion.getEstado().name(),
                            postulacion.getMensaje(),
                            postulacion.getFechaPostulacion());
                })
                .toList();
        return ResponseEntity.ok(applications);
    }

    @PostMapping("/{projectId}/applications")
    public ResponseEntity<?> apply(
            @PathVariable String projectId,
            @RequestBody(required = false) ApplicationRequest request,
            Authentication authentication) {
        Usuario usuario = usuarioRepository.findByUsernameIgnoreCase(authentication.getName()).orElse(null);
        if (usuario == null) return ResponseEntity.notFound().build();

        Proyecto proyecto = proyectoRepository.findById(projectId).orElse(null);
        if (proyecto == null) return ResponseEntity.notFound().build();
        if (!EstadoValidacion.APROBADO.equals(proyecto.getEstadoValidacion())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new ApiError("Este proyecto todavía no ha sido verificado por el administrador."));
        }
        if (!proyecto.isPostulacionesAbiertas() || proyecto.getContratistaAsignado() != null
                || (proyecto.getFechaLimitePostulacion() != null
                    && proyecto.getFechaLimitePostulacion().isBefore(LocalDate.now()))) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new ApiError("Las postulaciones para este proyecto están cerradas."));
        }
        if (!usuario.isVerificado()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ApiError("Debes tener la cuenta verificada para postularte."));
        }

        boolean isContractor = hasAuthority(authentication, "ROLE_CONTRACTOR");
        if (isContractor) {
            var perfil = perfilRepository.findByUsernameIgnoreCase(usuario.getUsername()).orElse(null);
            if (perfil == null || !contratistaProfileValidator.validate(perfil).isEmpty()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(new ApiError("Completa tu perfil profesional antes de postularte."));
            }
        }

        if (postulacionRepository.existsByProyectoIdAndUsuarioId(projectId, usuario.getId())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new ApiError("Ya te postulaste a este proyecto."));
        }

        Postulacion postulacion = new Postulacion();
        postulacion.setProyecto(proyecto);
        postulacion.setUsuario(usuario);
        postulacion.setFechaPostulacion(LocalDateTime.now());
        if (request != null && request.mensaje() != null && !request.mensaje().isBlank()) {
            postulacion.setMensaje(request.mensaje().trim());
        }
        Postulacion saved = postulacionRepository.save(postulacion);
        return ResponseEntity.created(URI.create("/api/participant/projects/" + projectId + "/applications/" + saved.getId()))
                .body(new ApplicationResult(saved.getId(), saved.getEstado().name()));
    }

    private boolean hasAuthority(Authentication authentication, String authority) {
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(authority::equals);
    }

    private ProjectItem toProjectItem(Proyecto proyecto, Usuario usuario) {
        return new ProjectItem(
                proyecto.getId(), proyecto.getTitulo(), proyecto.getDescripcion(), proyecto.getTipoProyecto(),
                proyecto.getUbicacion(), proyecto.getPresupuesto(), proyecto.getFechaInicio(),
                proyecto.getFechaEntrega(), proyecto.getFechaLimitePostulacion(),
                postulacionRepository.existsByProyectoIdAndUsuarioId(proyecto.getId(), usuario.getId()));
    }

    public record ProjectItem(
            String id,
            String titulo,
            String descripcion,
            String tipoProyecto,
            String ubicacion,
            Double presupuesto,
            LocalDate fechaInicio,
            LocalDate fechaEntrega,
            LocalDate fechaLimitePostulacion,
            boolean yaPostulado) {}

    public record ProjectMarketResponse(
            boolean usuarioVerificado,
            boolean perfilCompleto,
            List<String> requisitosFaltantes,
            List<ProjectItem> proyectos) {}

    public record ApplicationRequest(String mensaje) {}

    public record ApplicationResult(String id, String estado) {}

        public record MyApplicationItem(
            String id,
            String projectId,
            String projectTitle,
            String ubicacion,
            Double presupuesto,
            String estado,
            String mensaje,
            LocalDateTime fechaPostulacion) {}

    public record ApiError(String error) {}
}