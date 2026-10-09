package com.obratech.controllers;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.obratech.entity.InvitacionTrabajo;
import com.obratech.entity.Perfil;
import com.obratech.entity.Proyecto;
import com.obratech.entity.Usuario;
import com.obratech.entity.enums.EstadoEjecucion;
import com.obratech.repository.InvitacionTrabajoRepository;
import com.obratech.repository.PerfilRepository;
import com.obratech.repository.ProyectoRepository;
import jakarta.servlet.http.HttpSession;

/**
 * Controlador principal para gestionar el redireccionamiento y presentación
 * de los paneles de control (dashboards) según el rol del usuario autenticado.
 */
@Controller
public class DesboardController {

    @Autowired private PerfilRepository perfilRepository;
    @Autowired private ProyectoRepository proyectoRepository;
    @Autowired private InvitacionTrabajoRepository invitacionTrabajoRepository;
    @Autowired private com.obratech.repository.UsuarioRepository usuarioRepository;
    @Autowired private com.obratech.service.PerfilVerificationService perfilVerificationService;

    /**
     * Obtiene el usuario autenticado desde la sesión HTTP o desde el SecurityContext.
     */
    private Usuario obtenerUsuarioDeSesionOContexto(HttpSession session) {
        Usuario usuario = (Usuario) session.getAttribute("usuario");
        if (usuario != null) {
            return usuario;
        }

        org.springframework.security.core.Authentication auth = 
            org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        
        if (auth != null && auth.isAuthenticated() && !(auth instanceof org.springframework.security.authentication.AnonymousAuthenticationToken)) {
            String username = null;
            Object principal = auth.getPrincipal();
            
            if (principal instanceof org.springframework.security.core.userdetails.UserDetails userDetails) {
                username = userDetails.getUsername();
            } else if (principal instanceof org.springframework.security.oauth2.core.user.OAuth2User oauth2User) {
                username = oauth2User.getAttribute("email");
                if (username == null || username.isBlank()) {
                    username = oauth2User.getName();
                }
            } else if (principal instanceof String principalString) {
                username = principalString;
            }

            if (username != null && !username.isBlank()) {
                final String finalUsername = username.trim().toLowerCase();
                Usuario dbUsuario = usuarioRepository.findByUsernameIgnoreCase(finalUsername).orElse(null);
                if (dbUsuario != null) {
                    session.setAttribute("usuario", dbUsuario);
                    session.setAttribute("userRole", dbUsuario.getRole());
                    session.setAttribute("userRoles", dbUsuario.getRoles());
                    System.out.println("[DEBUG-Sesion] Sesión restaurada automáticamente desde SecurityContext para: " + finalUsername);
                    return dbUsuario;
                }
            }
        }
        return null;
    }

    /**
     * Punto de entrada principal (/desboard). Redirige al dashboard según el rol o la selección de perfil.
     */
    @GetMapping("/desboard")
    public String mostrarDesboard(
            @org.springframework.web.bind.annotation.RequestParam(value = "errorVerificacion", required = false) Boolean errorVerificacion,
            HttpSession session, 
            Model model) {
        Usuario usuario = obtenerUsuarioDeSesionOContexto(session);
        if (usuario == null) return "redirect:/login";

        model.addAttribute("usuario", usuario);

        String suffix = (errorVerificacion != null && errorVerificacion) ? "?errorVerificacion=true" : "";

        if (usuario.getRoles() == null || usuario.getRoles().isEmpty()) {
            return "redirect:/login";
        }

        // Si posee múltiples roles de negocio, solicita seleccionar con cuál ingresar
        long rolesNegocio = usuario.getRoles().stream()
            .filter(r -> r.equals("ROLE_CLIENT") || r.equals("ROLE_CONTRACTOR") || r.equals("ROLE_WORKER"))
            .count();
        
        if (rolesNegocio > 1) {
            return "redirect:/seleccionar-perfil" + suffix;
        }

        if (usuario.getRoles().contains("ROLE_ADMIN")) return "redirect:/admin/desboard";
        if (usuario.getRoles().contains("ROLE_CONTRACTOR")) return "redirect:/desboard-contratista" + suffix;
        if (usuario.getRoles().contains("ROLE_WORKER")) return "redirect:/desboard-trabajador" + suffix;
        if (usuario.getRoles().contains("ROLE_CLIENT")) return "redirect:/desboard-cliente" + suffix;
        
        return "redirect:/login";
    }

