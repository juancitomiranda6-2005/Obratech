package com.obratech.controllers;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.bson.types.ObjectId;
import org.springframework.core.io.Resource;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.mongodb.client.gridfs.model.GridFSFile;
import com.obratech.entity.EvidenciaAdjunta;
import com.obratech.entity.EquipoTrabajo;
import com.obratech.entity.Perfil;
import com.obratech.entity.Proyecto;
import com.obratech.entity.ReporteAvanceTrabajador;
import com.obratech.repository.EquipoTrabajoRepository;
import com.obratech.repository.PerfilRepository;
import com.obratech.repository.ProyectoRepository;
import com.obratech.repository.ReporteAvanceTrabajadorRepository;
import com.obratech.util.PerfilDisplayName;

@RestController
@RequestMapping("/api")
public class WorkerProgressReportApiController {

    private static final int MAX_CONTENT_LENGTH = 3000;
    private static final int MAX_FILES = 5;
    private static final int EDIT_WINDOW_MINUTES = 30;
    private static final long MAX_FILE_SIZE = 10L * 1024 * 1024;
    private static final List<String> ALLOWED_TYPES = List.of("image/jpeg", "image/png", "image/webp", "application/pdf");

    private final PerfilRepository perfilRepository;
    private final EquipoTrabajoRepository equipoTrabajoRepository;
    private final ProyectoRepository proyectoRepository;
    private final ReporteAvanceTrabajadorRepository reporteRepository;
    private final GridFsTemplate gridFsTemplate;

    public WorkerProgressReportApiController(
            PerfilRepository perfilRepository,
            EquipoTrabajoRepository equipoTrabajoRepository,
            ProyectoRepository proyectoRepository,
            ReporteAvanceTrabajadorRepository reporteRepository,
            GridFsTemplate gridFsTemplate) {
        this.perfilRepository = perfilRepository;
        this.equipoTrabajoRepository = equipoTrabajoRepository;
        this.proyectoRepository = proyectoRepository;
        this.reporteRepository = reporteRepository;
        this.gridFsTemplate = gridFsTemplate;
    }

    @PostMapping(value = "/worker/teams/{teamId}/progress-reports", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> createReport(
            @PathVariable String teamId,
            @RequestParam String contenido,
            @RequestParam(required = false) List<MultipartFile> evidencias,
            Authentication authentication) {
        String text = contenido == null ? "" : contenido.trim();
        if (text.length() < 10 || text.length() > MAX_CONTENT_LENGTH) {
            return ResponseEntity.badRequest().body(Map.of("error", "El avance debe tener entre 10 y 3000 caracteres."));
        }
        String fileError = validateFiles(evidencias);
        if (fileError != null) {
            return ResponseEntity.badRequest().body(Map.of("error", fileError));
        }

        Perfil trabajador = perfilRepository.findByUsernameIgnoreCase(authentication.getName()).orElse(null);
        EquipoTrabajo equipo = equipoTrabajoRepository.findById(teamId).orElse(null);
        if (!isTeamMember(trabajador, equipo) || equipo.getProyecto() == null || equipo.getProyecto().getId() == null) {
            return ResponseEntity.notFound().build();
        }
        if (!Boolean.TRUE.equals(trabajador.getVerificado())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Tu cuenta debe estar verificada para enviar avances."));
        }

        ReporteAvanceTrabajador reporte = new ReporteAvanceTrabajador();
        reporte.setEquipoId(equipo.getId());
        reporte.setProyectoId(equipo.getProyecto().getId());
        reporte.setTrabajadorId(trabajador.getId());
        reporte.setTrabajadorUsername(trabajador.getUsername());
        reporte.setTrabajadorNombre(PerfilDisplayName.of(trabajador));
        reporte.setContenido(text);
        reporte.setCreado(LocalDateTime.now());
        try {
            List<EvidenciaAdjunta> storedEvidence = new java.util.ArrayList<>();
            for (MultipartFile file : evidencias) {
                storedEvidence.add(storeEvidence(file));
            }
            reporte.setEvidencias(storedEvidence);
            return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(reporteRepository.save(reporte)));
        } catch (IOException exception) {
            return ResponseEntity.internalServerError().body(Map.of("error", "No se pudieron guardar las evidencias."));
        }
    }

