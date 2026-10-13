package com.obratech.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

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

class ClientApplicationsApiControllerTest {

    @Test
    void clientSelectionCreatesPendingContractorOffer() {
        ProyectoRepository proyectoRepository = mock(ProyectoRepository.class);
        PostulacionRepository postulacionRepository = mock(PostulacionRepository.class);
        PerfilRepository perfilRepository = mock(PerfilRepository.class);
        UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);
        ClientApplicationsApiController controller = new ClientApplicationsApiController(
                proyectoRepository, postulacionRepository, perfilRepository, usuarioRepository);

        Usuario client = new Usuario();
        client.setId("client-user-id");
        client.setUsername("client@example.com");
        Usuario contractorUser = new Usuario();
        contractorUser.setId("contractor-user-id");
        contractorUser.setUsername("contractor@example.com");

        Perfil contractor = new Perfil();
        contractor.setId("contractor-profile-id");
        contractor.setUsername(contractorUser.getUsername());
        contractor.setRoles(Set.of("ROLE_CONTRACTOR"));

        Proyecto project = new Proyecto();
        project.setId("project-id");
        project.setCliente(client);

        Postulacion application = new Postulacion();
        application.setId("application-id");
        application.setProyecto(project);
        application.setUsuario(contractorUser);
        application.setEstado(EstadoPostulacion.PENDING);

        when(usuarioRepository.findByUsernameIgnoreCase(client.getUsername())).thenReturn(Optional.of(client));
        when(proyectoRepository.findById(project.getId())).thenReturn(Optional.of(project));
        when(postulacionRepository.findById(application.getId())).thenReturn(Optional.of(application));
        when(perfilRepository.findByUsernameIgnoreCase(contractorUser.getUsername())).thenReturn(Optional.of(contractor));
        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn(client.getUsername());

        ResponseEntity<?> response = controller.update(
                project.getId(), application.getId(),
                new ClientApplicationsApiController.ApplicationAction("ACCEPTED"), authentication);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(EstadoPostulacion.OFFERED, application.getEstado());
        assertSame(contractor, project.getContratistaAsignado());
        assertEquals(EstadoAsignacion.SELECCIONADO_PENDIENTE_CONTRATACION, project.getEstadoAsignacion());
        assertEquals(false, project.isPostulacionesAbiertas());
    }
}
