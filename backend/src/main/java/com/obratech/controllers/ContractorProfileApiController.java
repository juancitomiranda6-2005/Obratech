package com.obratech.controllers;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import org.bson.types.ObjectId;
import org.springframework.core.io.Resource;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.mongodb.client.gridfs.model.GridFSFile;
import com.obratech.entity.Perfil;
import com.obratech.entity.Proyecto;
import com.obratech.entity.Usuario;
import com.obratech.entity.enums.EstadoEjecucion;
import com.obratech.repository.CalificacionRepository;
import com.obratech.repository.PerfilRepository;
import com.obratech.repository.ProyectoRepository;
import com.obratech.repository.UsuarioRepository;
import com.obratech.service.ContratistaProfileValidator;

@RestController
@RequestMapping("/api/contractor/profile")
public class ContractorProfileApiController {

    private static final long MAX_GENERAL_DOCUMENT_BYTES = 10L * 1024 * 1024;
    private static final Set<String> GENERAL_DOCUMENT_EXTENSIONS = Set.of("pdf", "doc", "docx");

    private final UsuarioRepository usuarioRepository;
    private final PerfilRepository perfilRepository;
    private final ProyectoRepository proyectoRepository;
    private final CalificacionRepository calificacionRepository;
    private final ContratistaProfileValidator profileValidator;
    private final GridFsTemplate gridFsTemplate;

    public ContractorProfileApiController(
            UsuarioRepository usuarioRepository,
            PerfilRepository perfilRepository,
            ProyectoRepository proyectoRepository,
            CalificacionRepository calificacionRepository,
            ContratistaProfileValidator profileValidator,
            GridFsTemplate gridFsTemplate) {
        this.usuarioRepository = usuarioRepository;
        this.perfilRepository = perfilRepository;
        this.proyectoRepository = proyectoRepository;
        this.calificacionRepository = calificacionRepository;
        this.profileValidator = profileValidator;
        this.gridFsTemplate = gridFsTemplate;
    }

    @GetMapping
    public ResponseEntity<?> getProfile(Authentication authentication) {
        Perfil perfil = perfilRepository.findByUsernameIgnoreCase(authentication.getName()).orElse(null);
        Usuario usuario = usuarioRepository.findByUsernameIgnoreCase(authentication.getName()).orElse(null);
        if (perfil == null || usuario == null) return ResponseEntity.notFound().build();

        List<Proyecto> activeProjects = proyectoRepository
                .findByContratistaAsignadoIdAndEstadoEjecucionNot(perfil.getId(), EstadoEjecucion.COMPLETADO);
        List<Proyecto> completedProjects = proyectoRepository
                .findByContratistaAsignadoIdAndEstadoEjecucion(perfil.getId(), EstadoEjecucion.COMPLETADO);
        List<ReviewItem> reviews = calificacionRepository.findByContratistaId(perfil.getId()).stream()
                .map(review -> new ReviewItem(
                        review.getId(), review.getPuntuacion(), review.getComentario(),
                        review.getFecha(), review.getProyecto() == null ? "Proyecto" : review.getProyecto().getTitulo()))
                .toList();

        return ResponseEntity.ok(toProfileData(perfil, usuario, activeProjects.size(), completedProjects.size(), reviews));
    }

    @PutMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> updateProfile(
            @RequestPart("profile") ContractorProfileRequest request,
            @RequestPart(value = "fotoFile", required = false) MultipartFile photo,
            @RequestPart(value = "matriculaFile", required = false) MultipartFile license,
            @RequestPart(value = "cvFile", required = false) MultipartFile cv,
            @RequestPart(value = "estatutosFile", required = false) MultipartFile statutes,
            Authentication authentication) {
        if (request == null || isBlank(request.nombre()) || isBlank(request.apellido())
                || isBlank(request.telefono()) || isBlank(request.especialidad())
                || isBlank(request.ciudad()) || isBlank(request.departamento())
                || isBlank(request.matriculaProfesional()) || request.experiencia() == null
                || request.experiencia() < 0 || request.descripcion() == null
                || request.descripcion().trim().length() < 150
                || request.subespecialidades() == null || request.subespecialidades().size() < 2) {
            return ResponseEntity.badRequest().body(new ApiError(
                    "Completa los datos obligatorios, una presentación de 150 caracteres y al menos dos subespecialidades."));
        }

        String fileError = photo == null || photo.isEmpty() ? null : profileValidator.validateImage(photo);
        if (fileError == null && license != null && !license.isEmpty()) {
            fileError = profileValidator.validatePdf(license, "la matrícula profesional");
        }
        if (fileError == null) fileError = validateGeneralDocument(cv, "la hoja de vida");
        if (fileError == null) fileError = validateGeneralDocument(statutes, "los estatutos");
        if (fileError != null) return ResponseEntity.badRequest().body(new ApiError(fileError));

        Perfil perfil = perfilRepository.findByUsernameIgnoreCase(authentication.getName()).orElse(null);
        if (perfil == null) return ResponseEntity.notFound().build();
        Usuario usuario = usuarioRepository.findByUsernameIgnoreCase(authentication.getName()).orElse(null);
        if (usuario == null) return ResponseEntity.notFound().build();

        perfil.setNombre(request.nombre().trim());
        perfil.setApellido(request.apellido().trim());
        perfil.setTelefono(request.telefono().trim());
        perfil.setEspecialidad(request.especialidad().trim());
        perfil.setDescripcion(request.descripcion().trim());
        perfil.setCiudad(request.ciudad().trim());
        perfil.setDepartamento(request.departamento().trim());
        perfil.setUbicacion(request.ciudad().trim() + ", " + request.departamento().trim());
        perfil.setMatriculaProfesional(request.matriculaProfesional().trim());
        perfil.setSubespecialidades(request.subespecialidades());
        perfil.setExperiencia(request.experiencia());

        try {
            storeIfPresent(photo, perfil, "foto");
            storeIfPresent(license, perfil, "matricula");
            storeIfPresent(cv, perfil, "cv");
            storeIfPresent(statutes, perfil, "estatutos");
        } catch (IOException exception) {
            return ResponseEntity.badRequest().body(new ApiError("No se pudieron guardar los documentos."));
        }

        Perfil saved = perfilRepository.save(perfil);
        usuario.setVerificado(false);
        usuarioRepository.save(usuario);
        List<Proyecto> activeProjects = proyectoRepository
                .findByContratistaAsignadoIdAndEstadoEjecucionNot(saved.getId(), EstadoEjecucion.COMPLETADO);
        List<Proyecto> completedProjects = proyectoRepository
                .findByContratistaAsignadoIdAndEstadoEjecucion(saved.getId(), EstadoEjecucion.COMPLETADO);
        List<ReviewItem> reviews = calificacionRepository.findByContratistaId(saved.getId()).stream()
                .map(review -> new ReviewItem(review.getId(), review.getPuntuacion(), review.getComentario(),
                        review.getFecha(), review.getProyecto() == null ? "Proyecto" : review.getProyecto().getTitulo()))
                .toList();
        return ResponseEntity.ok(toProfileData(saved, usuario, activeProjects.size(), completedProjects.size(), reviews));
    }

