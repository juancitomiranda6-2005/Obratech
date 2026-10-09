package com.obratech.controllers;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.mongodb.client.gridfs.model.GridFSFile;
import com.obratech.entity.Perfil;
import com.obratech.entity.Usuario;
import com.obratech.repository.PerfilRepository;
import com.obratech.repository.UsuarioRepository;
import com.obratech.repository.InvitacionTrabajoRepository;
import com.obratech.repository.EquipoTrabajoRepository;
import com.obratech.repository.ProyectoRepository;
import com.obratech.entity.InvitacionTrabajo;
import com.obratech.entity.EquipoTrabajo;
import com.obratech.entity.Proyecto;
import com.obratech.entity.enums.EstadoEjecucion;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/trabajadores")
public class TrabajadorControllers {

    @Autowired
    private PerfilRepository perfilRepository;
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private com.obratech.service.UsuarioService usuarioService;
    @Autowired
    private GridFsTemplate gridFsTemplate;
    @Autowired
    private InvitacionTrabajoRepository invitacionTrabajoRepository;
    @Autowired
    private EquipoTrabajoRepository equipoTrabajoRepository;
    @Autowired
    private ProyectoRepository proyectoRepository;

    @GetMapping("/crear")
    public String mostrarFormularioCrear(HttpSession session, Model model) {
        Usuario usuario = (Usuario) session.getAttribute("usuario");
        if (usuario == null || usuario.getRoles() == null || !usuario.getRoles().contains("ROLE_ADMIN"))
            return "redirect:/login";
        model.addAttribute("trabajador", new Perfil());
        model.addAttribute("usuario", usuario);
        return "crear-trabajador";
    }

    @PostMapping("/crear")
    public String crearTrabajador(@ModelAttribute Perfil trabajador, HttpSession session, Model model) {
        Usuario usuario = (Usuario) session.getAttribute("usuario");
        if (usuario == null || usuario.getRoles() == null || !usuario.getRoles().contains("ROLE_ADMIN"))
            return "redirect:/login";

        try {
            String email = trabajador.getEmail() != null ? trabajador.getEmail() : trabajador.getUsername();

            if (usuarioRepository.findByUsername(email).isPresent()) {
                model.addAttribute("error", "El email ya est registrado.");
                model.addAttribute("usuario", usuario);
                model.addAttribute("trabajador", trabajador);
                return "crear-trabajador";
            }

            Usuario nuevoUsuario = new Usuario();
            nuevoUsuario.setUsername(email);
            String passwordTemporal = generarPasswordTemporal();
            nuevoUsuario.setPassword(passwordTemporal);
            nuevoUsuario.setRole("ROLE_WORKER");
            usuarioService.register(nuevoUsuario);

            perfilRepository.findByUsername(email).ifPresent(p -> {
                p.setNombre(trabajador.getNombre());
                p.setApellido(trabajador.getApellido());
                p.setEmail(email);
                p.setTelefono(trabajador.getTelefono());
                p.setOficio(trabajador.getOficio());
                p.setExperiencia(trabajador.getExperiencia());
                p.setActivo(true);
                perfilRepository.save(p);
            });

            model.addAttribute("mensaje", "Trabajador creado. Contrasea temporal: " + passwordTemporal);
            model.addAttribute("usuario", usuario);
            return "redirect:/trabajadores?exito=true";

        } catch (IllegalArgumentException ex) {
            model.addAttribute("error", ex.getMessage());
            model.addAttribute("usuario", usuario);
            model.addAttribute("trabajador", trabajador);
            return "crear-trabajador";
        } catch (Exception e) {
            model.addAttribute("error", "Error al crear el trabajador: " + e.getMessage());
            model.addAttribute("usuario", usuario);
            model.addAttribute("trabajador", trabajador);
            return "crear-trabajador";
        }
    }

