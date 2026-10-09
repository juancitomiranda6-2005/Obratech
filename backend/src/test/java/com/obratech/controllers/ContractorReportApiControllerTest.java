package com.obratech.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import com.obratech.entity.Perfil;
import com.obratech.entity.Proyecto;
import com.obratech.entity.EvidenciaAdjunta;
import com.obratech.entity.ReporteAvanceTrabajador;
import com.obratech.entity.ReporteProyecto;
import com.obratech.repository.PerfilRepository;
import com.obratech.repository.ProyectoRepository;
import com.obratech.repository.ReporteAvanceTrabajadorRepository;
import com.obratech.repository.ReporteProyectoRepository;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.mockito.ArgumentCaptor;

@ExtendWith(MockitoExtension.class)
class ContractorReportApiControllerTest {

    @Mock
    private PerfilRepository perfilRepository;

    @Mock
    private ProyectoRepository proyectoRepository;

    @Mock
    private ReporteProyectoRepository reporteRepository;

    @Mock
    private ReporteAvanceTrabajadorRepository avanceRepository;

    @Mock
    private GridFsTemplate gridFsTemplate;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private ContractorReportApiController controller;

    @Test
    void createsReportOnlyForAssignedProject() {
        Perfil contractor = new Perfil();
        contractor.setId("contractor-1");
        contractor.setUsername("contractor@example.test");
        contractor.setVerificado(true);
        Perfil assignedContractor = new Perfil();
        assignedContractor.setId("contractor-1");
        Proyecto project = new Proyecto();
        project.setId("project-1");
        project.setTitulo("Remodelación");
        project.setContratistaAsignado(assignedContractor);
        when(authentication.getName()).thenReturn("contractor@example.test");
        when(perfilRepository.findByUsernameIgnoreCase("contractor@example.test")).thenReturn(Optional.of(contractor));
        when(proyectoRepository.findById("project-1")).thenReturn(Optional.of(project));
        when(reporteRepository.save(any(ReporteProyecto.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ResponseEntity<?> response = controller.createReport(
                new ContractorReportApiController.ReportRequest("project-1", "Se completó el replanteo."),
                authentication);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        verify(reporteRepository).save(any(ReporteProyecto.class));
    }

    @Test
    void rejectsReportForProjectAssignedToAnotherContractor() {
        Perfil contractor = new Perfil();
        contractor.setId("contractor-1");
        contractor.setVerificado(true);
        Perfil assignedContractor = new Perfil();
        assignedContractor.setId("contractor-2");
        Proyecto project = new Proyecto();
        project.setId("project-1");
        project.setContratistaAsignado(assignedContractor);
        when(authentication.getName()).thenReturn("contractor@example.test");
        when(perfilRepository.findByUsernameIgnoreCase("contractor@example.test")).thenReturn(Optional.of(contractor));
        when(proyectoRepository.findById("project-1")).thenReturn(Optional.of(project));

        ResponseEntity<?> response = controller.createReport(
                new ContractorReportApiController.ReportRequest("project-1", "Se completó el replanteo."),
                authentication);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void includesSelectedWorkerReportsAndEvidenceInClientReport() {
        Perfil contractor = new Perfil();
        contractor.setId("contractor-1");
        contractor.setUsername("contractor@example.test");
        contractor.setVerificado(true);
        Perfil assignedContractor = new Perfil();
        assignedContractor.setId("contractor-1");
        Proyecto project = new Proyecto();
        project.setId("project-1");
        project.setContratistaAsignado(assignedContractor);
        ReporteAvanceTrabajador advance = new ReporteAvanceTrabajador();
        advance.setId("worker-report-1");
        advance.setProyectoId("project-1");
        advance.setTrabajadorNombre("Trabajador Uno");
        advance.setContenido("Se instaló la estructura.");
        advance.setEvidencias(List.of(new EvidenciaAdjunta("file-1", "avance.jpg", "image/jpeg", null)));
        when(authentication.getName()).thenReturn("contractor@example.test");
        when(perfilRepository.findByUsernameIgnoreCase("contractor@example.test")).thenReturn(Optional.of(contractor));
        when(proyectoRepository.findById("project-1")).thenReturn(Optional.of(project));
        when(avanceRepository.findAllById(List.of("worker-report-1"))).thenReturn(List.of(advance));
        when(reporteRepository.save(any(ReporteProyecto.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ResponseEntity<?> response = controller.createReport(
                new ContractorReportApiController.ReportRequest(
                        "project-1", "Avance de la estructura.", List.of("worker-report-1")),
                authentication);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        ArgumentCaptor<ReporteProyecto> savedReport = forClass(ReporteProyecto.class);
        verify(reporteRepository).save(savedReport.capture());
        assertEquals(List.of("worker-report-1"), savedReport.getValue().getReportesTrabajadorIds());
        assertEquals("Se instaló la estructura.", savedReport.getValue().getAvancesTrabajador().get(0).getContenido());
        assertEquals("file-1", savedReport.getValue().getEvidencias().get(0).getFileId());
    }

    @Test
    void assignedVerifiedContractorCanEditOwnReport() {
        Perfil contractor = new Perfil();
        contractor.setId("contractor-1");
        contractor.setUsername("contractor@example.test");
        contractor.setVerificado(true);
        Perfil assignedContractor = new Perfil();
        assignedContractor.setId("contractor-1");
        Proyecto project = new Proyecto();
        project.setId("project-1");
        project.setContratistaAsignado(assignedContractor);
        ReporteProyecto report = new ReporteProyecto();
        report.setId("report-1");
        report.setProyectoId("project-1");
        report.setContratistaId("contractor-1");
        report.setContenido("Informe anterior de la obra.");
        when(authentication.getName()).thenReturn("contractor@example.test");
        when(perfilRepository.findByUsernameIgnoreCase("contractor@example.test")).thenReturn(Optional.of(contractor));
        when(reporteRepository.findById("report-1")).thenReturn(Optional.of(report));
        when(proyectoRepository.findById("project-1")).thenReturn(Optional.of(project));
        when(reporteRepository.save(any(ReporteProyecto.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ResponseEntity<?> response = controller.updateReport(
                "report-1",
                new ContractorReportApiController.ReportEditRequest("Informe corregido de la obra."),
                authentication);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Informe corregido de la obra.", report.getContenido());
        verify(reporteRepository).save(report);
    }

    @Test
    void unverifiedContractorCannotCreateClientReport() {
        Perfil contractor = new Perfil();
        contractor.setId("contractor-1");
        contractor.setUsername("contractor@example.test");
        contractor.setVerificado(false);
        Perfil assignedContractor = new Perfil();
        assignedContractor.setId("contractor-1");
        Proyecto project = new Proyecto();
        project.setId("project-1");
        project.setContratistaAsignado(assignedContractor);
        when(authentication.getName()).thenReturn("contractor@example.test");
        when(perfilRepository.findByUsernameIgnoreCase("contractor@example.test")).thenReturn(Optional.of(contractor));
        when(proyectoRepository.findById("project-1")).thenReturn(Optional.of(project));

        ResponseEntity<?> response = controller.createReport(
                new ContractorReportApiController.ReportRequest("project-1", "Avance terminado de obra."),
                authentication);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        verify(reporteRepository, org.mockito.Mockito.never()).save(any(ReporteProyecto.class));
    }
}