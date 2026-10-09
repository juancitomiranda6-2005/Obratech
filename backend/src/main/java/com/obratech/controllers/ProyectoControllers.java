package com.obratech.controllers;

import java.time.LocalDate;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.obratech.entity.Proyecto;
import com.obratech.entity.Usuario;
import com.obratech.entity.EquipoTrabajo;
import com.obratech.entity.enums.EstadoAsignacion;
import com.obratech.entity.enums.EstadoEjecucion;
import com.obratech.repository.ProyectoRepository;
import com.obratech.repository.EquipoTrabajoRepository;
import com.obratech.repository.PostulacionRepository;
import com.obratech.service.ProyectoService;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/proyectos")
public class ProyectoControllers {

    @Autowired
    private ProyectoService proyectoService;

    @Autowired
    private ProyectoRepository proyectoRepository;

    @Autowired
    private GridFsTemplate gridFsTemplate;

    @Autowired
    private com.obratech.repository.UsuarioRepository usuarioRepository;

    @Autowired
    private EquipoTrabajoRepository equipoTrabajoRepository;

    @Autowired
    private PostulacionRepository postulacionRepository;

    @GetMapping("/publicar")
    public String mostrarFormularioProyecto(HttpSession session, Model model) {
        Usuario usuario = (Usuario) session.getAttribute("usuario");
        if (usuario == null)
            return "redirect:/login";

        Usuario dbUsuario = usuarioRepository.findByUsernameIgnoreCase(usuario.getUsername()).orElse(null);
        if (dbUsuario == null || !dbUsuario.isVerificado()) {
            return "redirect:/desboard?errorVerificacion=true";
        }

        model.addAttribute("proyecto", new Proyecto());
        model.addAttribute("usuario", dbUsuario);
        return "publicar-proyecto-new";
    }

    @PostMapping("/publicar")
    public String publicarProyecto(
            @ModelAttribute Proyecto proyecto,
            @RequestParam(value = "documentoLegal", required = false) MultipartFile documentoLegal,
            HttpSession session,
            Model model) {

        Usuario usuario = (Usuario) session.getAttribute("usuario");
        if (usuario == null)
            return "redirect:/login";

        Usuario dbUsuario = usuarioRepository.findByUsernameIgnoreCase(usuario.getUsername()).orElse(null);
        if (dbUsuario == null || !dbUsuario.isVerificado()) {
            return "redirect:/desboard?errorVerificacion=true";
        }

        proyecto.setEstadoAsignacion(EstadoAsignacion.SIN_ASIGNAR);
        proyecto.setEstadoEjecucion(EstadoEjecucion.PENDIENTE);
        proyecto.setFechaCreacion(java.time.LocalDateTime.now());
        proyecto.setCliente(dbUsuario);

        if (proyecto.getFechaInicio() == null) {
            proyecto.setFechaInicio(LocalDate.now());
        }

        if (documentoLegal != null && !documentoLegal.isEmpty()) {
            try {
                String fileName = System.currentTimeMillis() + "_" + documentoLegal.getOriginalFilename();
                org.bson.types.ObjectId fileId = gridFsTemplate.store(
                        documentoLegal.getInputStream(), fileName, documentoLegal.getContentType());
                proyecto.setDocumentoLegalUrl(fileId.toString());
                proyecto.setDocumentoLegalNombre(documentoLegal.getOriginalFilename());
            } catch (Exception e) {
                model.addAttribute("error", "Error al subir el documento: " + e.getMessage());
                model.addAttribute("proyecto", proyecto);
                model.addAttribute("usuario", dbUsuario);
                return "publicar-proyecto-new";
            }
        }

        proyectoService.publicarProyecto(proyecto, usuario);
        return "redirect:/proyectos/mis-proyectos";
    }

    @GetMapping("/mis-proyectos")
    public String verMisProyectos(HttpSession session, Model model) {
        Usuario usuario = (Usuario) session.getAttribute("usuario");

        if (usuario == null) {
            return "redirect:/login";
        }

        Usuario dbUsuario = usuarioRepository.findByUsernameIgnoreCase(usuario.getUsername()).orElse(usuario);
        List<Proyecto> proyectos = proyectoRepository.findByClienteId(dbUsuario.getId());
        if (proyectos == null) {
            proyectos = new ArrayList<>();
        }

        long enProgreso = proyectoRepository.countByClienteIdAndEstadoEjecucion(usuario.getId(),
                EstadoEjecucion.EN_PROGRESO);
        long completados = proyectoRepository.countByClienteIdAndEstadoEjecucion(usuario.getId(),
                EstadoEjecucion.COMPLETADO);
        Map<String, Integer> postulantesPorProyecto = new HashMap<>();
        Map<String, Double> progresoPorProyecto = new HashMap<>();
        for (Proyecto proyecto : proyectos) {
            postulantesPorProyecto.put(proyecto.getId(),
                    postulacionRepository.findByProyectoId(proyecto.getId()).size());
            progresoPorProyecto.put(proyecto.getId(), calcularProgreso(proyecto));
        }

        model.addAttribute("proyectos", proyectos);
        model.addAttribute("usuario", usuario);
        model.addAttribute("totalProyectos", proyectoRepository.countByClienteId(usuario.getId()));
        model.addAttribute("proyectosEnProgreso", enProgreso);
        model.addAttribute("proyectosCompletados", completados);
        model.addAttribute("postulantesPorProyecto", postulantesPorProyecto);
        model.addAttribute("progresoPorProyecto", progresoPorProyecto);
        addFlashMessageToModel(session, model);

        return "mis-proyectos";
    }

