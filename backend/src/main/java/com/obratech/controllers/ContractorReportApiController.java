package com.obratech.controllers;

import java.time.LocalDateTime;
import java.util.ArrayList;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PutMapping;

import com.obratech.entity.Perfil;
import com.obratech.entity.Proyecto;
import com.obratech.entity.ReporteAvanceTrabajador;
import com.obratech.entity.ReporteProyecto;
import com.obratech.entity.EvidenciaAdjunta;
import com.obratech.repository.PerfilRepository;
import com.obratech.repository.ProyectoRepository;
import com.obratech.repository.ReporteAvanceTrabajadorRepository;
import com.obratech.repository.ReporteProyectoRepository;
import com.mongodb.client.gridfs.model.GridFSFile;

@RestController
@RequestMapping("/api/contractor/reports")
public class ContractorReportApiController {

    private static final int MAX_CONTENT_LENGTH = 3000;

    private final PerfilRepository perfilRepository;
    private final ProyectoRepository proyectoRepository;
    private final ReporteProyectoRepository reporteRepository;
    private final ReporteAvanceTrabajadorRepository avanceRepository;
    private final GridFsTemplate gridFsTemplate;

    public ContractorReportApiController(
            PerfilRepository perfilRepository,
            ProyectoRepository proyectoRepository,
            ReporteProyectoRepository reporteRepository,
            ReporteAvanceTrabajadorRepository avanceRepository,
            GridFsTemplate gridFsTemplate) {
        this.perfilRepository = perfilRepository;
        this.proyectoRepository = proyectoRepository;
        this.reporteRepository = reporteRepository;
        this.avanceRepository = avanceRepository;
        this.gridFsTemplate = gridFsTemplate;
    }

    @GetMapping
    public ResponseEntity<?> listReports(Authentication authentication) {
        Perfil contratista = perfilRepository.findByUsernameIgnoreCase(authentication.getName()).orElse(null);
        if (contratista == null || contratista.getId() == null) {
            return ResponseEntity.ok(List.of());
        }
        if (!Boolean.TRUE.equals(contratista.getVerificado())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Tu cuenta debe estar verificada para consultar informes."));
        }
        return ResponseEntity.ok(reporteRepository.findByContratistaIdOrderByCreadoDesc(contratista.getId())
                .stream().map(this::toResponse).toList());
    }

