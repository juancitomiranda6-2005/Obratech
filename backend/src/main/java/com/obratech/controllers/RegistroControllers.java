package com.obratech.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.obratech.entity.Usuario;
import com.obratech.service.UsuarioService;

import org.springframework.beans.factory.annotation.Autowired;

import com.obratech.entity.Perfil;
import com.obratech.repository.PerfilRepository;
import com.obratech.repository.UsuarioRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import org.springframework.security.crypto.password.PasswordEncoder;
import com.obratech.entity.Cliente;
import com.obratech.entity.Contratista;
import com.obratech.entity.Trabajador;

@Controller
public class RegistroControllers {

    private final UsuarioService usuarioService;
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private PerfilRepository perfilRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    public RegistroControllers(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping("/registro")
    public String mostrarRegistro() {
        return "registro";
    }

    @PostMapping("/registro")
    public String procesarRegistro(
            @RequestParam String username,
            @RequestParam String password,
            @RequestParam(name = "roles", required = false) java.util.List<String> rolesList,
            Model model) {

        if (rolesList == null || rolesList.isEmpty()) {
            model.addAttribute("mensaje", "Debes seleccionar al menos un tipo de usuario.");
            return "registro";
        }

        try {
            Usuario nuevo = new Usuario();
            nuevo.setUsername(username.trim().toLowerCase());
            nuevo.setPassword(password);

            java.util.Set<String> setRoles = new java.util.HashSet<>(rolesList);
            nuevo.setRoles(setRoles);

            usuarioService.register(nuevo);
            System.out.println("[REGISTRO] Usuario registrado exitosamente: " + username);

            return "redirect:/login?registered=true";

        } catch (IllegalArgumentException ex) {
            System.err.println("[REGISTRO] Error de validacin: " + ex.getMessage());
            model.addAttribute("mensaje", ex.getMessage());
            model.addAttribute("tipoMensaje", "error");
            return "registro";
        } catch (Exception ex) {
            System.err.println("[REGISTRO] Error inesperado: " + ex.getMessage());
            ex.printStackTrace();
            model.addAttribute("mensaje", "Ocurrió un error inesperado: " + ex.getMessage());
            model.addAttribute("tipoMensaje", "error");
            return "registro";
        }
    }

    @GetMapping("/completar-registro-oauth2")
    public String mostrarCompletarRegistro(HttpSession session, Model model) {
        Usuario usuario = (Usuario) session.getAttribute("usuario");

        if (usuario == null)
            return "redirect:/login";

        boolean tieneRolesNegocio = usuario.getRoles() != null && usuario.getRoles().stream()
                .anyMatch(r -> r.equals("ROLE_CLIENT") || r.equals("ROLE_CONTRACTOR") || r.equals("ROLE_WORKER"));

        if (tieneRolesNegocio && !usuario.getRoles().contains("ROLE_GUEST")) {
            return "redirect:/desboard";
        }

        return "completar-registro-oauth2";
    }

    @PostMapping("/completar-registro-oauth2")
    public String procesarCompletarRegistro(
            @RequestParam(name = "roles", required = false) java.util.List<String> rolesList,
            @RequestParam String password,
            HttpSession session,
            HttpServletRequest request,
            Model model) {

        Usuario sessionUsuario = (Usuario) session.getAttribute("usuario");
        if (sessionUsuario == null)
            return "redirect:/login";

        if (rolesList == null || rolesList.isEmpty()) {
            model.addAttribute("mensaje", "Debes seleccionar al menos un rol para continuar.");
            return "completar-registro-oauth2";
        }

        if (password == null || password.length() < 8) {
            model.addAttribute("mensaje", "La contraseña debe tener al menos 8 caracteres.");
            return "completar-registro-oauth2";
        }

        try {
            Usuario dbUsuario = usuarioRepository.findByUsername(sessionUsuario.getUsername()).orElse(null);
            if (dbUsuario == null)
                return "redirect:/login";

            dbUsuario.setPassword(passwordEncoder.encode(password));
            java.util.Set<String> rolesFinales = new java.util.HashSet<>();
            for (String r : rolesList) {
                if (r.equalsIgnoreCase("cliente"))
                    rolesFinales.add("ROLE_CLIENT");
                if (r.equalsIgnoreCase("contratista"))
                    rolesFinales.add("ROLE_CONTRACTOR");
                if (r.equalsIgnoreCase("trabajador"))
                    rolesFinales.add("ROLE_WORKER");
            }
            dbUsuario.setRoles(rolesFinales);
            dbUsuario.setActivo(true); // Permitir inicio de sesión inmediato
            dbUsuario.setVerificado(false); // Pendiente de verificación por el admin
            usuarioRepository.save(dbUsuario);

            Perfil perfil = perfilRepository.findByUsername(dbUsuario.getUsername()).orElse(new Perfil());
            perfil.setUsername(dbUsuario.getUsername());
            perfil.setEmail(dbUsuario.getUsername());
            perfil.setRoles(rolesFinales);
            perfil.setActivo(true);
            perfil.setVerificado(false); // No verificado inicialmente

            String oauth2Nombre = (String) session.getAttribute("oauth2_nombre");
            String oauth2Apellido = (String) session.getAttribute("oauth2_apellido");
            if (perfil.getNombre() == null)
                perfil.setNombre(oauth2Nombre != null ? oauth2Nombre : dbUsuario.getUsername());
            if (perfil.getApellido() == null)
                perfil.setApellido(oauth2Apellido != null ? oauth2Apellido : "");

            if (rolesFinales.contains("ROLE_CLIENT") && perfil.getCliente() == null) {
                perfil.setDetallesCliente(new Cliente());
            }
            if (rolesFinales.contains("ROLE_CONTRACTOR") && perfil.getContratista() == null) {
                Contratista dc = new Contratista();
                dc.setCalificacionPromedio(0.0);
                perfil.setDetallesContratista(dc);
            }
            if (rolesFinales.contains("ROLE_WORKER") && perfil.getTrabajador() == null) {
                Trabajador t = new Trabajador();
                t.setDisponibilidad(true);
                perfil.setTrabajador(t);
            }
            perfilRepository.save(perfil);

            session.removeAttribute("usuario");
            session.removeAttribute("userRole");
            session.removeAttribute("userRoles");
            session.removeAttribute("oauth2_nombre");
            session.removeAttribute("oauth2_apellido");

            System.out.println("[OAuth2] Registro premium completado (activo y pendiente verificación) para: "
                    + dbUsuario.getUsername());
            return "redirect:/login?registered=true";

        } catch (Exception e) {
            System.err.println("[OAuth2] Error en registro premium: " + e.getMessage());
            model.addAttribute("mensaje", "Ocurrió un error al procesar tu registro. Por favor intenta de nuevo.");
            return "completar-registro-oauth2";
        }
    }
}
