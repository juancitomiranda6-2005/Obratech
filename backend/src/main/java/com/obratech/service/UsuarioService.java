package com.obratech.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.obratech.entity.Perfil;
import com.obratech.entity.Usuario;
import com.obratech.repository.PerfilRepository;
import com.obratech.repository.UsuarioRepository;

@Service
public class UsuarioService {

    private final UsuarioRepository repo;
    private final PasswordEncoder passwordEncoder;
    private final PerfilRepository perfilRepository;

    public UsuarioService(UsuarioRepository repo, PasswordEncoder passwordEncoder,
                          PerfilRepository perfilRepository) {
        this.repo = repo;
        this.passwordEncoder = passwordEncoder;
        this.perfilRepository = perfilRepository;
    }

    public List<Usuario> findAll() { return repo.findAll(); }

    public Optional<Usuario> findById(String id) { 
        @SuppressWarnings("null")
        String safeId = id != null ? id : "";
        return repo.findById(safeId); 
    }

    public Optional<Usuario> findByUsername(String username) { return repo.findByUsername(username); }

    public Usuario register(Usuario u) {
        if (u.getUsername() == null || u.getUsername().isBlank())
            throw new IllegalArgumentException("El correo electrónico no puede estar vacío.");
        u.setUsername(u.getUsername().trim().toLowerCase());
        if (!u.getUsername().matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$"))
            throw new IllegalArgumentException("Debes ingresar un correo electrónico válido.");
        if (u.getPassword() == null || u.getPassword().isBlank())
            throw new IllegalArgumentException("La contraseña no puede estar vacía.");
        if (u.getPassword().length() < 8)
            throw new IllegalArgumentException("La contraseña debe tener al menos 8 caracteres.");
        if (repo.findByUsername(u.getUsername()).isPresent())
            throw new IllegalArgumentException("El correo electrónico ya está registrado.");

        java.util.Set<String> rolesNormalizados = new java.util.HashSet<>();
        if (u.getRoles() != null) {
            for (String r : u.getRoles()) {
                rolesNormalizados.add(mapearRol(r));
            }
        }
        if (rolesNormalizados.isEmpty()) {
            rolesNormalizados.add("ROLE_USER");
        }
        u.setRoles(rolesNormalizados);

        u.setPassword(passwordEncoder.encode(u.getPassword()));
        u.setCreado(LocalDateTime.now());
        u.setActivo(true); // Se activa automáticamente en desarrollo para permitir login inmediato

        Usuario savedUser = repo.save(u);

        boolean esRolDeNegocio = savedUser.getRoles().contains("ROLE_WORKER") || 
                                 savedUser.getRoles().contains("ROLE_CONTRACTOR") || 
                                 savedUser.getRoles().contains("ROLE_CLIENT");

        if (esRolDeNegocio && perfilRepository.findByUsername(savedUser.getUsername()).isEmpty()) {
            Perfil p = new Perfil();
            p.setUsername(savedUser.getUsername());
            p.setEmail(savedUser.getUsername());
            p.setRoles(savedUser.getRoles());
            p.setActivo(true); // Activo por defecto para permitir login inmediato
            p.setVerificado(false);
            p.setCreado(LocalDateTime.now());

            if (savedUser.getRoles().contains("ROLE_WORKER")) {
                p.setDisponibilidad(true);
            }

            if (savedUser.getRoles().contains("ROLE_CONTRACTOR")) {
                p.setCalificacionPromedio(0.0);
            }

            if (savedUser.getRoles().contains("ROLE_CLIENT")) {
            }

            perfilRepository.save(p);
        }

        return savedUser;
    }

    public void toggleActivo(String id) {
        @SuppressWarnings("null")
        String safeId = id != null ? id : "";
        repo.findById(safeId).ifPresent(u -> {
            boolean nuevoEstado = !u.isActivo();
            u.setActivo(nuevoEstado);
            repo.save(u);
            perfilRepository.findByUsername(u.getUsername()).ifPresent(p -> {
                p.setActivo(nuevoEstado);
                perfilRepository.save(p);
            });
        });
    }

    public void deleteById(String id) { 
        @SuppressWarnings("null")
        String safeId = id != null ? id : "";
        repo.findById(safeId).ifPresent(u -> {
            perfilRepository.findByUsername(u.getUsername()).ifPresent(p -> perfilRepository.deleteById(p.getId()));
        });
        repo.deleteById(safeId); 
    }

    private String mapearRol(String roleSolicitado) {
        if (roleSolicitado == null) return "ROLE_USER";
        return switch (roleSolicitado.toLowerCase()) {
            case "contratista" -> "ROLE_CONTRACTOR";
            case "cliente"     -> "ROLE_CLIENT";
            case "trabajador"  -> "ROLE_WORKER";
            default            -> "ROLE_USER";
        };
    }
}
