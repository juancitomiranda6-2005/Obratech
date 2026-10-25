package com.obratech.controllers;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.obratech.entity.InvitacionTrabajo;
import com.obratech.entity.Perfil;
import com.obratech.entity.Proyecto;
import com.obratech.entity.Usuario;
import com.obratech.entity.enums.EstadoEjecucion;
import com.obratech.entity.enums.EstadoAsignacion;
import com.obratech.repository.InvitacionTrabajoRepository;
import com.obratech.repository.EquipoTrabajoRepository;
import com.obratech.repository.PerfilRepository;
import com.obratech.repository.ProyectoRepository;
import com.obratech.repository.UsuarioRepository;
import com.obratech.service.PerfilVerificationService;
import com.obratech.service.InvitacionTrabajoService;
import com.obratech.service.ProjectRiskService;
import com.obratech.util.PerfilDisplayName;

@RestController
@RequestMapping("/api/dashboard")
public class ProfessionalDashboardApiController {

    private final UsuarioRepository usuarioRepository;
    private final PerfilRepository perfilRepository;
    private final ProyectoRepository proyectoRepository;
    private final InvitacionTrabajoRepository invitacionTrabajoRepository;
    private final EquipoTrabajoRepository equipoTrabajoRepository;
    private final PerfilVerificationService perfilVerificationService;
    private final InvitacionTrabajoService invitacionService;
    private final ProjectRiskService projectRiskService;

    public ProfessionalDashboardApiController(
            UsuarioRepository usuarioRepository,
            PerfilRepository perfilRepository,
            ProyectoRepository proyectoRepository,
            EquipoTrabajoRepository equipoTrabajoRepository,
            InvitacionTrabajoRepository invitacionTrabajoRepository,
            PerfilVerificationService perfilVerificationService,
            InvitacionTrabajoService invitacionService,
            ProjectRiskService projectRiskService) {
        this.usuarioRepository = usuarioRepository;
        this.perfilRepository = perfilRepository;
        this.proyectoRepository = proyectoRepository;
        this.equipoTrabajoRepository = equipoTrabajoRepository;
        this.invitacionTrabajoRepository = invitacionTrabajoRepository;
        this.perfilVerificationService = perfilVerificationService;
        this.invitacionService = invitacionService;
        this.projectRiskService = projectRiskService;
    }

    @GetMapping("/contratista")
    public ResponseEntity<ContractorDashboardResponse> getContractorDashboard(Authentication authentication) {
        Usuario usuario = usuarioRepository.findByUsernameIgnoreCase(authentication.getName()).orElse(null);
        if (usuario == null) {
            return ResponseEntity.notFound().build();
        }

        Perfil perfil = perfilRepository.findByUsernameIgnoreCase(usuario.getUsername()).orElse(null);
        List<Proyecto> proyectosActivos = perfil == null || perfil.getId() == null
                ? List.of()
                : proyectoRepository.findByContratistaAsignadoIdAndEstadoEjecucionNot(
                    perfil.getId(), EstadoEjecucion.COMPLETADO).stream()
                    .filter(project -> !EstadoAsignacion.SELECCIONADO_PENDIENTE_CONTRATACION
                        .equals(project.getEstadoAsignacion()))
                    .toList();
        List<Proyecto> proyectosCompletados = perfil == null || perfil.getId() == null
                ? List.of()
                : proyectoRepository.findByContratistaAsignadoIdAndEstadoEjecucion(
                        perfil.getId(), EstadoEjecucion.COMPLETADO);

        Set<String> miembrosEquipo = new HashSet<>();
        proyectosActivos.stream()
                .filter(proyecto -> proyecto.getEquipoTrabajo() != null)
                .flatMap(proyecto -> proyecto.getEquipoTrabajo().stream())
                .map(Perfil::getId)
                .filter(id -> id != null)
                .forEach(miembrosEquipo::add);

        List<String> requisitosFaltantes = perfilVerificationService.getMissingRequirements(perfil);
        List<ProjectCard> proyectos = proyectosActivos.parallelStream().map(this::toProjectCard).toList();

        return ResponseEntity.ok(new ContractorDashboardResponse(
                usuario.getUsername(),
                perfil == null || perfil.getNombre() == null || perfil.getNombre().isBlank()
                        ? usuario.getUsername() : perfil.getNombre(),
                usuario.isVerificado(),
                !requisitosFaltantes.isEmpty(),
                requisitosFaltantes,
                perfil == null ? null : perfil.getEspecialidad(),
                perfil == null ? null : perfil.getUbicacion(),
                perfil == null || perfil.getCalificacionPromedio() == null ? 0.0 : perfil.getCalificacionPromedio(),
                miembrosEquipo.size(),
                proyectosActivos.size(),
                proyectosCompletados.size(),
                proyectos));
    }

