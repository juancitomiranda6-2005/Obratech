package com.obratech.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;

import com.obratech.entity.CuotaProyecto;
import com.obratech.entity.Proyecto;
import com.obratech.entity.enums.FaseObra;
import com.obratech.repository.CuotaProyectoRepository;

@Service
public class ProjectPhaseStatusService {

    private final ProyectoFaseService proyectoFaseService;
    private final ProjectRiskService projectRiskService;
    private final CuotaProyectoRepository cuotaProyectoRepository;

    public ProjectPhaseStatusService(
            ProyectoFaseService proyectoFaseService,
            ProjectRiskService projectRiskService,
            CuotaProyectoRepository cuotaProyectoRepository) {
        this.proyectoFaseService = proyectoFaseService;
        this.projectRiskService = projectRiskService;
        this.cuotaProyectoRepository = cuotaProyectoRepository;
    }

    public PhaseStatus crearEstado(Proyecto proyecto, boolean incluirIndicadores) {
        FaseObra fase = proyecto.getFaseActual() == null ? FaseObra.PLANEACION : proyecto.getFaseActual();
        int cuotasPagadas = proyectoFaseService.cuotasPagadasHastaFase(proyecto);
        int cuotasEsperadas = proyectoFaseService.cuotasRequeridasHastaFase(proyecto);
        int evidencias = proyectoFaseService.contarEvidenciasFaseActual(proyecto);
        int cuotasFase = proyectoFaseService.cuotasPagadasEnFase(proyecto, fase);
        int cuotasFaseEsperadas = proyectoFaseService.cuotasEsperadasEnFase(proyecto, fase);
        return new PhaseStatus(
                proyecto.getId(),
                proyecto.getTitulo(),
                proyecto.getDescripcion(),
                proyecto.getUbicacion(),
                proyecto.getFechaEntrega(),
                proyecto.getPresupuesto(),
                fase.name(),
                proyecto.getEstadoFaseActual() == null ? "BLOQUEADA" : proyecto.getEstadoFaseActual().name(),
                proyectoFaseService.puedeDesbloquearSiguienteFase(proyecto),
                proyectoFaseService.calcularPlanPagos(proyecto),
                cuotasPagadas,
                cuotasEsperadas,
                evidencias,
                proyectoFaseService.calcularAvanceObra(proyecto),
                proyectoFaseService.calcularAvanceFases(proyecto),
                Boolean.TRUE.equals(proyecto.getTareasFaseCompletadas()),
                Boolean.TRUE.equals(proyecto.getAprobacionClienteFase()),
                Boolean.TRUE.equals(proyecto.getLimpiezaProfundaCompletada()),
                cuotasFase,
                cuotasFaseEsperadas,
                cuotasFase < cuotasFaseEsperadas,
                cuotasDelProyecto(proyecto, fase),
                new Requirements(
                        Boolean.TRUE.equals(proyecto.getTareasFaseCompletadas()),
                        evidencias > 0,
                        Boolean.TRUE.equals(proyecto.getAprobacionClienteFase()),
                        cuotasPagadas >= cuotasEsperadas,
                        fase != FaseObra.DETALLE_Y_ENTREGA_FINAL
                                || Boolean.TRUE.equals(proyecto.getLimpiezaProfundaCompletada())),
                incluirIndicadores ? projectRiskService.calcularIndicadores(proyecto) : null);
    }

    private List<Installment> cuotasDelProyecto(Proyecto proyecto, FaseObra fase) {
        return cuotaProyectoRepository.findByProyectoId(proyecto.getId()).stream()
                .filter(cuota -> cuota.getFase() == fase)
                .sorted(java.util.Comparator.comparingInt(CuotaProyecto::getNumeroCuota))
                .map(cuota -> new Installment(
                        cuota.getNumeroCuota(),
                        cuota.getMonto(),
                        cuota.getRegistradoEn(),
                        cuota.getRegistradoPor()))
                .toList();
    }

    public record PhaseStatus(
            String proyectoId,
            String titulo,
            String descripcion,
            String ubicacion,
            LocalDate fechaEntrega,
            Double presupuesto,
            String faseActual,
            String estadoFaseActual,
            boolean puedeDesbloquear,
            ProyectoFaseService.PlanPagos planPagos,
            int cuotasSemanaPagadas,
            int cuotasTotalesEsperadas,
            int totalEvidencias,
            double avanceObra,
            List<ProyectoFaseService.AvanceFase> avanceFases,
            boolean tareasFaseCompletadas,
            boolean aprobacionClienteFase,
            boolean limpiezaProfundaCompletada,
            int cuotasPagadasEnFase,
            int cuotasEsperadasEnFase,
            boolean puedeRegistrarCuota,
            List<Installment> cuotasFase,
            Requirements requisitos,
            ProjectRiskService.ProjectIndicators indicadores) {}

    public record Requirements(
            boolean tareasCompletadas,
            boolean evidenciasDisponibles,
            boolean aprobacionCliente,
            boolean cuotasPagadas,
            boolean limpiezaProfunda) {}

    public record Installment(int numero, double monto, java.time.LocalDateTime registradoEn, String registradoPor) {}
}
