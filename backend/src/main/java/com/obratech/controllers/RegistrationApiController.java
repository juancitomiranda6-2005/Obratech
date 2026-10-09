package com.obratech.controllers;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.obratech.entity.Cliente;
import com.obratech.entity.Contratista;
import com.obratech.entity.Perfil;
import com.obratech.entity.Trabajador;
import com.obratech.entity.Usuario;
import com.obratech.repository.PerfilRepository;
import com.obratech.repository.UsuarioRepository;
import com.obratech.security.JwtService;
import com.obratech.service.UsuarioService;

import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping("/api/auth")
public class RegistrationApiController {

    private static final Set<String> ALLOWED_ROLES = Set.of("cliente", "contratista", "trabajador");

    private final UsuarioService usuarioService;
    private final UsuarioRepository usuarioRepository;
    private final PerfilRepository perfilRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public RegistrationApiController(
            UsuarioService usuarioService,
            UsuarioRepository usuarioRepository,
            PerfilRepository perfilRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService) {
        this.usuarioService = usuarioService;
        this.usuarioRepository = usuarioRepository;
        this.perfilRepository = perfilRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegistrationRequest request) {
        if (request == null || !request.acceptedTerms()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Debes aceptar los términos y condiciones."));
        }
        if (request.roles() == null || request.roles().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Debes seleccionar al menos un tipo de usuario."));
        }
        if (!ALLOWED_ROLES.containsAll(request.roles())) {
            return ResponseEntity.badRequest().body(Map.of("error", "Uno o más perfiles seleccionados no son válidos."));
        }

        Usuario usuario = new Usuario();
        usuario.setUsername(request.username());
        usuario.setPassword(request.password());
        usuario.setRoles(new HashSet<>(request.roles()));

        try {
            Usuario saved = usuarioService.register(usuario);
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "message", "Cuenta creada exitosamente. Ya puedes iniciar sesión.",
                    "username", saved.getUsername()));
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().body(Map.of("error", exception.getMessage()));
        }
    }

    public record RegistrationRequest(String username, String password, List<String> roles, boolean acceptedTerms) {}

    public record OAuthCompletionRequest(String password, List<String> roles, boolean acceptedTerms) {}

    @PostMapping("/complete-registration")
    public ResponseEntity<?> completeOAuthRegistration(
            @RequestBody OAuthCompletionRequest request,
            HttpSession session) {
        Usuario sessionUser = (Usuario) session.getAttribute("usuario");
        if (sessionUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "La sesión de Google expiró. Inicia sesión de nuevo."));
        }
        if (!Boolean.TRUE.equals(session.getAttribute("oauth2_pending_registration"))) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "Esta sesión no requiere completar el registro."));
        }
        if (request == null || !request.acceptedTerms()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Debes aceptar los términos y condiciones."));
        }
        if (request.password() == null || request.password().length() < 8) {
            return ResponseEntity.badRequest().body(Map.of("error", "La contraseña debe tener al menos 8 caracteres."));
        }
        if (request.roles() == null || request.roles().isEmpty() || !ALLOWED_ROLES.containsAll(request.roles())) {
            return ResponseEntity.badRequest().body(Map.of("error", "Selecciona al menos un rol válido."));
        }

        Usuario usuario = usuarioRepository.findByUsernameIgnoreCase(sessionUser.getUsername()).orElse(null);
        if (usuario == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "No se encontró la cuenta de Google."));
        }

        Set<String> roles = new HashSet<>(request.roles().stream()
                .map(role -> "ROLE_" + role.toUpperCase())
                .toList());
        usuario.setPassword(passwordEncoder.encode(request.password()));
        usuario.setRoles(roles);
        usuario.setActivo(true);
        usuario.setVerificado(false);
        Usuario savedUser = usuarioRepository.save(usuario);

        Perfil perfil = perfilRepository.findByUsernameIgnoreCase(savedUser.getUsername()).orElseGet(Perfil::new);
        perfil.setUsername(savedUser.getUsername());
        perfil.setEmail(savedUser.getUsername());
        perfil.setRoles(roles);
        perfil.setActivo(true);
        perfil.setVerificado(false);
        String nombre = (String) session.getAttribute("oauth2_nombre");
        String apellido = (String) session.getAttribute("oauth2_apellido");
        if (perfil.getNombre() == null || perfil.getNombre().isBlank()) {
            perfil.setNombre(nombre == null || nombre.isBlank() ? savedUser.getUsername() : nombre);
        }
        if (perfil.getApellido() == null) {
            perfil.setApellido(apellido == null ? "" : apellido);
        }
        if (roles.contains("ROLE_CLIENT") && perfil.getCliente() == null) perfil.setDetallesCliente(new Cliente());
        if (roles.contains("ROLE_CONTRACTOR") && perfil.getContratista() == null) {
            Contratista contratista = new Contratista();
            contratista.setCalificacionPromedio(0.0);
            perfil.setDetallesContratista(contratista);
        }
        if (roles.contains("ROLE_WORKER") && perfil.getTrabajador() == null) perfil.setTrabajador(new Trabajador());
        perfilRepository.save(perfil);

        String token = jwtService.generarToken(savedUser.getUsername(), roles.stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList()));
        session.removeAttribute("usuario");
        session.removeAttribute("userRole");
        session.removeAttribute("userRoles");
        session.removeAttribute("oauth2_nombre");
        session.removeAttribute("oauth2_apellido");
        session.removeAttribute("oauth2_pending_registration");

        return ResponseEntity.ok(Map.of("token", token, "tipo", "Bearer", "username", savedUser.getUsername(), "roles", roles));
    }
}