package com.obratech.controllers;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.obratech.entity.Perfil;
import com.obratech.entity.Proyecto;
import com.obratech.entity.Usuario;
import com.obratech.entity.enums.EstadoValidacion;
import com.obratech.repository.PerfilRepository;
import com.obratech.repository.ProyectoRepository;
import com.obratech.repository.UsuarioRepository;
import com.obratech.service.PerfilVerificationService;
import com.obratech.service.UsuarioService;
import com.obratech.util.PerfilDisplayName;

@RestController
@RequestMapping("/api/admin")
public class AdminApiController {

    private static final String TEMP_PASSWORD_CHARACTERS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789!@#$%";
    private static final java.security.SecureRandom SECURE_RANDOM = new java.security.SecureRandom();

    private static final List<String> BUSINESS_ROLES = List.of(
            "ROLE_CONTRACTOR", "ROLE_CLIENT", "ROLE_WORKER");

    private final ProyectoRepository proyectoRepository;
    private final PerfilRepository perfilRepository;
    private final UsuarioRepository usuarioRepository;
    private final PerfilVerificationService perfilVerificationService;
    private final UsuarioService usuarioService;

    public AdminApiController(
            ProyectoRepository proyectoRepository,
            PerfilRepository perfilRepository,
            UsuarioRepository usuarioRepository,
            PerfilVerificationService perfilVerificationService,
            UsuarioService usuarioService) {
        this.proyectoRepository = proyectoRepository;
        this.perfilRepository = perfilRepository;
        this.usuarioRepository = usuarioRepository;
        this.perfilVerificationService = perfilVerificationService;
        this.usuarioService = usuarioService;
    }

    @GetMapping("/dashboard")
    public DashboardResponse getDashboard() {
        List<Perfil> perfiles = perfilRepository.findAll();
        List<Proyecto> proyectosPendientes = proyectoRepository.findByEstadoValidacion(EstadoValidacion.PENDIENTE);

        List<PerfilItem> contratistas = perfiles.stream()
                .filter(perfil -> hasRole(perfil, "ROLE_CONTRACTOR"))
                .filter(perfil -> !Boolean.FALSE.equals(perfil.getActivo()))
                .map(this::toPerfilItem)
                .toList();
        List<PerfilItem> clientes = perfiles.stream()
                .filter(perfil -> hasRole(perfil, "ROLE_CLIENT"))
                .filter(perfil -> !Boolean.FALSE.equals(perfil.getActivo()))
                .map(this::toPerfilItem)
                .toList();
        List<PerfilItem> verificacionesPendientes = perfiles.stream()
                .filter(this::hasBusinessRole)
                .filter(perfil -> !Boolean.TRUE.equals(perfil.getVerificado()))
                .map(this::toPerfilItem)
                .toList();
        List<PerfilItem> usuariosInactivos = perfiles.stream()
                .filter(this::hasBusinessRole)
                .filter(perfil -> Boolean.FALSE.equals(perfil.getActivo()))
                .map(this::toPerfilItem)
                .toList();
        List<PerfilItem> usuarios = perfiles.stream()
            .filter(this::hasBusinessRole)
            .map(this::toPerfilItem)
            .toList();

        return new DashboardResponse(
                usuarioRepository.count(),
                proyectoRepository.count(),
                proyectosPendientes.size(),
                verificacionesPendientes.size(),
                usuariosInactivos.size(),
                proyectosPendientes.stream().map(this::toProyectoItem).toList(),
                contratistas,
                clientes,
                verificacionesPendientes,
                usuariosInactivos,
                usuarios);
    }

    @GetMapping("/perfiles/{id}")
    public ResponseEntity<?> getProfileDetail(@PathVariable String id) {
        Perfil perfil = perfilRepository.findById(id).orElse(null);
        if (perfil == null) {
            return ResponseEntity.notFound().build();
        }
        var usuario = usuarioRepository.findByUsernameIgnoreCase(perfil.getUsername()).orElse(null);
        List<Proyecto> proyectos;
        if (hasRole(perfil, "ROLE_CLIENT") && usuario != null) {
            proyectos = proyectoRepository.findByClienteId(usuario.getId());
        } else if (hasRole(perfil, "ROLE_CONTRACTOR")) {
            proyectos = proyectoRepository.findByContratistaAsignadoId(perfil.getId());
        } else if (hasRole(perfil, "ROLE_WORKER")) {
            proyectos = proyectoRepository.findByEquipoTrabajoContaining(perfil);
        } else {
            proyectos = List.of();
        }

        return ResponseEntity.ok(new ProfileDetailResponse(
                perfil.getId(),
                toPerfilItem(perfil),
                perfil.getEmail(),
                perfil.getTelefono(),
                perfil.getEmpresa(),
                perfil.getEspecialidad(),
                perfil.getOficio(),
                perfil.getExperiencia(),
                perfil.getUbicacion(),
                perfil.getDescripcion(),
                perfil.getCvUrl(),
                perfil.getFotoPerfilUrl(),
                proyectos.stream().map(this::toAdminProjectItem).toList()));
    }

