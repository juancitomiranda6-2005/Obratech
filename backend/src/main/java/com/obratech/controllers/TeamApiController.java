package com.obratech.controllers;

import java.util.List;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.obratech.entity.EquipoTrabajo;
import com.obratech.entity.Perfil;
import com.obratech.entity.Proyecto;
import com.obratech.entity.enums.EstadoAsignacion;
import com.obratech.entity.enums.EstadoEjecucion;
import com.obratech.entity.enums.EstadoFaseObra;
import com.obratech.entity.enums.FaseObra;
import com.obratech.repository.EquipoTrabajoRepository;
import com.obratech.repository.PerfilRepository;
import com.obratech.repository.ProyectoRepository;
import com.obratech.util.PerfilDisplayName;

@RestController
@RequestMapping("/api")
public class TeamApiController {

    private final PerfilRepository perfilRepository;
    private final ProyectoRepository proyectoRepository;
    private final EquipoTrabajoRepository equipoTrabajoRepository;

    public TeamApiController(
            PerfilRepository perfilRepository,
            ProyectoRepository proyectoRepository,
            EquipoTrabajoRepository equipoTrabajoRepository) {
        this.perfilRepository = perfilRepository;
        this.proyectoRepository = proyectoRepository;
        this.equipoTrabajoRepository = equipoTrabajoRepository;
    }

    @GetMapping("/contractor/teams")
    public ResponseEntity<List<TeamCard>> getContractorTeams(
            @RequestParam(required = false) String projectId,
            Authentication authentication) {
        Perfil contratista = perfilRepository.findByUsernameIgnoreCase(authentication.getName()).orElse(null);
        if (contratista == null || contratista.getId() == null) {
            return ResponseEntity.ok(List.of());
        }

        if (projectId != null && !projectId.isBlank()) {
            Proyecto proyecto = proyectoRepository.findById(projectId).orElse(null);
            if (!ownsProject(contratista, proyecto)) {
                return ResponseEntity.notFound().build();
            }
            List<TeamCard> projectTeams = equipoTrabajoRepository.findByProyectoId(projectId).stream()
                    .map(this::toTeamCard).toList();
            return ResponseEntity.ok(projectTeams);
        }

        List<EquipoTrabajo> equipos = proyectoRepository.findByContratistaAsignadoId(contratista.getId()).stream()
                .filter(proyecto -> !isPendingContractorOffer(proyecto))
                .filter(proyecto -> proyecto.getId() != null)
                .flatMap(proyecto -> equipoTrabajoRepository.findByProyectoId(proyecto.getId()).stream())
                .toList();
        return ResponseEntity.ok(equipos.stream().map(this::toTeamCard).toList());
    }

    @GetMapping("/contractor/teams/options")
    public ResponseEntity<List<ProjectTeamOption>> getContractorTeamOptions(Authentication authentication) {
        Perfil contratista = perfilRepository.findByUsernameIgnoreCase(authentication.getName()).orElse(null);
        if (contratista == null || contratista.getId() == null) {
            return ResponseEntity.ok(List.of());
        }

        List<ProjectTeamOption> options = proyectoRepository.findByContratistaAsignadoId(contratista.getId()).stream()
            .filter(proyecto -> !isPendingContractorOffer(proyecto))
                .filter(proyecto -> proyecto.getId() != null)
            .filter(proyecto -> proyecto.getEstadoEjecucion() != EstadoEjecucion.COMPLETADO)
                .map(proyecto -> new ProjectTeamOption(
                        proyecto.getId(),
                        proyecto.getTitulo(),
                        proyecto.getFaseActual() == null ? FaseObra.PLANEACION.name() : proyecto.getFaseActual().name(),
                        proyecto.getEquipoTrabajo() == null
                                ? List.of()
                                : proyecto.getEquipoTrabajo().stream().map(this::toMemberCard).toList()))
                .toList();
        return ResponseEntity.ok(options);
    }

