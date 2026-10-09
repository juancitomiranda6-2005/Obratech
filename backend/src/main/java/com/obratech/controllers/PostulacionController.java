package com.obratech.controllers;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.obratech.entity.Proyecto;
import com.obratech.entity.Perfil;
import com.obratech.entity.Usuario;
import com.obratech.entity.enums.EstadoAsignacion;
import com.obratech.entity.enums.EstadoPostulacion;
import com.obratech.entity.enums.EstadoValidacion;
import com.obratech.repository.PerfilRepository;
import com.obratech.repository.ProyectoRepository;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/postulaciones")
public class PostulacionController {

    @Autowired private ProyectoRepository proyectoRepository;
    @Autowired private com.obratech.repository.PostulacionRepository postulacionRepository;
    @Autowired private PerfilRepository perfilRepository;
    @Autowired private com.obratech.repository.UsuarioRepository usuarioRepository;
    @Autowired private com.obratech.service.ContratistaProfileValidator contratistaProfileValidator;

    @PostMapping("/postular/{id}")
    public String postularseProyecto(
            @PathVariable String id,
            HttpSession session,
            Model model) {

        Usuario usuario = (Usuario) session.getAttribute("usuario");

        if (usuario == null) {
            return "redirect:/login";
        }

        if (usuario.getRoles() == null || (!usuario.getRoles().contains("ROLE_WORKER") && !usuario.getRoles().contains("ROLE_CONTRACTOR"))) {
            return "redirect:/desboard";
        }

        Proyecto proyecto = proyectoRepository.findById(id).orElse(null);
        if (proyecto == null) {
            return "redirect:/postulaciones";
        }

        if (!EstadoValidacion.APROBADO.equals(proyecto.getEstadoValidacion())) {
            session.setAttribute("error", "Este proyecto todavía no ha sido verificado por el administrador.");
            return "redirect:/postulaciones/" + id;
        }

        if (!proyecto.isPostulacionesAbiertas() || proyecto.getContratistaAsignado() != null) {
            session.setAttribute("error", "Las postulaciones están cerradas porque el cliente ya seleccionó un contratista.");
            return "redirect:/postulaciones/" + id;
        }

        Usuario dbUsuario = usuarioRepository.findById(usuario.getId()).orElse(null);
        if (dbUsuario == null || !dbUsuario.isVerificado()) {
            session.setAttribute("error", "No puedes postularte a proyectos hasta que tu cuenta sea verificada por el administrador.");
            return "redirect:/postulaciones/" + id;
        }

        Perfil perfil = perfilRepository.findByUsernameIgnoreCase(usuario.getUsername()).orElse(null);
        boolean esContratista = usuario.getRoles().contains("ROLE_CONTRACTOR");
        if (esContratista && (perfil == null || !contratistaProfileValidator.validate(perfil).isEmpty())) {
            session.setAttribute("error", "Completa tu perfil profesional antes de postularte a un proyecto.");
            return "redirect:/postulaciones/" + id;
        }

        try {
            java.util.List<com.obratech.entity.Postulacion> postulacionesExistentes =
                postulacionRepository.findByProyectoId(id).stream()
                    .filter(p -> p.getUsuario() != null && p.getUsuario().getId().equals(usuario.getId())
                             && (p.getEstado() == EstadoPostulacion.PENDING
                                 || p.getEstado() == EstadoPostulacion.REJECTED
                                 || p.getEstado() == EstadoPostulacion.ACCEPTED))
                    .toList();
            
            if (!postulacionesExistentes.isEmpty()) {
                session.setAttribute("mensaje", "Ya te postulaste a este proyecto.");
                return "redirect:/postulaciones/" + id;
            }

            com.obratech.entity.Postulacion post = new com.obratech.entity.Postulacion();
            post.setProyecto(proyecto);
            post.setUsuario(usuario);
            post.setFechaPostulacion(java.time.LocalDateTime.now());
            
            postulacionRepository.save(post);

            session.setAttribute("mensaje", "Postulacin enviada exitosamente! El cliente revisar tu solicitud pronto.");
            return "redirect:/postulaciones/" + id;
        } catch (Exception e) {
            model.addAttribute("error", "Error al procesar tu postulacin: " + e.getMessage());
            model.addAttribute("proyecto", proyecto);
            return "trabajadores/detalles-postulacion";
        }
    }