    /**
     * Muestra la vista para seleccionar perfil cuando el usuario tiene múltiples roles.
     */
    @GetMapping("/seleccionar-perfil")
    public String mostrarSeleccionPerfil(HttpSession session, Model model) {
        Usuario usuario = obtenerUsuarioDeSesionOContexto(session);
        if (usuario == null) return "redirect:/login";
        model.addAttribute("usuario", usuario);
        return "seleccionar-perfil";
    }

    /**
     * Carga el dashboard para usuarios con rol Cliente (proyectos creados y métricas).
     */
    @GetMapping("/desboard-cliente")
    public String mostrarDesboardCliente(
            @org.springframework.web.bind.annotation.RequestParam(value = "errorVerificacion", required = false) Boolean errorVerificacion,
            HttpSession session, 
            Model model) {
        Usuario usuario = obtenerUsuarioDeSesionOContexto(session);
        if (usuario == null || !usuario.getRoles().contains("ROLE_CLIENT")) return "redirect:/login";
        
        Usuario dbUsuario = usuarioRepository.findByUsernameIgnoreCase(usuario.getUsername()).orElse(usuario);
        model.addAttribute("usuario", dbUsuario);
        model.addAttribute("usuarioVerificado", dbUsuario.isVerificado());
        model.addAttribute("errorVerificacion", errorVerificacion != null && errorVerificacion);
        Perfil cliente = perfilRepository.findByUsernameIgnoreCase(dbUsuario.getUsername()).orElse(null);
        model.addAttribute("perfilCompleto", perfilVerificationService.isComplete(cliente));
        model.addAttribute("requisitosFaltantes", perfilVerificationService.getMissingRequirements(cliente));

        List<Proyecto> proyectosDelCliente = proyectoRepository.findByClienteId(dbUsuario.getId());

        long total = proyectosDelCliente == null ? 0 : proyectosDelCliente.size();
        long enProgreso = proyectosDelCliente == null ? 0 : proyectosDelCliente.stream()
                .filter(p -> p.getEstadoEjecucion() != null && p.getEstadoEjecucion() != EstadoEjecucion.COMPLETADO)
                .count();
        long completados = proyectosDelCliente == null ? 0 : proyectosDelCliente.stream()
                .filter(p -> p.getEstadoEjecucion() == EstadoEjecucion.COMPLETADO)
                .count();

        model.addAttribute("totalProyectos", total);
        model.addAttribute("proyectosEnProgreso", enProgreso);
        model.addAttribute("proyectosCompletados", completados);
        model.addAttribute("proyectos", proyectosDelCliente);
        return "desboard";
    }

