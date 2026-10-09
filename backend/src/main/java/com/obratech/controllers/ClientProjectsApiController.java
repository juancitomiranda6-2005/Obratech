package com.obratech.controllers;

import java.net.URI;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.obratech.entity.Proyecto;
import com.obratech.entity.EquipoTrabajo;
import com.obratech.entity.Usuario;
import com.obratech.repository.EquipoTrabajoRepository;
import com.obratech.repository.PostulacionRepository;
import com.obratech.repository.PerfilRepository;
import com.obratech.repository.ProyectoRepository;
import com.obratech.repository.UsuarioRepository;
import com.obratech.service.ProyectoService;

@RestController
@RequestMapping("/api/client/projects")
public class ClientProjectsApiController {

    private static final Set<String> PROJECT_TYPES = Set.of(
            "Residencial", "Comercial", "Industrial", "Institucional", "Infraestructura");

    private final UsuarioRepository usuarioRepository;
    private final PerfilRepository perfilRepository;
    private final ProyectoRepository proyectoRepository;
    private final ProyectoService proyectoService;
    private final GridFsTemplate gridFsTemplate;
    private final EquipoTrabajoRepository equipoTrabajoRepository;
    private final PostulacionRepository postulacionRepository;

    public ClientProjectsApiController(
            UsuarioRepository usuarioRepository,
            PerfilRepository perfilRepository,
            ProyectoRepository proyectoRepository,
            ProyectoService proyectoService,
            GridFsTemplate gridFsTemplate,
            EquipoTrabajoRepository equipoTrabajoRepository,
            PostulacionRepository postulacionRepository) {
        this.usuarioRepository = usuarioRepository;
        this.perfilRepository = perfilRepository;
        this.proyectoRepository = proyectoRepository;
        this.proyectoService = proyectoService;
        this.gridFsTemplate = gridFsTemplate;
        this.equipoTrabajoRepository = equipoTrabajoRepository;
        this.postulacionRepository = postulacionRepository;
    }

    @GetMapping
    public ResponseEntity<?> list(Authentication authentication) {
        Usuario cliente = usuarioRepository.findByUsernameIgnoreCase(authentication.getName()).orElse(null);
        if (cliente == null) {
            return ResponseEntity.notFound().build();
        }
        List<ProjectSummary> projects = proyectoRepository.findByClienteId(cliente.getId())
                .stream().map(this::toSummary).toList();
        return ResponseEntity.ok(projects);
    }

        @GetMapping("/profile")
        public ResponseEntity<?> getProfile(Authentication authentication) {
        Usuario cliente = usuarioRepository.findByUsernameIgnoreCase(authentication.getName()).orElse(null);
        if (cliente == null) return ResponseEntity.notFound().build();
        var perfil = perfilRepository.findByUsernameIgnoreCase(cliente.getUsername()).orElse(null);
        List<Proyecto> projects = proyectoRepository.findByClienteId(cliente.getId());
        long inProgress = projects.stream()
            .filter(project -> "EN_PROGRESO".equals(project.getEstadoEjecucion() == null ? null : project.getEstadoEjecucion().name()))
            .count();
        return ResponseEntity.ok(new ClientProfile(
            cliente.getUsername(),
            perfil == null ? "" : valueOrEmpty(perfil.getNombre()),
            perfil == null ? "" : valueOrEmpty(perfil.getApellido()),
            perfil == null ? "" : valueOrEmpty(perfil.getTelefono()),
            perfil == null ? "" : valueOrEmpty(perfil.getEmpresa()),
            perfil != null && Boolean.TRUE.equals(perfil.getVerificado()),
            projects.size(),
            inProgress,
            perfil == null ? null : perfil.getCreado()));
        }

