package com.obratech.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.Authentication;

import com.obratech.entity.EquipoTrabajo;
import com.obratech.entity.Perfil;
import com.obratech.entity.Proyecto;
import com.obratech.entity.ReporteAvanceTrabajador;
import com.obratech.repository.EquipoTrabajoRepository;
import com.obratech.repository.PerfilRepository;
import com.obratech.repository.ProyectoRepository;
import com.obratech.repository.ReporteAvanceTrabajadorRepository;

@ExtendWith(MockitoExtension.class)
class WorkerProgressReportApiControllerTest {

    @Mock
    private PerfilRepository perfilRepository;

    @Mock
    private EquipoTrabajoRepository equipoTrabajoRepository;

    @Mock
    private ProyectoRepository proyectoRepository;

    @Mock
    private ReporteAvanceTrabajadorRepository reporteRepository;

    @Mock
    private GridFsTemplate gridFsTemplate;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private WorkerProgressReportApiController controller;

    @Test
    void rejectsProgressReportFromWorkerOutsideTeam() {
        Perfil worker = new Perfil();
        worker.setId("worker-1");
        Proyecto project = new Proyecto();
        project.setId("project-1");
        EquipoTrabajo team = new EquipoTrabajo();
        team.setId("team-1");
        team.setProyecto(project);
        Perfil anotherWorker = new Perfil();
        anotherWorker.setId("worker-2");
        team.setIntegrantes(List.of(anotherWorker));
        when(authentication.getName()).thenReturn("worker@example.test");
        when(perfilRepository.findByUsernameIgnoreCase("worker@example.test")).thenReturn(Optional.of(worker));
        when(equipoTrabajoRepository.findById("team-1")).thenReturn(Optional.of(team));

        ResponseEntity<?> response = controller.createReport(
                "team-1",
                "Avance de estructura terminado.",
                List.of(new MockMultipartFile("evidencias", "avance.jpg", "image/jpeg", new byte[] { 1 })),
                authentication);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        verifyNoInteractions(gridFsTemplate, reporteRepository);
    }

    @Test
    void rejectsUnsupportedEvidenceType() {
        ResponseEntity<?> response = controller.createReport(
                "team-1",
                "Avance de estructura terminado.",
                List.of(new MockMultipartFile("evidencias", "avance.txt", "text/plain", new byte[] { 1 })),
                authentication);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verifyNoInteractions(perfilRepository, equipoTrabajoRepository, gridFsTemplate, reporteRepository);
    }

    @Test
    void workerCanEditOnlyOwnProgressReportText() {
        Perfil worker = new Perfil();
        worker.setId("worker-1");
        worker.setVerificado(true);
        ReporteAvanceTrabajador report = new ReporteAvanceTrabajador();
        report.setId("report-1");
        report.setEquipoId("team-1");
        report.setTrabajadorId("worker-1");
        report.setCreado(LocalDateTime.now().minusMinutes(5));
        report.setContenido("Texto anterior del avance.");
        when(authentication.getName()).thenReturn("worker@example.test");
        when(perfilRepository.findByUsernameIgnoreCase("worker@example.test")).thenReturn(Optional.of(worker));
        when(reporteRepository.findByIdAndEquipoIdAndTrabajadorId("report-1", "team-1", "worker-1"))
                .thenReturn(Optional.of(report));
        when(reporteRepository.save(any(ReporteAvanceTrabajador.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ResponseEntity<?> response = controller.updateOwnReport(
                "team-1", "report-1",
                new WorkerProgressReportApiController.ReportUpdateRequest("Texto corregido del avance."),
                authentication);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Texto corregido del avance.", report.getContenido());
        verify(reporteRepository).save(report);
    }

    @Test
    void rejectsEditingProgressReportAfterThirtyMinutes() {
        Perfil worker = new Perfil();
        worker.setId("worker-1");
        worker.setVerificado(true);
        ReporteAvanceTrabajador report = new ReporteAvanceTrabajador();
        report.setId("report-1");
        report.setEquipoId("team-1");
        report.setTrabajadorId("worker-1");
        report.setCreado(LocalDateTime.now().minusMinutes(31));
        when(authentication.getName()).thenReturn("worker@example.test");
        when(perfilRepository.findByUsernameIgnoreCase("worker@example.test")).thenReturn(Optional.of(worker));
        when(reporteRepository.findByIdAndEquipoIdAndTrabajadorId("report-1", "team-1", "worker-1"))
                .thenReturn(Optional.of(report));

        ResponseEntity<?> response = controller.updateOwnReport(
                "team-1", "report-1",
                new WorkerProgressReportApiController.ReportUpdateRequest("Texto corregido del avance."),
                authentication);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        verifyNoInteractions(gridFsTemplate);
    }

    @Test
    void unverifiedWorkerCannotSendProgressReport() {
        Perfil worker = new Perfil();
        worker.setId("worker-1");
        worker.setVerificado(false);
        Proyecto project = new Proyecto();
        project.setId("project-1");
        EquipoTrabajo team = new EquipoTrabajo();
        team.setId("team-1");
        team.setProyecto(project);
        team.setIntegrantes(List.of(worker));
        when(authentication.getName()).thenReturn("worker@example.test");
        when(perfilRepository.findByUsernameIgnoreCase("worker@example.test")).thenReturn(Optional.of(worker));
        when(equipoTrabajoRepository.findById("team-1")).thenReturn(Optional.of(team));

        ResponseEntity<?> response = controller.createReport(
                "team-1",
                "Avance de estructura terminado.",
                List.of(new MockMultipartFile("evidencias", "avance.jpg", "image/jpeg", new byte[] { 1 })),
                authentication);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        verifyNoInteractions(gridFsTemplate, reporteRepository);
    }
}