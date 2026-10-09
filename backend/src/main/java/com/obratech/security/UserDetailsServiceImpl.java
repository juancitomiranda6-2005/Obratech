package com.obratech.security;

import java.time.LocalDateTime;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.obratech.entity.Usuario;
import com.obratech.repository.UsuarioRepository;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    public UserDetailsServiceImpl(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        String normalizedUsername = username.trim().toLowerCase();
        
        Usuario usuario = usuarioRepository.findByUsername(normalizedUsername)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + normalizedUsername));

        if (!usuario.isActivo()) {
            throw new UsernameNotFoundException("La cuenta ha sido desactivada. Contacta al administrador.");
        }

        if (usuario.getBloqueadoHasta() != null && LocalDateTime.now().isBefore(usuario.getBloqueadoHasta())) {
            throw new UsernameNotFoundException("Cuenta temporalmente bloqueada. Intenta de nuevo ms tarde.");
        }

        java.util.List<GrantedAuthority> authorities = usuario.getRoles().stream()
                .map(SimpleGrantedAuthority::new)
                .collect(java.util.stream.Collectors.toList());

        return User.builder()
                .username(usuario.getUsername())
                .password(usuario.getPassword())
                .authorities(authorities)
                .accountLocked(usuario.getBloqueadoHasta() != null && LocalDateTime.now().isBefore(usuario.getBloqueadoHasta()))
                .disabled(!usuario.isActivo())
                .build();
    }
}
