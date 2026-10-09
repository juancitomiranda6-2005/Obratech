package com.obratech.security;

import java.io.IOException;
import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import com.obratech.entity.Usuario;
import com.obratech.repository.PerfilRepository;
import com.obratech.repository.UsuarioRepository;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@Component
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    @Value("${app.frontend-url:http://localhost:5173}")
    private String frontendUrl;

    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private PerfilRepository perfilRepository;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication)
            throws IOException, ServletException {

        String email;
        String nombre;
        String apellido;

        try {
            System.out.println("[DEBUG-OAuth2] Iniciando proceso de éxito de autenticación...");
            Object principal = authentication.getPrincipal();
            System.out.println("[DEBUG-OAuth2] Principal detectado: " + principal.getClass().getName());

            if (principal instanceof OidcUser oidcUser) {
                email = oidcUser.getEmail();
                nombre = oidcUser.getGivenName();
                apellido = oidcUser.getFamilyName();

            } else if (principal instanceof OAuth2User oauth2User) {
                email = oauth2User.getAttribute("email");
                nombre = oauth2User.getAttribute("given_name");
                apellido = oauth2User.getAttribute("family_name");

            } else {
                System.err.println("[OAuth2] Principal de tipo desconocido: "
                        + principal.getClass().getName());
                response.sendRedirect("/login?error=google");
                return;
            }

            if (email == null || email.isBlank()) {
                System.err.println("[DEBUG-OAuth2] ERROR: Google no proporcionó un email válido.");
                response.sendRedirect("/login?error=google");
                return;
            }
            System.out.println("[DEBUG-OAuth2] Email extraído: " + email);

            final String finalEmail = email.trim().toLowerCase();
            final String finalNombre = (nombre != null && !nombre.isBlank()) ? nombre : finalEmail;
            final String finalApellido = (apellido != null && !apellido.isBlank()) ? apellido : "";

            System.out.println("[DEBUG-OAuth2] Buscando usuario en base de datos...");
            Usuario usuario = usuarioRepository.findByUsername(finalEmail).orElse(null);
            boolean esNuevo = false;

            if (usuario == null) {
                System.out.println("[DEBUG-OAuth2] Usuario no encontrado. Creando nuevo perfil GUEST...");
                esNuevo = true;
                usuario = new Usuario();
                usuario.setUsername(finalEmail);
                usuario.setPassword("");
                usuario.setRoles(new java.util.HashSet<>(java.util.Collections.singleton("ROLE_GUEST")));
                usuario.setActivo(false);
                usuario.setVerificado(false);
                usuario.setCreado(LocalDateTime.now());
                usuario = usuarioRepository.save(usuario);
                System.out.println("[OAuth2] Creado nuevo usuario GUEST: " + finalEmail);
            } else {
                System.out.println("[DEBUG-OAuth2] Usuario existente encontrado: " + usuario.getUsername()
                        + " con roles: " + usuario.getRoles());
                boolean tienePerfil = perfilRepository.findByUsername(finalEmail).isPresent();
                boolean tieneRolesNegocio = usuario.getRoles().stream()
                        .anyMatch(
                                r -> r.equals("ROLE_CLIENT") || r.equals("ROLE_CONTRACTOR") || r.equals("ROLE_WORKER"));

                if (!tienePerfil || !tieneRolesNegocio) {
                    esNuevo = true;
                    System.out.println("[OAuth2] Usuario existente sin perfil o roles de negocio: " + finalEmail);
                }
            }

            usuario.setUltimoAcceso(LocalDateTime.now());
            usuario = usuarioRepository.save(usuario);

            HttpSession session = request.getSession(true);
            session.setAttribute("usuario", usuario);
            session.setAttribute("oauth2_pending_registration", esNuevo);
            session.setAttribute("userRole", usuario.getRole());
            session.setAttribute("userRoles", usuario.getRoles());
            session.setAttribute("oauth2_nombre", finalNombre);
            session.setAttribute("oauth2_apellido", finalApellido);

            try {
                java.util.List<org.springframework.security.core.authority.SimpleGrantedAuthority> authorities = usuario
                        .getRoles().stream()
                        .map(org.springframework.security.core.authority.SimpleGrantedAuthority::new)
                        .collect(java.util.stream.Collectors.toList());

                org.springframework.security.authentication.UsernamePasswordAuthenticationToken newAuth = new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                        authentication.getPrincipal(),
                        null,
                        authorities);

                org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(newAuth);

                session.setAttribute("SPRING_SECURITY_CONTEXT",
                        org.springframework.security.core.context.SecurityContextHolder.getContext());

                System.out.println("[DEBUG-OAuth2] SecurityContext sincronizado con roles: " + usuario.getRoles());
            } catch (Exception authEx) {
                System.err.println("[DEBUG-OAuth2] Error sincronizando SecurityContext: " + authEx.getMessage());
            }

            String targetUrl = frontendUrl + (esNuevo ? "/completar-registro-oauth2" : "/oauth/callback");
            System.out.println("[DEBUG-OAuth2] Finalizando. Redirigiendo a: " + targetUrl);

            response.sendRedirect(targetUrl);
            return;

        } catch (Exception e) {
            System.err.println("[OAuth2] Error durante el login con Google: "
                    + e.getClass().getSimpleName() + " - " + e.getMessage());

            if (!response.isCommitted()) {
                response.sendRedirect("/login?error=google");
            }
        }
    }
}
