package com.obratech.runners;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.obratech.entity.Usuario;
import com.obratech.repository.UsuarioRepository;

@Component
@Order(1)
public class AdminInitializer implements CommandLineRunner {

    private final UsuarioRepository usuarioRepo;
    private final PasswordEncoder passwordEncoder;

    public AdminInitializer(UsuarioRepository usuarioRepo, PasswordEncoder passwordEncoder) {
        this.usuarioRepo = usuarioRepo;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        String adminEmail = "admin@obratech.com";

        if (usuarioRepo.findByUsername(adminEmail).isEmpty()) {
            Usuario admin = new Usuario();
            admin.setUsername(adminEmail);
            admin.setPassword(passwordEncoder.encode("Admin2026!"));
            admin.setRole("ROLE_ADMIN");
            admin.setActivo(true);
            admin.setVerificado(true);
            usuarioRepo.save(admin);
            System.out.println("[AdminInitializer] Usuario admin creado: " + adminEmail);
        } else {
            System.out.println("[AdminInitializer] El usuario admin ya existe.");
        }
    }
}