    @PatchMapping("/proyectos/{id}/validacion")
    public ResponseEntity<?> updateProjectValidation(
            @PathVariable String id,
            @RequestBody ValidationRequest request) {
        if (request == null || (!"APROBADO".equals(request.estado())
            && !"RECHAZADO".equals(request.estado()))) {
            return ResponseEntity.badRequest().body(new ErrorResponse("Estado de validación no válido."));
        }

        EstadoValidacion estado = EstadoValidacion.valueOf(request.estado());
        return proyectoRepository.findById(id)
                .map(proyecto -> {
                proyecto.setEstadoValidacion(estado);
                    return ResponseEntity.ok(toProyectoItem(proyectoRepository.save(proyecto)));
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PatchMapping("/perfiles/{id}/activo")
    public ResponseEntity<?> updateProfileActive(
            @PathVariable String id,
            @RequestBody ActiveRequest request) {
        if (request == null || request.activo() == null) {
            return ResponseEntity.badRequest().body(new ErrorResponse("El estado activo es obligatorio."));
        }

        return perfilRepository.findById(id)
                .map(perfil -> {
                    perfil.setActivo(request.activo());
                    Perfil saved = perfilRepository.save(perfil);
                    usuarioRepository.findByUsernameIgnoreCase(saved.getUsername()).ifPresent(usuario -> {
                        usuario.setActivo(request.activo());
                        usuarioRepository.save(usuario);
                    });
                    return ResponseEntity.ok(toPerfilItem(saved));
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PatchMapping("/perfiles/{id}/verificado")
    public ResponseEntity<?> updateProfileVerified(
            @PathVariable String id,
            @RequestBody VerifiedRequest request) {
        if (request == null || request.verificado() == null) {
            return ResponseEntity.badRequest().body(new ErrorResponse("El estado de verificación es obligatorio."));
        }

        return perfilRepository.findById(id)
                .map(perfil -> {
                    if (request.verificado()) {
                        List<String> missing = perfilVerificationService.getMissingRequirements(perfil);
                        if (!missing.isEmpty()) {
                            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                                    .body(new VerificationError("No se puede verificar este usuario.", missing));
                        }
                    }

                    perfil.setVerificado(request.verificado());
                    Perfil saved = perfilRepository.save(perfil);
                    usuarioRepository.findByUsernameIgnoreCase(saved.getUsername()).ifPresent(usuario -> {
                        usuario.setVerificado(request.verificado());
                        usuarioRepository.save(usuario);
                    });
                    return ResponseEntity.ok(toPerfilItem(saved));
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/usuarios/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable String id, Authentication authentication) {
        var usuario = usuarioRepository.findById(id).orElse(null);
        if (usuario == null) {
            return ResponseEntity.notFound().build();
        }
        if (usuario.getUsername().equalsIgnoreCase(authentication.getName())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new ErrorResponse("No puedes eliminar tu propia cuenta de administrador."));
        }
        boolean isAdmin = usuario.getRoles() != null && usuario.getRoles().contains("ROLE_ADMIN");
        long adminCount = usuarioRepository.findAll().stream()
                .filter(candidate -> candidate.getRoles() != null && candidate.getRoles().contains("ROLE_ADMIN"))
                .count();
        if (isAdmin && adminCount <= 1) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new ErrorResponse("No se puede eliminar el último administrador."));
        }

        usuarioService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/trabajadores")
    public ResponseEntity<?> createWorker(@RequestBody WorkerCreateRequest request) {
        if (request == null || isBlank(request.email()) || isBlank(request.nombre())
                || isBlank(request.apellido()) || isBlank(request.telefono()) || isBlank(request.oficio())
                || request.experiencia() == null || request.experiencia() < 0) {
            return ResponseEntity.badRequest().body(new ErrorResponse(
                    "Completa correo, nombre, apellido, teléfono, oficio y años de experiencia válidos."));
        }

        String username = request.email().trim().toLowerCase(java.util.Locale.ROOT);
        if (usuarioRepository.findByUsernameIgnoreCase(username).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse("El correo ya está registrado."));
        }

        String temporaryPassword = generateTemporaryPassword();
        Usuario usuario = new Usuario();
        usuario.setUsername(username);
        usuario.setPassword(temporaryPassword);
        usuario.setRoles(java.util.Set.of("ROLE_WORKER"));
        try {
            usuarioService.register(usuario);
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().body(new ErrorResponse(exception.getMessage()));
        }

        Perfil perfil = perfilRepository.findByUsernameIgnoreCase(username).orElseThrow();
        perfil.setNombre(request.nombre().trim());
        perfil.setApellido(request.apellido().trim());
        perfil.setTelefono(request.telefono().trim());
        perfil.setEmail(username);
        perfil.setOficio(request.oficio().trim());
        perfil.setExperiencia(request.experiencia());
        perfil.setDisponibilidad(true);
        perfil.setActivo(true);
        perfil.setVerificado(false);
        Perfil saved = perfilRepository.save(perfil);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new WorkerCreatedResponse(toPerfilItem(saved), temporaryPassword));
    }

    private boolean hasBusinessRole(Perfil perfil) {
        return perfil.getRoles() != null && perfil.getRoles().stream().anyMatch(BUSINESS_ROLES::contains);
    }

    private String generateTemporaryPassword() {
        StringBuilder password = new StringBuilder(14);
        for (int index = 0; index < 14; index++) {
            password.append(TEMP_PASSWORD_CHARACTERS.charAt(SECURE_RANDOM.nextInt(TEMP_PASSWORD_CHARACTERS.length())));
        }
        return password.toString();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private boolean hasRole(Perfil perfil, String role) {
        return perfil.getRoles() != null && perfil.getRoles().contains(role);
    }

    private PerfilItem toPerfilItem(Perfil perfil) {
        return new PerfilItem(
            perfil.getId(), PerfilDisplayName.of(perfil),
                perfil.getUsername(),
                perfil.getRoles() == null ? List.of() : perfil.getRoles().stream().sorted().toList(),
                Boolean.TRUE.equals(perfil.getActivo()),
            Boolean.TRUE.equals(perfil.getVerificado()),
            perfilVerificationService.getMissingRequirements(perfil));
    }

    private ProyectoItem toProyectoItem(Proyecto proyecto) {
        String cliente = proyecto.getCliente() == null ? "Genérico" : proyecto.getCliente().getUsername();
        return new ProyectoItem(
                proyecto.getId(), proyecto.getTitulo(), proyecto.getDescripcion(), cliente,
                proyecto.getEstadoValidacion() == null ? null : proyecto.getEstadoValidacion().name());
    }

    private AdminProjectItem toAdminProjectItem(Proyecto proyecto) {
        return new AdminProjectItem(
                proyecto.getId(),
                proyecto.getTitulo(),
                proyecto.getUbicacion(),
                proyecto.getPresupuesto(),
                proyecto.getEstadoEjecucion() == null ? "PENDIENTE" : proyecto.getEstadoEjecucion().name());
    }

    public record DashboardResponse(
            long totalUsuarios,
            long totalProyectos,
            int proyectosPendientes,
            int usuariosPendientes,
            int usuariosInactivos,
            List<ProyectoItem> proyectos,
            List<PerfilItem> contratistas,
            List<PerfilItem> clientes,
            List<PerfilItem> verificaciones,
            List<PerfilItem> inactivos,
            List<PerfilItem> usuarios) {}

    public record ProyectoItem(String id, String titulo, String descripcion, String cliente, String estado) {}

    public record PerfilItem(String id, String nombre, String username, List<String> roles,
            boolean activo, boolean verificado, List<String> missingRequirements) {}

        public record ProfileDetailResponse(
            String id,
            PerfilItem perfil,
            String email,
            String telefono,
            String empresa,
            String especialidad,
            String oficio,
            Integer experiencia,
            String ubicacion,
            String descripcion,
            String cvUrl,
            String fotoUrl,
            List<AdminProjectItem> proyectos) {}

        public record AdminProjectItem(String id, String titulo, String ubicacion, Double presupuesto, String estado) {}

    public record ValidationRequest(String estado) {}

    public record ActiveRequest(Boolean activo) {}

    public record VerifiedRequest(Boolean verificado) {}

        public record WorkerCreateRequest(
            String email,
            String nombre,
            String apellido,
            String telefono,
            String oficio,
            Integer experiencia) {}

        public record WorkerCreatedResponse(PerfilItem trabajador, String temporaryPassword) {}

    public record ErrorResponse(String error) {}

    public record VerificationError(String error, List<String> missingRequirements) {}
}