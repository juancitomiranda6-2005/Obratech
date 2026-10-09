package com.obratech.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.mail.MailProperties;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.obratech.entity.PasswordResetToken;
import com.obratech.entity.Usuario;
import com.obratech.repository.PasswordResetTokenRepository;
import com.obratech.repository.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordResetTokenRepository tokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private ObjectProvider<JavaMailSender> mailSenderProvider;

    @Mock
    private MailProperties mailProperties;

    @InjectMocks
    private PasswordResetService service;

    @Test
    void resetsPasswordAndConsumesToken() {
        PasswordResetToken token = validToken();
        Usuario usuario = new Usuario();
        usuario.setId("user-1");
        when(tokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(token));
        when(usuarioRepository.findById("user-1")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.encode("new-password-123")).thenReturn("encoded-password");

        service.resetPassword("raw-token", "new-password-123");

        ArgumentCaptor<String> tokenHash = ArgumentCaptor.forClass(String.class);
        verify(tokenRepository).findByTokenHash(tokenHash.capture());
        assertNotEquals("raw-token", tokenHash.getValue());
        assertEquals("encoded-password", usuario.getPassword());
        assertEquals(0, usuario.getIntentosFallidos());
        verify(usuarioRepository).save(usuario);
        verify(tokenRepository).deleteAllByUserId("user-1");
    }

    @Test
    void rejectsExpiredTokenBeforeLoadingUser() {
        PasswordResetToken token = validToken();
        token.setExpiresAt(LocalDateTime.now().minusMinutes(1));
        when(tokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(token));

        assertThrows(IllegalArgumentException.class, () -> service.resetPassword("raw-token", "new-password-123"));

        verify(tokenRepository).delete(token);
        verifyNoInteractions(usuarioRepository);
    }

    private PasswordResetToken validToken() {
        PasswordResetToken token = new PasswordResetToken();
        token.setUserId("user-1");
        token.setTokenHash("stored-hash");
        token.setExpiresAt(LocalDateTime.now().plusMinutes(10));
        return token;
    }
}