    @PostMapping
    public ResponseEntity<?> createReport(@RequestBody ReportRequest request, Authentication authentication) {
        if (request == null || isBlank(request.proyectoId()) || isBlank(request.contenido())) {
            return ResponseEntity.badRequest().body(Map.of("error", "Selecciona un proyecto y escribe el informe."));
        }
        String contenido = request.contenido().trim();
        if (contenido.length() < 10 || contenido.length() > MAX_CONTENT_LENGTH) {
            return ResponseEntity.badRequest().body(Map.of("error", "El informe debe tener entre 10 y 3000 caracteres."));
        }

        Perfil contratista = perfilRepository.findByUsernameIgnoreCase(authentication.getName()).orElse(null);
        Proyecto proyecto = proyectoRepository.findById(request.proyectoId()).orElse(null);
        if (contratista == null || contratista.getId() == null || proyecto == null
                || proyecto.getContratistaAsignado() == null
                || !contratista.getId().equals(proyecto.getContratistaAsignado().getId())) {
            return ResponseEntity.notFound().build();
        }
        if (!Boolean.TRUE.equals(contratista.getVerificado())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Tu cuenta debe estar verificada para enviar informes."));
        }

        List<String> avanceIds = request.reportesTrabajadorIds() == null ? List.of() : request.reportesTrabajadorIds();
        List<ReporteAvanceTrabajador> avances = avanceIds.isEmpty()
                ? List.of()
                : avanceRepository.findAllById(avanceIds).stream()
                        .filter(avance -> proyecto.getId().equals(avance.getProyectoId()))
                        .toList();
        if (avances.size() != avanceIds.stream().distinct().count()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Uno o más avances no pertenecen a este proyecto."));
        }

        ReporteProyecto reporte = new ReporteProyecto();
        reporte.setProyectoId(proyecto.getId());
        reporte.setProyectoTitulo(proyecto.getTitulo());
        reporte.setContratistaId(contratista.getId());
        reporte.setContratistaUsername(contratista.getUsername());
        reporte.setContenido(contenido);
        reporte.setCreado(LocalDateTime.now());
        reporte.setReportesTrabajadorIds(avances.stream().map(ReporteAvanceTrabajador::getId).toList());
        reporte.setAvancesTrabajador(avances);
        List<EvidenciaAdjunta> evidencias = new ArrayList<>();
        avances.forEach(avance -> avance.getEvidencias().forEach(evidence -> evidencias.add(
                new EvidenciaAdjunta(evidence.getFileId(), evidence.getFilename(), evidence.getContentType(), avance.getId()))));
        reporte.setEvidencias(evidencias);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(reporteRepository.save(reporte)));
    }

    @PutMapping("/{reportId}")
    public ResponseEntity<?> updateReport(
            @PathVariable String reportId,
            @RequestBody ReportEditRequest request,
            Authentication authentication) {
        String contenido = request == null || request.contenido() == null ? "" : request.contenido().trim();
        if (contenido.length() < 10 || contenido.length() > MAX_CONTENT_LENGTH) {
            return ResponseEntity.badRequest().body(Map.of("error", "El informe debe tener entre 10 y 3000 caracteres."));
        }
        Perfil contratista = perfilRepository.findByUsernameIgnoreCase(authentication.getName()).orElse(null);
        ReporteProyecto reporte = reporteRepository.findById(reportId).orElse(null);
        Proyecto proyecto = reporte == null ? null : proyectoRepository.findById(reporte.getProyectoId()).orElse(null);
        if (contratista == null || reporte == null || proyecto == null || proyecto.getContratistaAsignado() == null
                || !contratista.getId().equals(proyecto.getContratistaAsignado().getId())
                || !contratista.getId().equals(reporte.getContratistaId())) {
            return ResponseEntity.notFound().build();
        }
        if (!Boolean.TRUE.equals(contratista.getVerificado())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Tu cuenta debe estar verificada para editar informes."));
        }
        reporte.setContenido(contenido);
        return ResponseEntity.ok(toResponse(reporteRepository.save(reporte)));
    }

    @GetMapping("/{reportId}/evidence/{fileId}")
    public ResponseEntity<Resource> getEvidence(
            @PathVariable String reportId,
            @PathVariable String fileId,
            Authentication authentication) {
        ReporteProyecto reporte = reporteRepository.findById(reportId).orElse(null);
        Proyecto proyecto = reporte == null ? null : proyectoRepository.findById(reporte.getProyectoId()).orElse(null);
        Perfil viewer = perfilRepository.findByUsernameIgnoreCase(authentication.getName()).orElse(null);
        if (proyecto == null || viewer == null || !Boolean.TRUE.equals(viewer.getVerificado())
            || !canAccess(proyecto, authentication.getName())) {
            return ResponseEntity.notFound().build();
        }
        EvidenciaAdjunta evidence = reporte.getEvidencias().stream()
                .filter(item -> fileId.equals(item.getFileId()))
                .findFirst().orElse(null);
        if (evidence == null) {
            return ResponseEntity.notFound().build();
        }
        try {
            GridFSFile file = gridFsTemplate.findOne(new Query(Criteria.where("_id").is(new ObjectId(fileId))));
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

    private boolean canAccess(Proyecto proyecto, String username) {
        boolean client = proyecto.getCliente() != null && proyecto.getCliente().getUsername() != null
                && proyecto.getCliente().getUsername().equalsIgnoreCase(username);
        boolean contractor = proyecto.getContratistaAsignado() != null
                && proyecto.getContratistaAsignado().getUsername() != null
                && proyecto.getContratistaAsignado().getUsername().equalsIgnoreCase(username);
        return client || contractor;
    }

    private ReportResponse toResponse(ReporteProyecto reporte) {
        return new ReportResponse(reporte.getId(), reporte.getProyectoId(), reporte.getProyectoTitulo(),
            reporte.getContenido(), reporte.getCreado(),
            reporte.getAvancesTrabajador().stream().map(this::toProgressResponse).toList(),
            reporte.getEvidencias().stream().map(file -> toEvidenceResponse(reporte, file)).toList());
    }

        private WorkerProgressResponse toProgressResponse(ReporteAvanceTrabajador advance) {
        return new WorkerProgressResponse(advance.getId(), advance.getTrabajadorNombre(), advance.getContenido(),
            advance.getCreado(), advance.getEvidencias().stream()
                .map(file -> new EvidenceResponse(file.getFileId(), file.getFilename(), file.getContentType(), null))
                .toList());
        }

        private EvidenceResponse toEvidenceResponse(ReporteProyecto report, EvidenciaAdjunta file) {
        return new EvidenceResponse(file.getFileId(), file.getFilename(), file.getContentType(),
                    "/projects/" + report.getProyectoId() + "/reports/" + report.getId() + "/evidence/" + file.getFileId());
        }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    public record ReportRequest(String proyectoId, String contenido, List<String> reportesTrabajadorIds) {
        public ReportRequest(String proyectoId, String contenido) {
            this(proyectoId, contenido, List.of());
        }
    }

    public record ReportEditRequest(String contenido) {}

    public record ReportResponse(
            String id,
            String proyectoId,
            String proyecto,
            String contenido,
            LocalDateTime creado,
            List<WorkerProgressResponse> avancesTrabajador,
            List<EvidenceResponse> evidencias) {}

    public record WorkerProgressResponse(String id, String trabajador, String contenido, LocalDateTime creado,
            List<EvidenceResponse> evidencias) {}

    public record EvidenceResponse(String fileId, String filename, String contentType, String url) {}
}