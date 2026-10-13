package com.obratech.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.obratech.entity.InvitacionTrabajo;
import com.obratech.entity.Perfil;
import com.obratech.entity.Proyecto;
import com.obratech.entity.enums.EstadoEjecucion;
import com.obratech.repository.InvitacionTrabajoRepository;
import com.obratech.repository.ProyectoRepository;

class InvitacionTrabajoServiceTest {

    private final InvitacionTrabajoRepository invitationRepository = mock(InvitacionTrabajoRepository.class);
    private final ProyectoRepository projectRepository = mock(ProyectoRepository.class);
    private final InvitacionTrabajoService service = new InvitacionTrabajoService(invitationRepository, projectRepository);

    @Test
    void invitationExpiresAtTwentyFourHoursAndIsPersisted() {
        LocalDateTime created = LocalDateTime.of(2026, 10, 13, 10, 0);
        InvitacionTrabajo invitation = invitation(created);

        assertFalse(service.expireIfNeeded(invitation, created.plusHours(23)));
        assertTrue(service.expireIfNeeded(invitation, created.plusHours(24)));
        assertEquals("EXPIRADA", invitation.getEstado());
        verify(invitationRepository).save(invitation);
    }

    @Test
    void reportsWorkersAssignedToActiveProjectsAsOccupied() {
        Perfil worker = new Perfil();
        worker.setId("worker-1");
        Proyecto project = new Proyecto();
        project.setId("project-1");
        project.setEstadoEjecucion(EstadoEjecucion.EN_PROGRESO);
        when(projectRepository.findByEquipoTrabajoContaining(worker)).thenReturn(List.of(project));

        assertTrue(service.hasActiveProject(worker));

        project.setEstadoEjecucion(EstadoEjecucion.COMPLETADO);
        assertFalse(service.hasActiveProject(worker));
    }

    private InvitacionTrabajo invitation(LocalDateTime created) {
        InvitacionTrabajo invitation = new InvitacionTrabajo();
        invitation.setEstado("PENDIENTE");
        invitation.setFechaCreacion(created);
        invitation.setFechaExpiracion(created.plusHours(24));
        return invitation;
    }
}