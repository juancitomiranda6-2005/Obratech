package com.obratech.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;

import com.obratech.entity.Perfil;
import com.obratech.entity.Usuario;
import com.obratech.repository.PerfilRepository;
import com.obratech.repository.UsuarioRepository;

class WorkerProfileApiControllerTest {

    private UsuarioRepository usuarioRepository;
    private PerfilRepository perfilRepository;
    private GridFsTemplate gridFsTemplate;
    private WorkerProfileApiController controller;

    @BeforeEach
    void setUp() {
        usuarioRepository = mock(UsuarioRepository.class);
        perfilRepository = mock(PerfilRepository.class);
        gridFsTemplate = mock(GridFsTemplate.class);
        controller = new WorkerProfileApiController(usuarioRepository, perfilRepository, gridFsTemplate);
    }

    @Test
    void getProfileReturnsWorkerProfileData() {
        Usuario usuario = new Usuario();
        usuario.setUsername("trabajador@demo.com");
        usuario.setRoles(java.util.Set.of("ROLE_WORKER"));

        Perfil perfil = new Perfil();
        perfil.setUsername(usuario.getUsername());
        perfil.setNombre("Ana");
        perfil.setApellido("Pérez");
        perfil.setTelefono("3001234567");
        perfil.setOficio("Electricista");
        perfil.setExperiencia(5);
        perfil.setDisponibilidad(true);

        when(usuarioRepository.findByUsernameIgnoreCase(usuario.getUsername())).thenReturn(Optional.of(usuario));
        when(perfilRepository.findByUsernameIgnoreCase(usuario.getUsername())).thenReturn(Optional.of(perfil));

        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn(usuario.getUsername());

        ResponseEntity<?> response = controller.getProfile(authentication);

        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
    }
}
