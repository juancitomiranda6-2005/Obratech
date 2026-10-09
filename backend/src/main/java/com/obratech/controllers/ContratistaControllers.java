package com.obratech.controllers;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.obratech.entity.EquipoTrabajo;
import com.obratech.entity.InvitacionTrabajo;
import com.obratech.entity.Perfil;
import com.obratech.entity.Usuario;
import com.obratech.entity.enums.EstadoEjecucion;
import com.obratech.repository.CalificacionRepository;
import com.obratech.repository.EquipoTrabajoRepository;
import com.obratech.repository.InvitacionTrabajoRepository;
import com.obratech.repository.PerfilRepository;
import com.obratech.repository.ProyectoRepository;

import jakarta.servlet.http.HttpSession;

/**
 * Controlador para la gestión de contratistas, perfiles, envío de invitaciones
 * a trabajadores
 * y administración de equipos de trabajo por proyecto.
 */
@Controller
@RequestMapping("/contratistas")
public class ContratistaControllers {

    @Autowired
    private PerfilRepository perfilRepository;
    @Autowired
    private CalificacionRepository calificacionRepository;
    @Autowired
    private ProyectoRepository proyectoRepository;
    @Autowired
    private InvitacionTrabajoRepository invitacionTrabajoRepository;
    @Autowired
    private EquipoTrabajoRepository equipoTrabajoRepository;
    @Autowired
    private org.springframework.data.mongodb.gridfs.GridFsTemplate gridFsTemplate;
    @Autowired
    private com.obratech.service.ContratistaProfileValidator contratistaProfileValidator;

    @GetMapping
    public String listarContratistas(HttpSession session, Model model) {
        Usuario usuario = (Usuario) session.getAttribute("usuario");
        if (usuario == null)
            return "redirect:/login";

        List<Perfil> contratistas = perfilRepository.findByRolesAndActivoTrue("ROLE_CONTRACTOR");
        model.addAttribute("contratistas", contratistas);
        model.addAttribute("usuario", usuario);
        model.addAttribute("totalContratistas", contratistas.size());

        return "listar-contratistas";
    }

    // Ver detalles de un contratista
    @GetMapping("/{id}")
    public String verDetallesContratista(@PathVariable String id, HttpSession session, Model model) {
        Usuario usuario = (Usuario) session.getAttribute("usuario");
        if (usuario == null)
            return "redirect:/login";

        @SuppressWarnings("null")
        String safeId = id != null ? id : "";
        Perfil contratista = perfilRepository.findById(safeId).orElse(null);
        if (contratista == null)
            return "redirect:/contratistas";
        model.addAttribute("contratista", contratista);

        if (contratista.getEmail() != null) {
            perfilRepository.findByEmailIgnoreCase(contratista.getEmail()).ifPresent(p -> {
                model.addAttribute("calificaciones", calificacionRepository.findByContratistaId(p.getId()));
                model.addAttribute("personaId", p.getId());
            });
        }

        model.addAttribute("usuario", usuario);
        return "detalles-contratista";
    }

    /**
     * Muestra los detalles de un contratista buscando por su nombre de usuario.
     * esto hay que verficar si ya esta implementado en el frontend, si no, se puede
     * eliminar este método y usar el anterior que busca por ID.
     */

    @GetMapping("/por-username/{username}")
    public String verPorUsername(@PathVariable String username, HttpSession session, Model model) {
        Usuario usuario = (Usuario) session.getAttribute("usuario");
        if (usuario == null)
            return "redirect:/login";

        Perfil contratista = perfilRepository.findByUsernameIgnoreCase(username).orElse(null);
        if (contratista == null)
            return "redirect:/contratistas";
        model.addAttribute("contratista", contratista);

        if (contratista.getEmail() != null) {
            perfilRepository.findByEmailIgnoreCase(contratista.getEmail()).ifPresent(p -> {
                model.addAttribute("calificaciones", calificacionRepository.findByContratistaId(p.getId()));
                model.addAttribute("personaId", p.getId());
            });
        }

        model.addAttribute("usuario", usuario);
        return "detalles-contratista";
    }

    /**
     * Presenta el formulario para que el contratista edite sus propios datos de
     * perfil.
     */
    @GetMapping("/editar")
    public String mostrarFormularioEditarPerfil(HttpSession session, Model model) {
        Usuario usuario = (Usuario) session.getAttribute("usuario");
        if (usuario == null || usuario.getRoles() == null || !usuario.getRoles().contains("ROLE_CONTRACTOR"))
            return "redirect:/login";

        Perfil contratista = perfilRepository.findByUsernameIgnoreCase(usuario.getUsername()).orElse(null);
        if (contratista == null)
            return "redirect:/perfil-contratista";

        model.addAttribute("contratista", contratista);
        model.addAttribute("usuario", usuario);
        return "editar-contratista";
    }