    @GetMapping("/files/{fileId}")
    public ResponseEntity<Resource> getFile(
            @PathVariable String fileId,
            Authentication authentication) {
        Perfil perfil = perfilRepository.findByUsernameIgnoreCase(authentication.getName()).orElse(null);
        if (perfil == null || !ownsFile(perfil, fileId)) return ResponseEntity.notFound().build();

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
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(resource);
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.notFound().build();
        }
    }

    private ContractorProfileData toProfileData(
            Perfil perfil,
            Usuario usuario,
            int activeProjects,
            int completedProjects,
            List<ReviewItem> reviews) {
        return new ContractorProfileData(
                usuario.getUsername(), perfil.getNombre(), perfil.getApellido(), perfil.getEmail(),
                perfil.getTelefono(), perfil.getEspecialidad(), perfil.getDescripcion(), perfil.getCiudad(),
                perfil.getDepartamento(), perfil.getExperiencia(), perfil.getCalificacionPromedio() == null
                        ? 0.0 : perfil.getCalificacionPromedio(),
                perfil.getMatriculaProfesional(), perfil.getSubespecialidades() == null
                        ? Set.of() : perfil.getSubespecialidades(),
                perfil.getFotoPerfilUrl(), perfil.getMatriculaDocumentoUrl(), perfil.getCvUrl(), perfil.getEstatutosUrl(),
                usuario.isVerificado(), activeProjects, completedProjects,
                profileValidator.validate(perfil), reviews);
    }

    private String validateGeneralDocument(MultipartFile file, String label) {
        if (file == null || file.isEmpty()) return null;
        if (file.getSize() > MAX_GENERAL_DOCUMENT_BYTES) return "El archivo de " + label + " no puede superar 10 MB.";
        String filename = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
        String extension = filename.contains(".") ? filename.substring(filename.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT) : "";
        return GENERAL_DOCUMENT_EXTENSIONS.contains(extension) ? null : "El archivo de " + label + " debe ser PDF, DOC o DOCX.";
    }

    private void storeIfPresent(MultipartFile file, Perfil perfil, String type) throws IOException {
        if (file == null || file.isEmpty()) return;
        String originalName = file.getOriginalFilename() == null ? "documento" : file.getOriginalFilename();
        ObjectId fileId = gridFsTemplate.store(
                file.getInputStream(), UUID.randomUUID() + "_" + type + "_" + originalName, file.getContentType());
        switch (type) {
            case "foto" -> perfil.setFotoPerfilUrl(fileId.toString());
            case "matricula" -> perfil.setMatriculaDocumentoUrl(fileId.toString());
            case "cv" -> perfil.setCvUrl(fileId.toString());
            case "estatutos" -> perfil.setEstatutosUrl(fileId.toString());
            default -> throw new IllegalArgumentException("Tipo de archivo no admitido.");
        }
    }

    private boolean ownsFile(Perfil perfil, String fileId) {
        return fileId.equals(perfil.getFotoPerfilUrl()) || fileId.equals(perfil.getMatriculaDocumentoUrl())
                || fileId.equals(perfil.getCvUrl()) || fileId.equals(perfil.getEstatutosUrl());
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    public record ContractorProfileRequest(
            String nombre, String apellido, String telefono, String especialidad, String descripcion,
            String ciudad, String departamento, String matriculaProfesional, Integer experiencia,
            Set<String> subespecialidades) {}

    public record ContractorProfileData(
            String username, String nombre, String apellido, String email, String telefono,
            String especialidad, String descripcion, String ciudad, String departamento,
            Integer experiencia, double calificacionPromedio, String matriculaProfesional,
            Set<String> subespecialidades, String fotoPerfilUrl, String matriculaDocumentoUrl,
            String cvUrl, String estatutosUrl, boolean usuarioVerificado, int proyectosEnProgreso,
            int proyectosCompletados, List<String> requisitosFaltantes, List<ReviewItem> reviews) {}

    public record ReviewItem(String id, int puntuacion, String comentario,
            java.time.LocalDateTime fecha, String proyecto) {}

    public record ApiError(String error) {}
}