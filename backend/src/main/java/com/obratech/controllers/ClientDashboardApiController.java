package com.obratech.controllers;

import java.util.Comparator;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.time.LocalDateTime;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.obratech.entity.Perfil;
import com.obratech.entity.Proyecto;
import com.obratech.entity.EvidenciaAdjunta;
import com.obratech.entity.ReporteProyecto;
import com.obratech.entity.Usuario;
import com.obratech.entity.enums.EstadoEjecucion;
import com.obratech.repository.CalificacionRepository;
import com.obratech.repository.PerfilRepository;
import com.obratech.repository.ProyectoRepository;
import com.obratech.repository.ReporteProyectoRepository;
import com.obratech.repository.UsuarioRepository;
import com.obratech.service.PerfilVerificationService;
import com.obratech.util.PerfilDisplayName;

@RestController
@RequestMapping("/api/dashboard")
public class ClientDashboardApiController {

    private final UsuarioRepository usuarioRepository;
    private final PerfilRepository perfilRepository;
    private final ProyectoRepository proyectoRepository;
        private final ReporteProyectoRepository reporteRepository;
        private final CalificacionRepository calificacionRepository;
    private final PerfilVerificationService perfilVerificationService;

    public ClientDashboardApiController(
            UsuarioRepository usuarioRepository,
            PerfilRepository perfilRepository,
            ProyectoRepository proyectoRepository,
            ReporteProyectoRepository reporteRepository,
            CalificacionRepository calificacionRepository,
            PerfilVerificationService perfilVerificationService) {
        this.usuarioRepository = usuarioRepository;
        this.perfilRepository = perfilRepository;
        this.proyectoRepository = proyectoRepository;
        this.reporteRepository = reporteRepository;
                this.calificacionRepository = calificacionRepository;
        this.perfilVerificationService = perfilVerificationService;
    }