    /**
     * Guarda la actualización del perfil del contratista, incluyendo la subida
     * opcional de hoja de vida (CV).
     */
    @PostMapping("/editar")
    public String guardarPerfilPropio(
            @ModelAttribute Perfil form,
            @RequestParam(value = "cvFile", required = false) org.springframework.web.multipart.MultipartFile cvFile,
            @RequestParam(value = "estatutosFile", required = false) org.springframework.web.multipart.MultipartFile estatutosFile,
            @RequestParam(value = "fotoFile", required = false) org.springframework.web.multipart.MultipartFile fotoFile,
            @RequestParam(value = "matriculaFile", required = false) org.springframework.web.multipart.MultipartFile matriculaFile,
            HttpSession session,
            Model model) {

        Usuario usuario = (Usuario) session.getAttribute("usuario");
        if (usuario == null || usuario.getRoles() == null || !usuario.getRoles().contains("ROLE_CONTRACTOR"))
            return "redirect:/login";

        Perfil contratista = perfilRepository.findByUsernameIgnoreCase(usuario.getUsername()).orElse(null);
        if (contratista == null)
            return "redirect:/perfil-contratista";

        contratista.setNombre(form.getNombre());
        contratista.setApellido(form.getApellido());
        contratista.setEmail(form.getEmail());
        contratista.setTelefono(form.getTelefono());
        contratista.setEspecialidad(form.getEspecialidad());
        contratista.setDescripcion(form.getDescripcion());
        contratista.setCiudad(form.getCiudad());
        contratista.setDepartamento(form.getDepartamento());
        contratista.setUbicacion((form.getCiudad() == null ? "" : form.getCiudad().trim())
                + ", " + (form.getDepartamento() == null ? "" : form.getDepartamento().trim()));
        contratista.setMatriculaProfesional(form.getMatriculaProfesional());
        contratista.setSubespecialidades(form.getSubespecialidades());
        if (form.getExperiencia() != null && form.getExperiencia() >= 0)
            contratista.setExperiencia(form.getExperiencia());

        String fileError = null;
        if (fotoFile != null && !fotoFile.isEmpty())
            fileError = contratistaProfileValidator.validateImage(fotoFile);
        if (fileError == null && matriculaFile != null && !matriculaFile.isEmpty()) {
            fileError = contratistaProfileValidator.validatePdf(matriculaFile, "la matrícula profesional");
        }
        if (fileError != null) {
            model.addAttribute("error", fileError);
            model.addAttribute("contratista", contratista);
            model.addAttribute("usuario", usuario);
            return "editar-contratista";
        }

        try {
            if (fotoFile != null && !fotoFile.isEmpty()) {
                String fileName = java.util.UUID.randomUUID() + "_foto_" + fotoFile.getOriginalFilename();
                org.bson.types.ObjectId fileId = gridFsTemplate.store(
                        fotoFile.getInputStream(), fileName, fotoFile.getContentType());
                contratista.setFotoPerfilUrl(fileId.toString());
            }
            if (matriculaFile != null && !matriculaFile.isEmpty()) {
                String fileName = java.util.UUID.randomUUID() + "_matricula_" + matriculaFile.getOriginalFilename();
                org.bson.types.ObjectId fileId = gridFsTemplate.store(
                        matriculaFile.getInputStream(), fileName, matriculaFile.getContentType());
                contratista.setMatriculaDocumentoUrl(fileId.toString());
            }
        } catch (java.io.IOException e) {
            model.addAttribute("error", "No se pudieron guardar los documentos: " + e.getMessage());
            model.addAttribute("contratista", contratista);
            model.addAttribute("usuario", usuario);
            return "editar-contratista";
        }

        if (cvFile != null && !cvFile.isEmpty()) {
            try {
                String fileName = java.util.UUID.randomUUID() + "_cv_" + cvFile.getOriginalFilename();
                org.bson.types.ObjectId fileId = gridFsTemplate.store(
                        cvFile.getInputStream(), fileName, cvFile.getContentType());
                contratista.setCvUrl(fileId.toString());
            } catch (java.io.IOException e) {
                System.err.println("Error uploading CV: " + e.getMessage());
            }
        }

        if (estatutosFile != null && !estatutosFile.isEmpty()) {
            try {
                String fileName = java.util.UUID.randomUUID() + "_estatutos_" + estatutosFile.getOriginalFilename();
                org.bson.types.ObjectId fileId = gridFsTemplate.store(
                        estatutosFile.getInputStream(), fileName, estatutosFile.getContentType());
                contratista.setEstatutosUrl(fileId.toString());
            } catch (java.io.IOException e) {
                System.err.println("Error uploading estatutos: " + e.getMessage());
            }
        }

        perfilRepository.save(contratista);
        return "redirect:/perfil-contratista?exito=true";
    }

