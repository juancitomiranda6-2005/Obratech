package com.obratech.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;

import java.util.HashMap;
import java.util.Map;

import com.obratech.entity.Calificacion;
import com.obratech.entity.Perfil;
import com.obratech.entity.Proyecto;
import com.obratech.entity.Usuario;

import jakarta.servlet.http.HttpSession;
import java.util.List;

@Controller
@RequestMapping("/calificaciones")
public class CalificacionController {

    @Autowired
    private com.obratech.repository.CalificacionRepository calificacionRepository;
    @Autowired
    private com.obratech.repository.PerfilRepository perfilRepository;
    @Autowired
    private com.obratech.repository.ProyectoRepository proyectoRepository;

    @GetMapping("/calificar/{proyectoId}/{contratistaId}")
    public String calificarDesdeProyecto(
            @PathVariable String proyectoId,
            @PathVariable String contratistaId,
            HttpSession session,
            Model model) {

        Usuario usuario = (Usuario) session.getAttribute("usuario");
        if (usuario == null)
            return "redirect:/login";

        Proyecto proyecto = proyectoRepository.findById(proyectoId != null ? proyectoId : "").orElse(null);
        Perfil contratista = perfilRepository.findById(contratistaId != null ? contratistaId : "").orElse(null);
        if (proyecto == null || contratista == null)
            return "redirect:/desboard";

        if (calificacionRepository.existsByProyectoIdAndContratistaId(proyectoId, contratista.getId())) {
            List<Calificacion> existingList = calificacionRepository
                    .findByProyectoIdAndContratistaId(proyectoId, contratista.getId());
            if (!existingList.isEmpty()) {
                return "redirect:/calificaciones/" + existingList.get(0).getId() + "/editar";
            }
        }

        model.addAttribute("proyecto", proyecto);
        model.addAttribute("contratista", contratista);
        model.addAttribute("calificacion", new Calificacion());
        return "clientes/calificar";
    }

    @GetMapping("/calificar/{proyectoId}")
    public String calificarDesdeProyectoSolo(@PathVariable String proyectoId, HttpSession session) {
        Proyecto proyecto = proyectoRepository.findById(proyectoId != null ? proyectoId : "").orElse(null);
        if (proyecto == null)
            return "redirect:/desboard";

        if (proyecto.getContratistaAsignado() != null && proyecto.getContratistaAsignado().getId() != null) {
            return "redirect:/calificaciones/calificar/" + proyectoId + "/" + proyecto.getContratistaAsignado().getId();
        }

        session.setAttribute("error", "No hay contratista asignado para calificar en este proyecto.");
        return "redirect:/proyectos/" + proyectoId;
    }

    @GetMapping("/calificar-por-contratista/{contratistaId}")
    public String calificarPorContratista(@PathVariable String contratistaId, HttpSession session) {
        Perfil cont = perfilRepository.findById(contratistaId != null ? contratistaId : "").orElse(null);
        if (cont == null)
            return "redirect:/contratistas";

        Proyecto proyecto = proyectoRepository.findByContratistaAsignadoId(contratistaId)
                .stream().findFirst().orElse(null);

        if (proyecto != null) {
            return "redirect:/calificaciones/calificar/" + proyecto.getId() + "/" + contratistaId;
        }

        session.setAttribute("error", "No se encontr un proyecto asignado para este contratista.");
        return "redirect:/contratistas/" + contratistaId;
    }

