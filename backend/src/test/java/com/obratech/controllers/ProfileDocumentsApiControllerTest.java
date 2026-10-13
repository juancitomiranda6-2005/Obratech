package com.obratech.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import com.obratech.entity.Perfil;
import com.obratech.entity.Usuario;
import com.obratech.repository.PerfilRepository;
import com.obratech.repository.PostulacionRepository;
import com.obratech.repository.ProyectoRepository;
import com.obratech.repository.UsuarioRepository;

class ProfileDocumentsApiControllerTest {

    private PerfilRepository perfilRepository;
    private UsuarioRepository usuarioRepository;
    private ProyectoRepository proyectoRepository;
    private PostulacionRepository postulacionRepository;
    private GridFsTemplate gridFsTemplate;
    private ProfileDocumentsApiController controller;

    @BeforeEach
    void setUp() {
        perfilRepository = mock(PerfilRepository.class);
        usuarioRepository = mock(UsuarioRepository.class);
        proyectoRepository = mock(ProyectoRepository.class);
        postulacionRepository = mock(PostulacionRepository.class);
        gridFsTemplate = mock(GridFsTemplate.class);
        controller = new ProfileDocumentsApiController(
                perfilRepository, usuarioRepository, proyectoRepository, postulacionRepository, gridFsTemplate);
    }

    @Test
    void clientWithoutContractorRelationshipCannotReadDocuments() {
        Perfil clientProfile = profile("client", "ROLE_CLIENT");
        Perfil contractorProfile = profile("contractor", "ROLE_CONTRACTOR");
        Usuario client = new Usuario();
        client.setId("client-user-id");
        Usuario contractor = new Usuario();
        contractor.setId("contractor-user-id");

        when(perfilRepository.findByUsernameIgnoreCase("client")).thenReturn(Optional.of(clientProfile));
        when(perfilRepository.findById("contractor-id")).thenReturn(Optional.of(contractorProfile));
        when(usuarioRepository.findByUsernameIgnoreCase("client")).thenReturn(Optional.of(client));
        when(usuarioRepository.findByUsernameIgnoreCase("contractor")).thenReturn(Optional.of(contractor));
        when(proyectoRepository.findByClienteId("client-user-id")).thenReturn(List.of());

        ResponseEntity<?> response = controller.getContractorDocumentForClient(
                "contractor-id", "estatutos", authentication("client", "ROLE_CLIENT"));

        assertEquals(404, response.getStatusCode().value());
        verify(gridFsTemplate, never()).findOne(org.mockito.ArgumentMatchers.any(Query.class));
    }

    @Test
    void workerWithoutAssignedProjectCannotReadContractorStatutes() {
        Perfil worker = profile("worker", "ROLE_WORKER");
        Perfil contractor = profile("contractor", "ROLE_CONTRACTOR");
        when(perfilRepository.findByUsernameIgnoreCase("worker")).thenReturn(Optional.of(worker));
        when(perfilRepository.findById("contractor-id")).thenReturn(Optional.of(contractor));
        when(proyectoRepository.findByEquipoTrabajoContaining(worker)).thenReturn(List.of());

        ResponseEntity<?> response = controller.getContractorStatutesForWorker(
                "contractor-id", authentication("worker", "ROLE_WORKER"));

        assertEquals(404, response.getStatusCode().value());
        verify(gridFsTemplate, never()).findOne(org.mockito.ArgumentMatchers.any(Query.class));
    }

    private Perfil profile(String username, String role) {
        Perfil profile = new Perfil();
        profile.setId(username + "-id");
        profile.setUsername(username);
        profile.setRoles(Set.of(role));
        profile.setEstatutosUrl("507f1f77bcf86cd799439011");
        return profile;
    }

    private Authentication authentication(String username, String role) {
        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn(username);
        when(authentication.getAuthorities()).thenAnswer(invocation -> List.of(new SimpleGrantedAuthority(role)));
        return authentication;
    }
}