    @GetMapping
    public String verProyectosDisponibles(HttpSession session, Model model) {
        Usuario usuario = (Usuario) session.getAttribute("usuario");

        if (usuario == null) {
            return "redirect:/login";
        }

        if (usuario.getRoles() == null || (!usuario.getRoles().contains("ROLE_WORKER") && !usuario.getRoles().contains("ROLE_CONTRACTOR"))) {
            return "redirect:/desboard";
        }

    List<Proyecto> proyectos = proyectoRepository.findByFechaLimitePostulacionIsNullOrFechaLimitePostulacionGreaterThanEqual(java.time.LocalDate.now())
        .stream()
        .filter(proyecto -> EstadoValidacion.APROBADO.equals(proyecto.getEstadoValidacion()))
        .filter(proyecto -> proyecto.getContratistaAsignado() == null && proyecto.isPostulacionesAbiertas())
        .toList();

        model.addAttribute("proyectos", proyectos);
        model.addAttribute("usuario", usuario);
        model.addAttribute("totalProyectos", proyectos.size());

        return "trabajadores/proyectos-disponibles";
    }

    @PostMapping
    public String postulacionesRoot() {
        return "redirect:/postulaciones";
    }

    @GetMapping({"/ver/{id}", "/{id}"})
    public String verDetallesProyecto(
            @PathVariable String id,
            HttpSession session,
            Model model) {

        Usuario usuario = (Usuario) session.getAttribute("usuario");

        if (usuario == null) {
            return "redirect:/login";
        }

        if (usuario.getRoles() == null || (!usuario.getRoles().contains("ROLE_WORKER") && !usuario.getRoles().contains("ROLE_CONTRACTOR"))) {
            return "redirect:/desboard";
        }

        Proyecto proyecto = proyectoRepository.findById(id).orElse(null);

        if (proyecto == null) {
            return "redirect:/postulaciones";
        }

        model.addAttribute("proyecto", proyecto);
        model.addAttribute("usuario", usuario);
        Perfil perfil = perfilRepository.findByUsernameIgnoreCase(usuario.getUsername()).orElse(null);
        boolean esContratista = usuario.getRoles().contains("ROLE_CONTRACTOR");
        model.addAttribute("perfilCompleto", !esContratista || (perfil != null && contratistaProfileValidator.validate(perfil).isEmpty()));

        if (session.getAttribute("mensaje") != null) {
            model.addAttribute("mensaje", session.getAttribute("mensaje"));
            session.removeAttribute("mensaje");
        }
        if (session.getAttribute("error") != null) {
            model.addAttribute("error", session.getAttribute("error"));
            session.removeAttribute("error");
        }

        return "trabajadores/detalles-postulacion";
    }

    @GetMapping("/{id}/postulantes")
    public String verPostulantesProyecto(
            @PathVariable String id,
            HttpSession session,
            Model model) {

        Usuario usuario = (Usuario) session.getAttribute("usuario");

        if (usuario == null) {
            return "redirect:/login";
        }

        if (usuario.getRoles() == null || (!usuario.getRoles().contains("ROLE_CLIENT") && !usuario.getRoles().contains("ROLE_ADMIN"))) {
            return "redirect:/desboard";
        }

        Proyecto proyecto = proyectoRepository.findById(id).orElse(null);
        if (proyecto == null) {
            return "redirect:/mis-proyectos";
        }

        java.util.List<com.obratech.entity.Postulacion> postulaciones = postulacionRepository.findByProyectoId(id);

        boolean tieneAceptada = postulaciones.stream().anyMatch(p -> EstadoPostulacion.ACCEPTED.equals(p.getEstado()));

        model.addAttribute("proyecto", proyecto);
        model.addAttribute("postulaciones", postulaciones);
        model.addAttribute("usuario", usuario);
        model.addAttribute("tieneAceptada", tieneAceptada);
        return "clientes/postulantes-proyecto";
    }