    @GetMapping("/{id}")
    public String verDetallesProyecto(
            @PathVariable String id,
            HttpSession session,
            Model model) {

        Usuario usuario = (Usuario) session.getAttribute("usuario");

        if (usuario == null) {
            return "redirect:/login";
        }

        @SuppressWarnings("null")
        String safeId1 = id != null ? id : "";
        Proyecto proyecto = proyectoRepository.findById(safeId1).orElse(null);

        boolean isOwner = proyecto != null && proyecto.getCliente() != null
                && proyecto.getCliente().getUsername() != null
                && proyecto.getCliente().getUsername().equalsIgnoreCase(usuario.getUsername());

        boolean isAssignedContractor = proyecto != null && proyecto.getContratistaAsignado() != null
                && proyecto.getContratistaAsignado().getUsername() != null
                && proyecto.getContratistaAsignado().getUsername().equalsIgnoreCase(usuario.getUsername());

        boolean isAdmin = usuario.getRoles() != null && usuario.getRoles().contains("ROLE_ADMIN");

        if (proyecto == null || !(isOwner || isAssignedContractor || isAdmin)) {
            return "redirect:/proyectos/mis-proyectos";
        }

        model.addAttribute("proyecto", proyecto);
        model.addAttribute("usuario", usuario);
        List<EquipoTrabajo> equipos = equipoTrabajoRepository.findByProyectoId(proyecto.getId());
        model.addAttribute("equipos", equipos);
        model.addAttribute("totalPostulantes", postulacionRepository.findByProyectoId(proyecto.getId()).size());
        model.addAttribute("progresoProyecto", calcularProgreso(proyecto));
        return "detalles-proyecto";
    }

