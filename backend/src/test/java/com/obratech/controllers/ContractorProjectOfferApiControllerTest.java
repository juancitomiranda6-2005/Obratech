package com.obratech.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

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

class ContractorProjectOfferApiControllerTest {

    private PerfilRepository perfilRepository;
    private UsuarioRepository usuarioRepository;
    private ProyectoRepository proyectoRepository;
    private PostulacionRepository postulacionRepository;
    private ContractorProjectOfferApiController controller;
    private Perfil contractor;
    private Usuario contractorUser;
    private Proyecto project;
    private Postulacion offer;
    private Postulacion otherApplication;
    private Authentication authentication;

    @BeforeEach
    void setUp() {
        perfilRepository = mock(PerfilRepository.class);
        usuarioRepository = mock(UsuarioRepository.class);
        proyectoRepository = mock(ProyectoRepository.class);
        postulacionRepository = mock(PostulacionRepository.class);
        controller = new ContractorProjectOfferApiController(
                perfilRepository, usuarioRepository, proyectoRepository, postulacionRepository);

        contractor = new Perfil();
        contractor.setId("contractor-profile-id");
        contractor.setUsername("contractor@example.com");
        contractor.setRoles(Set.of("ROLE_CONTRACTOR"));
        contractorUser = new Usuario();
        contractorUser.setId("contractor-user-id");
        contractorUser.setUsername(contractor.getUsername());

        project = new Proyecto();
        project.setId("project-id");
        project.setContratistaAsignado(contractor);
        project.setEstadoAsignacion(EstadoAsignacion.SELECCIONADO_PENDIENTE_CONTRATACION);
        project.setPostulacionesAbiertas(false);

        offer = application("offer-id", contractorUser, project, EstadoPostulacion.OFFERED);
        otherApplication = application("other-id", new Usuario(), project, EstadoPostulacion.PENDING);

        when(perfilRepository.findByUsernameIgnoreCase(contractor.getUsername())).thenReturn(Optional.of(contractor));
        when(usuarioRepository.findByUsernameIgnoreCase(contractor.getUsername())).thenReturn(Optional.of(contractorUser));
        when(postulacionRepository.findById(offer.getId())).thenReturn(Optional.of(offer));
        when(postulacionRepository.save(any(Postulacion.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(proyectoRepository.save(any(Proyecto.class))).thenAnswer(invocation -> invocation.getArgument(0));

        authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn(contractor.getUsername());
        when(authentication.getAuthorities()).thenAnswer(invocation ->
                List.of(new SimpleGrantedAuthority("ROLE_CONTRACTOR")));
    }

    @Test
    void contractorAcceptingOfferFinalizesAssignmentAndRejectsOtherApplications() {
        when(postulacionRepository.findByProyectoId(project.getId())).thenReturn(List.of(offer, otherApplication));

        ResponseEntity<?> response = controller.decideOffer(
                offer.getId(), new ContractorProjectOfferApiController.OfferDecision("ACCEPTED"), authentication);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(EstadoPostulacion.ACCEPTED, offer.getEstado());
        assertEquals(EstadoPostulacion.REJECTED, otherApplication.getEstado());
        assertEquals(EstadoAsignacion.ASIGNADO, project.getEstadoAsignacion());
        assertEquals(false, project.isPostulacionesAbiertas());
    }

    @Test
    void contractorRejectingOfferReopensProjectAndLeavesOtherApplicationsPending() {
        ResponseEntity<?> response = controller.decideOffer(
                offer.getId(), new ContractorProjectOfferApiController.OfferDecision("REJECTED"), authentication);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(EstadoPostulacion.REJECTED, offer.getEstado());
        assertEquals(EstadoPostulacion.PENDING, otherApplication.getEstado());
        assertNull(project.getContratistaAsignado());
        assertEquals(EstadoAsignacion.SIN_ASIGNAR, project.getEstadoAsignacion());
        assertEquals(true, project.isPostulacionesAbiertas());
    }

    private Postulacion application(String id, Usuario user, Proyecto target, EstadoPostulacion state) {
        Postulacion application = new Postulacion();
        application.setId(id);
        application.setUsuario(user);
        application.setProyecto(target);
        application.setEstado(state);
        return application;
    }
}