    /**
     * Formulario de selección de proyecto para contratar a un trabajador.
     */
    @GetMapping("/contratar/{trabajadorId}")
    public String formularioContratar(@PathVariable String trabajadorId, HttpSession session, Model model) {
        Usuario usuario = (Usuario) session.getAttribute("usuario");
        if (usuario == null || usuario.getRoles() == null || !usuario.getRoles().contains("ROLE_CONTRACTOR"))
            return "redirect:/login";

        Perfil contratista = perfilRepository.findByUsernameIgnoreCase(usuario.getUsername()).orElse(null);
        if (contratista == null)
            return "redirect:/desboard";

        Perfil trabajador = perfilRepository.findById(trabajadorId).orElse(null);
        if (trabajador == null)
            return "redirect:/trabajadores";

        List<com.obratech.entity.Proyecto> proyectosActivos = proyectoRepository
                .findByContratistaAsignadoIdAndEstadoEjecucionNot(contratista.getId(), EstadoEjecucion.COMPLETADO);

        model.addAttribute("usuario", usuario);
        model.addAttribute("trabajador", trabajador);
        model.addAttribute("proyectosActivos", proyectosActivos);
        return "seleccionar-proyecto-contrato";
    }

    /**
     * Envía una invitación de trabajo formal a un trabajador para unirse a un
     * proyecto específico.
     */
    @PostMapping("/contratar/{trabajadorId}")
    public String contratarTrabajador(
            @PathVariable String trabajadorId,
            @RequestParam("proyectoId") String proyectoId,
            HttpSession session) {

        Usuario usuario = (Usuario) session.getAttribute("usuario");
        if (usuario == null || usuario.getRoles() == null || !usuario.getRoles().contains("ROLE_CONTRACTOR"))
            return "redirect:/login";

        Perfil contratista = perfilRepository.findByUsernameIgnoreCase(usuario.getUsername()).orElse(null);
        @SuppressWarnings("null")
        String safeTId = trabajadorId != null ? trabajadorId : "";
        Perfil trabajador = perfilRepository.findById(safeTId).orElse(null);
        @SuppressWarnings("null")
        String safePId = proyectoId != null ? proyectoId : "";
        com.obratech.entity.Proyecto proyecto = proyectoRepository.findById(safePId).orElse(null);

        if (contratista == null || trabajador == null || proyecto == null) {
            return "redirect:/trabajadores";
        }

        if (proyecto.getContratistaAsignado() != null
                && proyecto.getContratistaAsignado().getId().equals(contratista.getId())) {

            boolean yaEnProyecto = proyecto.getEquipoTrabajo() != null && proyecto.getEquipoTrabajo().stream()
                    .anyMatch(t -> t.getId() != null && t.getId().equals(trabajador.getId()));

            if (yaEnProyecto) {
                return "redirect:/contratistas/contratar/" + trabajadorId + "?error_contrato=ya_en_proyecto";
            }

            List<InvitacionTrabajo> pendientes = invitacionTrabajoRepository
                    .findByTrabajadorIdAndEstado(trabajador.getId(), "PENDIENTE");
            boolean yaInvitado = pendientes.stream()
                    .anyMatch(inv -> inv.getProyecto() != null && inv.getProyecto().getId().equals(proyecto.getId()));
            if (yaInvitado) {
                return "redirect:/contratistas/contratar/" + trabajadorId + "?error_contrato=ya_invitado";
            }

            InvitacionTrabajo invitacion = new InvitacionTrabajo();
            invitacion.setProyecto(proyecto);
            invitacion.setTrabajador(trabajador);
            invitacion.setContratista(contratista);
            invitacion.setEstado("PENDIENTE");
            invitacion.setFechaCreacion(java.time.LocalDateTime.now());

            invitacionTrabajoRepository.save(invitacion);

            return "redirect:/proyectos/" + proyectoId + "?invitacion_enviada=true";
        }

        return "redirect:/contratistas/proyectos-asignados";
    }