    @GetMapping("/cliente")
    public ResponseEntity<ClientDashboardResponse> getClientDashboard(Authentication authentication) {
        Usuario usuario = usuarioRepository.findByUsernameIgnoreCase(authentication.getName()).orElse(null);
        if (usuario == null) {
            return ResponseEntity.notFound().build();
        }

        Perfil perfil = perfilRepository.findByUsernameIgnoreCase(usuario.getUsername()).orElse(null);
        List<Proyecto> proyectos = proyectoRepository.findByClienteId(usuario.getId());
        long proyectosEnProgreso = proyectos.stream()
                .filter(proyecto -> proyecto.getEstadoEjecucion() != null
                        && proyecto.getEstadoEjecucion() != EstadoEjecucion.COMPLETADO)
                .count();
        long proyectosCompletados = proyectos.stream()
                .filter(proyecto -> proyecto.getEstadoEjecucion() == EstadoEjecucion.COMPLETADO)
                .count();
        Set<String> trabajadores = proyectos.stream()
                .filter(proyecto -> proyecto.getEquipoTrabajo() != null)
                .flatMap(proyecto -> proyecto.getEquipoTrabajo().stream())
                .map(Perfil::getId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());
        double presupuestoTotal = proyectos.stream()
                .map(Proyecto::getPresupuesto)
                .filter(presupuesto -> presupuesto != null)
                .mapToDouble(Double::doubleValue)
                .sum();
        int tasaExito = proyectos.isEmpty()
                ? 0
                : (int) Math.round(proyectosCompletados * 100.0 / proyectos.size());
        List<String> requisitosFaltantes = perfilVerificationService.getMissingRequirements(perfil);

        List<ProjectCard> proyectosRecientes = proyectos.stream()
                .sorted(Comparator.comparing(Proyecto::getFechaCreacion,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(6)
                .map(this::toProjectCard)
                .toList();

        return ResponseEntity.ok(new ClientDashboardResponse(
                usuario.getUsername(),
                usuario.isVerificado(),
                !requisitosFaltantes.isEmpty(),
                requisitosFaltantes,
                proyectos.size(),
                proyectosEnProgreso,
                proyectosCompletados,
                trabajadores.size(),
                presupuestoTotal,
                tasaExito,
                proyectosRecientes));
    }

    @GetMapping("/cliente/reportes")
    public ResponseEntity<ClientReportsResponse> getClientReports(Authentication authentication) {
        Usuario usuario = usuarioRepository.findByUsernameIgnoreCase(authentication.getName()).orElse(null);
        if (usuario == null) {
            return ResponseEntity.notFound().build();
        }
                if (!usuario.isVerificado()) {
                        return ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN).build();
                }

        List<Proyecto> proyectos = proyectoRepository.findByClienteId(usuario.getId());
        if (proyectos == null) {
            proyectos = List.of();
        }

        List<String> projectIds = proyectos.stream()
                .map(Proyecto::getId)
                .filter(id -> id != null)
                .toList();
        List<ClientReportCard> informes = projectIds.isEmpty()
                ? List.of()
                : reporteRepository.findByProyectoIdInOrderByCreadoDesc(projectIds).stream()
                        .map(this::toClientReportCard)
                        .toList();

        long completados = proyectos.stream()
                .filter(proyecto -> proyecto.getEstadoEjecucion() == EstadoEjecucion.COMPLETADO)
                .count();
        long enProgreso = proyectos.stream()
                .filter(proyecto -> proyecto.getEstadoEjecucion() == EstadoEjecucion.EN_PROGRESO)
                .count();
        long pendientes = proyectos.stream()
                .filter(proyecto -> proyecto.getEstadoEjecucion() == null
                        || proyecto.getEstadoEjecucion() == EstadoEjecucion.PENDIENTE)
                .count();
        long conContratista = proyectos.stream()
                .filter(proyecto -> proyecto.getContratistaAsignado() != null)
                .count();

        List<ReportProjectCard> detalle = proyectos.stream()
                .sorted(Comparator.comparing(Proyecto::getFechaCreacion,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .map(this::toReportProjectCard)
                .toList();

        return ResponseEntity.ok(new ClientReportsResponse(
                usuario.getUsername(),
                proyectos.size(),
                completados,
                enProgreso,
                pendientes,
                conContratista,
                calificacionRepository.countByClienteId(usuario.getId()),
                detalle,
                informes));
    }

        @GetMapping("/cliente/contratistas")
        public ResponseEntity<List<ClientContractorCard>> getClientContractors(Authentication authentication) {
                Usuario usuario = usuarioRepository.findByUsernameIgnoreCase(authentication.getName()).orElse(null);
                if (usuario == null) {
                        return ResponseEntity.notFound().build();
                }

                Map<String, List<Proyecto>> projectsByContractor = new LinkedHashMap<>();
                for (Proyecto proyecto : proyectoRepository.findByClienteId(usuario.getId())) {
                        Perfil contratista = proyecto.getContratistaAsignado();
                        if (contratista != null && contratista.getId() != null) {
                                projectsByContractor.computeIfAbsent(contratista.getId(), ignored -> new ArrayList<>()).add(proyecto);
                        }
                }

                List<ClientContractorCard> contractors = new ArrayList<>();
                for (List<Proyecto> assignedProjects : projectsByContractor.values()) {
                        Perfil contratista = assignedProjects.get(0).getContratistaAsignado();
                        contractors.add(new ClientContractorCard(
                                        contratista.getId(),
                                        PerfilDisplayName.of(contratista),
                                        contratista.getUsername(),
                                        contratista.getEmail(),
                                        contratista.getTelefono(),
                                        contratista.getEspecialidad(),
                                        contratista.getUbicacion(),
                                        contratista.getCalificacionPromedio(),
                                        assignedProjects.stream().map(this::toAssignedProjectCard).toList()));
                }
                return ResponseEntity.ok(contractors);
        }

    private ProjectCard toProjectCard(Proyecto proyecto) {
        return new ProjectCard(
                proyecto.getId(),
                proyecto.getTitulo(),
                proyecto.getDescripcion(),
                proyecto.getTipoProyecto(),
                proyecto.getUbicacion(),
                proyecto.getPresupuesto(),
                proyecto.getEstadoEjecucion() == null ? "PENDIENTE" : proyecto.getEstadoEjecucion().name(),
                proyecto.getEquipoTrabajo() == null ? 0 : proyecto.getEquipoTrabajo().size());
    }

    private ReportProjectCard toReportProjectCard(Proyecto proyecto) {
        Perfil contratista = proyecto.getContratistaAsignado();
        String nombreContratista = contratista == null
                ? "Sin asignar"
                : contratista.getNombre() == null || contratista.getNombre().isBlank()
                        ? contratista.getUsername()
                        : contratista.getNombre();
        return new ReportProjectCard(
                proyecto.getId(),
                proyecto.getTitulo(),
                proyecto.getTipoProyecto(),
                proyecto.getEstadoEjecucion() == null ? "PENDIENTE" : proyecto.getEstadoEjecucion().name(),
                nombreContratista,
                proyecto.getPresupuesto());
    }

        private ClientReportCard toClientReportCard(ReporteProyecto reporte) {
                List<ClientWorkerProgressCard> avances = reporte.getAvancesTrabajador().stream()
                        .map(advance -> new ClientWorkerProgressCard(
                                advance.getId(),
                                advance.getTrabajadorNombre(),
                                advance.getContenido(),
                                advance.getCreado(),
                                advance.getEvidencias().stream()
                                        .map(file -> toClientEvidence(reporte, file))
                                        .toList()))
                        .toList();
                return new ClientReportCard(
                                reporte.getId(),
                                reporte.getProyectoId(),
                                reporte.getProyectoTitulo(),
                                reporte.getContratistaUsername(),
                                reporte.getContenido(),
                        reporte.getCreado(),
                        avances,
                        reporte.getEvidencias().stream().map(file -> toClientEvidence(reporte, file)).toList());
            }

        private ClientEvidenceCard toClientEvidence(ReporteProyecto reporte, EvidenciaAdjunta file) {
                return new ClientEvidenceCard(file.getFileId(), file.getFilename(), file.getContentType(),
                        "/projects/" + reporte.getProyectoId() + "/reports/" + reporte.getId()
                                + "/evidence/" + file.getFileId());
        }

        private AssignedProjectCard toAssignedProjectCard(Proyecto proyecto) {
                return new AssignedProjectCard(
                                proyecto.getId(),
                                proyecto.getTitulo(),
                                proyecto.getEstadoEjecucion() == null ? "PENDIENTE" : proyecto.getEstadoEjecucion().name());
        }

    public record ClientDashboardResponse(
            String username,
            boolean usuarioVerificado,
            boolean perfilIncompleto,
            List<String> requisitosFaltantes,
            int totalProyectos,
            long proyectosEnProgreso,
            long proyectosCompletados,
            int miembrosEquipo,
            double presupuestoTotal,
            int tasaExito,
            List<ProjectCard> proyectosRecientes) {}

    public record ProjectCard(
            String id,
            String titulo,
            String descripcion,
            String tipoProyecto,
            String ubicacion,
            Double presupuesto,
            String estado,
            int miembrosEquipo) {}

    public record ClientReportsResponse(
            String username,
            int totalProyectos,
            long proyectosCompletados,
            long proyectosEnProgreso,
            long proyectosPendientes,
            long proyectosConContratista,
            long calificacionesDadas,
            List<ReportProjectCard> proyectos,
            List<ClientReportCard> informes) {}

    public record ClientReportCard(
            String id,
            String proyectoId,
            String proyecto,
            String contratista,
            String contenido,
            LocalDateTime creado,
            List<ClientWorkerProgressCard> avancesTrabajador,
            List<ClientEvidenceCard> evidencias) {}

    public record ClientWorkerProgressCard(
            String id,
            String trabajador,
            String contenido,
            LocalDateTime creado,
            List<ClientEvidenceCard> evidencias) {}

    public record ClientEvidenceCard(String fileId, String filename, String contentType, String url) {}

    public record ReportProjectCard(
            String id,
            String titulo,
            String tipoProyecto,
            String estado,
            String contratista,
            Double presupuesto) {}

    public record ClientContractorCard(
            String id,
            String nombre,
            String username,
            String email,
            String telefono,
            String especialidad,
            String ubicacion,
            Double calificacionPromedio,
            List<AssignedProjectCard> proyectos) {}

    public record AssignedProjectCard(String id, String titulo, String estado) {}
}