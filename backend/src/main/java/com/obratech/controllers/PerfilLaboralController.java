package com.obratech.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.obratech.entity.Perfil;
import com.obratech.entity.Usuario;
import com.obratech.repository.PerfilRepository;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/perfil-laboral")
public class PerfilLaboralController {

    @Autowired private PerfilRepository perfilRepository;

    @GetMapping
    public String verPerfilLaboral(HttpSession session, Model model) {
        Usuario usuario = (Usuario) session.getAttribute("usuario");
        if (usuario == null) return "redirect:/login";
        if (usuario.getRoles() == null || !usuario.getRoles().contains("ROLE_WORKER")) return "redirect:/desboard";

        Perfil trabajador = perfilRepository.findByUsernameIgnoreCase(usuario.getUsername()).orElseGet(() -> {
            Perfil nuevo = new Perfil();
            nuevo.setUsername(usuario.getUsername());
            nuevo.setRole("ROLE_WORKER");
            return nuevo;
        });

        model.addAttribute("usuario", usuario);
        model.addAttribute("trabajador", trabajador);
        return "trabajadores/perfil-laboral";
    }

    @PostMapping
    public String guardarPerfilLaboral(
            @RequestParam String nombre,
            @RequestParam String apellido,
            @RequestParam String email,
            @RequestParam String telefono,
            @RequestParam String oficio,
            @RequestParam String experiencia,
            @RequestParam String disponibilidad,
            @RequestParam(required = false, defaultValue = "") String descripcion,
            HttpSession session,
            Model model) {

        Usuario usuario = (Usuario) session.getAttribute("usuario");
        if (usuario == null) return "redirect:/login";
        if (usuario.getRoles() == null || !usuario.getRoles().contains("ROLE_WORKER")) return "redirect:/desboard";

        try {
            Perfil trabajador = perfilRepository.findByUsernameIgnoreCase(usuario.getUsername())
                    .orElseGet(() -> {
                        Perfil nuevo = new Perfil();
                        nuevo.setUsername(usuario.getUsername());
                        nuevo.setRole("ROLE_WORKER");
                        return nuevo;
                    });

            trabajador.setNombre(nombre);
            trabajador.setApellido(apellido);
            trabajador.setEmail(email);
            trabajador.setTelefono(telefono);
            trabajador.setOficio(oficio);
            trabajador.setExperiencia(parseExperiencia(experiencia));
            trabajador.setDisponibilidad("true".equalsIgnoreCase(disponibilidad) || "yes".equalsIgnoreCase(disponibilidad));
            trabajador.setDescripcion(descripcion);

            perfilRepository.save(trabajador);

            model.addAttribute("mensaje", "Perfil laboral actualizado exitosamente!");
            return "redirect:/desboard-trabajador";

        } catch (Exception e) {
            model.addAttribute("error", "Error al guardar el perfil: " + e.getMessage());
            model.addAttribute("usuario", usuario);
            return "trabajadores/perfil-laboral";
        }
    }

    @SuppressWarnings({"java:S1166", "java:S127"})
    private Integer parseExperiencia(String experiencia) {
        try {
            if (experiencia.contains(" ")) {
                String[] parts = experiencia.split(" ");
                if ("Menos".equals(parts[0])) {
                    return 0;
                }
                return Integer.valueOf(parts[0]);
            }
            return Integer.valueOf(experiencia);
        } catch (NumberFormatException e) {
            return switch (experiencia.toLowerCase()) {
                case "menos de 1 ao" -> 0;
                case "1 a 2 aos"     -> 1;
                case "2 a 5 aos"     -> 2;
                case "5 a 10 aos"    -> 5;
                case "ms de 10 aos" -> 10;
                default               -> 0;
            };
        }
    }
}