    @GetMapping("/proyectos/{proyectoId}/equipos/crear")
    public String mostrarFormularioCrearEquipo(
            @PathVariable String proyectoId,
            HttpSession session,
            Model model) {

        Usuario usuario = (Usuario) session.getAttribute("usuario");
        if (usuario == null || usuario.getRoles() == null || !usuario.getRoles().contains("ROLE_CONTRACTOR")) {
            return "redirect:/login";
        }

        Perfil contratista = perfilRepository.findByUsernameIgnoreCase(usuario.getUsername()).orElse(null);
        com.obratech.entity.Proyecto proyecto = proyectoRepository.findById(proyectoId).orElse(null);
        if (contratista == null || proyecto == null || proyecto.getContratistaAsignado() == null
                || !proyecto.getContratistaAsignado().getId().equals(contratista.getId())) {
            return "redirect:/contratistas/proyectos-asignados";
        }

        model.addAttribute("proyecto", proyecto);
        model.addAttribute("usuario", usuario);
        return "contratistas/crear-equipo";
    }

    @PostMapping("/proyectos/{proyectoId}/equipos/crear")
    public String crearEquipoTrabajo(
            @PathVariable String proyectoId,
            @RequestParam("nombre") String nombre,
            @RequestParam("actividad") String actividad,
            @RequestParam(value = "porcentajeAvance", defaultValue = "0.0") Double porcentajeAvance,
            @RequestParam(value = "integranteIds", required = false) List<String> integranteIds,
            HttpSession session) {

        Usuario usuario = (Usuario) session.getAttribute("usuario");
        if (usuario == null || usuario.getRoles() == null || !usuario.getRoles().contains("ROLE_CONTRACTOR"))
            return "redirect:/login";

        Perfil contratista = perfilRepository.findByUsernameIgnoreCase(usuario.getUsername()).orElse(null);
        com.obratech.entity.Proyecto proyecto = proyectoRepository.findById(proyectoId).orElse(null);

        if (contratista == null || proyecto == null) {
            return "redirect:/desboard";
        }

        if (proyecto.getContratistaAsignado() == null
                || !proyecto.getContratistaAsignado().getId().equals(contratista.getId())) {
            return "redirect:/desboard";
        }

        if (integranteIds == null || integranteIds.isEmpty()) {
            return "redirect:/proyectos/" + proyectoId + "?error_equipo=minimo_un_integrante";
        }

        List<Perfil> integrantes = new java.util.ArrayList<>();
        for (String id : integranteIds) {
            Perfil integrante = perfilRepository.findById(id).orElse(null);
            if (integrante == null) {
                return "redirect:/proyectos/" + proyectoId + "?error_equipo=integrante_invalido";
            }
            boolean perteneceAlPool = proyecto.getEquipoTrabajo().stream()
                    .anyMatch(t -> t.getId() != null && t.getId().equals(integrante.getId()));
            if (!perteneceAlPool) {
                return "redirect:/proyectos/" + proyectoId + "?error_equipo=integrante_fuera_de_pool";
            }
            integrantes.add(integrante);
        }

        EquipoTrabajo nuevoEquipo = new EquipoTrabajo();
        nuevoEquipo.setNombre(nombre);
        nuevoEquipo.setActividad(actividad);
        nuevoEquipo.setPorcentajeAvance(porcentajeAvance);
        nuevoEquipo.setProyecto(proyecto);
        nuevoEquipo.setIntegrantes(integrantes);

        equipoTrabajoRepository.save(nuevoEquipo);

        return "redirect:/proyectos/" + proyectoId + "?equipo_creado=true";
    }

    @PostMapping("/proyectos/{proyectoId}/equipos/{equipoId}/eliminar")
    public String eliminarEquipoTrabajo(
            @PathVariable String proyectoId,
            @PathVariable String equipoId,
            HttpSession session) {

        Usuario usuario = (Usuario) session.getAttribute("usuario");
        if (usuario == null || usuario.getRoles() == null || !usuario.getRoles().contains("ROLE_CONTRACTOR"))
            return "redirect:/login";

        Perfil contratista = perfilRepository.findByUsernameIgnoreCase(usuario.getUsername()).orElse(null);
        com.obratech.entity.Proyecto proyecto = proyectoRepository.findById(proyectoId).orElse(null);

        if (contratista == null || proyecto == null) {
            return "redirect:/desboard";
        }

        if (proyecto.getContratistaAsignado() == null
                || !proyecto.getContratistaAsignado().getId().equals(contratista.getId())) {
            return "redirect:/desboard";
        }

        equipoTrabajoRepository.deleteById(equipoId);

        return "redirect:/proyectos/" + proyectoId + "?equipo_eliminado=true";
    }

