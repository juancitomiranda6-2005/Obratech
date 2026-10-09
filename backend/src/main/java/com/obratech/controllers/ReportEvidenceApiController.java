package com.obratech.controllers;

import org.bson.types.ObjectId;
import org.springframework.core.io.Resource;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mongodb.client.gridfs.model.GridFSFile;
import com.obratech.entity.EvidenciaAdjunta;
import com.obratech.entity.EquipoTrabajo;
import com.obratech.entity.Perfil;
import com.obratech.entity.Proyecto;
import com.obratech.entity.ReporteAvanceTrabajador;
import com.obratech.entity.ReporteProyecto;
import com.obratech.entity.Usuario;
import com.obratech.repository.EquipoTrabajoRepository;
import com.obratech.repository.PerfilRepository;
import com.obratech.repository.ProyectoRepository;
import com.obratech.repository.ReporteAvanceTrabajadorRepository;
import com.obratech.repository.ReporteProyectoRepository;
import com.obratech.repository.UsuarioRepository;

@RestController
@RequestMapping("/api/projects")
public class ReportEvidenceApiController {

    private final ProyectoRepository proyectoRepository;
    private final UsuarioRepository usuarioRepository;
    private final PerfilRepository perfilRepository;
    private final EquipoTrabajoRepository equipoTrabajoRepository;
    private final ReporteAvanceTrabajadorRepository avanceRepository;
    private final ReporteProyectoRepository reporteRepository;
    private final GridFsTemplate gridFsTemplate;

    public ReportEvidenceApiController(
            ProyectoRepository proyectoRepository,
            UsuarioRepository usuarioRepository,
            PerfilRepository perfilRepository,
            EquipoTrabajoRepository equipoTrabajoRepository,
            ReporteAvanceTrabajadorRepository avanceRepository,
            ReporteProyectoRepository reporteRepository,
            GridFsTemplate gridFsTemplate) {
        this.proyectoRepository = proyectoRepository;
        this.usuarioRepository = usuarioRepository;
        this.perfilRepository = perfilRepository;
        this.equipoTrabajoRepository = equipoTrabajoRepository;
        this.avanceRepository = avanceRepository;
        this.reporteRepository = reporteRepository;
        this.gridFsTemplate = gridFsTemplate;
    }

    @GetMapping("/{projectId}/progress-reports/{reportId}/evidence/{fileId}")
    public ResponseEntity<Resource> getWorkerEvidence(
            @PathVariable String projectId,
            @PathVariable String reportId,
            @PathVariable String fileId,
            Authentication authentication) {
        Proyecto project = proyectoRepository.findById(projectId).orElse(null);
        ReporteAvanceTrabajador report = avanceRepository.findById(reportId).orElse(null);
        Perfil viewer = perfilRepository.findByUsernameIgnoreCase(authentication.getName()).orElse(null);
        if (project == null || report == null || viewer == null || !Boolean.TRUE.equals(viewer.getVerificado())
            || !projectId.equals(report.getProyectoId())) {
            return ResponseEntity.notFound().build();
        }
        EvidenciaAdjunta evidence = report.getEvidencias().stream()
                .filter(file -> fileId.equals(file.getFileId()))
                .findFirst().orElse(null);
        EquipoTrabajo team = equipoTrabajoRepository.findById(report.getEquipoId()).orElse(null);
        boolean isTeamMember = team != null && team.getIntegrantes() != null
                && team.getIntegrantes().stream().anyMatch(member -> viewer.getId().equals(member.getId()));
        boolean isAssignedContractor = project.getContratistaAsignado() != null
                && viewer.getId().equals(project.getContratistaAsignado().getId());
        if (evidence == null || (!isTeamMember && !isAssignedContractor)) {
            return ResponseEntity.notFound().build();
        }
        return readEvidence(evidence);
    }

    @GetMapping("/{projectId}/reports/{reportId}/evidence/{fileId}")
    public ResponseEntity<Resource> getClientReportEvidence(
            @PathVariable String projectId,
            @PathVariable String reportId,
            @PathVariable String fileId,
            Authentication authentication) {
        Proyecto project = proyectoRepository.findById(projectId).orElse(null);
        Usuario viewer = usuarioRepository.findByUsernameIgnoreCase(authentication.getName()).orElse(null);
        ReporteProyecto report = reporteRepository.findById(reportId).orElse(null);
        if (project == null || viewer == null || report == null || !projectId.equals(report.getProyectoId())
                || !canAccessProject(project, viewer)) {
            return ResponseEntity.notFound().build();
        }
        EvidenciaAdjunta evidence = report.getEvidencias().stream()
                .filter(file -> fileId.equals(file.getFileId()))
                .findFirst().orElse(null);
        if (evidence == null) {
            return ResponseEntity.notFound().build();
        }
        return readEvidence(evidence);
    }

    private boolean canAccessProject(Proyecto project, Usuario viewer) {
        if (!viewer.isVerificado()) {
            return false;
        }
        boolean client = project.getCliente() != null && project.getCliente().getId() != null
                && project.getCliente().getId().equals(viewer.getId());
        boolean contractor = project.getContratistaAsignado() != null
                && project.getContratistaAsignado().getUsername() != null
                && project.getContratistaAsignado().getUsername().equalsIgnoreCase(viewer.getUsername());
        boolean administrator = viewer.getRoles() != null && viewer.getRoles().contains("ROLE_ADMIN");
        return client || contractor || administrator;
    }

    private ResponseEntity<Resource> readEvidence(EvidenciaAdjunta evidence) {
        try {
            GridFSFile file = gridFsTemplate.findOne(new Query(Criteria.where("_id").is(new ObjectId(evidence.getFileId()))));
            if (file == null) {
                return ResponseEntity.notFound().build();
            }
            GridFsResource resource = gridFsTemplate.getResource(file);
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(evidence.getContentType()))
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            ContentDisposition.inline().filename(evidence.getFilename()).build().toString())
                    .body(resource);
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.notFound().build();
        }
    }
}