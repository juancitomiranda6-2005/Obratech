// AdminController
package com.obratech.controllers;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.obratech.entity.Perfil;
import com.obratech.entity.Proyecto;
import com.obratech.entity.Usuario;
import com.obratech.entity.enums.EstadoValidacion;
import com.obratech.repository.CalificacionRepository;
import com.obratech.repository.PerfilRepository;
import com.obratech.repository.ProyectoRepository;
import com.obratech.repository.UsuarioRepository;

import jakarta.servlet.http.HttpSession;


@Controller
@RequestMapping("/admin")
public class AdminController {

    @Autowired private ProyectoRepository      proyectoRepository;
    @Autowired private PerfilRepository        perfilRepository;
    @Autowired private CalificacionRepository  calificacionRepository;
    @Autowired private UsuarioRepository       usuarioRepository;
    @Autowired private com.obratech.service.PerfilVerificationService perfilVerificationService;


    @GetMapping({"", "/", "/desboard"})
    public String dashboardAdmin(HttpSession session, Model model) {

        Usuario usuario = resolverUsuarioAdmin(session);
        if (usuario == null) return "redirect:/login";

        List<Proyecto> proyectosPendientes =
                proyectoRepository.findByEstadoValidacion(EstadoValidacion.PENDIENTE);
        List<Proyecto> proyectosAprobados =
                proyectoRepository.findByEstadoValidacion(EstadoValidacion.APROBADO);
        List<Proyecto> proyectosRechazados =
                proyectoRepository.findByEstadoValidacion(EstadoValidacion.RECHAZADO);

        List<Perfil> contratistas       = perfilRepository.findByRolesAndActivoTrue("ROLE_CONTRACTOR");
        List<Perfil> contratistasNoVer  = perfilRepository.findByRolesAndVerificadoFalse("ROLE_CONTRACTOR");

        List<Perfil> clientes           = perfilRepository.findByRolesAndActivoTrue("ROLE_CLIENT");
        List<Perfil> clientesNoVer      = perfilRepository.findByRolesAndVerificadoFalse("ROLE_CLIENT");

        List<Perfil> trabajadores       = perfilRepository.findByRoles("ROLE_WORKER");
        List<Perfil> trabajadoresNoVer  = perfilRepository.findByRolesAndVerificadoFalse("ROLE_WORKER");

        List<Perfil> contratistasInactivos = perfilRepository.findByRolesAndActivoFalse("ROLE_CONTRACTOR");
        List<Perfil> clientesInactivos     = perfilRepository.findByRolesAndActivoFalse("ROLE_CLIENT");
        List<Perfil> trabajadoresInactivos = perfilRepository.findByRolesAndActivoFalse("ROLE_WORKER");

        model.addAttribute("usuario",                  usuario);
        model.addAttribute("proyectosPendientes",      proyectosPendientes);
        model.addAttribute("proyectosAprobados",       proyectosAprobados);
        model.addAttribute("proyectosRechazados",      proyectosRechazados);
        model.addAttribute("totalProyectos",           proyectosPendientes.size());
        model.addAttribute("totalProyectosAprobados",  proyectosAprobados.size());
        model.addAttribute("totalProyectosRechazados", proyectosRechazados.size());
        model.addAttribute("contratistas",             contratistas);
        model.addAttribute("totalContratistas",        contratistas.size());
        model.addAttribute("clientes",                 clientes);
        model.addAttribute("totalClientes",            clientes.size());
        model.addAttribute("contratistasNoVerificados", contratistasNoVer);
        model.addAttribute("clientesNoVerificados",    clientesNoVer);
        model.addAttribute("trabajadoresNoVerificados", trabajadoresNoVer);
        model.addAttribute("trabajadores",             trabajadores);
        model.addAttribute("totalTrabajadores",        trabajadores.size());
        
        model.addAttribute("contratistasInactivos",    contratistasInactivos);
        model.addAttribute("clientesInactivos",        clientesInactivos);
        model.addAttribute("trabajadoresInactivos",    trabajadoresInactivos);
        model.addAttribute("totalInactivos",           contratistasInactivos.size() + clientesInactivos.size() + trabajadoresInactivos.size());

        model.addAttribute("statsTotalUsuarios",       usuarioRepository.count());
        model.addAttribute("statsTotalProyectos",      proyectoRepository.count());
        model.addAttribute("statsUsuariosPendientes",  contratistasNoVer.size() + clientesNoVer.size() + trabajadoresNoVer.size());
        model.addAttribute("statsProyectosPendientes", proyectosPendientes.size());

        return "desboard-admin";
    }

    @PostMapping("/proyectos/{id}/aprobar")
    public String aprobarProyecto(@PathVariable String id, HttpSession session) {
        if (!isAdmin(session)) return "redirect:/login";
        @SuppressWarnings("null")
        String safeId = id != null ? id : "";
        proyectoRepository.findById(safeId).ifPresent(p -> {
            p.setEstadoValidacion(EstadoValidacion.APROBADO);
            proyectoRepository.save(p);
        });
        return "redirect:/admin/desboard";
    }

    @PostMapping("/proyectos/{id}/rechazar")
    public String rechazarProyecto(@PathVariable String id, HttpSession session) {
        if (!isAdmin(session)) return "redirect:/login";
        @SuppressWarnings("null")
        String safeId2 = id != null ? id : "";
        proyectoRepository.findById(safeId2).ifPresent(p -> {
            p.setEstadoValidacion(EstadoValidacion.RECHAZADO);
            proyectoRepository.save(p);
        });
        return "redirect:/admin/desboard";
    }