    private String generarPasswordTemporal() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789!@#$%";
        StringBuilder pass = new StringBuilder();
        java.util.Random random = new java.util.Random();
        for (int i = 0; i < 12; i++)
            pass.append(chars.charAt(random.nextInt(chars.length())));
        return pass.toString();
    }

    @GetMapping
    public String listarTrabajadores(HttpSession session, Model model) {
        Usuario usuario = (Usuario) session.getAttribute("usuario");
        if (usuario == null)
            return "redirect:/login";
        if (usuario.getRoles() != null && usuario.getRoles().contains("ROLE_WORKER"))
            return "redirect:/desboard-trabajador";

        List<Perfil> trabajadores;
        if (usuario.getRoles() != null && usuario.getRoles().contains("ROLE_ADMIN")) {
            trabajadores = perfilRepository.findByRoles("ROLE_WORKER");
        } else {
            trabajadores = perfilRepository.findByRolesAndActivoTrue("ROLE_WORKER");
        }

        long pendientes = perfilRepository.countByRolesAndActivoFalse("ROLE_WORKER");

        model.addAttribute("trabajadores", trabajadores);
        model.addAttribute("usuario", usuario);
        model.addAttribute("totalTrabajadores", trabajadores.size());
        model.addAttribute("totalPendientes", pendientes);
        return "listar-trabajadores";
    }

    @GetMapping("/{id}")
    public String verDetallesTrabajador(@PathVariable String id, HttpSession session, Model model) {
        Usuario usuario = (Usuario) session.getAttribute("usuario");
        if (usuario == null)
            return "redirect:/login";
        if (usuario.getRoles() != null && usuario.getRoles().contains("ROLE_WORKER"))
            return "redirect:/desboard-trabajador";

        Perfil trabajador = perfilRepository.findById(id != null ? id : "").orElse(null);
        if (trabajador == null)
            return "redirect:/trabajadores";

        model.addAttribute("trabajador", trabajador);
        model.addAttribute("usuario", usuario);
        return "detalles-trabajador";
    }

    @GetMapping("/perfil")
    public String perfilTrabajador(HttpSession session, Model model) {
        Usuario usuario = (Usuario) session.getAttribute("usuario");
        if (usuario == null || usuario.getRoles() == null || !usuario.getRoles().contains("ROLE_WORKER"))
            return "redirect:/login";

        Perfil trabajador = perfilRepository.findByUsernameIgnoreCase(usuario.getUsername()).orElseGet(() -> {
            Perfil nuevo = new Perfil();
            nuevo.setUsername(usuario.getUsername());
            nuevo.setRole("ROLE_WORKER");
            return nuevo;
        });

        model.addAttribute("usuario", usuario);
        model.addAttribute("trabajador", trabajador);
        return "editar-trabajador";
    }

    @PostMapping("/perfil")
    public String actualizarPerfil(
            HttpSession session,
            @ModelAttribute Perfil trabajadorEditado,
            @org.springframework.web.bind.annotation.RequestParam(value = "cvFile", required = false) org.springframework.web.multipart.MultipartFile cvFile,
            Model model) {

        Usuario usuario = (Usuario) session.getAttribute("usuario");
        if (usuario == null || usuario.getRoles() == null || !usuario.getRoles().contains("ROLE_WORKER"))
            return "redirect:/login";

        Perfil trabajador = perfilRepository.findByUsernameIgnoreCase(usuario.getUsername()).orElseGet(() -> {
            Perfil nuevo = new Perfil();
            nuevo.setUsername(usuario.getUsername());
            nuevo.setRole("ROLE_WORKER");
            return nuevo;
        });

        trabajador.setNombre(trabajadorEditado.getNombre());
        trabajador.setApellido(trabajadorEditado.getApellido());
        trabajador.setEmail(trabajadorEditado.getEmail());
        trabajador.setTelefono(trabajadorEditado.getTelefono());
        trabajador.setOficio(trabajadorEditado.getOficio());
        trabajador.setExperiencia(trabajadorEditado.getExperiencia());
        trabajador.setDisponibilidad(trabajadorEditado.getDisponibilidad());

        // Cada modificación debe volver a revisión administrativa.
        trabajador.setVerificado(false);

        if (cvFile != null && !cvFile.isEmpty()) {
            try {
                String fileName = java.util.UUID.randomUUID() + "_" + cvFile.getOriginalFilename();
                org.bson.types.ObjectId fileId = gridFsTemplate.store(
                        cvFile.getInputStream(), fileName, cvFile.getContentType());
                trabajador.setCvUrl(fileId.toString());
            } catch (java.io.IOException e) {
                model.addAttribute("error", "Error al subir CV: " + e.getMessage());
                System.err.println("Error uploading CV: " + e.getMessage());
            }
        }

        perfilRepository.save(trabajador);
        usuarioRepository.findByUsernameIgnoreCase(usuario.getUsername()).ifPresent(dbUsuario -> {
            dbUsuario.setVerificado(false);
            usuarioRepository.save(dbUsuario);
        });
        return "redirect:/trabajadores/perfil?exito=true";
    }

    @GetMapping("/disponibles")
    public String listarDisponibles(HttpSession session, Model model) {
        Usuario usuario = (Usuario) session.getAttribute("usuario");
        if (usuario == null)
            return "redirect:/login";

        List<Perfil> trabajadores = perfilRepository.findByDisponibilidadTrueAndRoles("ROLE_WORKER");
        model.addAttribute("trabajadores", trabajadores);
        model.addAttribute("usuario", usuario);
        model.addAttribute("totalTrabajadores", trabajadores.size());
        return "listar-trabajadores";
    }

    @GetMapping("/cv/{fileId}")
    public org.springframework.http.ResponseEntity<org.springframework.core.io.Resource> descargarCv(
            @PathVariable String fileId) {
        try {
            GridFSFile gridFSFile = gridFsTemplate.findOne(
                    new Query(Criteria.where("_id").is(new org.bson.types.ObjectId(fileId))));
            if (gridFSFile != null) {
                org.springframework.data.mongodb.gridfs.GridFsResource resource = gridFsTemplate
                        .getResource(gridFSFile);
                String contentType = resource.getContentType();
                if (gridFSFile.getFilename() != null
                        && gridFSFile.getFilename().toLowerCase(java.util.Locale.ROOT).endsWith(".pdf")) {
                    contentType = org.springframework.http.MediaType.APPLICATION_PDF_VALUE;
                } else if (contentType == null || contentType.isBlank()) {
                    contentType = org.springframework.http.MediaType.APPLICATION_OCTET_STREAM_VALUE;
                }
                return org.springframework.http.ResponseEntity.ok()
                        .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,
                                "inline; filename=\"" + gridFSFile.getFilename() + "\"")
                        .contentType(org.springframework.http.MediaType.parseMediaType(contentType))
                        .body(resource);
            }
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        }
        return org.springframework.http.ResponseEntity.notFound().build();
    }

    @GetMapping("/cv/{fileId}/download")
    public org.springframework.http.ResponseEntity<org.springframework.core.io.Resource> descargarCvComoArchivo(
            @PathVariable String fileId) {
        try {
            GridFSFile gridFSFile = gridFsTemplate.findOne(
                    new Query(Criteria.where("_id").is(new org.bson.types.ObjectId(fileId))));
            if (gridFSFile != null) {
                org.springframework.data.mongodb.gridfs.GridFsResource resource = gridFsTemplate
                        .getResource(gridFSFile);
                return org.springframework.http.ResponseEntity.ok()
                        .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,
                                "attachment; filename=\"" + gridFSFile.getFilename() + "\"")
                        .contentType(org.springframework.http.MediaType.parseMediaType(resource.getContentType()))
                        .body(resource);
            }
        } catch (Exception e) {
            System.err.println("Error descargando CV: " + e.getMessage());
        }
        return org.springframework.http.ResponseEntity.notFound().build();
    }

    @PostMapping("/invitaciones/{id}/aceptar")
    public String aceptarInvitacion(@PathVariable String id, HttpSession session) {
        Usuario usuario = (Usuario) session.getAttribute("usuario");
        if (usuario == null || usuario.getRoles() == null || !usuario.getRoles().contains("ROLE_WORKER"))
            return "redirect:/login";

        Perfil trabajador = perfilRepository.findByUsernameIgnoreCase(usuario.getUsername()).orElse(null);
        InvitacionTrabajo invitacion = invitacionTrabajoRepository.findById(id).orElse(null);

        if (trabajador == null || invitacion == null) {
            return "redirect:/desboard-trabajador";
        }

        if (!invitacion.getTrabajador().getId().equals(trabajador.getId())) {
            return "redirect:/desboard-trabajador";
        }

        invitacion.setEstado("ACEPTADA");
        invitacionTrabajoRepository.save(invitacion);

        Proyecto proyecto = invitacion.getProyecto();
        if (proyecto != null) {
            if (proyecto.getEquipoTrabajo() == null) {
                proyecto.setEquipoTrabajo(new java.util.ArrayList<>());
            }
            boolean yaEnEquipo = proyecto.getEquipoTrabajo().stream()
                    .anyMatch(t -> t.getId() != null && t.getId().equals(trabajador.getId()));
            if (!yaEnEquipo) {
                proyecto.getEquipoTrabajo().add(trabajador);
                proyectoRepository.save(proyecto);
            }
        }

        trabajador.setDisponibilidad(false);
        perfilRepository.save(trabajador);

        return "redirect:/desboard-trabajador?invitacion_aceptada=true";
    }

    @PostMapping("/invitaciones/{id}/rechazar")
    public String rechazarInvitacion(@PathVariable String id, HttpSession session) {
        Usuario usuario = (Usuario) session.getAttribute("usuario");
        if (usuario == null || usuario.getRoles() == null || !usuario.getRoles().contains("ROLE_WORKER"))
            return "redirect:/login";

        Perfil trabajador = perfilRepository.findByUsernameIgnoreCase(usuario.getUsername()).orElse(null);
        InvitacionTrabajo invitacion = invitacionTrabajoRepository.findById(id).orElse(null);

        if (trabajador == null || invitacion == null) {
            return "redirect:/desboard-trabajador";
        }

        if (!invitacion.getTrabajador().getId().equals(trabajador.getId())) {
            return "redirect:/desboard-trabajador";
        }

        invitacion.setEstado("RECHAZADA");
        invitacionTrabajoRepository.save(invitacion);

        return "redirect:/desboard-trabajador?invitacion_rechazada=true";
    }

    @PostMapping("/proyectos/{proyectoId}/equipos/{equipoId}/actualizar-avance")
    public String actualizarAvanceEquipo(
            @PathVariable String proyectoId,
            @PathVariable String equipoId,
            @RequestParam("porcentajeAvance") Double porcentajeAvance,
            HttpSession session) {

        Usuario usuario = (Usuario) session.getAttribute("usuario");
        if (usuario == null || usuario.getRoles() == null || !usuario.getRoles().contains("ROLE_WORKER")) {
            return "redirect:/login";
        }

        Perfil trabajador = perfilRepository.findByUsernameIgnoreCase(usuario.getUsername()).orElse(null);
        EquipoTrabajo equipo = equipoTrabajoRepository.findById(equipoId).orElse(null);
        if (trabajador == null || equipo == null || equipo.getProyecto() == null
                || !proyectoId.equals(equipo.getProyecto().getId())
                || equipo.getIntegrantes() == null
                || equipo.getIntegrantes().stream()
                        .noneMatch(integrante -> trabajador.getId().equals(integrante.getId()))) {
            return "redirect:/trabajadores/mi-equipo";
        }

        if (porcentajeAvance == null || porcentajeAvance < 0 || porcentajeAvance > 100) {
            return "redirect:/trabajadores/mi-equipo?error_avance=invalido";
        }

        equipo.setPorcentajeAvance(porcentajeAvance);
        equipoTrabajoRepository.save(equipo);
        actualizarEstadoProyecto(equipo.getProyecto());
        return "redirect:/trabajadores/mi-equipo?avance_actualizado=true";
    }

    private void actualizarEstadoProyecto(Proyecto proyecto) {
        List<EquipoTrabajo> equipos = equipoTrabajoRepository.findByProyectoId(proyecto.getId());
        if (equipos == null || equipos.isEmpty()) {
            return;
        }

        boolean todosCompletos = equipos.stream()
                .allMatch(equipo -> equipo.getPorcentajeAvance() != null && equipo.getPorcentajeAvance() >= 100.0);
        boolean hayAvance = equipos.stream()
                .anyMatch(equipo -> equipo.getPorcentajeAvance() != null && equipo.getPorcentajeAvance() > 0.0);

        if (todosCompletos) {
            proyecto.setEstadoEjecucion(EstadoEjecucion.COMPLETADO);
        } else if (hayAvance) {
            proyecto.setEstadoEjecucion(EstadoEjecucion.EN_PROGRESO);
        } else {
            proyecto.setEstadoEjecucion(EstadoEjecucion.PENDIENTE);
        }
        proyectoRepository.save(proyecto);
    }

    @GetMapping("/mi-equipo")
    public String verMiEquipo(HttpSession session, Model model) {
        Usuario usuario = (Usuario) session.getAttribute("usuario");
        if (usuario == null || usuario.getRoles() == null || !usuario.getRoles().contains("ROLE_WORKER"))
            return "redirect:/login";

        Perfil trabajador = perfilRepository.findByUsernameIgnoreCase(usuario.getUsername()).orElse(null);
        if (trabajador == null) {
            return "redirect:/desboard-trabajador";
        }

        List<EquipoTrabajo> misEquipos = equipoTrabajoRepository.findByIntegrantesContaining(trabajador);
        model.addAttribute("usuario", usuario);
        model.addAttribute("trabajador", trabajador);
        model.addAttribute("equipos", misEquipos);

        return "trabajadores/trabajador-equipo";
    }
}
