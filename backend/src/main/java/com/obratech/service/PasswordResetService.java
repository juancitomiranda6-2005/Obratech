package com.obratech.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.mail.MailProperties;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.obratech.entity.PasswordResetToken;
import com.obratech.entity.Usuario;
import com.obratech.repository.PasswordResetTokenRepository;
import com.obratech.repository.UsuarioRepository;

@Service
public class PasswordResetService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PasswordResetService.class);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int TOKEN_BYTES = 32;
    private static final int TOKEN_LIFETIME_MINUTES = 30;

    private final UsuarioRepository usuarioRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final MailProperties mailProperties;
    private final String frontendUrl;
    private final String fromAddress;

    public PasswordResetService(
            UsuarioRepository usuarioRepository,
            PasswordResetTokenRepository tokenRepository,
            PasswordEncoder passwordEncoder,
            ObjectProvider<JavaMailSender> mailSenderProvider,
            MailProperties mailProperties,
            @Value("${app.frontend-url:http://localhost:5173}") String frontendUrl,
            @Value("${app.mail.from:no-reply@obratech.local}") String fromAddress) {
        this.usuarioRepository = usuarioRepository;
        this.tokenRepository = tokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.mailSenderProvider = mailSenderProvider;
        this.mailProperties = mailProperties;
        this.frontendUrl = frontendUrl;
        this.fromAddress = fromAddress;
    }

    public void requestReset(String email) {
        if (email == null || email.isBlank() || mailProperties.getHost() == null || mailProperties.getHost().isBlank()) {
            return;
        }

        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        Usuario usuario = usuarioRepository.findByUsernameIgnoreCase(email.trim()).orElse(null);
        if (mailSender == null || usuario == null || usuario.getId() == null) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        if (tokenRepository.findByUserIdAndExpiresAtAfter(usuario.getId(), now).isPresent()) {
            return;
        }
        tokenRepository.deleteAllByUserId(usuario.getId());

        byte[] randomBytes = new byte[TOKEN_BYTES];
        SECURE_RANDOM.nextBytes(randomBytes);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);

        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setUserId(usuario.getId());
        resetToken.setTokenHash(hash(rawToken));
        resetToken.setExpiresAt(now.plusMinutes(TOKEN_LIFETIME_MINUTES));
        tokenRepository.save(resetToken);

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(usuario.getUsername());
        message.setSubject("Restablece tu contraseña de ObraTech");
        message.setText("Usa este enlace dentro de los próximos 30 minutos para elegir una nueva contraseña:\n\n"
                + frontendUrl.replaceAll("/$", "") + "/restablecer-contrasena?token=" + rawToken
                + "\n\nSi no solicitaste el cambio, puedes ignorar este mensaje.");

        try {
            mailSender.send(message);
        } catch (MailException exception) {
            tokenRepository.delete(resetToken);
            LOGGER.warn("No se pudo enviar el correo de recuperación de contraseña.");
        }
    }

    public void resetPassword(String token, String newPassword) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("El enlace de recuperación no es válido o ya venció.");
        }
        if (newPassword == null || newPassword.length() < 8) {
            throw new IllegalArgumentException("La contraseña debe tener al menos 8 caracteres.");
        }

        PasswordResetToken resetToken = tokenRepository.findByTokenHash(hash(token))
                .orElseThrow(() -> new IllegalArgumentException("El enlace de recuperación no es válido o ya venció."));
        if (resetToken.getExpiresAt() == null || !resetToken.getExpiresAt().isAfter(LocalDateTime.now())) {
            tokenRepository.delete(resetToken);
            throw new IllegalArgumentException("El enlace de recuperación no es válido o ya venció.");
        }

        Usuario usuario = usuarioRepository.findById(resetToken.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("El enlace de recuperación no es válido o ya venció."));
        usuario.setPassword(passwordEncoder.encode(newPassword));
        usuario.setIntentosFallidos(0);
        usuario.setBloqueadoHasta(null);
        usuarioRepository.save(usuario);
        tokenRepository.deleteAllByUserId(usuario.getId());
    }

    private String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (java.security.NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 no está disponible.", exception);
        }
    }
}