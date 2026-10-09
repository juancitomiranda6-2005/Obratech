package com.obratech.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.obratech.entity.Perfil;
import com.obratech.entity.Usuario;
import com.obratech.entity.enums.EstadoEjecucion;
import com.obratech.repository.CalificacionRepository;
import com.obratech.repository.PerfilRepository;
import com.obratech.repository.ProyectoRepository;

import jakarta.servlet.http.HttpSession;

@Controller
public class IndexControllers {

    @Autowired private PerfilRepository perfilRepository;
    @Autowired private CalificacionRepository calificacionRepository;
    @Autowired private ProyectoRepository proyectoRepository;

    @GetMapping({"/index"})
    public String mostrarIndex() {
        return "index";
    
    }


    @GetMapping("/perfil-contratista")
    public String verPerfilContratista(HttpSession session, Model model) {
        Usuario usuario = (Usuario) session.getAttribute("usuario");
        if (usuario == null) return "redirect:/login";
        if (usuario.getRoles() == null || !usuario.getRoles().contains("ROLE_CONTRACTOR")) return "redirect:/desboard";

        Perfil contratista = perfilRepository.findByUsernameIgnoreCase(usuario.getUsername()).orElse(null);
        if (contratista == null) return "redirect:/desboard";

        model.addAttribute("contratista", contratista);
        model.addAttribute("usuario", usuario);
        model.addAttribute("proyectosCompletados", proyectoRepository.countByContratistaAsignadoIdAndEstadoEjecucion(
            contratista.getId(), EstadoEjecucion.COMPLETADO));
        model.addAttribute("proyectosEnProgreso", proyectoRepository.countByContratistaAsignadoIdAndEstadoEjecucionNot(
            contratista.getId(), EstadoEjecucion.COMPLETADO));


            if (contratista.getEmail() != null) {
            perfilRepository.findByEmailIgnoreCase(contratista.getEmail()).ifPresent(p -> {
                model.addAttribute("calificaciones", calificacionRepository.findByContratistaId(p.getId()));
                model.addAttribute("personaId", p.getId());
            });
        }

        return "perfil-contratista";
    }


    @GetMapping("/perfil-cliente")
    public String verPerfilCliente(HttpSession session, Model model) {
        Usuario usuario = (Usuario) session.getAttribute("usuario");
        if (usuario == null) return "redirect:/login";
        if (usuario.getRoles() == null || !usuario.getRoles().contains("ROLE_CLIENT")) return "redirect:/desboard";

        Perfil cliente = perfilRepository.findByUsernameIgnoreCase(usuario.getUsername()).orElseGet(() -> {
            Perfil nuevo = new Perfil();
            nuevo.setUsername(usuario.getUsername());
            nuevo.setEmail(usuario.getUsername());
            nuevo.setRole("ROLE_CLIENT");
            nuevo.setNombre("Cliente");
            nuevo.setApellido("");
            nuevo.setActivo(true);
            return perfilRepository.save(nuevo);
        });

        model.addAttribute("cliente", cliente);
        model.addAttribute("usuario", usuario);
    
        return "perfil-cliente";
    }


    @GetMapping("/perfil-cliente/editar")
    public String mostrarFormularioEditarPerfilCliente(HttpSession session, Model model) {
        Usuario usuario = (Usuario) session.getAttribute("usuario");
        if (usuario == null) return "redirect:/login";
        if (usuario.getRoles() == null || !usuario.getRoles().contains("ROLE_CLIENT")) return "redirect:/desboard";

        Perfil cliente = perfilRepository.findByUsernameIgnoreCase(usuario.getUsername()).orElseGet(() -> {
            Perfil nuevo = new Perfil();
            nuevo.setUsername(usuario.getUsername());
            nuevo.setEmail(usuario.getUsername());
            nuevo.setRole("ROLE_CLIENT");
            nuevo.setNombre("Cliente");
            nuevo.setActivo(true);
            return perfilRepository.save(nuevo);
        });


        model.addAttribute("cliente", cliente);
        model.addAttribute("usuario", usuario);
        
        return "editar-perfil-cliente";
    }


    @PostMapping("/perfil-cliente/editar")
    public String guardarPerfilCliente(
            @RequestParam String nombre,
            @RequestParam String apellido,
            @RequestParam String telefono,
            @RequestParam String empresa,
            HttpSession session,
            Model model) {

        Usuario usuario = (Usuario) session.getAttribute("usuario");
        if (usuario == null) return "redirect:/login";
        if (usuario.getRoles() == null || !usuario.getRoles().contains("ROLE_CLIENT")) return "redirect:/desboard";

        Perfil cliente = perfilRepository.findByUsernameIgnoreCase(usuario.getUsername()).orElseGet(() -> {
            Perfil nuevo = new Perfil();
            nuevo.setUsername(usuario.getUsername());
            nuevo.setEmail(usuario.getUsername());
            nuevo.setRole("ROLE_CLIENT");
            nuevo.setActivo(true);
            return nuevo;
        });

        cliente.setNombre(nombre);
        cliente.setApellido(apellido);
        cliente.setTelefono(telefono);
        cliente.setEmpresa(empresa);
        perfilRepository.save(cliente);

        return "redirect:/perfil-cliente?exito=true";
    }
}
