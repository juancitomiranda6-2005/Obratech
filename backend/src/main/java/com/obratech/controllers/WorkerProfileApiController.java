package com.obratech.controllers;

import java.io.IOException;
import java.util.Map;
import java.util.Locale;
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
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.mongodb.client.gridfs.model.GridFSFile;

import com.obratech.entity.Perfil;
import com.obratech.entity.Usuario;
import com.obratech.repository.PerfilRepository;
import com.obratech.repository.UsuarioRepository;

@RestController
@RequestMapping("/api/worker")
public class WorkerProfileApiController {

    private final UsuarioRepository usuarioRepository;
    private final PerfilRepository perfilRepository;
    private final GridFsTemplate gridFsTemplate;

    public WorkerProfileApiController(
            UsuarioRepository usuarioRepository,
            PerfilRepository perfilRepository,
            GridFsTemplate gridFsTemplate) {
        this.usuarioRepository = usuarioRepository;
        this.perfilRepository = perfilRepository;
        this.gridFsTemplate = gridFsTemplate;
    }

    @GetMapping("/profile")
    public ResponseEntity<?> getProfile(Authentication authentication) {
        Usuario usuario = usuarioRepository.findByUsernameIgnoreCase(authentication.getName()).orElse(null);
        if (usuario == null) {
            return ResponseEntity.notFound().build();
        }

        Perfil perfil = perfilRepository.findByUsernameIgnoreCase(usuario.getUsername()).orElseGet(() -> {
            Perfil nuevo = new Perfil();
            nuevo.setUsername(usuario.getUsername());
            nuevo.setEmail(usuario.getUsername());
            nuevo.setRoles(usuario.getRoles());
            nuevo.setActivo(true);
            nuevo.setVerificado(false);
            return nuevo;
        });

        return ResponseEntity.ok(new WorkerProfileResponse(
                perfil.getUsername(),
                perfil.getNombre(),
                perfil.getApellido(),
                perfil.getTelefono(),
                perfil.getOficio(),
                perfil.getExperiencia() == null ? 0 : perfil.getExperiencia(),
                perfil.getDisponibilidad() != null && perfil.getDisponibilidad(),
                perfil.getDescripcion(),
                perfil.getCvUrl(),
                perfil.getVerificado()))
        ;
    }

        @PutMapping(value = "/profile", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
        public ResponseEntity<?> updateProfile(
            @RequestPart("profile") WorkerProfileRequest request,
            @RequestPart(value = "cvFile", required = false) MultipartFile cvFile,
            Authentication authentication) {
        if (request == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Los datos del perfil son obligatorios."));
        }
        if (isBlank(request.nombre()) || isBlank(request.apellido()) || isBlank(request.telefono())
                || isBlank(request.oficio()) || request.experiencia() == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Completa nombre, apellido, teléfono, oficio y experiencia."));
        }
        String cvError = validateCv(cvFile);
        if (cvError != null) {
            return ResponseEntity.badRequest().body(Map.of("error", cvError));
        }

        Usuario usuario = usuarioRepository.findByUsernameIgnoreCase(authentication.getName()).orElse(null);
        if (usuario == null) {
            return ResponseEntity.notFound().build();
        }

        Perfil perfil = perfilRepository.findByUsernameIgnoreCase(usuario.getUsername()).orElseGet(() -> {
            Perfil nuevo = new Perfil();
            nuevo.setUsername(usuario.getUsername());
            nuevo.setEmail(usuario.getUsername());
            nuevo.setRoles(usuario.getRoles());
            nuevo.setActivo(true);
            return nuevo;
        });

        perfil.setNombre(request.nombre().trim());
        perfil.setApellido(request.apellido().trim());
        perfil.setTelefono(request.telefono().trim());
        perfil.setOficio(request.oficio().trim());
        perfil.setExperiencia(request.experiencia());
        perfil.setDisponibilidad(Boolean.TRUE.equals(request.disponible()));
        perfil.setDescripcion(request.descripcion() == null ? "" : request.descripcion().trim());
        perfil.setEmail(usuario.getUsername());
        perfil.setVerificado(false);
        usuario.setVerificado(false);
        usuarioRepository.save(usuario);

        if (cvFile != null && !cvFile.isEmpty()) {
            try {
                String filename = UUID.randomUUID() + "_cv_" + cvFile.getOriginalFilename();
                ObjectId fileId = gridFsTemplate.store(cvFile.getInputStream(), filename, cvFile.getContentType());
                perfil.setCvUrl(fileId.toString());
            } catch (IOException exception) {
                return ResponseEntity.badRequest().body(Map.of("error", "No se pudo guardar el CV."));
            }
        }

        Perfil saved = perfilRepository.save(perfil);
        return ResponseEntity.ok(new WorkerProfileResponse(
                saved.getUsername(),
                saved.getNombre(),
                saved.getApellido(),
                saved.getTelefono(),
                saved.getOficio(),
                saved.getExperiencia() == null ? 0 : saved.getExperiencia(),
                saved.getDisponibilidad() != null && saved.getDisponibilidad(),
                saved.getDescripcion(),
                saved.getCvUrl(),
                saved.getVerificado()));
    }

    @GetMapping("/profile/files/{fileId}")
    public ResponseEntity<Resource> getCv(@PathVariable String fileId, Authentication authentication) {
        Perfil perfil = perfilRepository.findByUsernameIgnoreCase(authentication.getName()).orElse(null);
        if (perfil == null || !fileId.equals(perfil.getCvUrl())) {
            return ResponseEntity.notFound().build();
        }
        try {
            GridFSFile file = gridFsTemplate.findOne(new Query(Criteria.where("_id").is(new ObjectId(fileId))));
            if (file == null) return ResponseEntity.notFound().build();
            GridFsResource resource = gridFsTemplate.getResource(file);
            String contentType = resource.getContentType();
            if (contentType == null || contentType.isBlank()) contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
            String filename = file.getFilename() == null ? "hoja-de-vida" : file.getFilename();
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(resource);
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.notFound().build();
        }
    }

    private String validateCv(MultipartFile file) {
        if (file == null || file.isEmpty()) return null;
        if (file.getSize() > 10L * 1024 * 1024) return "El CV no puede superar 10 MB.";
        String filename = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase(Locale.ROOT);
        if (!(filename.endsWith(".pdf") || filename.endsWith(".doc") || filename.endsWith(".docx"))) {
            return "El CV debe estar en formato PDF, DOC o DOCX.";
        }
        return null;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    public record WorkerProfileRequest(
            String nombre,
            String apellido,
            String telefono,
            String oficio,
            Integer experiencia,
            Boolean disponible,
            String descripcion) {}

    public record WorkerProfileResponse(
            String username,
            String nombre,
            String apellido,
            String telefono,
            String oficio,
            Integer experiencia,
            Boolean disponible,
            String descripcion,
            String cvUrl,
            Boolean verificado) {}
}
