package com.obratech.controllers;

import org.bson.types.ObjectId;
import org.springframework.core.io.Resource;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
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
import com.obratech.entity.Perfil;
import com.obratech.entity.Proyecto;
import com.obratech.entity.Usuario;
import com.obratech.repository.PerfilRepository;
import com.obratech.repository.PostulacionRepository;
import com.obratech.repository.ProyectoRepository;
import com.obratech.repository.UsuarioRepository;

@RestController
@RequestMapping("/api")
public class ProfileDocumentsApiController {

    private final PerfilRepository perfilRepository;
    private final UsuarioRepository usuarioRepository;
    private final ProyectoRepository proyectoRepository;
    private final PostulacionRepository postulacionRepository;
    private final GridFsTemplate gridFsTemplate;

    public ProfileDocumentsApiController(
            PerfilRepository perfilRepository,
            UsuarioRepository usuarioRepository,
            ProyectoRepository proyectoRepository,
            PostulacionRepository postulacionRepository,
            GridFsTemplate gridFsTemplate) {
        this.perfilRepository = perfilRepository;
        this.usuarioRepository = usuarioRepository;
        this.proyectoRepository = proyectoRepository;
        this.postulacionRepository = postulacionRepository;
        this.gridFsTemplate = gridFsTemplate;
    }

    @GetMapping("/client/contractors/{contractorId}/documents/{documentType}")
    public ResponseEntity<Resource> getContractorDocumentForClient(
            @PathVariable String contractorId,
            @PathVariable String documentType,
            Authentication authentication) {
        Perfil clientProfile = perfilRepository.findByUsernameIgnoreCase(authentication.getName()).orElse(null);
        Usuario client = usuarioRepository.findByUsernameIgnoreCase(authentication.getName()).orElse(null);
        Perfil contractor = perfilRepository.findById(contractorId).orElse(null);
        if (!hasRole(authentication, "ROLE_CLIENT") || clientProfile == null || client == null
                || !hasRole(contractor, "ROLE_CONTRACTOR")
                || !clientHasContractorRelationship(client, contractor)) {
            return ResponseEntity.notFound().build();
        }
        return readDocument(contractorDocumentId(contractor, documentType));
    }

    @GetMapping("/contractor/workers/{workerId}/cv")
    public ResponseEntity<Resource> getWorkerCvForContractor(
            @PathVariable String workerId,
            Authentication authentication) {
        Perfil contractor = perfilRepository.findByUsernameIgnoreCase(authentication.getName()).orElse(null);
        Perfil worker = perfilRepository.findById(workerId).orElse(null);
        if (!hasRole(authentication, "ROLE_CONTRACTOR") || !hasRole(contractor, "ROLE_CONTRACTOR")
                || !hasRole(worker, "ROLE_WORKER")) {
            return ResponseEntity.notFound().build();
        }
        return readDocument(worker.getCvUrl());
    }

    @GetMapping("/worker/contractors/{contractorId}/statutes")
    public ResponseEntity<Resource> getContractorStatutesForWorker(
            @PathVariable String contractorId,
            Authentication authentication) {
        Perfil worker = perfilRepository.findByUsernameIgnoreCase(authentication.getName()).orElse(null);
        Perfil contractor = perfilRepository.findById(contractorId).orElse(null);
        if (!hasRole(authentication, "ROLE_WORKER") || !hasRole(worker, "ROLE_WORKER")
                || !hasRole(contractor, "ROLE_CONTRACTOR")
                || !workerSharesProjectWithContractor(worker, contractor)) {
            return ResponseEntity.notFound().build();
        }
        return readDocument(contractor.getEstatutosUrl());
    }

    private boolean clientHasContractorRelationship(Usuario client, Perfil contractor) {
        Usuario contractorUser = usuarioRepository.findByUsernameIgnoreCase(contractor.getUsername()).orElse(null);
        if (contractorUser == null) return false;
        return proyectoRepository.findByClienteId(client.getId()).stream().anyMatch(project -> {
            boolean assigned = project.getContratistaAsignado() != null
                    && contractor.getId().equals(project.getContratistaAsignado().getId());
            boolean applied = project.getId() != null
                    && postulacionRepository.existsByProyectoIdAndUsuarioId(project.getId(), contractorUser.getId());
            return assigned || applied;
        });
    }

    private boolean workerSharesProjectWithContractor(Perfil worker, Perfil contractor) {
        if (worker.getId() == null || contractor.getId() == null) return false;
        return proyectoRepository.findByEquipoTrabajoContaining(worker).stream()
                .map(Proyecto::getContratistaAsignado)
                .anyMatch(assignedContractor -> assignedContractor != null
                        && contractor.getId().equals(assignedContractor.getId()));
    }

    private String contractorDocumentId(Perfil contractor, String documentType) {
        return switch (documentType) {
            case "cv" -> contractor.getCvUrl();
            case "matricula" -> contractor.getMatriculaDocumentoUrl();
            case "estatutos" -> contractor.getEstatutosUrl();
            default -> null;
        };
    }

    private ResponseEntity<Resource> readDocument(String fileId) {
        if (fileId == null || fileId.isBlank()) return ResponseEntity.notFound().build();
        try {
            GridFSFile file = gridFsTemplate.findOne(new Query(Criteria.where("_id").is(new ObjectId(fileId))));
            if (file == null) return ResponseEntity.notFound().build();
            GridFsResource resource = gridFsTemplate.getResource(file);
            String contentType = resource.getContentType();
            if (contentType == null || contentType.isBlank()) {
                contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
            }
            String filename = file.getFilename() == null ? "documento" : file.getFilename();
            return ResponseEntity.ok()
                    .header(HttpHeaders.CACHE_CONTROL, "private, no-store")
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            ContentDisposition.inline().filename(filename).build().toString())
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(resource);
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.notFound().build();
        }
    }

    private boolean hasRole(Perfil profile, String role) {
        return profile != null && profile.getRoles() != null && profile.getRoles().contains(role);
    }

    private boolean hasRole(Authentication authentication, String role) {
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> role.equals(authority.getAuthority()));
    }
}