package com.obratech.runners;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.obratech.entity.Perfil;
import com.obratech.repository.PerfilRepository;
import com.obratech.repository.UsuarioRepository;
import com.obratech.service.PerfilVerificationService;

@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class ProfileVerificationRunner implements CommandLineRunner {

    private final PerfilRepository perfilRepository;
    private final UsuarioRepository usuarioRepository;
    private final PerfilVerificationService verificationService;

    public ProfileVerificationRunner(
            PerfilRepository perfilRepository,
            UsuarioRepository usuarioRepository,
            PerfilVerificationService verificationService) {
        this.perfilRepository = perfilRepository;
        this.usuarioRepository = usuarioRepository;
        this.verificationService = verificationService;
    }

    @Override
    public void run(String... args) {
        for (Perfil perfil : perfilRepository.findAll()) {
            var usuario = usuarioRepository.findByUsernameIgnoreCase(perfil.getUsername()).orElse(null);
            boolean wasVerified = Boolean.TRUE.equals(perfil.getVerificado());
            boolean userWasVerified = usuario != null && usuario.isVerificado();
            boolean verified = verificationService.synchronizeVerification(perfil, usuario);

            if (wasVerified != verified || userWasVerified != verified) {
                perfilRepository.save(perfil);
                if (usuario != null) {
                    usuarioRepository.save(usuario);
                }
            }
        }
    }
}