    @PostMapping("/contractor/projects/{projectId}/teams")
    public ResponseEntity<?> createContractorTeam(
            @PathVariable String projectId,
            @RequestBody TeamCreateRequest request,
            Authentication authentication) {
        if (request == null || isBlank(request.nombre()) || isBlank(request.actividad())) {
            return ResponseEntity.badRequest().body(new ErrorResponse("El nombre y la actividad del equipo son obligatorios."));
        }
        if (request.integrantesIds() == null || request.integrantesIds().isEmpty()) {
            return ResponseEntity.badRequest().body(new ErrorResponse("Selecciona al menos un integrante."));
        }
        if (request.porcentajeAvance() != null
                && (request.porcentajeAvance() < 0 || request.porcentajeAvance() > 100)) {
            return ResponseEntity.badRequest().body(new ErrorResponse("El avance debe estar entre 0 y 100."));
        }

        Perfil contratista = perfilRepository.findByUsernameIgnoreCase(authentication.getName()).orElse(null);
        Proyecto proyecto = proyectoRepository.findById(projectId).orElse(null);
        if (!ownsProject(contratista, proyecto)) {
            return ResponseEntity.notFound().build();
        }
        if (proyecto.getEstadoEjecucion() == EstadoEjecucion.COMPLETADO) {
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                    .body(new ErrorResponse("No se pueden crear equipos para un proyecto completado."));
        }

        List<Perfil> pool = proyecto.getEquipoTrabajo() == null ? List.of() : proyecto.getEquipoTrabajo();
        Set<String> requestedIds = new HashSet<>(request.integrantesIds());
        if (requestedIds.size() != request.integrantesIds().size()) {
            return ResponseEntity.badRequest().body(new ErrorResponse("No repitas integrantes en el equipo."));
        }

        List<EquipoTrabajo> existingTeams = equipoTrabajoRepository.findByProyectoId(projectId);
        Set<String> alreadyAssignedWorkerIds = existingTeams.stream()
                .filter(t -> t.getIntegrantes() != null)
                .flatMap(t -> t.getIntegrantes().stream())
                .map(Perfil::getId)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());

        List<Perfil> integrantes = new ArrayList<>();
        for (String integranteId : requestedIds) {
            Perfil integrante = pool.stream()
                    .filter(miembro -> integranteId.equals(miembro.getId()))
                    .findFirst()
                    .orElse(null);
            if (integrante == null) {
                return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                        .body(new ErrorResponse("Todos los integrantes deben pertenecer al equipo del proyecto."));
            }
            if (alreadyAssignedWorkerIds.contains(integranteId)) {
                return ResponseEntity.badRequest().body(new ErrorResponse(
                        PerfilDisplayName.of(integrante) + " ya pertenece a otro equipo en este proyecto. Un trabajador no puede estar en dos grupos."));
            }
            integrantes.add(integrante);
        }