    @GetMapping("/contratista/historial")
    public ResponseEntity<List<HistoryProjectCard>> getContractorHistory(Authentication authentication) {
        Usuario usuario = usuarioRepository.findByUsernameIgnoreCase(authentication.getName()).orElse(null);
        if (usuario == null) {
            return ResponseEntity.notFound().build();
        }

        Perfil contratista = perfilRepository.findByUsernameIgnoreCase(usuario.getUsername()).orElse(null);
        if (contratista == null || contratista.getId() == null) {
            return ResponseEntity.ok(List.of());
        }

        List<HistoryProjectCard> history = proyectoRepository
                .findByContratistaAsignadoIdAndEstadoEjecucion(contratista.getId(), EstadoEjecucion.COMPLETADO)
                .stream()
                .sorted(Comparator.comparing(Proyecto::getFechaEntrega, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(this::toHistoryProjectCard)
                .toList();
        return ResponseEntity.ok(history);
    }

    @GetMapping("/trabajador")
    public ResponseEntity<WorkerDashboardResponse> getWorkerDashboard(Authentication authentication) {
        Usuario usuario = usuarioRepository.findByUsernameIgnoreCase(authentication.getName()).orElse(null);
        if (usuario == null) {
            return ResponseEntity.notFound().build();
        }

        Perfil perfil = perfilRepository.findByUsernameIgnoreCase(usuario.getUsername()).orElseGet(() -> {
            Perfil nuevo = new Perfil();
            nuevo.setUsername(usuario.getUsername());
            nuevo.setRole("ROLE_WORKER");
            nuevo.setActivo(true);
            nuevo.setNombre(usuario.getUsername());
            nuevo.setApellido("");
            nuevo.setDisponibilidad(false);
            return nuevo;
        });

        List<InvitacionTrabajo> pendingInvitations = perfil.getId() == null
            ? List.of()
            : invitacionTrabajoRepository.findByTrabajadorIdAndEstado(perfil.getId(), "PENDIENTE");
        LocalDateTime now = LocalDateTime.now();
        pendingInvitations.forEach(invitation -> invitacionService.expireIfNeeded(invitation, now));
        List<InvitacionCard> invitaciones = pendingInvitations.stream()
            .filter(invitation -> "PENDIENTE".equals(invitation.getEstado()))
            .map(this::toInvitationCard).toList();
        Set<String> assignedProjectIds = new java.util.LinkedHashSet<>();
        if (perfil.getId() != null) {
            proyectoRepository.findByEquipoTrabajoContaining(perfil).stream()
                    .map(Proyecto::getId)
                    .filter(java.util.Objects::nonNull)
                    .forEach(assignedProjectIds::add);
            equipoTrabajoRepository.findByIntegrantesContaining(perfil).stream()
                    .map(equipo -> equipo.getProyecto() == null ? null : equipo.getProyecto().getId())
                    .filter(java.util.Objects::nonNull)
                    .forEach(assignedProjectIds::add);
        }
        List<Proyecto> proyectos = assignedProjectIds.stream()
                .map(projectId -> proyectoRepository.findById(projectId).orElse(null))
                .filter(java.util.Objects::nonNull)
                .toList();
        List<String> requisitosFaltantes = perfilVerificationService.getMissingRequirements(perfil);

        return ResponseEntity.ok(new WorkerDashboardResponse(
                usuario.getUsername(),
                PerfilDisplayName.of(perfil),
                usuario.isVerificado(),
                !requisitosFaltantes.isEmpty(),
                requisitosFaltantes,
                perfil.getDisponibilidad(),
                perfil.getOficio(),
                perfil.getExperiencia() == null ? 0 : perfil.getExperiencia(),
                perfil.getCvUrl() != null,
                invitaciones,
                proyectos.parallelStream().map(this::toProjectCard).toList()));
    }

    @PatchMapping("/trabajador/invitaciones/{id}")
    public ResponseEntity<?> updateInvitation(
            @PathVariable String id,
            @RequestBody InvitationRequest request,
            Authentication authentication) {
        if (request == null || (!"ACEPTADA".equals(request.estado()) && !"RECHAZADA".equals(request.estado()))) {
            return ResponseEntity.badRequest().body(new ErrorResponse("Estado de invitación no válido."));
        }

        Perfil trabajador = perfilRepository.findByUsernameIgnoreCase(authentication.getName()).orElse(null);
        InvitacionTrabajo invitacion = invitacionTrabajoRepository.findById(id).orElse(null);
        if (trabajador == null || invitacion == null) {
            return ResponseEntity.notFound().build();
        }
        if (invitacion.getTrabajador() == null || !trabajador.getId().equals(invitacion.getTrabajador().getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ErrorResponse("Invitación no pertenece al usuario."));
        }
        if (!"PENDIENTE".equals(invitacion.getEstado())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse("La invitación ya fue respondida."));
        }
        if (invitacionService.expireIfNeeded(invitacion, LocalDateTime.now())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse("La invitación expiró después de 24 horas."));
        }

        Proyecto proyecto = null;
        if ("ACEPTADA".equals(request.estado())) {
            String projectId = invitacion.getProyectoId();
            proyecto = projectId == null ? null : proyectoRepository.findById(projectId).orElse(null);
            if (proyecto == null) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(new ErrorResponse("El proyecto ya no está disponible; no puedes aceptar esta invitación."));
            }
            if (invitacion.getPagoOfrecido() == null || invitacion.getPagoOfrecido() <= 0) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(new ErrorResponse("La invitación no indica cuánto te pagarán. Recházala y pide una nueva oferta."));
            }
            if (!Boolean.TRUE.equals(trabajador.getDisponibilidad()) || invitacionService.hasActiveProject(trabajador)) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(new ErrorResponse("Ya estás ocupado en otro proyecto y no puedes aceptar esta invitación."));
            }
        }

        invitacion.setEstado(request.estado());
        invitacionTrabajoRepository.save(invitacion);

        if ("ACEPTADA".equals(request.estado())) {
            if (proyecto.getEquipoTrabajo() == null) {
                proyecto.setEquipoTrabajo(new ArrayList<>());
            }
            boolean yaEnEquipo = proyecto.getEquipoTrabajo().stream()
                    .anyMatch(perfilEquipo -> trabajador.getId().equals(perfilEquipo.getId()));
            if (!yaEnEquipo) {
                proyecto.getEquipoTrabajo().add(trabajador);
                proyectoRepository.save(proyecto);
            }
            trabajador.setDisponibilidad(false);
            perfilRepository.save(trabajador);
        }

        return ResponseEntity.ok(new ActionResponse(invitacion.getId(), invitacion.getEstado()));
    }

    private ProjectCard toProjectCard(Proyecto proyecto) {
        Perfil contratista = proyecto.getContratistaAsignado();
        String contratistaNombre = contratista == null ? "Sin asignar" : PerfilDisplayName.of(contratista);
        return new ProjectCard(
                proyecto.getId(), proyecto.getTitulo(), proyecto.getDescripcion(), proyecto.getUbicacion(),
                proyecto.getPresupuesto(),
                proyecto.getEstadoEjecucion() == null ? "PENDIENTE" : proyecto.getEstadoEjecucion().name(),
                contratistaNombre,
                proyecto.getFechaEntrega(),
                projectRiskService.calcularIndicadores(proyecto));
    }

    private HistoryProjectCard toHistoryProjectCard(Proyecto proyecto) {
        String cliente = proyecto.getCliente() == null ? "Usuario no disponible" : proyecto.getCliente().getUsername();
        return new HistoryProjectCard(
                proyecto.getId(),
                proyecto.getTitulo(),
                proyecto.getDescripcion(),
                proyecto.getUbicacion(),
                proyecto.getPresupuesto(),
                proyecto.getFechaEntrega(),
                cliente);
    }

    private InvitacionCard toInvitationCard(InvitacionTrabajo invitacion) {
        String projectId = invitacion.getProyectoId();
        Proyecto proyecto = projectId == null ? null : proyectoRepository.findById(projectId).orElse(null);
        Perfil contratista = invitacion.getContratista();
        return new InvitacionCard(
                invitacion.getId(),
            projectId,
            proyecto == null ? invitacion.getProyectoTitulo() : proyecto.getTitulo(),
            proyecto == null ? invitacion.getProyectoUbicacion() : proyecto.getUbicacion(),
                proyecto == null ? invitacion.getProyectoDescripcion() : proyecto.getDescripcion(),
            contratista == null ? "Contratista" : PerfilDisplayName.of(contratista),
            invitacion.getPagoOfrecido(),
                invitacion.getContratistaPerfilId(),
                invitacion.getFechaExpiracion() == null && invitacion.getFechaCreacion() != null
                    ? invitacion.getFechaCreacion().plusHours(InvitacionTrabajoService.EXPIRATION_HOURS)
                    : invitacion.getFechaExpiracion(),
            proyecto != null);
    }

    public record ContractorDashboardResponse(
            String username,
            String nombre,
            boolean usuarioVerificado,
            boolean perfilIncompleto,
            List<String> requisitosFaltantes,
            String especialidad,
            String ubicacion,
            double calificacionPromedio,
            int miembrosEquipo,
            int proyectosEnProgreso,
            int proyectosCompletados,
            List<ProjectCard> proyectos) {}

    public record WorkerDashboardResponse(
            String username,
            String nombre,
            boolean usuarioVerificado,
            boolean perfilIncompleto,
            List<String> requisitosFaltantes,
            boolean disponible,
            String oficio,
            int experiencia,
            boolean cvDisponible,
            List<InvitacionCard> invitacionesPendientes,
            List<ProjectCard> proyectosAsignados) {}

    public record ProjectCard(
            String id,
            String titulo,
            String descripcion,
            String ubicacion,
            Double presupuesto,
            String estado,
            String contratista,
            LocalDate fechaEntrega,
            ProjectRiskService.ProjectIndicators indicadores) {}

    public record HistoryProjectCard(
            String id,
            String titulo,
            String descripcion,
            String ubicacion,
            Double presupuesto,
            LocalDate fechaEntrega,
            String cliente) {}

        public record InvitacionCard(
            String id,
            String proyectoId,
            String proyecto,
            String ubicacion,
            String descripcion,
            String contratista,
            Double pagoOfrecido,
            String contratistaPerfilId,
            LocalDateTime fechaExpiracion,
            boolean proyectoDisponible) {}

    public record InvitationRequest(String estado) {}

    public record ActionResponse(String id, String estado) {}

    public record ErrorResponse(String error) {}
}