    /**
     * Carga el dashboard para usuarios con rol Contratista (proyectos asignados, calificación y equipo).
     */
    @GetMapping("/desboard-contratista")
    public String mostrarDesboardContratista(
            @org.springframework.web.bind.annotation.RequestParam(value = "errorVerificacion", required = false) Boolean errorVerificacion,
            HttpSession session, 
            Model model) {
        Usuario usuario = obtenerUsuarioDeSesionOContexto(session);
        if (usuario == null || !usuario.getRoles().contains("ROLE_CONTRACTOR")) return "redirect:/login";

        Usuario dbUsuario = usuarioRepository.findByUsernameIgnoreCase(usuario.getUsername()).orElse(usuario);
        model.addAttribute("usuario", dbUsuario);
        model.addAttribute("usuarioVerificado", dbUsuario.isVerificado());
        model.addAttribute("errorVerificacion", errorVerificacion != null && errorVerificacion);

        Perfil contratista = perfilRepository.findByUsernameIgnoreCase(dbUsuario.getUsername()).orElse(null);
        model.addAttribute("perfilCompleto", perfilVerificationService.isComplete(contratista));
        model.addAttribute("requisitosFaltantes", perfilVerificationService.getMissingRequirements(contratista));
        if (contratista != null) {
            model.addAttribute("contratista", contratista);
            
            String displayUsername = (contratista.getNombre() != null && !contratista.getNombre().isBlank()) 
                                    ? contratista.getNombre() : dbUsuario.getUsername();
            
            model.addAttribute("contratistaUsername",
                (displayUsername != null && !displayUsername.isEmpty()) 
                    ? displayUsername.substring(0, 1).toUpperCase() : "U");
                    
            model.addAttribute("contratistaCalificacionPromedio",
                contratista.getCalificacionPromedio() != null
                    ? String.format("%.1f", contratista.getCalificacionPromedio()) : "0.0");
            model.addAttribute("contratistaEmail",
                contratista.getEmail() != null ? contratista.getEmail() : "correo@ejemplo.com");
            model.addAttribute("contratistaEspecialidad",
                contratista.getEspecialidad() != null ? contratista.getEspecialidad() : "N/A");
            model.addAttribute("contratistaTelefono",
                contratista.getTelefono() != null ? contratista.getTelefono() : "N/A");
            model.addAttribute("contratistaUbicacion",
                contratista.getUbicacion() != null ? contratista.getUbicacion() : "N/A");
            
            List<Proyecto> proyectosActivos = proyectoRepository
                    .findByContratistaAsignadoIdAndEstadoEjecucionNot(contratista.getId(), com.obratech.entity.enums.EstadoEjecucion.COMPLETADO);
            List<Proyecto> proyectosCompletados = proyectoRepository
                    .findByContratistaAsignadoIdAndEstadoEjecucion(contratista.getId(), com.obratech.entity.enums.EstadoEjecucion.COMPLETADO);
            
            model.addAttribute("proyectosEnProgreso", proyectosActivos != null ? proyectosActivos.size() : 0);
            model.addAttribute("proyectosCompletados", proyectosCompletados != null ? proyectosCompletados.size() : 0);
            
            // Calcula trabajadores únicos involucrados en los proyectos activos
            java.util.Set<String> trabajadoresUnicos = new java.util.HashSet<>();
            if (proyectosActivos != null) {
                for (Proyecto proyecto : proyectosActivos) {
                    if (proyecto.getEquipoTrabajo() != null) {
                        for (Perfil perfil : proyecto.getEquipoTrabajo()) {
                            if (perfil.getId() != null) {
                                trabajadoresUnicos.add(perfil.getId());
                            }
                        }
                    }
                }
            }
            model.addAttribute("totalTrabajadoresEnEquipo", trabajadoresUnicos.size());
        } else {
            model.addAttribute("contratistaUsername", "U");
            model.addAttribute("contratistaCalificacionPromedio", "0.0");
            model.addAttribute("contratistaEmail", "correo@ejemplo.com");
            model.addAttribute("contratistaEspecialidad", "N/A");
            model.addAttribute("contratistaTelefono", "N/A");
            model.addAttribute("contratistaUbicacion", "N/A");
            model.addAttribute("proyectosEnProgreso", 0);
            model.addAttribute("proyectosCompletados", 0);
            model.addAttribute("totalTrabajadoresEnEquipo", 0);
        }

        return "desboard-contratista";
    }

    /**
     * Carga el dashboard para usuarios con rol Trabajador (invitaciones y proyectos asignados).
     */
    @GetMapping("/desboard-trabajador")
    public String mostrarDesboardTrabajador(
            @org.springframework.web.bind.annotation.RequestParam(value = "errorVerificacion", required = false) Boolean errorVerificacion,
            HttpSession session, 
            Model model) {
        Usuario usuario = obtenerUsuarioDeSesionOContexto(session);
        if (usuario == null || !usuario.getRoles().contains("ROLE_WORKER")) return "redirect:/login";

        Usuario dbUsuario = usuarioRepository.findByUsernameIgnoreCase(usuario.getUsername()).orElse(usuario);
        model.addAttribute("usuario", dbUsuario);
        model.addAttribute("usuarioVerificado", dbUsuario.isVerificado());
        model.addAttribute("errorVerificacion", errorVerificacion != null && errorVerificacion);

        Perfil trabajador = perfilRepository.findByUsernameIgnoreCase(dbUsuario.getUsername()).orElseGet(() -> {
            Perfil nuevo = new Perfil();
            nuevo.setUsername(dbUsuario.getUsername());
            nuevo.setRole("ROLE_WORKER");
            nuevo.setActivo(true);
            nuevo.setNombre(dbUsuario.getUsername());
            nuevo.setApellido("");
            nuevo.setDisponibilidad(false);
            return nuevo;
        });

        model.addAttribute("trabajador", trabajador);
        model.addAttribute("perfilCompleto", perfilVerificationService.isComplete(trabajador));
        model.addAttribute("requisitosFaltantes", perfilVerificationService.getMissingRequirements(trabajador));

        List<InvitacionTrabajo> invitacionesPendientes = (trabajador.getId() != null) 
                ? invitacionTrabajoRepository.findByTrabajadorIdAndEstado(trabajador.getId(), "PENDIENTE") 
                : new java.util.ArrayList<>();
        model.addAttribute("invitacionesPendientes", invitacionesPendientes);

        if (trabajador.getId() != null) {
            List<Proyecto> proyectosAsignados = proyectoRepository.findByEquipoTrabajoContaining(trabajador);
            model.addAttribute("proyectosAsignados", proyectosAsignados);
        } else {
            model.addAttribute("proyectosAsignados", new java.util.ArrayList<>());
        }

        return "desboard-trabajador";
    }
}