    @GetMapping("/worker/teams/{teamId}/progress-reports")
    public ResponseEntity<?> listOwnReports(@PathVariable String teamId, Authentication authentication) {
        Perfil trabajador = perfilRepository.findByUsernameIgnoreCase(authentication.getName()).orElse(null);
        EquipoTrabajo equipo = equipoTrabajoRepository.findById(teamId).orElse(null);
        if (!isTeamMember(trabajador, equipo)) {
            return ResponseEntity.notFound().build();
        }
        if (!Boolean.TRUE.equals(trabajador.getVerificado())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Tu cuenta debe estar verificada para consultar avances."));
        }
        return ResponseEntity.ok(reporteRepository
                .findByEquipoIdAndTrabajadorIdOrderByCreadoDesc(teamId, trabajador.getId())
                .stream().map(this::toResponse).toList());
    }

    @PutMapping("/worker/teams/{teamId}/progress-reports/{reportId}")
    public ResponseEntity<?> updateOwnReport(
            @PathVariable String teamId,
            @PathVariable String reportId,
            @RequestBody ReportUpdateRequest request,
            Authentication authentication) {
        String text = request == null || request.contenido() == null ? "" : request.contenido().trim();
        if (text.length() < 10 || text.length() > MAX_CONTENT_LENGTH) {
            return ResponseEntity.badRequest().body(Map.of("error", "El avance debe tener entre 10 y 3000 caracteres."));
        }
        Perfil trabajador = perfilRepository.findByUsernameIgnoreCase(authentication.getName()).orElse(null);
        if (trabajador == null || trabajador.getId() == null || !Boolean.TRUE.equals(trabajador.getVerificado())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Tu cuenta debe estar verificada para editar avances."));
        }
        ReporteAvanceTrabajador reporte = reporteRepository
                .findByIdAndEquipoIdAndTrabajadorId(reportId, teamId, trabajador.getId()).orElse(null);
        if (reporte == null) {
            return ResponseEntity.notFound().build();
        }
        if (reporte.getCreado() == null
                || !LocalDateTime.now().isBefore(reporte.getCreado().plusMinutes(EDIT_WINDOW_MINUTES))) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "El plazo de edición de 30 minutos para este avance terminó."));
        }
        reporte.setContenido(text);
        return ResponseEntity.ok(toResponse(reporteRepository.save(reporte)));
    }

    @GetMapping("/contractor/projects/{projectId}/progress-reports")
    public ResponseEntity<?> listProjectReports(@PathVariable String projectId, Authentication authentication) {
        Perfil contratista = perfilRepository.findByUsernameIgnoreCase(authentication.getName()).orElse(null);
        Proyecto proyecto = proyectoRepository.findById(projectId).orElse(null);
        if (contratista == null || contratista.getId() == null || proyecto == null
                || proyecto.getContratistaAsignado() == null
                || !contratista.getId().equals(proyecto.getContratistaAsignado().getId())) {
            return ResponseEntity.notFound().build();
        }
        if (!Boolean.TRUE.equals(contratista.getVerificado())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Tu cuenta debe estar verificada para consultar avances."));
        }
        return ResponseEntity.ok(reporteRepository.findByProyectoIdOrderByCreadoDesc(projectId).stream()
                .map(this::toResponse)
                .toList());
    }

    @GetMapping("/worker/teams/{teamId}/progress-reports/{reportId}/evidence/{fileId}")
    public ResponseEntity<Resource> getEvidence(
            @PathVariable String teamId,
            @PathVariable String reportId,
            @PathVariable String fileId,
            Authentication authentication) {
        Perfil viewer = perfilRepository.findByUsernameIgnoreCase(authentication.getName()).orElse(null);
        EquipoTrabajo team = equipoTrabajoRepository.findById(teamId).orElse(null);
        ReporteAvanceTrabajador report = reporteRepository.findByIdAndEquipoId(reportId, teamId).orElse(null);
        if (viewer == null || !Boolean.TRUE.equals(viewer.getVerificado())
            || team == null || report == null || !canViewEvidence(viewer, team)) {
            return ResponseEntity.notFound().build();
        }
        EvidenciaAdjunta evidence = report.getEvidencias().stream()
                .filter(item -> fileId.equals(item.getFileId()))
                .findFirst().orElse(null);
        if (evidence == null) {
            return ResponseEntity.notFound().build();
        }
        return readEvidence(evidence);
    }

    private boolean isTeamMember(Perfil profile, EquipoTrabajo team) {
        return profile != null && profile.getId() != null && team != null && team.getIntegrantes() != null
                && team.getIntegrantes().stream().anyMatch(member -> profile.getId().equals(member.getId()));
    }

    private boolean canViewEvidence(Perfil viewer, EquipoTrabajo team) {
        if (isTeamMember(viewer, team)) {
            return true;
        }
        Proyecto project = team.getProyecto();
        return project != null && project.getContratistaAsignado() != null
                && viewer.getId().equals(project.getContratistaAsignado().getId());
    }

    private String validateFiles(List<MultipartFile> files) {
        if (files == null || files.isEmpty() || files.size() > MAX_FILES) {
            return "Adjunta entre 1 y 5 evidencias (fotos o PDF).";
        }
        for (MultipartFile file : files) {
            if (file == null || file.isEmpty() || file.getSize() > MAX_FILE_SIZE
                    || !ALLOWED_TYPES.contains(file.getContentType())) {
                return "Cada evidencia debe ser una foto JPG, PNG, WebP o PDF de hasta 10 MB.";
            }
        }
        return null;
    }

    private EvidenciaAdjunta storeEvidence(MultipartFile file) throws IOException {
        String originalName = file.getOriginalFilename() == null ? "evidencia" : file.getOriginalFilename();
        String filename = originalName.replace('\\', '/');
        filename = filename.substring(filename.lastIndexOf('/') + 1);
        ObjectId fileId = gridFsTemplate.store(file.getInputStream(), filename, file.getContentType());
        return new EvidenciaAdjunta(fileId.toHexString(), filename, file.getContentType(), null);
    }

    private WorkerReportResponse toResponse(ReporteAvanceTrabajador report) {
        List<EvidenceResponse> evidence = report.getEvidencias().stream()
                .map(file -> new EvidenceResponse(
                        file.getFileId(),
                        file.getFilename(),
                        file.getContentType(),
                    "/projects/" + report.getProyectoId() + "/progress-reports/" + report.getId()
                                + "/evidence/" + file.getFileId()))
                .toList();
        return new WorkerReportResponse(report.getId(), report.getProyectoId(), report.getEquipoId(),
            report.getTrabajadorNombre(), report.getContenido(), report.getCreado(),
                report.getCreado() == null ? null : report.getCreado().plusMinutes(EDIT_WINDOW_MINUTES),
                isEditable(report), evidence);
    }

    private boolean isEditable(ReporteAvanceTrabajador report) {
        return report.getCreado() != null
                && LocalDateTime.now().isBefore(report.getCreado().plusMinutes(EDIT_WINDOW_MINUTES));
    }

    private ResponseEntity<Resource> readEvidence(EvidenciaAdjunta evidence) {
        try {
            GridFSFile file = gridFsTemplate.findOne(new Query(Criteria.where("_id").is(new ObjectId(evidence.getFileId()))));
            if (file == null) {
                return ResponseEntity.notFound().build();
            }
            GridFsResource resource = gridFsTemplate.getResource(file);
            MediaType type = MediaType.parseMediaType(evidence.getContentType());
            return ResponseEntity.ok()
                    .contentType(type)
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            ContentDisposition.inline().filename(evidence.getFilename()).build().toString())
                    .body(resource);
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.notFound().build();
        }
    }

    public record WorkerReportResponse(
            String id,
            String proyectoId,
            String equipoId,
            String trabajador,
            String contenido,
            LocalDateTime creado,
            LocalDateTime editableHasta,
            boolean editable,
            List<EvidenceResponse> evidencias) {}

    public record EvidenceResponse(String fileId, String filename, String contentType, String url) {}

    public record ReportUpdateRequest(String contenido) {}
}