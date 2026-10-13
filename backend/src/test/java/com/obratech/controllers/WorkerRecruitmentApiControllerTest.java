package com.obratech.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import com.obratech.entity.InvitacionTrabajo;
import com.obratech.entity.Perfil;
import com.obratech.entity.Proyecto;
import com.obratech.entity.enums.EstadoEjecucion;
import com.obratech.repository.InvitacionTrabajoRepository;
import com.obratech.repository.PerfilRepository;
import com.obratech.repository.ProyectoRepository;
import com.obratech.service.InvitacionTrabajoService;

class WorkerRecruitmentApiControllerTest {

    private final PerfilRepository perfilRepository = mock(PerfilRepository.class);
    private final ProyectoRepository proyectoRepository = mock(ProyectoRepository.class);
    private final InvitacionTrabajoRepository invitationRepository = mock(InvitacionTrabajoRepository.class);
    private final InvitacionTrabajoService invitationService = new InvitacionTrabajoService(
            invitationRepository, proyectoRepository);
    private final WorkerRecruitmentApiController controller = new WorkerRecruitmentApiController(
            perfilRepository, proyectoRepository, invitationRepository, invitationService);
    private final Authentication authentication = mock(Authentication.class);

    @Test
    void onlyListsAvailableWorkersWithoutActiveProjectAssignments() {
        Perfil contractor = profile("contractor-1", true);
        Perfil freeWorker = profile("worker-free", true);
        Perfil busyWorker = profile("worker-busy", true);
        Perfil unavailableWorker = profile("worker-unavailable", false);
        Proyecto activeProject = new Proyecto();
        activeProject.setEstadoEjecucion(EstadoEjecucion.EN_PROGRESO);
        activeProject.setEquipoTrabajo(List.of(busyWorker));

        when(authentication.getName()).thenReturn("contractor@example.test");
        when(perfilRepository.findByUsernameIgnoreCase("contractor@example.test")).thenReturn(Optional.of(contractor));
        when(perfilRepository.findByRolesAndActivoTrue("ROLE_WORKER"))
                .thenReturn(List.of(freeWorker, busyWorker, unavailableWorker));
        when(proyectoRepository.findAll()).thenReturn(List.of(activeProject));
        when(invitationRepository.findByContratistaIdAndEstado("contractor-1", "PENDIENTE")).thenReturn(List.of());

        ResponseEntity<List<WorkerRecruitmentApiController.WorkerCard>> response =
                controller.getAvailableWorkers(null, authentication);

        assertEquals(1, response.getBody().size());
        assertEquals("worker-free", response.getBody().get(0).id());
    }

    @Test
    void invitationIncludesProjectDescriptionAndExpiresInTwentyFourHours() {
        Perfil contractor = profile("contractor-1", true);
        Perfil worker = profile("worker-1", true);
        Proyecto project = new Proyecto();
        project.setId("project-1");
        project.setEstadoEjecucion(EstadoEjecucion.EN_PROGRESO);
        project.setTitulo("Obra residencial");
        project.setDescripcion("Construcción de vivienda de dos plantas.");
        project.setContratistaAsignado(contractor);

        when(authentication.getName()).thenReturn("contractor@example.test");
        when(perfilRepository.findByUsernameIgnoreCase("contractor@example.test")).thenReturn(Optional.of(contractor));
        when(perfilRepository.findById("worker-1")).thenReturn(Optional.of(worker));
        when(proyectoRepository.findById("project-1")).thenReturn(Optional.of(project));
        when(proyectoRepository.findByEquipoTrabajoContaining(worker)).thenReturn(List.of());
        when(invitationRepository.findByTrabajadorIdAndEstado("worker-1", "PENDIENTE")).thenReturn(List.of());
        when(invitationRepository.save(any(InvitacionTrabajo.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ResponseEntity<?> response = controller.inviteWorker(
                "worker-1",
                new WorkerRecruitmentApiController.InvitationCreateRequest("project-1", 2_500_000.0),
                authentication);

        assertEquals(201, response.getStatusCode().value());
        var body = (WorkerRecruitmentApiController.InvitationResponse) response.getBody();
        assertEquals("Construcción de vivienda de dos plantas.", body.descripcion());
        ArgumentCaptor<InvitacionTrabajo> invitationCaptor = ArgumentCaptor.forClass(InvitacionTrabajo.class);
        verify(invitationRepository).save(invitationCaptor.capture());
        assertEquals(24, java.time.Duration.between(
                invitationCaptor.getValue().getFechaCreacion(), invitationCaptor.getValue().getFechaExpiracion()).toHours());
    }

    @Test
    void expiredInvitationDoesNotBlockSendingANewInvitation() {
        Perfil contractor = profile("contractor-1", true);
        Perfil worker = profile("worker-1", true);
        Proyecto project = new Proyecto();
        project.setId("project-1");
        project.setEstadoEjecucion(EstadoEjecucion.EN_PROGRESO);
        project.setTitulo("Obra residencial");
        project.setContratistaAsignado(contractor);
        InvitacionTrabajo expiredInvitation = new InvitacionTrabajo();
        expiredInvitation.setTrabajador(worker);
        expiredInvitation.setProyectoId("project-1");
        expiredInvitation.setEstado("PENDIENTE");
        expiredInvitation.setFechaCreacion(java.time.LocalDateTime.now().minusHours(25));
        expiredInvitation.setFechaExpiracion(java.time.LocalDateTime.now().minusSeconds(1));

        when(authentication.getName()).thenReturn("contractor@example.test");
        when(perfilRepository.findByUsernameIgnoreCase("contractor@example.test")).thenReturn(Optional.of(contractor));
        when(perfilRepository.findById("worker-1")).thenReturn(Optional.of(worker));
        when(proyectoRepository.findById("project-1")).thenReturn(Optional.of(project));
        when(proyectoRepository.findByEquipoTrabajoContaining(worker)).thenReturn(List.of());
        when(invitationRepository.findByTrabajadorIdAndEstado("worker-1", "PENDIENTE"))
                .thenReturn(List.of(expiredInvitation));
        when(invitationRepository.save(any(InvitacionTrabajo.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ResponseEntity<?> response = controller.inviteWorker(
                "worker-1",
                new WorkerRecruitmentApiController.InvitationCreateRequest("project-1", 2_500_000.0),
                authentication);

        assertEquals(201, response.getStatusCode().value());
        assertEquals("EXPIRADA", expiredInvitation.getEstado());
    }

    private Perfil profile(String id, boolean available) {
        Perfil profile = new Perfil();
        profile.setId(id);
        profile.setUsername(id.equals("contractor-1") ? "contractor@example.test" : id + "@example.test");
        profile.setRoles(id.equals("contractor-1") ? java.util.Set.of("ROLE_CONTRACTOR") : java.util.Set.of("ROLE_WORKER"));
        profile.setActivo(true);
        profile.setDisponibilidad(available);
        return profile;
    }
}