    @GetMapping("/mis-postulaciones")
    public String verMisPostulaciones(HttpSession session, Model model) {
        Usuario usuario = (Usuario) session.getAttribute("usuario");

        if (usuario == null) {
            return "redirect:/login";
        }

        if (usuario.getRoles() == null || (!usuario.getRoles().contains("ROLE_CLIENT") && !usuario.getRoles().contains("ROLE_ADMIN"))) {
            return "redirect:/desboard";
        }

        Usuario dbUsuario = usuarioRepository.findByUsernameIgnoreCase(usuario.getUsername()).orElse(usuario);
        java.util.List<Proyecto> proyectos = proyectoRepository.findByClienteId(dbUsuario.getId());
        
        java.util.List<com.obratech.entity.Postulacion> todas = proyectos.isEmpty()
                ? new java.util.ArrayList<>()
                : postulacionRepository.findByProyectoIn(proyectos);

        model.addAttribute("postulaciones", todas);
        model.addAttribute("usuario", usuario);
        model.addAttribute("totalPostulaciones", todas.size());
        return "clientes/mis-postulaciones";
    }

    @GetMapping("/mis-postulaciones-contratista")
    public String verMisPostulacionesContratista(HttpSession session, Model model) {
        Usuario usuario = (Usuario) session.getAttribute("usuario");

        if (usuario == null) {
            return "redirect:/login";
        }

        if (usuario.getRoles() == null || !usuario.getRoles().contains("ROLE_CONTRACTOR")) {
            return "redirect:/desboard";
        }

        java.util.List<com.obratech.entity.Postulacion> postulaciones = postulacionRepository.findByUsuarioId(usuario.getId());

        model.addAttribute("postulaciones", postulaciones);
        model.addAttribute("usuario", usuario);
        model.addAttribute("totalPostulaciones", postulaciones.size());
        return "trabajadores/mis-postulaciones";
    }

    @PostMapping("/{proyectoId}/postulantes/{postId}/aceptar")
    public String aceptarPostulante(
            @PathVariable String proyectoId,
            @PathVariable String postId,
            HttpSession session,
            Model model) {

        Usuario usuario = (Usuario) session.getAttribute("usuario");
        if (usuario == null) return "redirect:/login";

        if (usuario.getRoles() == null || (!usuario.getRoles().contains("ROLE_CLIENT") && !usuario.getRoles().contains("ROLE_ADMIN"))) {
            return "redirect:/desboard";
        }

        Proyecto proyecto = proyectoRepository.findById(proyectoId).orElse(null);
        if (proyecto == null) return "redirect:/mis-proyectos";

        if ((usuario.getRoles() == null || !usuario.getRoles().contains("ROLE_ADMIN")) && (proyecto.getCliente() == null || proyecto.getCliente().getUsername() == null || !proyecto.getCliente().getUsername().equals(usuario.getUsername()))) {
            return "redirect:/desboard";
        }

        com.obratech.entity.Postulacion post = postulacionRepository.findById(postId).orElse(null);
        if (post == null || post.getProyecto() == null || !post.getProyecto().getId().equals(proyectoId)) {
            return "redirect:/postulaciones/" + proyectoId + "/postulantes";
        }

        try {
            java.util.List<com.obratech.entity.Postulacion> todas = postulacionRepository.findByProyectoId(proyectoId);
            for (com.obratech.entity.Postulacion p : todas) {
                if (!p.getId().equals(postId)) {
                    p.setEstado(EstadoPostulacion.REJECTED);
                    postulacionRepository.save(p);
                }
            }

            post.setEstado(EstadoPostulacion.ACCEPTED);
            postulacionRepository.save(post);

            String username = post.getUsuario() != null ? post.getUsuario().getUsername() : null;
            if (username != null) {
                Perfil cont = perfilRepository.findByUsernameIgnoreCase(username).orElse(null);
                if (cont != null && "ROLE_CONTRACTOR".equals(cont.getRole())) {
                    proyecto.setContratistaAsignado(cont);
                    proyecto.setEstadoAsignacion(EstadoAsignacion.SELECCIONADO_PENDIENTE_CONTRATACION);
                    proyecto.setPostulacionesAbiertas(false);
                    proyectoRepository.save(proyecto);
                }
            }
            session.setAttribute("mensaje", "Has aceptado al postulante " + username + ".");
        } catch (Exception ex) {
            session.setAttribute("error", "Ocurrió un error al aceptar la postulación: " + ex.getMessage());
        }

        return "redirect:/proyectos/mis-proyectos";
    }

