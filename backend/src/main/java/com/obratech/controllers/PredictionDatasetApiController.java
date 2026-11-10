package com.obratech.controllers;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.obratech.entity.Proyecto;
import com.obratech.entity.ProyectoObservacionPredictiva;
import com.obratech.entity.Usuario;
import com.obratech.repository.ProyectoObservacionPredictivaRepository;
import com.obratech.repository.ProyectoRepository;
import com.obratech.repository.UsuarioRepository;

@RestController
@RequestMapping("/api/projects")
public class PredictionDatasetApiController {

    private final ProyectoRepository proyectoRepository;
    private final UsuarioRepository usuarioRepository;
    private final ProyectoObservacionPredictivaRepository observationRepository;

    public PredictionDatasetApiController(
            ProyectoRepository proyectoRepository,
            UsuarioRepository usuarioRepository,
            ProyectoObservacionPredictivaRepository observationRepository) {
        this.proyectoRepository = proyectoRepository;
        this.usuarioRepository = usuarioRepository;
        this.observationRepository = observationRepository;
    }

    @PostMapping("/{projectId}/prediction-observations")
    public ResponseEntity<?> create(
            @PathVariable String projectId,
            @RequestBody ObservationRequest request,
            Authentication authentication) {
        Proyecto project = proyectoRepository.findById(projectId).orElse(null);
        Usuario user = findUser(authentication);
        if (!canAccess(project, user)) {
            return ResponseEntity.notFound().build();
        }
        String validationError = validate(request);
        if (validationError != null) {
            return ResponseEntity.badRequest().body(Map.of("error", validationError));
        }

        ProyectoObservacionPredictiva observation = new ProyectoObservacionPredictiva();
        observation.setProyectoId(projectId);
        observation.setObservadoEn(request.observadoEn() == null ? LocalDateTime.now() : request.observadoEn());
        observation.setPresupuesto(request.presupuesto());
        observation.setAvanceActual(request.avanceActual());
        observation.setDiasSinActividad(request.diasSinActividad());
        observation.setEquiposActivos(request.equiposActivos());
        observation.setEquiposTotales(request.equiposTotales());
        observation.setPagosPendientes(request.pagosPendientes());
        observation.setAprobacionesPendientes(request.aprobacionesPendientes());
        observation.setProbabilidadLluvia(request.probabilidadLluvia());
        observation.setFaseActual(request.faseActual());
        observation.setTerminoConRetraso(request.terminoConRetraso());
        observation.setDiasRetraso(request.diasRetraso());
        return ResponseEntity.status(HttpStatus.CREATED).body(observationRepository.save(observation));
    }

    @GetMapping("/{projectId}/prediction-observations")
    public ResponseEntity<?> list(
            @PathVariable String projectId,
            Authentication authentication) {
        Proyecto project = proyectoRepository.findById(projectId).orElse(null);
        if (!canAccess(project, findUser(authentication))) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(observationRepository.findByProyectoIdOrderByObservadoEnAsc(projectId));
    }

    private Usuario findUser(Authentication authentication) {
        return authentication == null ? null
                : usuarioRepository.findByUsernameIgnoreCase(authentication.getName()).orElse(null);
    }

    private boolean canAccess(Proyecto project, Usuario user) {
        if (project == null || user == null) {
            return false;
        }
        boolean admin = user.getRoles() != null && user.getRoles().contains("ROLE_ADMIN");
        boolean owner = project.getCliente() != null
                && user.getId() != null
                && user.getId().equals(project.getCliente().getId());
        boolean contractor = project.getContratistaAsignado() != null
                && user.getId() != null
                && user.getId().equals(project.getContratistaAsignado().getId());
        return admin || owner || contractor;
    }

    private String validate(ObservationRequest request) {
        if (request == null || request.presupuesto() == null || request.presupuesto() < 0
                || request.avanceActual() == null || request.avanceActual() < 0
                || request.avanceActual() > 100) {
            return "El presupuesto y el avance deben ser valores válidos.";
        }
        if (request.diasSinActividad() == null || request.diasSinActividad() < 0
                || request.equiposActivos() == null || request.equiposActivos() < 0
                || request.equiposTotales() == null || request.equiposTotales() < 0
                || request.equiposActivos() > request.equiposTotales()
                || request.pagosPendientes() == null || request.pagosPendientes() < 0
                || request.aprobacionesPendientes() == null || request.aprobacionesPendientes() < 0) {
            return "Los contadores de actividad, equipos, pagos y aprobaciones deben ser válidos.";
        }
        if (request.probabilidadLluvia() == null || request.probabilidadLluvia() < 0
                || request.probabilidadLluvia() > 100 || request.faseActual() == null
                || request.faseActual().isBlank()) {
            return "La probabilidad de lluvia y la fase actual son obligatorias.";
        }
        if (request.terminoConRetraso() != null
                && (request.diasRetraso() == null || request.diasRetraso() < 0)) {
            return "Los días de retraso son obligatorios cuando el proyecto ya terminó.";
        }
        return null;
    }

    public record ObservationRequest(
            LocalDateTime observadoEn,
            Double presupuesto,
            Double avanceActual,
            Integer diasSinActividad,
            Integer equiposActivos,
            Integer equiposTotales,
            Integer pagosPendientes,
            Integer aprobacionesPendientes,
            Double probabilidadLluvia,
            String faseActual,
            Boolean terminoConRetraso,
            Integer diasRetraso) {
    }
}
