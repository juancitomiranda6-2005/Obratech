package com.obratech.controllers;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.obratech.entity.InvitacionTrabajo;
import com.obratech.entity.Perfil;
import com.obratech.entity.Proyecto;
import com.obratech.entity.enums.EstadoEjecucion;
import com.obratech.repository.InvitacionTrabajoRepository;
import com.obratech.repository.PerfilRepository;
import com.obratech.repository.ProyectoRepository;
import com.obratech.util.PerfilDisplayName;

@RestController
@RequestMapping("/api/contractor/workers")
public class WorkerRecruitmentApiController {

    private final PerfilRepository perfilRepository;
    private final ProyectoRepository proyectoRepository;
    private final InvitacionTrabajoRepository invitacionTrabajoRepository;

    public WorkerRecruitmentApiController(
            PerfilRepository perfilRepository,
            ProyectoRepository proyectoRepository,
            InvitacionTrabajoRepository invitacionTrabajoRepository) {
        this.perfilRepository = perfilRepository;
        this.proyectoRepository = proyectoRepository;
        this.invitacionTrabajoRepository = invitacionTrabajoRepository;
    }

    @GetMapping("/available")
    public ResponseEntity<List<WorkerCard>> getAvailableWorkers(
            @org.springframework.web.bind.annotation.RequestParam(required = false) String projectId,
            Authentication authentication) {
        Perfil contratista = perfilRepository.findByUsernameIgnoreCase(authentication.getName()).orElse(null);

        // Listar todos los trabajadores activos
        List<Perfil> allWorkers = perfilRepository.findByRolesAndActivoTrue("ROLE_WORKER");

        // Invitaciones pendientes
        List<InvitacionTrabajo> pendingInvitations = contratista != null && contratista.getId() != null
                ? invitacionTrabajoRepository.findByContratistaIdAndEstado(contratista.getId(), "PENDIENTE")
                : List.of();

        // Proyecto seleccionado
        Proyecto proyectoSeleccionado = (projectId != null && !projectId.isBlank())
                ? proyectoRepository.findById(projectId).orElse(null)
                : null;

        java.util.Set<String> contratadosEnProyecto = new java.util.HashSet<>();
        if (proyectoSeleccionado != null && proyectoSeleccionado.getEquipoTrabajo() != null) {
            proyectoSeleccionado.getEquipoTrabajo().forEach(p -> {
                if (p != null && p.getId() != null) contratadosEnProyecto.add(p.getId());
            });
        }

        // Mapa trabajadorId -> Set de proyectos invitados
        java.util.Map<String, java.util.Set<String>> invitacionesPendientesMap = new java.util.HashMap<>();
        for (InvitacionTrabajo inv : pendingInvitations) {
            if (inv.getTrabajador() != null && inv.getTrabajador().getId() != null && inv.getProyecto() != null) {
                invitacionesPendientesMap.computeIfAbsent(inv.getTrabajador().getId(), k -> new java.util.HashSet<>())
                        .add(inv.getProyecto().getId());
            }
        }

        List<WorkerCard> workers = allWorkers.stream().map(perfil -> {
            boolean estaContratado = contratadosEnProyecto.contains(perfil.getId());
            boolean estaInvitado = projectId != null && invitacionesPendientesMap.getOrDefault(perfil.getId(), java.util.Set.of()).contains(projectId);
            String estado;
            if (estaContratado) {
                estado = "CONTRATADO";
            } else if (estaInvitado) {
                estado = "INVITADO";
            } else if (Boolean.TRUE.equals(perfil.getDisponibilidad())) {
                estado = "DISPONIBLE";
            } else {
                estado = "NO_DISPONIBLE";
            }

            return new WorkerCard(
                    perfil.getId(),
                    PerfilDisplayName.of(perfil),
                    perfil.getEmail(),
                    perfil.getTelefono(),
                    perfil.getOficio(),
                    perfil.getExperiencia() == null ? 0 : perfil.getExperiencia(),
                    Boolean.TRUE.equals(perfil.getVerificado()),
                    estado,
                    estaInvitado,
                    estaContratado);
        }).toList();

        return ResponseEntity.ok(workers);
    }

    @PostMapping("/{workerId}/invitations")
    public ResponseEntity<?> inviteWorker(
            @PathVariable String workerId,
            @RequestBody InvitationCreateRequest request,
            Authentication authentication) {
        if (request == null || request.proyectoId() == null || request.proyectoId().isBlank()) {
            return ResponseEntity.badRequest().body(new ErrorResponse("Selecciona un proyecto."));
        }

        Perfil contratista = perfilRepository.findByUsernameIgnoreCase(authentication.getName()).orElse(null);
        Perfil trabajador = perfilRepository.findById(workerId).orElse(null);
        Proyecto proyecto = proyectoRepository.findById(request.proyectoId()).orElse(null);
        if (contratista == null || contratista.getId() == null || trabajador == null || proyecto == null
                || proyecto.getContratistaAsignado() == null
                || !contratista.getId().equals(proyecto.getContratistaAsignado().getId())) {
            return ResponseEntity.notFound().build();
        }
        if (trabajador.getRoles() == null || !trabajador.getRoles().contains("ROLE_WORKER")
                || Boolean.FALSE.equals(trabajador.getActivo())) {
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                    .body(new ErrorResponse("El trabajador no está activo en la plataforma."));
        }
        if (proyecto.getEstadoEjecucion() == EstadoEjecucion.COMPLETADO) {
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                    .body(new ErrorResponse("No se pueden enviar invitaciones a un proyecto completado."));
        }

        boolean alreadyOnProject = proyecto.getEquipoTrabajo() != null && proyecto.getEquipoTrabajo().stream()
                .anyMatch(member -> workerId.equals(member.getId()));
        if (alreadyOnProject) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new ErrorResponse("El trabajador ya pertenece a este proyecto (ya está contratado)."));
        }

        boolean invitationExists = invitacionTrabajoRepository.findByTrabajadorIdAndEstado(workerId, "PENDIENTE")
                .stream().anyMatch(invitation -> invitation.getProyecto() != null
                        && request.proyectoId().equals(invitation.getProyecto().getId()));
        if (invitationExists) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new ErrorResponse("Ya hay una invitación pendiente para este proyecto."));
        }

        InvitacionTrabajo invitation = new InvitacionTrabajo();
        invitation.setTrabajador(trabajador);
        invitation.setContratista(contratista);
        invitation.setProyecto(proyecto);
        invitation.setEstado("PENDIENTE");
        invitation.setFechaCreacion(LocalDateTime.now());
        InvitacionTrabajo saved = invitacionTrabajoRepository.save(invitation);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new InvitationResponse(saved.getId(), saved.getEstado(), proyecto.getTitulo()));
    }

    public record WorkerCard(
            String id,
            String nombre,
            String email,
            String telefono,
            String oficio,
            Integer experiencia,
            boolean verificado,
            String estado,
            boolean invitado,
            boolean contratado) {}

    public record InvitationCreateRequest(String proyectoId) {}

    public record InvitationResponse(String id, String estado, String proyecto) {}

    public record ErrorResponse(String error) {}
}