    @PostMapping("/{proyectoId}/postulantes/{postId}/rechazar")
    public String rechazarPostulante(
            @PathVariable String proyectoId,
            @PathVariable String postId,
            HttpSession session,
            Model model) {

        Usuario usuario = (Usuario) session.getAttribute("usuario");
        if (usuario == null) return "redirect:/login";

        if (usuario.getRoles() == null || (!usuario.getRoles().contains("ROLE_CLIENT") && !usuario.getRoles().contains("ROLE_ADMIN"))) {
            return "redirect:/desboard";
        }

        Proyecto proyecto = proyectoRepository.findById(proyectoId).orElse(null);
        if (proyecto == null) return "redirect:/mis-proyectos";

        if ((usuario.getRoles() == null || !usuario.getRoles().contains("ROLE_ADMIN")) && (proyecto.getCliente() == null || proyecto.getCliente().getUsername() == null || !proyecto.getCliente().getUsername().equals(usuario.getUsername()))) {
            return "redirect:/desboard";
        }

        com.obratech.entity.Postulacion post = postulacionRepository.findById(postId).orElse(null);
        if (post == null || post.getProyecto() == null || !post.getProyecto().getId().equals(proyectoId)) {
            return "redirect:/postulaciones/" + proyectoId + "/postulantes";
        }

        post.setEstado(EstadoPostulacion.REJECTED);
        postulacionRepository.save(post);
        session.setAttribute("mensaje", "Has rechazado al postulante.");

        return "redirect:/proyectos/mis-proyectos";
    }

    @PostMapping("/{proyectoId}/postulantes/{postId}/eliminar")
    public String eliminarPostulante(
            @PathVariable String proyectoId,
            @PathVariable String postId,
            HttpSession session,
            Model model) {

        Usuario usuario = (Usuario) session.getAttribute("usuario");
        if (usuario == null) return "redirect:/login";

        if (usuario.getRoles() == null || (!usuario.getRoles().contains("ROLE_CLIENT") && !usuario.getRoles().contains("ROLE_ADMIN"))) {
            return "redirect:/desboard";
        }

        Proyecto proyecto = proyectoRepository.findById(proyectoId).orElse(null);
        if (proyecto == null) return "redirect:/mis-proyectos";

        if ((usuario.getRoles() == null || !usuario.getRoles().contains("ROLE_ADMIN")) && (proyecto.getCliente() == null || proyecto.getCliente().getUsername() == null || !proyecto.getCliente().getUsername().equals(usuario.getUsername()))) {
            return "redirect:/desboard";
        }

        com.obratech.entity.Postulacion post = postulacionRepository.findById(postId).orElse(null);
        if (post == null || post.getProyecto() == null || !post.getProyecto().getId().equals(proyectoId)) {
            return "redirect:/postulaciones/" + proyectoId + "/postulantes";
        }

        if (proyecto.getContratistaAsignado() != null && 
            post.getUsuario() != null &&
            proyecto.getContratistaAsignado().getUsername() != null &&
            proyecto.getContratistaAsignado().getUsername().equals(post.getUsuario().getUsername())) {
            proyecto.setContratistaAsignado(null);
            proyecto.setEstadoAsignacion(EstadoAsignacion.SIN_ASIGNAR);
            proyecto.setPostulacionesAbiertas(true);
            proyectoRepository.save(proyecto);

            // verificar si esta integrado, Disolver el contrato inicia un nuevo ciclo de selección.
            postulacionRepository.findByProyectoId(proyectoId).forEach(postulacionRepository::delete);
            session.setAttribute("mensaje", "Contrato disuelto. El proyecto volvió a abrir sus postulaciones.");
            return "redirect:/proyectos/mis-proyectos";
        }

        postulacionRepository.delete(post);
        session.setAttribute("mensaje", "Postulacin eliminada exitosamente.");

        return "redirect:/postulaciones/mis-postulaciones";
    }
}