        EquipoTrabajo equipo = new EquipoTrabajo();
        equipo.setNombre(request.nombre().trim());
        equipo.setActividad(request.actividad().trim());
        equipo.setPorcentajeAvance(request.porcentajeAvance() == null ? 0.0 : request.porcentajeAvance());
        FaseObra fase = parsePhase(request.fase());
        if (!isBlank(request.fase()) && fase == null) {
            return ResponseEntity.badRequest().body(new ErrorResponse("Selecciona una fase válida."));
        }
        equipo.setFase(fase == null ? proyecto.getFaseActual() : fase);
        equipo.setProyecto(proyecto);
        equipo.setIntegrantes(integrantes);
        EquipoTrabajo saved = equipoTrabajoRepository.save(equipo);
        updateProjectProgressState(proyecto);
        return ResponseEntity.status(HttpStatus.CREATED).body(toTeamCard(saved));
    }

    @PutMapping("/contractor/projects/{projectId}/teams/{teamId}")
    public ResponseEntity<?> updateContractorTeam(
            @PathVariable String projectId,
            @PathVariable String teamId,
            @RequestBody TeamUpdateRequest request,
            Authentication authentication) {
        if (request == null || isBlank(request.nombre()) || isBlank(request.actividad())) {
            return ResponseEntity.badRequest().body(new ErrorResponse("El nombre y la actividad del equipo son obligatorios."));
        }
        FaseObra fase = parsePhase(request.fase());
        if (fase == null) {
            return ResponseEntity.badRequest().body(new ErrorResponse("Selecciona una fase válida."));
        }
        Perfil contratista = perfilRepository.findByUsernameIgnoreCase(authentication.getName()).orElse(null);
        Proyecto proyecto = proyectoRepository.findById(projectId).orElse(null);
        EquipoTrabajo equipo = equipoTrabajoRepository.findById(teamId).orElse(null);
        if (!ownsProject(contratista, proyecto) || equipo == null || equipo.getProyecto() == null
                || !projectId.equals(equipo.getProyecto().getId())) {
            return ResponseEntity.notFound().build();
        }
        equipo.setNombre(request.nombre().trim());
        equipo.setActividad(request.actividad().trim());
        equipo.setFase(fase);
        return ResponseEntity.ok(toTeamCard(equipoTrabajoRepository.save(equipo)));
    }

    @DeleteMapping("/contractor/projects/{projectId}/teams/{teamId}")
    public ResponseEntity<?> deleteContractorTeam(
            @PathVariable String projectId,
            @PathVariable String teamId,
            Authentication authentication) {
        Perfil contratista = perfilRepository.findByUsernameIgnoreCase(authentication.getName()).orElse(null);
        Proyecto proyecto = proyectoRepository.findById(projectId).orElse(null);
        EquipoTrabajo equipo = equipoTrabajoRepository.findById(teamId).orElse(null);
        if (!ownsProject(contratista, proyecto) || equipo == null || equipo.getProyecto() == null
                || !projectId.equals(equipo.getProyecto().getId())) {
            return ResponseEntity.notFound().build();
        }

        equipoTrabajoRepository.delete(equipo);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/contractor/projects/{projectId}/teams/{teamId}/members")
    public ResponseEntity<?> addMemberToTeam(
            @PathVariable String projectId,
            @PathVariable String teamId,
            @RequestBody MemberAddRequest request,
            Authentication authentication) {
        if (request == null || isBlank(request.memberId())) {
            return ResponseEntity.badRequest().body(new ErrorResponse("El ID del trabajador es obligatorio."));
        }

        Perfil contratista = perfilRepository.findByUsernameIgnoreCase(authentication.getName()).orElse(null);
        Proyecto proyecto = proyectoRepository.findById(projectId).orElse(null);
        EquipoTrabajo equipo = equipoTrabajoRepository.findById(teamId).orElse(null);
        if (!ownsProject(contratista, proyecto) || equipo == null || equipo.getProyecto() == null
                || !projectId.equals(equipo.getProyecto().getId())) {
            return ResponseEntity.notFound().build();
        }

        List<Perfil> pool = proyecto.getEquipoTrabajo() == null ? List.of() : proyecto.getEquipoTrabajo();
        Perfil integrante = pool.stream()
                .filter(miembro -> request.memberId().equals(miembro.getId()))
                .findFirst()
                .orElse(null);
        if (integrante == null) {
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                    .body(new ErrorResponse("El trabajador debe pertenecer al equipo del proyecto."));
        }

        boolean yaEnEsteEquipo = equipo.getIntegrantes() != null
            && equipo.getIntegrantes().stream()
                .anyMatch(miembro -> request.memberId().equals(miembro.getId()));
        if (yaEnEsteEquipo) {
            return ResponseEntity.badRequest().body(new ErrorResponse(
                PerfilDisplayName.of(integrante) + " ya pertenece a este equipo."));
        }

        List<EquipoTrabajo> existingTeams = equipoTrabajoRepository.findByProyectoId(projectId);
        boolean yaEnOtroEquipo = existingTeams.stream()
            .filter(t -> !teamId.equals(t.getId()))
                .filter(t -> t.getIntegrantes() != null)
                .flatMap(t -> t.getIntegrantes().stream())
                .anyMatch(m -> request.memberId().equals(m.getId()));
        if (yaEnOtroEquipo) {
            return ResponseEntity.badRequest().body(new ErrorResponse(
                    PerfilDisplayName.of(integrante) + " ya pertenece a un grupo en este proyecto. No puede estar en dos grupos a la vez."));
        }

        if (equipo.getIntegrantes() == null) {
            equipo.setIntegrantes(new ArrayList<>());
        }

        equipo.getIntegrantes().add(integrante);
        EquipoTrabajo saved = equipoTrabajoRepository.save(equipo);
        return ResponseEntity.ok(toTeamCard(saved));
    }

    @DeleteMapping("/contractor/projects/{projectId}/teams/{teamId}/members/{memberId}")
    public ResponseEntity<?> removeMemberFromTeam(
            @PathVariable String projectId,
            @PathVariable String teamId,
            @PathVariable String memberId,
            Authentication authentication) {
        Perfil contratista = perfilRepository.findByUsernameIgnoreCase(authentication.getName()).orElse(null);
        Proyecto proyecto = proyectoRepository.findById(projectId).orElse(null);
        EquipoTrabajo equipo = equipoTrabajoRepository.findById(teamId).orElse(null);
        if (!ownsProject(contratista, proyecto) || equipo == null || equipo.getProyecto() == null
                || !projectId.equals(equipo.getProyecto().getId())) {
            return ResponseEntity.notFound().build();
        }

        if (equipo.getIntegrantes() != null) {
            equipo.getIntegrantes().removeIf(m -> memberId.equals(m.getId()));
            EquipoTrabajo saved = equipoTrabajoRepository.save(equipo);
            return ResponseEntity.ok(toTeamCard(saved));
        }

        return ResponseEntity.ok(toTeamCard(equipo));
    }

    @GetMapping("/worker/teams")
    public ResponseEntity<List<TeamCard>> getWorkerTeams(
            @RequestParam(required = false) String projectId,
            Authentication authentication) {
        Perfil trabajador = perfilRepository.findByUsernameIgnoreCase(authentication.getName()).orElse(null);
        if (trabajador == null) {
            return ResponseEntity.ok(List.of());
        }

        List<EquipoTrabajo> equipos = projectId == null || projectId.isBlank()
                ? equipoTrabajoRepository.findByIntegrantesContaining(trabajador)
                : equipoTrabajoRepository.findByProyectoIdAndIntegrantesContaining(projectId, trabajador);
        return ResponseEntity.ok(equipos.stream().map(this::toTeamCard).toList());
    }

    @PutMapping("/worker/teams/{teamId}/progress")
    public ResponseEntity<?> updateWorkerTeamProgress(
            @PathVariable String teamId,
            @RequestBody TeamProgressRequest request,
            Authentication authentication) {
        if (request == null || request.porcentajeAvance() == null
                || request.porcentajeAvance() < 0 || request.porcentajeAvance() > 100) {
            return ResponseEntity.badRequest().body(new ErrorResponse("El avance debe estar entre 0 y 100."));
        }

        Perfil trabajador = perfilRepository.findByUsernameIgnoreCase(authentication.getName()).orElse(null);
        EquipoTrabajo equipo = equipoTrabajoRepository.findById(teamId).orElse(null);
        if (trabajador == null || trabajador.getId() == null || equipo == null
                || equipo.getIntegrantes() == null
                || equipo.getIntegrantes().stream().noneMatch(integrante -> trabajador.getId().equals(integrante.getId()))) {
            return ResponseEntity.notFound().build();
        }

        String projectId = equipo.getProyecto() == null ? null : equipo.getProyecto().getId();
        Proyecto proyecto = projectId == null ? null : proyectoRepository.findById(projectId).orElse(null);
        if (proyecto == null) {
            return ResponseEntity.notFound().build();
        }

        equipo.setPorcentajeAvance(request.porcentajeAvance());
        equipo.setAvanceActualizadoEn(java.time.LocalDateTime.now());
        EquipoTrabajo saved = equipoTrabajoRepository.save(equipo);
        updateProjectProgressState(proyecto);
        return ResponseEntity.ok(toTeamCard(saved));
    }

    private void updateProjectProgressState(Proyecto proyecto) {
        if (proyecto == null || proyecto.getId() == null) {
            return;
        }
        List<EquipoTrabajo> equipos = equipoTrabajoRepository.findByProyectoId(proyecto.getId());
        if (equipos.isEmpty()) {
            return;
        }

        boolean allComplete = proyecto.getFaseActual() == FaseObra.DETALLE_Y_ENTREGA_FINAL
                && proyecto.getEstadoFaseActual() == EstadoFaseObra.APROBADA;
        boolean hasProgress = equipos.stream()
                .anyMatch(equipo -> equipo.getPorcentajeAvance() != null && equipo.getPorcentajeAvance() > 0.0);
        proyecto.setEstadoEjecucion(allComplete
                ? EstadoEjecucion.COMPLETADO
                : hasProgress ? EstadoEjecucion.EN_PROGRESO : EstadoEjecucion.PENDIENTE);
        proyectoRepository.save(proyecto);
    }

    private boolean ownsProject(Perfil contratista, Proyecto proyecto) {
        return contratista != null && contratista.getId() != null && proyecto != null
                && proyecto.getContratistaAsignado() != null
                && contratista.getId().equals(proyecto.getContratistaAsignado().getId())
                && !isPendingContractorOffer(proyecto);
    }

    private boolean isPendingContractorOffer(Proyecto proyecto) {
        return EstadoAsignacion.SELECCIONADO_PENDIENTE_CONTRATACION.equals(proyecto.getEstadoAsignacion());
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private FaseObra parsePhase(String value) {
        if (isBlank(value)) {
            return null;
        }
        try {
            return FaseObra.valueOf(value);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private TeamCard toTeamCard(EquipoTrabajo equipo) {
        String projectId = equipo.getProyecto() == null ? null : equipo.getProyecto().getId();
        Proyecto proyecto = projectId == null ? null : proyectoRepository.findById(projectId).orElse(null);
        List<MemberCard> integrantes = equipo.getIntegrantes() == null
                ? List.of()
                : equipo.getIntegrantes().stream().map(this::toMemberCard).toList();
        return new TeamCard(
                equipo.getId(),
                equipo.getNombre(),
                equipo.getActividad(),
                (equipo.getFase() == null
                        ? proyecto == null || proyecto.getFaseActual() == null ? FaseObra.PLANEACION : proyecto.getFaseActual()
                        : equipo.getFase()).name(),
                equipo.getPorcentajeAvance() == null ? 0.0 : equipo.getPorcentajeAvance(),
                projectId,
                proyecto == null ? "Proyecto no disponible" : proyecto.getTitulo(),
                proyecto == null ? null : proyecto.getUbicacion(),
                proyecto == null || proyecto.getEstadoEjecucion() == null ? "PENDIENTE" : proyecto.getEstadoEjecucion().name(),
                proyecto != null,
                integrantes);
    }

    private MemberCard toMemberCard(Perfil perfil) {
        return new MemberCard(
                perfil.getId(),
            PerfilDisplayName.of(perfil),
                perfil.getUsername(),
                perfil.getOficio(),
                Boolean.TRUE.equals(perfil.getVerificado()));
    }

    public record TeamProgressRequest(Double porcentajeAvance) {}

    public record MemberAddRequest(String memberId) {}

    public record TeamCreateRequest(String nombre, String actividad, Double porcentajeAvance, List<String> integrantesIds, String fase) {}

    public record TeamUpdateRequest(String nombre, String actividad, String fase) {}

    public record ProjectTeamOption(String id, String titulo, String faseActual, List<MemberCard> integrantesDisponibles) {}

    public record TeamCard(
            String id,
            String nombre,
            String actividad,
            String fase,
            double porcentajeAvance,
            String proyectoId,
            String proyecto,
            String ubicacion,
            String estadoProyecto,
            boolean proyectoDisponible,
            List<MemberCard> integrantes) {}

    public record MemberCard(String id, String nombre, String username, String oficio, boolean verificado) {}

    public record ErrorResponse(String error) {}
}