    /**
     * Muestra la lista de proyectos en ejecución asignados al contratista.
     */
    @GetMapping("/proyectos-asignados")
    public String proyectosAsignados(HttpSession session, Model model) {
        Usuario usuario = (Usuario) session.getAttribute("usuario");
        if (usuario == null)
            return "redirect:/login";

        Perfil contratista = perfilRepository.findByUsernameIgnoreCase(usuario.getUsername()).orElse(null);
        if (contratista == null)
            return "redirect:/desboard";

        List<com.obratech.entity.Proyecto> proyectos = proyectoRepository
                .findByContratistaAsignadoIdAndEstadoEjecucionNot(contratista.getId(), EstadoEjecucion.COMPLETADO);
        model.addAttribute("proyectos", proyectos);
        model.addAttribute("usuario", usuario);
        return "contratista-proyectos-asignados";
    }

    /**
     * Muestra el historial de proyectos completados por el contratista.
     */
    @GetMapping("/historial")
    public String historialProyectos(HttpSession session, Model model) {
        Usuario usuario = (Usuario) session.getAttribute("usuario");
        if (usuario == null)
            return "redirect:/login";

        Perfil contratista = perfilRepository.findByUsernameIgnoreCase(usuario.getUsername()).orElse(null);
        if (contratista == null)
            return "redirect:/desboard";

        List<com.obratech.entity.Proyecto> historial = proyectoRepository
                .findByContratistaAsignadoIdAndEstadoEjecucion(contratista.getId(), EstadoEjecucion.COMPLETADO);
        model.addAttribute("proyectos", historial);
        model.addAttribute("usuario", usuario);
        model.addAttribute("esHistorial", true);
        return "contratista-historial-proyectos";
    }

    /**
     * Muestra todas las cuadrillas/equipos de trabajo asociadas a los proyectos del
     * contratista.
     */
    @GetMapping("/mi-equipo")
    public String verMisEquipos(HttpSession session, Model model) {
        Usuario usuario = (Usuario) session.getAttribute("usuario");
        if (usuario == null || usuario.getRoles() == null || !usuario.getRoles().contains("ROLE_CONTRACTOR"))
            return "redirect:/login";

        Perfil contratista = perfilRepository.findByUsernameIgnoreCase(usuario.getUsername()).orElse(null);
        if (contratista == null)
            return "redirect:/desboard";

        java.util.List<com.obratech.entity.Proyecto> proyectos = proyectoRepository
                .findByContratistaAsignadoId(contratista.getId());
        java.util.List<EquipoTrabajo> equipos = new ArrayList<>();
        if (proyectos != null) {
            for (com.obratech.entity.Proyecto p : proyectos) {
                if (p == null || p.getId() == null)
                    continue;
                java.util.List<EquipoTrabajo> porProyecto = equipoTrabajoRepository.findByProyectoId(p.getId());
                if (porProyecto != null && !porProyecto.isEmpty())
                    equipos.addAll(porProyecto);
            }
        }

        model.addAttribute("usuario", usuario);
        model.addAttribute("contratista", contratista);
        model.addAttribute("equipos", equipos);
        return "contratistas/contratista-equipos";
    }

    /**
     * Filtra los contratistas por especialidad.
     */
    @GetMapping("/buscar/{especialidad}")
    public String buscarPorEspecialidad(@PathVariable String especialidad, HttpSession session, Model model) {
        Usuario usuario = (Usuario) session.getAttribute("usuario");
        if (usuario == null)
            return "redirect:/login";

        List<Perfil> contratistas = perfilRepository
                .findByEspecialidadContainingIgnoreCaseAndRoles(especialidad, "ROLE_CONTRACTOR");

        model.addAttribute("contratistas", contratistas);
        model.addAttribute("usuario", usuario);
        model.addAttribute("especialidad", especialidad);

        model.addAttribute("totalContratistas", contratistas.size());
           return "listar-contratistas";
    }

    /**
     * Muestra la lista de contratistas disponibles para calificar en un proyecto.
     */
    @GetMapping("/para-calificar/{proyectoId}")
    public String contratistasParaCalificar(@PathVariable String proyectoId, HttpSession session, Model model) {
        Usuario usuario = (Usuario) session.getAttribute("usuario");
        if (usuario == null)
            return "redirect:/login";

        List<Perfil> contratistas = perfilRepository.findByRolesAndActivoTrue("ROLE_CONTRACTOR");
        model.addAttribute("contratistas", contratistas);
        model.addAttribute("usuario", usuario);
        model.addAttribute("totalContratistas", contratistas.size());
        model.addAttribute("proyectoId", proyectoId);
        return "clientes/contratistas-para-calificar";
 
   }
}