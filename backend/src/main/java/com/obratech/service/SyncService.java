package com.obratech.service;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.obratech.entity.Usuario;
import com.obratech.repository.UsuarioRepository;

@Component
public class SyncService implements ApplicationRunner {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public SyncService(UsuarioRepository usuarioRepository,
                    PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {

        ensureAdminExists();

        System.out.println("[SyncRunner] sincronizacin completada");
    }

    private void ensureAdminExists() {

        String adminEmail = "admin@gmail.com";

        boolean exists = usuarioRepository.findByUsername(adminEmail).isPresent();

        if (!exists) {
            Usuario admin = new Usuario();
            admin.setUsername(adminEmail);
            admin.setPassword(passwordEncoder.encode("administrador2026"));
            admin.setRole("ROLE_ADMIN");

            usuarioRepository.save(admin);

            System.out.println("[SyncRunner] Admin creado");
        } else {
            System.out.println("[SyncRunner] Admin ya existe");
        }
    }
}
