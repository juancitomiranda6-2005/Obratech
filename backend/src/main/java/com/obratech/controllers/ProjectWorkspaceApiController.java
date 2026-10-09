package com.obratech.controllers;

import java.time.LocalDate;
import java.util.List;

import org.bson.types.ObjectId;
import org.springframework.core.io.Resource;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.obratech.entity.EquipoTrabajo;
import com.obratech.entity.Perfil;
import com.obratech.entity.Proyecto;
import com.obratech.entity.Usuario;
import com.obratech.repository.EquipoTrabajoRepository;
import com.obratech.repository.PerfilRepository;
import com.obratech.repository.PostulacionRepository;
import com.obratech.repository.ProyectoRepository;
import com.obratech.repository.UsuarioRepository;
import com.obratech.util.PerfilDisplayName;

@RestController
@RequestMapping("/api/projects")
public class ProjectWorkspaceApiController {

    private final ProyectoRepository proyectoRepository;
    private final UsuarioRepository usuarioRepository;
    private final PerfilRepository perfilRepository;
    private final PostulacionRepository postulacionRepository;
    private final EquipoTrabajoRepository equipoTrabajoRepository;
        private final GridFsTemplate gridFsTemplate;

    public ProjectWorkspaceApiController(
            ProyectoRepository proyectoRepository,
            UsuarioRepository usuarioRepository,
            PerfilRepository perfilRepository,
            PostulacionRepository postulacionRepository,
            EquipoTrabajoRepository equipoTrabajoRepository,
            GridFsTemplate gridFsTemplate) {
        this.proyectoRepository = proyectoRepository;
        this.usuarioRepository = usuarioRepository;
        this.perfilRepository = perfilRepository;
        this.postulacionRepository = postulacionRepository;
        this.equipoTrabajoRepository = equipoTrabajoRepository;
        this.gridFsTemplate = gridFsTemplate;
    }

