package com.obratech.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import com.obratech.entity.Proyecto;
import com.obratech.entity.ReporteProyecto;
import com.obratech.entity.Usuario;
import com.obratech.repository.CalificacionRepository;
import com.obratech.repository.PerfilRepository;
import com.obratech.repository.ProyectoRepository;
import com.obratech.repository.ReporteProyectoRepository;
import com.obratech.repository.UsuarioRepository;
import com.obratech.service.PerfilVerificationService;

@ExtendWith(MockitoExtension.class)
class ClientDashboardApiControllerTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PerfilRepository perfilRepository;

    @Mock
    private ProyectoRepository proyectoRepository;

    @Mock
    private ReporteProyectoRepository reporteRepository;

    @Mock
    private CalificacionRepository calificacionRepository;

    @Mock
    private PerfilVerificationService perfilVerificationService;

    @Mock
    private Authentication authentication;

    @Test
    void returnsReportsOnlyForProjectsOwnedByAuthenticatedClient() {
        Usuario client = new Usuario();
        client.setId("client-1");
        client.setUsername("client@example.test");
        client.setVerificado(true);
        Proyecto project = new Proyecto();
        project.setId("project-1");
        project.setTitulo("Remodelación");
        ReporteProyecto report = new ReporteProyecto();
        report.setId("report-1");
        report.setProyectoId("project-1");
        report.setProyectoTitulo("Remodelación");
        report.setContratistaUsername("contractor@example.test");
        report.setContenido("Avance de obra reportado.");
        report.setCreado(LocalDateTime.now());

        when(authentication.getName()).thenReturn("client@example.test");
        when(usuarioRepository.findByUsernameIgnoreCase("client@example.test")).thenReturn(Optional.of(client));
        when(proyectoRepository.findByClienteId("client-1")).thenReturn(List.of(project));
        when(reporteRepository.findByProyectoIdInOrderByCreadoDesc(List.of("project-1"))).thenReturn(List.of(report));

        ClientDashboardApiController controller = new ClientDashboardApiController(
                usuarioRepository,
                perfilRepository,
                proyectoRepository,
                reporteRepository,
                calificacionRepository,
                perfilVerificationService);

        ResponseEntity<ClientDashboardApiController.ClientReportsResponse> response =
                controller.getClientReports(authentication);

        assertEquals(1, response.getBody().informes().size());
        assertEquals("project-1", response.getBody().informes().get(0).proyectoId());
        assertEquals("contractor@example.test", response.getBody().informes().get(0).contratista());
        verify(reporteRepository).findByProyectoIdInOrderByCreadoDesc(List.of("project-1"));
    }

    @Test
    void unverifiedClientCannotReadReports() {
        Usuario client = new Usuario();
        client.setId("client-1");
        client.setUsername("client@example.test");
        client.setVerificado(false);
        when(authentication.getName()).thenReturn("client@example.test");
        when(usuarioRepository.findByUsernameIgnoreCase("client@example.test")).thenReturn(Optional.of(client));

        ClientDashboardApiController controller = new ClientDashboardApiController(
                usuarioRepository,
                perfilRepository,
                proyectoRepository,
                reporteRepository,
                calificacionRepository,
                perfilVerificationService);

        ResponseEntity<ClientDashboardApiController.ClientReportsResponse> response =
                controller.getClientReports(authentication);

        assertEquals(org.springframework.http.HttpStatus.FORBIDDEN, response.getStatusCode());
    }
}