        @PutMapping("/profile")
        public ResponseEntity<?> updateProfile(
            @RequestBody ClientProfileRequest request,
            Authentication authentication) {
        if (request == null || isBlank(request.nombre()) || isBlank(request.apellido())
            || isBlank(request.telefono()) || isBlank(request.empresa())) {
            return ResponseEntity.badRequest().body(new ApiError("Completa nombre, apellido, teléfono y empresa."));
        }
        Usuario cliente = usuarioRepository.findByUsernameIgnoreCase(authentication.getName()).orElse(null);
        if (cliente == null) return ResponseEntity.notFound().build();

        var perfil = perfilRepository.findByUsernameIgnoreCase(cliente.getUsername()).orElseGet(() -> {
            var nuevo = new com.obratech.entity.Perfil();
            nuevo.setUsername(cliente.getUsername());
            nuevo.setEmail(cliente.getUsername());
            nuevo.setRoles(cliente.getRoles());
            nuevo.setActivo(true);
            nuevo.setVerificado(false);
            return nuevo;
        });
        perfil.setNombre(request.nombre().trim());
        perfil.setApellido(request.apellido().trim());
        perfil.setTelefono(request.telefono().trim());
        perfil.setEmpresa(request.empresa().trim());
        var saved = perfilRepository.save(perfil);
        List<Proyecto> projects = proyectoRepository.findByClienteId(cliente.getId());
        long inProgress = projects.stream()
            .filter(project -> project.getEstadoEjecucion() != null
                && project.getEstadoEjecucion().name().equals("EN_PROGRESO"))
            .count();
        return ResponseEntity.ok(new ClientProfile(
            cliente.getUsername(), saved.getNombre(), saved.getApellido(), saved.getTelefono(),
            saved.getEmpresa(), Boolean.TRUE.equals(saved.getVerificado()), projects.size(),
            inProgress, saved.getCreado()));
        }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable String id, Authentication authentication) {
        Usuario cliente = usuarioRepository.findByUsernameIgnoreCase(authentication.getName()).orElse(null);
        Proyecto proyecto = proyectoRepository.findById(id).orElse(null);
        if (cliente == null || !isOwner(proyecto, cliente)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(toSummary(proyecto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(
            @PathVariable String id,
            @RequestBody ProjectRequest request,
            Authentication authentication) {
        if (!isValid(request)) {
            return ResponseEntity.badRequest().body(new ApiError("Revisa los campos obligatorios, el presupuesto y las fechas."));
        }
        Usuario cliente = usuarioRepository.findByUsernameIgnoreCase(authentication.getName()).orElse(null);
        Proyecto proyecto = proyectoRepository.findById(id).orElse(null);
        if (cliente == null || !isOwner(proyecto, cliente)) {
            return ResponseEntity.notFound().build();
        }

        applyRequest(proyecto, request);
        return ResponseEntity.ok(toSummary(proyectoRepository.save(proyecto)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable String id, Authentication authentication) {
        Usuario cliente = usuarioRepository.findByUsernameIgnoreCase(authentication.getName()).orElse(null);
        Proyecto proyecto = proyectoRepository.findById(id).orElse(null);
        if (cliente == null || !isOwner(proyecto, cliente)) {
            return ResponseEntity.notFound().build();
        }
        proyectoRepository.delete(proyecto);
        return ResponseEntity.noContent().build();
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody ProjectRequest request, Authentication authentication) {
        return createProject(request, null, authentication);
    }

    @PostMapping(path = "/with-document", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> createWithDocument(
            @RequestPart("project") ProjectRequest request,
            @RequestPart(value = "documentoLegal", required = false) MultipartFile documentoLegal,
            Authentication authentication) {
        if (documentoLegal != null && !documentoLegal.isEmpty()) {
            String originalName = documentoLegal.getOriginalFilename() == null ? "" : documentoLegal.getOriginalFilename();
            String extension = originalName.contains(".")
                    ? originalName.substring(originalName.lastIndexOf('.') + 1).toLowerCase()
                    : "";
            if (!Set.of("pdf", "doc", "docx").contains(extension)) {
                return ResponseEntity.badRequest().body(new ApiError("El archivo debe ser PDF, DOC o DOCX."));
            }
            if (documentoLegal.getSize() > 50L * 1024 * 1024) {
                return ResponseEntity.badRequest().body(new ApiError("El archivo no puede superar 50 MB."));
            }
        }
        return createProject(request, documentoLegal, authentication);
    }

    private ResponseEntity<?> createProject(
            ProjectRequest request,
            MultipartFile documentoLegal,
            Authentication authentication) {
        if (request == null) {
            return ResponseEntity.badRequest().body(new ApiError("Los datos del proyecto son obligatorios."));
        }
        if (isBlank(request.titulo()) || isBlank(request.descripcion()) || isBlank(request.ubicacion())) {
            return ResponseEntity.badRequest().body(new ApiError("Completa el título, la descripción y la ubicación."));
        }
        if (!PROJECT_TYPES.contains(request.tipoProyecto())) {
            return ResponseEntity.badRequest().body(new ApiError("Selecciona un tipo de proyecto válido."));
        }
        if (request.presupuesto() == null || request.presupuesto() < 1_000_000) {
            return ResponseEntity.badRequest().body(new ApiError("El presupuesto mínimo es de 1.000.000 COP."));
        }
        if (request.fechaInicio() == null || request.fechaEntrega() == null
                || request.fechaEntrega().isBefore(request.fechaInicio())) {
            return ResponseEntity.badRequest().body(new ApiError("Revisa las fechas de inicio y entrega."));
        }
        if ((request.plazoEstimado() != null && request.plazoEstimado() < 1)
                || (request.areaTotal() != null && request.areaTotal() < 0)) {
            return ResponseEntity.badRequest().body(new ApiError("El plazo y el área deben ser valores válidos."));
        }

        Usuario cliente = usuarioRepository.findByUsernameIgnoreCase(authentication.getName()).orElse(null);
        if (cliente == null) {
            return ResponseEntity.notFound().build();
        }
        if (!cliente.isVerificado()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ApiError("Tu cuenta debe estar verificada para publicar proyectos."));
        }

        Proyecto proyecto = new Proyecto();
        applyRequest(proyecto, request);

        if (documentoLegal != null && !documentoLegal.isEmpty()) {
            try {
                String originalName = documentoLegal.getOriginalFilename() == null
                        ? "documento-legal" : documentoLegal.getOriginalFilename();
                ObjectId fileId = gridFsTemplate.store(
                        documentoLegal.getInputStream(), System.currentTimeMillis() + "_" + originalName,
                        documentoLegal.getContentType());
                proyecto.setDocumentoLegalUrl(fileId.toString());
                proyecto.setDocumentoLegalNombre(originalName);
            } catch (IOException exception) {
                return ResponseEntity.badRequest().body(new ApiError("No se pudo almacenar el documento legal."));
            }
        }

        Proyecto saved = proyectoService.publicarProyecto(proyecto, cliente);
        return ResponseEntity.created(URI.create("/api/client/projects/" + saved.getId()))
                .body(toSummary(saved));
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String valueOrEmpty(String value) {
        return value == null ? "" : value;
    }

    private boolean isValid(ProjectRequest request) {
        return request != null
                && !isBlank(request.titulo())
                && !isBlank(request.descripcion())
                && !isBlank(request.ubicacion())
                && PROJECT_TYPES.contains(request.tipoProyecto())
                && request.presupuesto() != null && request.presupuesto() >= 1_000_000
                && request.fechaInicio() != null && request.fechaEntrega() != null
                && !request.fechaEntrega().isBefore(request.fechaInicio())
                && (request.plazoEstimado() == null || request.plazoEstimado() >= 1)
                && (request.areaTotal() == null || request.areaTotal() >= 0);
    }

    private void applyRequest(Proyecto proyecto, ProjectRequest request) {
        proyecto.setTitulo(request.titulo().trim());
        proyecto.setDescripcion(request.descripcion().trim());
        proyecto.setUbicacion(request.ubicacion().trim());
        proyecto.setTipoProyecto(request.tipoProyecto());
        proyecto.setPresupuesto(request.presupuesto());
        proyecto.setFechaInicio(request.fechaInicio());
        proyecto.setFechaEntrega(request.fechaEntrega());
        proyecto.setPlazoEstimado(request.plazoEstimado());
        proyecto.setAreaTotal(request.areaTotal());
        proyecto.setObservaciones(request.observaciones());
    }

    private boolean isOwner(Proyecto proyecto, Usuario cliente) {
        return proyecto != null && proyecto.getCliente() != null
                && proyecto.getCliente().getUsername() != null
                && proyecto.getCliente().getUsername().equalsIgnoreCase(cliente.getUsername());
    }

    private ProjectSummary toSummary(Proyecto project) {
        List<EquipoTrabajo> teams = equipoTrabajoRepository.findByProyectoId(project.getId());
        double progress = teams.stream()
            .map(EquipoTrabajo::getPorcentajeAvance)
            .filter(java.util.Objects::nonNull)
            .mapToDouble(Double::doubleValue)
            .average()
            .orElse(0.0);
        return new ProjectSummary(
                project.getId(), project.getTitulo(), project.getDescripcion(), project.getTipoProyecto(),
                project.getUbicacion(), project.getPresupuesto(),
                project.getEstadoValidacion() == null ? "PENDIENTE" : project.getEstadoValidacion().name(),
                project.getEstadoEjecucion() == null ? "PENDIENTE" : project.getEstadoEjecucion().name(), project.getFechaInicio(), project.getFechaEntrega(),
                project.getPlazoEstimado(), project.getAreaTotal(),
                project.getEquipoTrabajo() == null ? 0 : project.getEquipoTrabajo().size(),
                project.getObservaciones(), postulacionRepository.findByProyectoId(project.getId()).size(), progress);
    }

    public record ProjectRequest(
            String titulo,
            String descripcion,
            String ubicacion,
            String tipoProyecto,
            Double presupuesto,
            LocalDate fechaInicio,
            LocalDate fechaEntrega,
            Integer plazoEstimado,
            Double areaTotal,
            String observaciones) {}

    public record ProjectSummary(
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
            int miembrosEquipo,
            String observaciones,
            int totalPostulantes,
            double progresoProyecto) {}

    public record ApiError(String error) {}

    public record ClientProfileRequest(String nombre, String apellido, String telefono, String empresa) {}

    public record ClientProfile(
            String username,
            String nombre,
            String apellido,
            String telefono,
            String empresa,
            boolean verificado,
            int proyectosPublicados,
            long proyectosEnProgreso,
            java.time.LocalDateTime creado) {}
}