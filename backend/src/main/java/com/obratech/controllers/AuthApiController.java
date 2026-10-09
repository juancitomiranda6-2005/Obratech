package com.obratech.controllers;

import java.util.Map;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.obratech.security.JwtService;
import com.obratech.entity.Usuario;
import com.obratech.service.PasswordResetService;
import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping("/api/auth")
public class AuthApiController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final PasswordResetService passwordResetService;

    public AuthApiController(
            AuthenticationManager authenticationManager,
            JwtService jwtService,
            PasswordResetService passwordResetService) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.passwordResetService = passwordResetService;
    }

    public record LoginRequest(String username, String password) {}
    public record PasswordResetRequest(String email) {}
    public record PasswordUpdateRequest(String token, String password) {}

    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> requestPasswordReset(@RequestBody PasswordResetRequest request) {
        passwordResetService.requestReset(request == null ? null : request.email());
        return ResponseEntity.ok(Map.of("message",
                "Si la cuenta existe y el correo está configurado, recibirás instrucciones para restablecer la contraseña."));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody PasswordUpdateRequest request) {
        try {
            passwordResetService.resetPassword(
                    request == null ? null : request.token(),
                    request == null ? null : request.password());
            return ResponseEntity.ok(Map.of("message", "La contraseña se actualizó correctamente."));
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().body(Map.of("error", exception.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        try {
            String username = loginRequest.username().trim().toLowerCase();

            Authentication auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, loginRequest.password())
            );

            String token = jwtService.generarToken(auth.getName(), auth.getAuthorities());

            return ResponseEntity.ok(Map.of(
                    "token",    token,
                    "tipo",     "Bearer",
                    "username", auth.getName()
            ));

        } catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Credenciales incorrectas"));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error interno del servidor"));
        }
    }

    @GetMapping("/oauth-session")
    public ResponseEntity<?> exchangeOAuthSession(HttpSession session) {
        Usuario usuario = (Usuario) session.getAttribute("usuario");
        if (usuario == null || usuario.getRoles() == null || usuario.getRoles().isEmpty()
                || usuario.getRoles().contains("ROLE_GUEST")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "No hay una sesión OAuth lista para usar."));
        }
        List<SimpleGrantedAuthority> authorities = usuario.getRoles().stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
        return ResponseEntity.ok(Map.of(
                "token", jwtService.generarToken(usuario.getUsername(), authorities),
                "tipo", "Bearer",
                "username", usuario.getUsername(),
                "roles", usuario.getRoles()));
    }
}