    @GetMapping("/{projectId}/workspace")
    public ResponseEntity<?> getWorkspace(@PathVariable String projectId, Authentication authentication) {
        Proyecto project = proyectoRepository.findById(projectId).orElse(null);
        Usuario user = usuarioRepository.findByUsernameIgnoreCase(authentication.getName()).orElse(null);
        if (project == null || user == null) {
            return ResponseEntity.notFound().build();
        }

        boolean owner = project.getCliente() != null
                && project.getCliente().getUsername() != null
                && project.getCliente().getUsername().equalsIgnoreCase(user.getUsername());
        boolean assignedContractor = project.getContratistaAsignado() != null
                && project.getContratistaAsignado().getUsername() != null
                && project.getContratistaAsignado().getUsername().equalsIgnoreCase(user.getUsername());
        boolean administrator = user.getRoles() != null && user.getRoles().contains("ROLE_ADMIN");
        if (!owner && !assignedContractor && !administrator) {
            return ResponseEntity.notFound().build();
        }

        List<EquipoTrabajo> teams = equipoTrabajoRepository.findByProyectoId(projectId);
        double progress = teams.stream()
                .map(EquipoTrabajo::getPorcentajeAvance)
                .filter(java.util.Objects::nonNull)
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);
        Perfil contractor = project.getContratistaAsignado();
        return ResponseEntity.ok(new ProjectWorkspace(
                project.getId(), project.getTitulo(), project.getDescripcion(), project.getTipoProyecto(),
                project.getUbicacion(), project.getPresupuesto(),
                project.getEstadoValidacion() == null ? "PENDIENTE" : project.getEstadoValidacion().name(),
                project.getEstadoEjecucion() == null ? "PENDIENTE" : project.getEstadoEjecucion().name(),
                project.getFechaInicio(), project.getFechaEntrega(), project.getPlazoEstimado(),
                project.getAreaTotal(), project.getObservaciones(), project.getDocumentoLegalUrl(),
                project.getDocumentoLegalNombre(), postulacionRepository.findByProyectoId(projectId).size(),
                project.getEquipoTrabajo() == null ? 0 : project.getEquipoTrabajo().size(),
                progress, owner, assignedContractor, administrator,
                contractor == null ? null : new ContractorCard(
                        contractor.getId(), PerfilDisplayName.of(contractor), contractor.getEspecialidad(),
                        contractor.getCalificacionPromedio() == null ? 0.0 : contractor.getCalificacionPromedio(),
                        Boolean.TRUE.equals(contractor.getVerificado())),
                project.getEquipoTrabajo() == null ? List.of() : project.getEquipoTrabajo().stream()
                        .map(this::toMemberCard).toList(),
                teams.stream().map(this::toTeamCard).toList()));
    }

        @GetMapping("/{projectId}/document")
        public ResponseEntity<Resource> getLegalDocument(
                        @PathVariable String projectId,
                        @org.springframework.web.bind.annotation.RequestParam(defaultValue = "false") boolean download,
                        Authentication authentication) {
                Proyecto project = proyectoRepository.findById(projectId).orElse(null);
                Usuario user = usuarioRepository.findByUsernameIgnoreCase(authentication.getName()).orElse(null);
                if (!canAccess(project, user)) {
                        return ResponseEntity.notFound().build();
                }
                if (project.getDocumentoLegalUrl() == null || project.getDocumentoLegalUrl().isBlank()) {
                        return ResponseEntity.notFound().build();
                }

                try {
                        var file = gridFsTemplate.findOne(new Query(Criteria.where("_id").is(new ObjectId(project.getDocumentoLegalUrl()))));
                        if (file == null) {
                                return ResponseEntity.notFound().build();
                        }
                        GridFsResource resource = gridFsTemplate.getResource(file);
                        String contentType = file.getMetadata() == null ? null : file.getMetadata().getString("_contentType");
                        MediaType mediaType = contentType == null ? MediaType.APPLICATION_OCTET_STREAM : MediaType.parseMediaType(contentType);
                        String filename = project.getDocumentoLegalNombre() == null
                                        ? file.getFilename() : project.getDocumentoLegalNombre();
                        ContentDisposition disposition = download
                                        ? ContentDisposition.attachment().filename(filename).build()
                                        : ContentDisposition.inline().filename(filename).build();
                        return ResponseEntity.ok()
                                        .contentType(mediaType)
                                        .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                                        .body(resource);
                } catch (IllegalArgumentException exception) {
                        return ResponseEntity.notFound().build();
                }
        }

        private boolean canAccess(Proyecto project, Usuario user) {
                if (project == null || user == null) return false;
                boolean owner = project.getCliente() != null && project.getCliente().getUsername() != null
                                && project.getCliente().getUsername().equalsIgnoreCase(user.getUsername());
                boolean assignedContractor = project.getContratistaAsignado() != null
                                && project.getContratistaAsignado().getUsername() != null
                                && project.getContratistaAsignado().getUsername().equalsIgnoreCase(user.getUsername());
                boolean administrator = user.getRoles() != null && user.getRoles().contains("ROLE_ADMIN");
                return owner || assignedContractor || administrator;
        }

    private MemberCard toMemberCard(Perfil profile) {
        return new MemberCard(profile.getId(), PerfilDisplayName.of(profile), profile.getOficio(),
                Boolean.TRUE.equals(profile.getVerificado()));
    }

    private TeamCard toTeamCard(EquipoTrabajo team) {
        return new TeamCard(team.getId(), team.getNombre(), team.getActividad(),
                team.getPorcentajeAvance() == null ? 0.0 : team.getPorcentajeAvance(),
                team.getIntegrantes() == null ? List.of() : team.getIntegrantes().stream().map(this::toMemberCard).toList());
    }

    public record ProjectWorkspace(
            String id,
            String titulo,
            String descripcion,
            String tipoProyecto,
            String ubicacion,
            Double presupuesto,
            String estadoValidacion,
            String estadoEjecucion,
            LocalDate fechaInicio,
            LocalDate fechaEntrega,
            Integer plazoEstimado,
            Double areaTotal,
            String observaciones,
            String documentoLegalUrl,
            String documentoLegalNombre,
            int totalPostulantes,
            int miembrosEquipo,
            double progresoProyecto,
            boolean propietario,
            boolean contratistaAsignado,
            boolean administrador,
            ContractorCard contratista,
            List<MemberCard> poolTrabajadores,
            List<TeamCard> equipos) {}

    public record ContractorCard(String id, String nombre, String especialidad, double calificacionPromedio, boolean verificado) {}

    public record MemberCard(String id, String nombre, String oficio, boolean verificado) {}

    public record TeamCard(String id, String nombre, String actividad, double porcentajeAvance, List<MemberCard> integrantes) {}
}