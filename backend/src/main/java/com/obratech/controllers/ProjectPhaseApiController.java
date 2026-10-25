package com.obratech.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.obratech.entity.Proyecto;
import com.obratech.entity.Usuario;
import com.obratech.repository.ProyectoRepository;
import com.obratech.repository.UsuarioRepository;
import com.obratech.service.ProyectoFaseService;
import com.obratech.service.ProyectoAccessService;
import com.obratech.service.ProjectPhaseStatusService;
import com.obratech.entity.enums.EstadoEjecucion;
import com.obratech.entity.enums.EstadoFaseObra;
import com.obratech.entity.enums.FaseObra;

@RestController
@RequestMapping("/api/projects")
public class ProjectPhaseApiController {

    private final ProyectoRepository proyectoRepository;
    private final UsuarioRepository usuarioRepository;
    private final ProyectoFaseService proyectoFaseService;
    private final ProyectoAccessService proyectoAccessService;
    private final ProjectPhaseStatusService phaseStatusService;

    public ProjectPhaseApiController(ProyectoRepository proyectoRepository,
            UsuarioRepository usuarioRepository,
            ProyectoFaseService proyectoFaseService,
            ProyectoAccessService proyectoAccessService,
            ProjectPhaseStatusService phaseStatusService) {
        this.proyectoRepository = proyectoRepository;
        this.usuarioRepository = usuarioRepository;
        this.proyectoFaseService = proyectoFaseService;
        this.proyectoAccessService = proyectoAccessService;
        this.phaseStatusService = phaseStatusService;
    }

    @GetMapping("/{projectId}/phase-status")
    public ResponseEntity<?> getPhaseStatus(@PathVariable String projectId, Authentication authentication) {
        Proyecto proyecto = proyectoRepository.findById(projectId).orElse(null);
        Usuario usuario = usuarioRepository.findByUsernameIgnoreCase(authentication.getName()).orElse(null);
        if (proyecto == null || usuario == null) {
            return ResponseEntity.notFound().build();
        }
        if (!proyectoAccessService.puedeVerFases(proyecto, usuario)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ApiError("No tienes acceso a las fases de este proyecto."));
        }

        return ResponseEntity.ok(phaseStatusService.crearEstado(proyecto, false));
    }

    @PostMapping("/{projectId}/phase/advance")
    public ResponseEntity<?> advancePhase(@PathVariable String projectId, Authentication authentication) {
        Proyecto proyecto = proyectoRepository.findById(projectId).orElse(null);
        Usuario usuario = usuarioRepository.findByUsernameIgnoreCase(authentication.getName()).orElse(null);
        if (proyecto == null || usuario == null) {
            return ResponseEntity.notFound().build();
        }
        if (!proyectoAccessService.puedeVerFases(proyecto, usuario)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ApiError("No tienes acceso a las fases de este proyecto."));
        }
        if (!proyectoAccessService.puedeAvanzarFase(proyecto, usuario)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ApiError("Solo el cliente propietario o un administrador puede avanzar la fase."));
        }

        if (!proyectoFaseService.avanzarFase(proyecto)) {
            return ResponseEntity.badRequest().body(new ApiError("No se puede desbloquear la siguiente fase porque faltan requisitos de validación, evidencias o pagos."));
        }

        if (proyecto.getFaseActual() == FaseObra.DETALLE_Y_ENTREGA_FINAL
                && proyecto.getEstadoFaseActual() == EstadoFaseObra.APROBADA) {
            proyecto.setEstadoEjecucion(EstadoEjecucion.COMPLETADO);
        }
        proyectoRepository.save(proyecto);
        return ResponseEntity.ok(phaseStatusService.crearEstado(proyecto, false));
    }

    @PutMapping("/{projectId}/phase/tasks")
    public ResponseEntity<?> updatePhaseTasks(
            @PathVariable String projectId,
            @RequestBody(required = false) PhaseTasksRequest request,
            Authentication authentication) {
        Proyecto proyecto = proyectoRepository.findById(projectId).orElse(null);
        Usuario usuario = usuarioRepository.findByUsernameIgnoreCase(authentication.getName()).orElse(null);
        if (proyecto == null || usuario == null) return ResponseEntity.notFound().build();
        if (!proyectoAccessService.puedeGestionarTareasFase(proyecto, usuario)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ApiError("Solo el contratista asignado o un administrador puede actualizar estas tareas."));
        }
        if (request == null || request.completadas() == null) {
            return ResponseEntity.badRequest().body(new ApiError("Indica si las tareas de la fase están completas."));
        }
        proyecto.setTareasFaseCompletadas(request.completadas());
        if (proyecto.getFaseActual() == FaseObra.DETALLE_Y_ENTREGA_FINAL) {
            proyecto.setLimpiezaProfundaCompletada(Boolean.TRUE.equals(request.limpiezaProfundaCompletada()));
        }
        proyectoRepository.save(proyecto);
        return ResponseEntity.ok(phaseStatusService.crearEstado(proyecto, false));
    }

    @PutMapping("/{projectId}/phase/approval")
    public ResponseEntity<?> updatePhaseApproval(
            @PathVariable String projectId,
            @RequestBody(required = false) PhaseApprovalRequest request,
            Authentication authentication) {
        Proyecto proyecto = proyectoRepository.findById(projectId).orElse(null);
        Usuario usuario = usuarioRepository.findByUsernameIgnoreCase(authentication.getName()).orElse(null);
        if (proyecto == null || usuario == null) return ResponseEntity.notFound().build();
        if (!proyectoAccessService.puedeAvanzarFase(proyecto, usuario)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ApiError("Solo el cliente propietario o un administrador puede registrar la aprobación."));
        }
        if (request == null || request.aprobada() == null) {
            return ResponseEntity.badRequest().body(new ApiError("Indica el estado de aprobación de la fase."));
        }
        proyecto.setAprobacionClienteFase(request.aprobada());
        proyectoRepository.save(proyecto);
        return ResponseEntity.ok(phaseStatusService.crearEstado(proyecto, false));
    }

    @PostMapping("/{projectId}/phase/quotas")
    public ResponseEntity<?> registerPhaseInstallment(
            @PathVariable String projectId,
            Authentication authentication) {
        Proyecto proyecto = proyectoRepository.findById(projectId).orElse(null);
        Usuario usuario = usuarioRepository.findByUsernameIgnoreCase(authentication.getName()).orElse(null);
        if (proyecto == null || usuario == null) return ResponseEntity.notFound().build();
        if (!proyectoAccessService.puedeAvanzarFase(proyecto, usuario)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ApiError("Solo el cliente propietario o un administrador puede registrar una cuota."));
        }
        try {
            var cuota = proyectoFaseService.registrarSiguienteCuota(proyecto, usuario.getUsername());
            if (cuota == null) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(new ApiError("Ya se registraron todas las cuotas de esta fase."));
            }
            proyectoRepository.save(proyecto);
            return ResponseEntity.ok(phaseStatusService.crearEstado(proyecto, false));
        } catch (DuplicateKeyException exception) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(new ApiError("La cuota ya fue registrada."));
        }
    }

    public record PhaseTasksRequest(Boolean completadas, Boolean limpiezaProfundaCompletada) {}

    public record PhaseApprovalRequest(Boolean aprobada) {}

    public record ApiError(String error) {
    }
}
