package com.obratech.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
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

@RestController
@RequestMapping("/api/projects")
public class ProjectPhaseApiController {

    private final ProyectoRepository proyectoRepository;
    private final UsuarioRepository usuarioRepository;
    private final ProyectoFaseService proyectoFaseService;

    public ProjectPhaseApiController(ProyectoRepository proyectoRepository,
            UsuarioRepository usuarioRepository,
            ProyectoFaseService proyectoFaseService) {
        this.proyectoRepository = proyectoRepository;
        this.usuarioRepository = usuarioRepository;
        this.proyectoFaseService = proyectoFaseService;
    }

    @GetMapping("/{projectId}/phase-status")
    public ResponseEntity<?> getPhaseStatus(@PathVariable String projectId, Authentication authentication) {
        Proyecto proyecto = proyectoRepository.findById(projectId).orElse(null);
        Usuario usuario = usuarioRepository.findByUsernameIgnoreCase(authentication.getName()).orElse(null);
        if (proyecto == null || usuario == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(new PhaseStatus(
                proyecto.getFaseActual() == null ? "PLANEACION" : proyecto.getFaseActual().name(),
                proyecto.getEstadoFaseActual() == null ? "BLOQUEADA" : proyecto.getEstadoFaseActual().name(),
                proyectoFaseService.puedeDesbloquearSiguienteFase(proyecto),
                proyectoFaseService.calcularPlanPagos(proyecto),
                proyecto.getCuotasSemanaPagadas(),
                proyecto.getCuotasTotalesEsperadas(),
                proyecto.getEvidenciasFotograficas() == null ? 0 : proyecto.getEvidenciasFotograficas().size()));
    }

    @PostMapping("/{projectId}/phase/advance")
    public ResponseEntity<?> advancePhase(@PathVariable String projectId, Authentication authentication) {
        Proyecto proyecto = proyectoRepository.findById(projectId).orElse(null);
        Usuario usuario = usuarioRepository.findByUsernameIgnoreCase(authentication.getName()).orElse(null);
        if (proyecto == null || usuario == null) {
            return ResponseEntity.notFound().build();
        }

        if (!proyectoFaseService.avanzarFase(proyecto)) {
            return ResponseEntity.badRequest().body(new ApiError("No se puede desbloquear la siguiente fase porque faltan requisitos de validación, evidencias o pagos."));
        }

        proyectoRepository.save(proyecto);
        return ResponseEntity.ok(new PhaseStatus(
                proyecto.getFaseActual() == null ? "PLANEACION" : proyecto.getFaseActual().name(),
                proyecto.getEstadoFaseActual() == null ? "BLOQUEADA" : proyecto.getEstadoFaseActual().name(),
                proyectoFaseService.puedeDesbloquearSiguienteFase(proyecto),
                proyectoFaseService.calcularPlanPagos(proyecto),
                proyecto.getCuotasSemanaPagadas(),
                proyecto.getCuotasTotalesEsperadas(),
                proyecto.getEvidenciasFotograficas() == null ? 0 : proyecto.getEvidenciasFotograficas().size()));
    }

    public record PhaseStatus(
            String faseActual,
            String estadoFaseActual,
            boolean puedeDesbloquear,
            ProyectoFaseService.PlanPagos planPagos,
            Integer cuotasSemanaPagadas,
            Integer cuotasTotalesEsperadas,
            int totalEvidencias)
    {
    }

    public record ApiError(String error) {
    }
}
