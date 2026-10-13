package com.obratech.service;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.obratech.entity.InvitacionTrabajo;
import com.obratech.entity.Perfil;
import com.obratech.entity.Proyecto;
import com.obratech.entity.enums.EstadoEjecucion;
import com.obratech.repository.InvitacionTrabajoRepository;
import com.obratech.repository.ProyectoRepository;

@Service
public class InvitacionTrabajoService {

    public static final long EXPIRATION_HOURS = 24;

    private final InvitacionTrabajoRepository invitacionRepository;
    private final ProyectoRepository proyectoRepository;

    public InvitacionTrabajoService(
            InvitacionTrabajoRepository invitacionRepository,
            ProyectoRepository proyectoRepository) {
        this.invitacionRepository = invitacionRepository;
        this.proyectoRepository = proyectoRepository;
    }

    public boolean expireIfNeeded(InvitacionTrabajo invitacion, LocalDateTime now) {
        if (invitacion == null || !"PENDIENTE".equals(invitacion.getEstado()) || !isExpired(invitacion, now)) {
            return false;
        }
        invitacion.setEstado("EXPIRADA");
        invitacionRepository.save(invitacion);
        return true;
    }

    public boolean isExpired(InvitacionTrabajo invitacion, LocalDateTime now) {
        LocalDateTime expiresAt = invitacion.getFechaExpiracion();
        if (expiresAt == null) {
            LocalDateTime createdAt = invitacion.getFechaCreacion();
            expiresAt = createdAt == null ? now : createdAt.plusHours(EXPIRATION_HOURS);
        }
        return !now.isBefore(expiresAt);
    }

    public boolean hasActiveProject(Perfil trabajador) {
        return trabajador != null && proyectoRepository.findByEquipoTrabajoContaining(trabajador).stream()
                .anyMatch(this::isActiveProject);
    }

    public boolean hasOtherActiveProject(Perfil trabajador, String currentProjectId) {
        return trabajador != null && proyectoRepository.findByEquipoTrabajoContaining(trabajador).stream()
                .filter(this::isActiveProject)
                .anyMatch(project -> project.getId() != null && !project.getId().equals(currentProjectId));
    }

    public Set<String> getWorkersWithActiveProjects() {
        Set<String> workerIds = new HashSet<>();
        for (Proyecto proyecto : proyectoRepository.findAll()) {
            if (!isActiveProject(proyecto) || proyecto.getEquipoTrabajo() == null) {
                continue;
            }
            proyecto.getEquipoTrabajo().stream()
                    .filter(member -> member != null && member.getId() != null)
                    .map(Perfil::getId)
                    .forEach(workerIds::add);
        }
        return workerIds;
    }

    public boolean isActiveProject(Proyecto proyecto) {
        return proyecto != null && proyecto.getEstadoEjecucion() != EstadoEjecucion.COMPLETADO
                && proyecto.getEstadoEjecucion() != EstadoEjecucion.CANCELADO;
    }
}