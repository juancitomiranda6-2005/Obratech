package com.obratech.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import com.obratech.entity.InvitacionTrabajo;
import com.obratech.entity.Perfil;
import com.obratech.entity.Proyecto;
import com.obratech.entity.enums.EstadoEjecucion;
import com.obratech.repository.InvitacionTrabajoRepository;
import com.obratech.repository.PerfilRepository;
import com.obratech.repository.ProyectoRepository;
import com.obratech.repository.UsuarioRepository;
import com.obratech.service.InvitacionTrabajoService;
import com.obratech.service.PerfilVerificationService;

class ProfessionalDashboardApiControllerTest {

    private final UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);
    private final PerfilRepository perfilRepository = mock(PerfilRepository.class);
    private final ProyectoRepository proyectoRepository = mock(ProyectoRepository.class);
    private final InvitacionTrabajoRepository invitationRepository = mock(InvitacionTrabajoRepository.class);
    private final PerfilVerificationService verificationService = mock(PerfilVerificationService.class);
    private final InvitacionTrabajoService invitationService = new InvitacionTrabajoService(
            invitationRepository, proyectoRepository);
    private final ProfessionalDashboardApiController controller = new ProfessionalDashboardApiController(
            usuarioRepository, perfilRepository, proyectoRepository, invitationRepository,
            verificationService, invitationService);
    private final Authentication authentication = mock(Authentication.class);

    @Test
    void refusesAcceptanceWhenWorkerAlreadyHasAnActiveProject() {
        Perfil worker = worker();
        InvitacionTrabajo invitation = pendingInvitation(worker, LocalDateTime.now().plusHours(24));
        Proyecto offeredProject = project("offered-project");
        Proyecto existingProject = project("existing-project");
        existingProject.setEstadoEjecucion(EstadoEjecucion.EN_PROGRESO);
        when(authentication.getName()).thenReturn(worker.getUsername());
        when(perfilRepository.findByUsernameIgnoreCase(worker.getUsername())).thenReturn(Optional.of(worker));
        when(invitationRepository.findById("invitation-1")).thenReturn(Optional.of(invitation));
        when(proyectoRepository.findById("offered-project")).thenReturn(Optional.of(offeredProject));
        when(proyectoRepository.findByEquipoTrabajoContaining(worker)).thenReturn(List.of(existingProject));

        ResponseEntity<?> response = controller.updateInvitation(
                "invitation-1", new ProfessionalDashboardApiController.InvitationRequest("ACEPTADA"), authentication);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals("PENDIENTE", invitation.getEstado());
    }

    @Test
    void expiresInvitationWhenWorkerAttemptsAcceptanceAfterTwentyFourHours() {
        Perfil worker = worker();
        InvitacionTrabajo invitation = pendingInvitation(worker, LocalDateTime.now().minusSeconds(1));
        when(authentication.getName()).thenReturn(worker.getUsername());
        when(perfilRepository.findByUsernameIgnoreCase(worker.getUsername())).thenReturn(Optional.of(worker));
        when(invitationRepository.findById("invitation-1")).thenReturn(Optional.of(invitation));

        ResponseEntity<?> response = controller.updateInvitation(
                "invitation-1", new ProfessionalDashboardApiController.InvitationRequest("ACEPTADA"), authentication);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals("EXPIRADA", invitation.getEstado());
        verify(invitationRepository).save(invitation);
        assertTrue(invitationService.isExpired(invitation, LocalDateTime.now()));
    }

    private Perfil worker() {
        Perfil worker = new Perfil();
        worker.setId("worker-1");
        worker.setUsername("worker@example.test");
        worker.setDisponibilidad(true);
        return worker;
    }

    private InvitacionTrabajo pendingInvitation(Perfil worker, LocalDateTime expiresAt) {
        InvitacionTrabajo invitation = new InvitacionTrabajo();
        invitation.setId("invitation-1");
        invitation.setTrabajador(worker);
        invitation.setProyectoId("offered-project");
        invitation.setEstado("PENDIENTE");
        invitation.setPagoOfrecido(1_000_000.0);
        invitation.setFechaCreacion(expiresAt.minusHours(24));
        invitation.setFechaExpiracion(expiresAt);
        return invitation;
    }

    private Proyecto project(String id) {
        Proyecto project = new Proyecto();
        project.setId(id);
        project.setEstadoEjecucion(EstadoEjecucion.PENDIENTE);
        project.setEquipoTrabajo(List.of());
        return project;
    }
}