    @GetMapping("/api/proyecto-por-contratista/{contratistaId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> apiProyectoPorContratista(@PathVariable String contratistaId) {
        Map<String, Object> resp = new HashMap<>();
        Perfil cont = perfilRepository.findById(contratistaId != null ? contratistaId : "").orElse(null);
        if (cont == null) {
            resp.put("ok", false);
            resp.put("message", "Contratista no encontrado");
            return ResponseEntity.status(404).body(resp);
        }

        Proyecto proyecto = proyectoRepository.findByContratistaAsignadoId(contratistaId)
                .stream().findFirst().orElse(null);

        if (proyecto == null) {
            resp.put("ok", false);
            resp.put("message", "No hay proyecto asignado para este contratista");
            return ResponseEntity.status(404).body(resp);
        }

        resp.put("ok", true);
        resp.put("proyectoId", proyecto.getId());
        resp.put("contratistaId", contratistaId);
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/contratistas")
    public String contratistasParaCalificar(HttpSession session, Model model) {
        Usuario usuario = (Usuario) session.getAttribute("usuario");
        if (usuario == null)
            return "redirect:/desboard";

        List<Perfil> contratistas = perfilRepository.findByRolesAndActivoTrue("ROLE_CONTRACTOR");
        model.addAttribute("contratistas", contratistas);
        model.addAttribute("usuario", usuario);
        model.addAttribute("totalContratistas", contratistas.size());

        Object proyectoId = session.getAttribute("proyectoId");
        if (proyectoId != null)
            model.addAttribute("proyectoId", proyectoId);

        return "clientes/contratistas-para-calificar";
    }

    @GetMapping("/crearPorContratista/{proyectoId}/{contratistaId}")
    public String crearPorContratista(
            @PathVariable String proyectoId,
            @PathVariable String contratistaId,
            HttpSession session) {

        Usuario usuario = (Usuario) session.getAttribute("usuario");
        if (usuario == null)
            return "redirect:/desboard";

        Proyecto proyecto = proyectoRepository.findById(proyectoId != null ? proyectoId : "").orElse(null);
        Perfil cont = perfilRepository.findById(contratistaId != null ? contratistaId : "").orElse(null);
        if (proyecto == null || cont == null)
            return "redirect:/desboard";

        return "redirect:/calificaciones/crear/" + proyectoId + "/" + cont.getId();
    }

    @GetMapping("/crear/{proyectoId}/{perfilId}")
    public String formCalificar(
            @PathVariable String proyectoId,
            @PathVariable String perfilId,
            HttpSession session,
            Model model) {

        Usuario usuario = (Usuario) session.getAttribute("usuario");
        if (usuario == null)
            return "redirect:/desboard";

        Proyecto proyecto = proyectoRepository.findById(proyectoId != null ? proyectoId : "").orElse(null);
        Perfil persona = perfilRepository.findById(perfilId != null ? perfilId : "").orElse(null);
        if (proyecto == null || persona == null)
            return "redirect:/desboard";

        if (calificacionRepository.existsByProyectoIdAndContratistaId(proyectoId, perfilId)) {
            List<Calificacion> existingList = calificacionRepository.findByProyectoIdAndContratistaId(proyectoId,
                    perfilId);
            if (!existingList.isEmpty()) {
                return "redirect:/calificaciones/" + existingList.get(0).getId() + "/editar";
            }
        }

        model.addAttribute("proyecto", proyecto);
        model.addAttribute("contratista", persona);
        model.addAttribute("calificacion", new Calificacion());
        return "clientes/calificar";
    }

    @PostMapping("/crear/{proyectoId}/{perfilId}")
    public String crearCalificacion(
            @PathVariable String proyectoId,
            @PathVariable String perfilId,
            Calificacion calificacion,
            HttpSession session) {

        try {
            Usuario usuario = (Usuario) session.getAttribute("usuario");
            if (usuario == null)
                return "redirect:/desboard";

            Proyecto proyecto = proyectoRepository.findById(proyectoId).orElse(null);
            Perfil persona = perfilRepository.findById(perfilId).orElse(null);
            if (proyecto == null || persona == null)
                return "redirect:/desboard";

            if (calificacionRepository.existsByProyectoIdAndContratistaId(proyectoId, perfilId)) {
                session.setAttribute("error", "Ya existe una calificacin para este proyecto y contratista.");
                return "redirect:/calificaciones/contratistas";
            }

            calificacion.setContratista(persona);
            calificacion.setCliente(usuario);
            calificacion.setProyecto(proyecto);
            calificacion.setFecha(java.time.LocalDateTime.now());
            calificacionRepository.save(calificacion);

            List<Calificacion> lista = calificacionRepository.findByContratistaId(perfilId);
            double avg = lista.stream().mapToInt(Calificacion::getPuntuacion).average().orElse(0.0);
            persona.setCalificacionPromedio(avg);
            perfilRepository.save(persona);

            session.setAttribute("mensaje", "Calificacin guardada.");
            return "redirect:/calificaciones/contratistas";

        } catch (Exception e) {
            return "redirect:/desboard";
        }
    }

    @GetMapping("/{id}/editar")
    public String editarForm(@PathVariable String id, HttpSession session, Model model) {
        try {
            Usuario usuario = (Usuario) session.getAttribute("usuario");
            if (usuario == null)
                return "redirect:/desboard";

            Calificacion calificacion = calificacionRepository.findById(id).orElse(null);
            if (calificacion == null || calificacion.getProyecto() == null)
                return "redirect:/desboard";

            model.addAttribute("proyecto", calificacion.getProyecto());
            model.addAttribute("contratista", calificacion.getContratista());
            model.addAttribute("calificacion", calificacion);
            return "clientes/calificar";

        } catch (Exception e) {
            return "redirect:/desboard";
        }
    }

    @PostMapping("/{id}/editar")
    public String editarCalificacion(@PathVariable String id, Calificacion calificacionForm, HttpSession session) {
        try {
            Usuario usuario = (Usuario) session.getAttribute("usuario");
            if (usuario == null)
                return "redirect:/desboard";

            Calificacion calificacion = calificacionRepository.findById(id != null ? id : "").orElse(null);
            if (calificacion == null || calificacion.getProyecto() == null)
                return "redirect:/desboard";

            calificacion.setPuntuacion(calificacionForm.getPuntuacion());
            calificacion.setComentario(calificacionForm.getComentario());
            calificacionRepository.save(calificacion);

            if (calificacion.getContratista() != null) {
                String contratistaId = calificacion.getContratista().getId();
                if (contratistaId != null) {
                    List<Calificacion> lista = calificacionRepository.findByContratistaId(contratistaId);
                    double avg = lista.stream().mapToInt(Calificacion::getPuntuacion).average().orElse(0.0);
                    perfilRepository.findById(contratistaId).ifPresent(p -> {
                        p.setCalificacionPromedio(avg);
                        perfilRepository.save(p);
                    });
                }
            }
            session.setAttribute("mensaje", "Calificacin actualizada.");
            return "redirect:/calificaciones/contratistas";

        } catch (Exception e) {
            return "redirect:/desboard";
        }
    }
}
