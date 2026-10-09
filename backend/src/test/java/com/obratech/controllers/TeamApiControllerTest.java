package com.obratech.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import com.obratech.entity.EquipoTrabajo;
import com.obratech.entity.Perfil;
import com.obratech.entity.Proyecto;
import com.obratech.repository.EquipoTrabajoRepository;
import com.obratech.repository.PerfilRepository;
import com.obratech.repository.ProyectoRepository;

class TeamApiControllerTest {

    private PerfilRepository perfilRepository;
    private ProyectoRepository proyectoRepository;
    private EquipoTrabajoRepository equipoTrabajoRepository;
    private TeamApiController controller;

    @BeforeEach
    void setUp() {
        perfilRepository = mock(PerfilRepository.class);
        proyectoRepository = mock(ProyectoRepository.class);
        equipoTrabajoRepository = mock(EquipoTrabajoRepository.class);
        controller = new TeamApiController(perfilRepository, proyectoRepository, equipoTrabajoRepository);
    }

    @Test
    void addMemberAlreadyInTeamReturnsDuplicateMessageWithoutSaving() {
        Perfil contratista = new Perfil();
        contratista.setId("contractor-id");
        contratista.setUsername("contractor");

        Perfil trabajador = new Perfil();
        trabajador.setId("worker-id");
        trabajador.setNombre("Ana");

        Proyecto proyecto = new Proyecto();
        proyecto.setId("project-id");
        proyecto.setContratistaAsignado(contratista);
        proyecto.setEquipoTrabajo(List.of(trabajador));

        EquipoTrabajo equipo = new EquipoTrabajo();
        equipo.setId("team-id");
        equipo.setProyecto(proyecto);
        equipo.setIntegrantes(new ArrayList<>(List.of(trabajador)));

        when(perfilRepository.findByUsernameIgnoreCase("contractor")).thenReturn(Optional.of(contratista));
        when(proyectoRepository.findById("project-id")).thenReturn(Optional.of(proyecto));
        when(equipoTrabajoRepository.findById("team-id")).thenReturn(Optional.of(equipo));

        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn("contractor");

        ResponseEntity<?> response = controller.addMemberToTeam(
                "project-id",
                "team-id",
                new TeamApiController.MemberAddRequest("worker-id"),
                authentication);

        assertEquals(400, response.getStatusCode().value());
        TeamApiController.ErrorResponse error = (TeamApiController.ErrorResponse) response.getBody();
        assertTrue(error.error().contains("ya pertenece a este equipo"));
        verify(equipoTrabajoRepository, never()).save(equipo);
    }
}