    @PostMapping("/contratistas/{id}/toggle")
    public String toggleContratista(@PathVariable String id, HttpSession session) {
        if (!isAdmin(session)) return "redirect:/login";
        @SuppressWarnings("null")
        String safeId3 = id != null ? id : "";
        perfilRepository.findById(safeId3).ifPresent(p -> {
            boolean nuevoActivo = !Boolean.TRUE.equals(p.getActivo());
            p.setActivo(nuevoActivo);
            perfilRepository.save(p);
            usuarioRepository.findByUsernameIgnoreCase(p.getUsername()).ifPresent(u -> {
                u.setActivo(nuevoActivo);
                usuarioRepository.save(u);
            });
        });
        return "redirect:/admin/desboard";
    }

    @PostMapping("/usuarios/{id}/verificar")
    public String verificarUsuario(@PathVariable String id, HttpSession session, RedirectAttributes redirectAttributes) {
        if (!isAdmin(session)) return "redirect:/login";
        @SuppressWarnings("null")
        String safeId = id != null ? id : "";
        perfilRepository.findById(safeId).ifPresent(perfil -> {
            List<String> missing = perfilVerificationService.getMissingRequirements(perfil);
            if (!missing.isEmpty()) {
                redirectAttributes.addFlashAttribute(
                        "mensajeError",
                        "No se puede verificar este usuario. Faltan: " + String.join(", ", missing) + ".");
                return;
            }
            perfil.setVerificado(true);
            perfilRepository.save(perfil);
            usuarioRepository.findByUsernameIgnoreCase(perfil.getUsername()).ifPresent(usuario -> {
                usuario.setVerificado(true);
                usuarioRepository.save(usuario);
            });
        });
        return "redirect:/admin/desboard";
    }

    @GetMapping("/contratistas/{id}")
    public String verDetallesContratista(@PathVariable String id,
                                        HttpSession session,
                                        Model model) {
        Usuario usuario = resolverUsuarioAdmin(session);
        if (usuario == null) return "redirect:/login";

        @SuppressWarnings("null")
        String safeId5 = id != null ? id : "";
        Perfil contratista = perfilRepository.findById(safeId5).orElse(null);
        if (contratista == null) return "redirect:/admin/desboard";

        model.addAttribute("contratista", contratista);
        model.addAttribute("usuario", usuario);

        if (contratista.getEmail() != null) {
            perfilRepository.findByEmailIgnoreCase(contratista.getEmail())
                    .ifPresent(p -> model.addAttribute(
                            "calificaciones",
                            calificacionRepository.findByContratistaId(p.getId())
                    ));
        }

        List<Proyecto> proyectosDelContratista =
                proyectoRepository.findByContratistaAsignadoId(id);
        model.addAttribute("proyectosDelContratista", proyectosDelContratista);

        return "admin-detalles-contratista";
    }

    
    @PostMapping("/clientes/{id}/toggle")
    public String toggleCliente(@PathVariable String id, HttpSession session) {
        if (!isAdmin(session)) return "redirect:/login";
        @SuppressWarnings("null")
        String safeId6 = id != null ? id : "";
        perfilRepository.findById(safeId6).ifPresent(p -> {
            boolean nuevoActivo = !Boolean.TRUE.equals(p.getActivo());
            p.setActivo(nuevoActivo);
            perfilRepository.save(p);
            usuarioRepository.findByUsernameIgnoreCase(p.getUsername()).ifPresent(u -> {
                u.setActivo(nuevoActivo);
                usuarioRepository.save(u);
            });
        });
        return "redirect:/admin/desboard";
    }

    @GetMapping("/clientes/{id}")
    public String verDetallesCliente(@PathVariable String id, HttpSession session, Model model) {
        Usuario usuario = resolverUsuarioAdmin(session);
        if (usuario == null) return "redirect:/login";

        Perfil cliente = perfilRepository.findById(id != null ? id : "").orElse(null);
        if (cliente == null) return "redirect:/admin/desboard";

        model.addAttribute("usuario", usuario);
        model.addAttribute("cliente", cliente);
        model.addAttribute("requisitosFaltantes", perfilVerificationService.getMissingRequirements(cliente));
        return "admin-detalles-cliente";
    }

    @PostMapping("/trabajadores/{id}/toggle")
    public String toggleTrabajador(@PathVariable String id, HttpSession session) {
        if (!isAdmin(session)) return "redirect:/login";
        @SuppressWarnings("null")
        String safeId8 = id != null ? id : "";
        perfilRepository.findById(safeId8).ifPresent(t -> {
            boolean nuevoActivo = !Boolean.TRUE.equals(t.getActivo());
            t.setActivo(nuevoActivo);
            perfilRepository.save(t);
            usuarioRepository.findByUsernameIgnoreCase(t.getUsername()).ifPresent(u -> {
                u.setActivo(nuevoActivo);
                usuarioRepository.save(u);
            });
        });
        return "redirect:/admin/desboard";
    }

    private Usuario resolverUsuarioAdmin(HttpSession session) {
        Usuario usuario = (Usuario) session.getAttribute("usuario");

        if (usuario == null) {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated()) {
                usuario = usuarioRepository.findByUsernameIgnoreCase(auth.getName()).orElse(null);
                if (usuario != null) {
                    session.setAttribute("usuario", usuario);
                }
            }
        }

        if (usuario == null || !usuario.getRoles().contains("ROLE_ADMIN")) {
            return null;
        }
        return usuario;
    }

    private boolean isAdmin(HttpSession session) {
        return resolverUsuarioAdmin(session) != null;
    }
}
