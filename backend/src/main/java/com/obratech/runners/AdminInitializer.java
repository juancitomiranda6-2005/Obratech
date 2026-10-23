package com.obratech.runners;

import java.time.LocalDateTime;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.obratech.entity.Perfil;
import com.obratech.entity.Usuario;
import com.obratech.repository.PerfilRepository;
import com.obratech.repository.UsuarioRepository;

@Component
@Order(1)
public class AdminInitializer implements CommandLineRunner {

    private final UsuarioRepository usuarioRepo;
    private final PerfilRepository perfilRepo;
    private final PasswordEncoder passwordEncoder;

    public AdminInitializer(UsuarioRepository usuarioRepo,
                            PerfilRepository perfilRepo,
                            PasswordEncoder passwordEncoder) {
        this.usuarioRepo = usuarioRepo;
        this.perfilRepo = perfilRepo;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        ensureAdminUser("admin@obratech.com", "Admin2026!");
    }

    private void ensureAdminUser(String adminEmail, String rawPassword) {
        if (usuarioRepo.findByUsernameIgnoreCase(adminEmail).isEmpty()) {
            Usuario admin = new Usuario();
            admin.setUsername(adminEmail);
            admin.setPassword(passwordEncoder.encode(rawPassword));
            admin.setRole("ROLE_ADMIN");
            admin.setActivo(true);
            admin.setVerificado(true);
            usuarioRepo.save(admin);
            System.out.println("[AdminInitializer] Usuario admin creado: " + adminEmail);
        } else {
            System.out.println("[AdminInitializer] El usuario admin ya existe.");
        }

        if (perfilRepo.findByUsernameIgnoreCase(adminEmail).isEmpty()) {
            Perfil perfilAdmin = new Perfil("Administrador", adminEmail);
            perfilAdmin.setEmail(adminEmail);
            perfilAdmin.setRole("ROLE_ADMIN");
            perfilAdmin.setActivo(true);
            perfilAdmin.setVerificado(true);
            perfilAdmin.setCreado(LocalDateTime.now());
            perfilRepo.save(perfilAdmin);
        }
    }
}