    private double calcularProgreso(Proyecto proyecto) {
        List<EquipoTrabajo> equipos = equipoTrabajoRepository.findByProyectoId(proyecto.getId());
        if (equipos == null || equipos.isEmpty())
            return 0.0;
        return equipos.stream()
                .map(EquipoTrabajo::getPorcentajeAvance)
                .filter(java.util.Objects::nonNull)
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);
    }

    private void addFlashMessageToModel(HttpSession session, Model model) {
        Object mensaje = session.getAttribute("mensaje");
        Object error = session.getAttribute("error");
        if (mensaje != null) {
            model.addAttribute("notificacion", mensaje);
            model.addAttribute("notificacionTipo", "success");
            session.removeAttribute("mensaje");
        } else if (error != null) {
            model.addAttribute("notificacion", error);
            model.addAttribute("notificacionTipo", "error");
            session.removeAttribute("error");
        }
    }

    @GetMapping("/{id}/editar")
    public String mostrarFormularioEditar(
            @PathVariable String id,
            HttpSession session,
            Model model) {

        Usuario usuario = (Usuario) session.getAttribute("usuario");

        if (usuario == null) {
            return "redirect:/login";
        }

        @SuppressWarnings("null")
        String safeId3 = id != null ? id : "";
        Proyecto proyecto = proyectoRepository.findById(safeId3).orElse(null);

        if (proyecto == null ||
                proyecto.getCliente() == null || proyecto.getCliente().getUsername() == null
                || !proyecto.getCliente().getUsername().equals(usuario.getUsername())) {
            return "redirect:/proyectos/mis-proyectos";
        }

        model.addAttribute("proyecto", proyecto);
        model.addAttribute("usuario", usuario);
        return "editar-proyecto";
    }

    @PostMapping("/{id}/editar")
    public String editarProyecto(
            @PathVariable String id,
            @ModelAttribute Proyecto proyectoEditado,
            HttpSession session) {

        Usuario usuario = (Usuario) session.getAttribute("usuario");

        if (usuario == null) {
            return "redirect:/login";
        }

        @SuppressWarnings("null")
        String safeId4 = id != null ? id : "";
        Proyecto proyecto = proyectoRepository.findById(safeId4).orElse(null);

        if (proyecto == null ||
                proyecto.getCliente() == null || proyecto.getCliente().getUsername() == null
                || !proyecto.getCliente().getUsername().equals(usuario.getUsername())) {
            return "redirect:/proyectos/mis-proyectos";
        }
        proyecto.setTitulo(proyectoEditado.getTitulo());
        proyecto.setDescripcion(proyectoEditado.getDescripcion());
        proyecto.setUbicacion(proyectoEditado.getUbicacion());
        proyecto.setTipoProyecto(proyectoEditado.getTipoProyecto());
        proyecto.setPresupuesto(proyectoEditado.getPresupuesto());
        proyecto.setFechaInicio(proyectoEditado.getFechaInicio());
        proyecto.setFechaEntrega(proyectoEditado.getFechaEntrega());
        proyecto.setPlazoEstimado(proyectoEditado.getPlazoEstimado());
        proyecto.setAreaTotal(proyectoEditado.getAreaTotal());
        proyecto.setObservaciones(proyectoEditado.getObservaciones());

        proyectoRepository.save(proyecto);

        return "redirect:/proyectos/" + id;
    }

    @PostMapping("/{id}/eliminar")
    public String eliminarProyecto(
            @PathVariable String id,
            HttpSession session) {

        Usuario usuario = (Usuario) session.getAttribute("usuario");

        if (usuario == null) {
            return "redirect:/login";
        }

        Proyecto proyecto = proyectoRepository.findById(id).orElse(null);

        if (proyecto != null && (proyecto.getCliente() == null || (proyecto.getCliente().getUsername() != null
                && proyecto.getCliente().getUsername().equals(usuario.getUsername())))) {
            @SuppressWarnings("null")
            String safeId2 = id != null ? id : "";
            proyectoRepository.deleteById(safeId2);
        }

        return "redirect:/proyectos/mis-proyectos";
    }

    @GetMapping("/documento/{id}/preview")
    public org.springframework.http.ResponseEntity<org.springframework.core.io.Resource> previewDocumento(
            @PathVariable String id) {
        try {
            com.mongodb.client.gridfs.model.GridFSFile file = gridFsTemplate.findOne(
                    new org.springframework.data.mongodb.core.query.Query(
                            org.springframework.data.mongodb.core.query.Criteria.where("_id")
                                    .is(new org.bson.types.ObjectId(id))));
            if (file != null) {
                org.springframework.data.mongodb.gridfs.GridFsResource resource = gridFsTemplate.getResource(file);
                String contentType = contentTypeFor(file, resource);
                return org.springframework.http.ResponseEntity.ok()
                        .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,
                                "inline; filename=\"" + file.getFilename() + "\"")
                        .contentType(org.springframework.http.MediaType.parseMediaType(contentType))
                        .body(resource);
            }
        } catch (Exception e) {
            System.err.println("Error previsualizando documento: " + e.getMessage());
        }
        return org.springframework.http.ResponseEntity.notFound().build();
    }

    @GetMapping("/documento/{id}/download")
    public org.springframework.http.ResponseEntity<org.springframework.core.io.Resource> downloadDocumento(
            @PathVariable String id) {
        try {
            com.mongodb.client.gridfs.model.GridFSFile file = gridFsTemplate.findOne(
                    new org.springframework.data.mongodb.core.query.Query(
                            org.springframework.data.mongodb.core.query.Criteria.where("_id")
                                    .is(new org.bson.types.ObjectId(id))));
            if (file != null) {
                org.springframework.data.mongodb.gridfs.GridFsResource resource = gridFsTemplate.getResource(file);
                return org.springframework.http.ResponseEntity.ok()
                        .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,
                                "attachment; filename=\"" + file.getFilename() + "\"")
                        .contentType(org.springframework.http.MediaType.parseMediaType(contentTypeFor(file, resource)))
                        .body(resource);
            }
        } catch (Exception e) {
            System.err.println("Error descargando documento: " + e.getMessage());
        }
        return org.springframework.http.ResponseEntity.notFound().build();
    }

    private String contentTypeFor(
            com.mongodb.client.gridfs.model.GridFSFile file,
            org.springframework.data.mongodb.gridfs.GridFsResource resource) {
        String filename = file.getFilename() == null ? "" : file.getFilename().toLowerCase(java.util.Locale.ROOT);
        if (filename.endsWith(".pdf")) {
            return org.springframework.http.MediaType.APPLICATION_PDF_VALUE;
        }
        String contentType = resource.getContentType();
        return contentType == null || contentType.isBlank()
                ? org.springframework.http.MediaType.APPLICATION_OCTET_STREAM_VALUE
                : contentType;
    }
}
