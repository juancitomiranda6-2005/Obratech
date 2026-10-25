package com.obratech.service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.obratech.entity.EquipoTrabajo;
import com.obratech.entity.Proyecto;
import com.obratech.entity.ReporteAvanceTrabajador;
import com.obratech.entity.enums.FaseObra;
import com.obratech.repository.EquipoTrabajoRepository;
import com.obratech.repository.ReporteAvanceTrabajadorRepository;

@Service
public class ProjectRiskService {

    private final ProyectoFaseService proyectoFaseService;
    private final EquipoTrabajoRepository equipoTrabajoRepository;
    private final ReporteAvanceTrabajadorRepository reporteAvanceRepository;
    private final PronosticoClimaService pronosticoClimaService;

    public ProjectRiskService(
            ProyectoFaseService proyectoFaseService,
            EquipoTrabajoRepository equipoTrabajoRepository,
            ReporteAvanceTrabajadorRepository reporteAvanceRepository,
            PronosticoClimaService pronosticoClimaService) {
        this.proyectoFaseService = proyectoFaseService;
        this.equipoTrabajoRepository = equipoTrabajoRepository;
        this.reporteAvanceRepository = reporteAvanceRepository;
        this.pronosticoClimaService = pronosticoClimaService;
    }

    private double exposicionClimatica(FaseObra fase) {
        return switch (fase) {
            case PLANEACION -> 0.5;
            case DESARROLLO -> 1.0;
            case DETALLE_Y_ENTREGA_FINAL -> 0.25;
        };
    }

    public ProjectIndicators calcularIndicadores(Proyecto proyecto) {
        List<EquipoTrabajo> equipos = proyecto == null || proyecto.getId() == null
                ? List.of() : equipoTrabajoRepository.findByProyectoId(proyecto.getId());
        List<ReporteAvanceTrabajador> reportes = proyecto == null || proyecto.getId() == null
                ? List.of() : reporteAvanceRepository.findByProyectoIdOrderByCreadoDesc(proyecto.getId());
        PronosticoClimaService.Pronostico pronostico = pronosticoClimaService.obtenerPronostico(proyecto);
        double avanceObra = proyectoFaseService.calcularAvanceObra(proyecto, equipos);

        ScheduleResult schedule = evaluarCronograma(proyecto, equipos, avanceObra);
        RiskFactor teamFactor = evaluarEquipos(proyecto, equipos, reportes);
        RiskFactor climateFactor = evaluarClima(proyecto, pronostico);
        RiskFactor paymentFactor = evaluarPagosYAprobaciones(proyecto, equipos, reportes);
        List<RiskFactor> factors = List.of(schedule.factor(), teamFactor, climateFactor, paymentFactor);
        int availableWeight = factors.stream()
                .filter(RiskFactor::disponible)
                .mapToInt(RiskFactor::peso)
                .sum();
        Double score = availableWeight == 0 ? null
                : factors.stream().filter(RiskFactor::disponible)
                        .mapToDouble(RiskFactor::aporte).sum() * 100.0 / availableWeight;
        String nivel = score == null ? "Sin datos"
                : score < ProjectRiskPolicy.LOW_RISK_MAX
                        ? "Bajo" : score <= ProjectRiskPolicy.MEDIUM_RISK_MAX ? "Medio" : "Alto";
        RiskAssessment risk = new RiskAssessment(
                score == null ? null : Math.round(score * 10.0) / 10.0,
                nivel,
                factors,
                schedule.deliveryDate(),
                schedule.delayDays(),
                factors.stream().filter(RiskFactor::disponible).count(),
                factors.size());

        List<TeamProgress> teamProgress = equipos.stream()
                .map(team -> new TeamProgress(
                        team.getId(),
                        team.getNombre(),
                        team.getActividad(),
                        faseEquipo(proyecto, team).name(),
                        team.getPorcentajeAvance() == null ? 0.0 : team.getPorcentajeAvance()))
                .sorted(java.util.Comparator.comparing(TeamProgress::fase))
                .toList();
        FaseObra faseActual = proyecto == null || proyecto.getFaseActual() == null
                ? FaseObra.PLANEACION : proyecto.getFaseActual();
        return new ProjectIndicators(
                avanceObra,
                faseActual.name(),
                exposicionClimatica(faseActual),
                proyectoFaseService.calcularAvanceFases(proyecto, equipos),
                teamProgress,
                pronostico,
                risk);
    }

    private ScheduleResult evaluarCronograma(Proyecto proyecto, List<EquipoTrabajo> equipos, double actual) {
        if (proyecto == null || proyecto.getFechaInicio() == null || proyecto.getFechaEntrega() == null
                || equipos.isEmpty()) {
            return new ScheduleResult(
                    factorSinDatos("Cronograma", ProjectRiskPolicy.CRONOGRAMA_WEIGHT),
                    null, null);
        }
        LocalDate today = LocalDate.now();
        long totalDays = Math.max(1, ChronoUnit.DAYS.between(proyecto.getFechaInicio(), proyecto.getFechaEntrega()));
        long elapsedDays = Math.max(0, Math.min(totalDays, ChronoUnit.DAYS.between(proyecto.getFechaInicio(), today)));
        int weeks1 = Math.max(1, proyecto.getSemanasEstimadasFase1() == null ? 1 : proyecto.getSemanasEstimadasFase1());
        int weeks2 = Math.max(1, proyecto.getSemanasEstimadasFase2() == null ? 1 : proyecto.getSemanasEstimadasFase2());
        long days1 = weeks1 * 7L;
        long days2 = weeks2 * 7L;
        long days3 = Math.max(1, totalDays - days1 - days2);
        double expected;
        if (elapsedDays <= days1) {
            expected = 30.0 * elapsedDays / days1;
        } else if (elapsedDays <= days1 + days2) {
            expected = 30.0 + 50.0 * (elapsedDays - days1) / days2;
        } else {
            expected = 80.0 + 20.0 * Math.min(1.0, (elapsedDays - days1 - days2) / (double) days3);
        }
        double lateness = expected <= 0.0 ? 0.0 : clamp((expected - actual) * 100.0 / expected);
        RiskFactor factor = factor(
                "Cronograma",
                ProjectRiskPolicy.CRONOGRAMA_WEIGHT,
                lateness,
                true,
                String.format("Avance esperado %.1f%% frente a %.1f%% real.", expected, actual));
        LocalDate deliveryDate = null;
        Integer delayDays = null;
        if (actual > 0.0) {
            double daysPerPercent = Math.max(1, ChronoUnit.DAYS.between(proyecto.getFechaInicio(), today))
                    / actual;
            long remainingDays = Math.max(0, Math.round((100.0 - actual) * daysPerPercent));
            deliveryDate = today.plusDays(remainingDays);
            delayDays = (int) ChronoUnit.DAYS.between(proyecto.getFechaEntrega(), deliveryDate);
        }
        return new ScheduleResult(factor, deliveryDate, delayDays);
    }

    private RiskFactor evaluarEquipos(
            Proyecto proyecto,
            List<EquipoTrabajo> equipos,
            List<ReporteAvanceTrabajador> reportes) {
        if (equipos.isEmpty()) {
            int workersWithoutTeam = proyecto == null || proyecto.getEquipoTrabajo() == null
                    ? 0 : proyecto.getEquipoTrabajo().size();
            return workersWithoutTeam > 0
                    ? factor("Equipos", ProjectRiskPolicy.EQUIPOS_WEIGHT, 100.0, true,
                            "No hay cuadrillas registradas; " + workersWithoutTeam + " trabajadores no tienen equipo.")
                    : factorSinDatos("Equipos", ProjectRiskPolicy.EQUIPOS_WEIGHT);
        }
        LocalDate staleBefore = LocalDate.now().minusDays(ProjectRiskPolicy.STALE_DAYS);
        int knownActivity = 0;
        int staleTeams = 0;
        for (EquipoTrabajo team : equipos) {
            LocalDate lastUpdate = team.getAvanceActualizadoEn() == null
                    ? null : team.getAvanceActualizadoEn().toLocalDate();
            LocalDate lastReport = reportes.stream()
                    .filter(report -> team.getId() != null && team.getId().equals(report.getEquipoId()))
                    .map(ReporteAvanceTrabajador::getCreado)
                    .filter(java.util.Objects::nonNull)
                    .map(java.time.LocalDateTime::toLocalDate)
                    .max(LocalDate::compareTo)
                    .orElse(null);
            LocalDate lastActivity = lastUpdate == null ? lastReport
                    : lastReport == null || lastUpdate.isAfter(lastReport) ? lastUpdate : lastReport;
            if (lastActivity != null) {
                knownActivity++;
                if (lastActivity.isBefore(staleBefore)) {
                    staleTeams++;
                }
            }
        }
        double staleScore = knownActivity == 0 ? Double.NaN : 100.0 * staleTeams / knownActivity;
        long unassignedActivities = equipos.stream()
                .filter(team -> team.getActividad() == null || team.getActividad().isBlank())
                .count();
        double activityScore = 100.0 * unassignedActivities / equipos.size();
        int workers = proyecto.getEquipoTrabajo() == null ? 0 : proyecto.getEquipoTrabajo().size();
        double workersWithoutTeamScore = Double.NaN;
        if (workers > 0) {
            long workersWithTeams = equipos.stream()
                    .filter(team -> team.getIntegrantes() != null)
                    .flatMap(team -> team.getIntegrantes().stream())
                    .map(profile -> profile.getId())
                    .filter(java.util.Objects::nonNull)
                    .distinct()
                    .count();
            workersWithoutTeamScore = 100.0 * Math.max(0, workers - workersWithTeams) / workers;
        }
        List<Double> knownScores = new ArrayList<>();
        if (Double.isFinite(staleScore)) knownScores.add(staleScore);
        knownScores.add(activityScore);
        if (Double.isFinite(workersWithoutTeamScore)) knownScores.add(workersWithoutTeamScore);
        double score = knownScores.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        long workersWithoutTeam = workers == 0 ? 0
                : Math.max(0, workers - Math.round(workersWithoutTeamScore * workers / 100.0));
        String details = "Equipos sin actividad conocida en 7 días: "
                + (knownActivity == 0 ? "sin datos" : staleTeams + "/" + knownActivity)
                + "; actividades sin asignar: " + unassignedActivities + "/" + equipos.size()
                + "; trabajadores sin equipo: "
                + (workers == 0 ? "sin datos" : workersWithoutTeam + "/" + workers);
        return factor("Equipos", ProjectRiskPolicy.EQUIPOS_WEIGHT, score, true, details);
    }

    private RiskFactor evaluarClima(Proyecto proyecto, PronosticoClimaService.Pronostico pronostico) {
        if (pronostico == null || !pronostico.disponible() || pronostico.dias().isEmpty()) {
            return factorSinDatos("Clima", ProjectRiskPolicy.CLIMA_WEIGHT);
        }
        long rainyDays = pronostico.dias().stream()
                .filter(day -> day.probabilidadMaxima() >= ProjectRiskPolicy.HIGH_RAIN_PROBABILITY)
                .count();
        double exposure = exposicionClimatica(proyecto.getFaseActual() == null
                ? FaseObra.PLANEACION : proyecto.getFaseActual());
        double score = 100.0 * rainyDays / pronostico.dias().size() * exposure;
        return factor("Clima", ProjectRiskPolicy.CLIMA_WEIGHT, score, true,
                rainyDays + " de " + pronostico.dias().size() + " días con probabilidad de lluvia ≥ "
                        + ProjectRiskPolicy.HIGH_RAIN_PROBABILITY + "%; exposición de fase " + exposure + ".");
    }

    private RiskFactor evaluarPagosYAprobaciones(
            Proyecto proyecto,
            List<EquipoTrabajo> equipos,
            List<ReporteAvanceTrabajador> reportes) {
        if (proyecto == null) {
            return factorSinDatos("Pagos y aprobaciones", ProjectRiskPolicy.PAGOS_APROBACIONES_WEIGHT);
        }
        int expectedToDate = cuotasEsperadasVencidas(proyecto);
        int paidToDate = proyectoFaseService.cuotasPagadasEnFase(
                proyecto, proyecto.getFaseActual() == null ? FaseObra.PLANEACION : proyecto.getFaseActual());
        double paymentScore = expectedToDate == 0
                ? 0.0 : 100.0 * Math.max(0, expectedToDate - paidToDate) / expectedToDate;
        double approvalScore = Boolean.TRUE.equals(proyecto.getAprobacionClienteFase()) ? 0.0 : 100.0;
        double evidenceScore = proyectoFaseService.contarEvidenciasFaseActual(proyecto, equipos, reportes) > 0
                ? 0.0 : 100.0;
        double score = paymentScore * 0.5 + approvalScore * 0.3 + evidenceScore * 0.2;
        return factor("Pagos y aprobaciones", ProjectRiskPolicy.PAGOS_APROBACIONES_WEIGHT, score, true,
                "Cuotas vencidas sin registrar: " + Math.max(0, expectedToDate - paidToDate)
                        + "; aprobación " + (approvalScore == 0 ? "registrada" : "pendiente")
                        + "; evidencias " + (evidenceScore == 0 ? "disponibles" : "pendientes") + ".");
    }

    private int cuotasEsperadasVencidas(Proyecto proyecto) {
        if (proyecto.getFechaInicio() == null) {
            return 0;
        }
        long days = Math.max(0, ChronoUnit.DAYS.between(proyecto.getFechaInicio(), LocalDate.now()));
        FaseObra fase = proyecto.getFaseActual() == null ? FaseObra.PLANEACION : proyecto.getFaseActual();
        int weeks;
        long startOffset;
        switch (fase) {
            case PLANEACION -> {
                weeks = Math.max(1, proyecto.getSemanasEstimadasFase1() == null ? 1 : proyecto.getSemanasEstimadasFase1());
                startOffset = 0;
            }
            case DESARROLLO -> {
                weeks = Math.max(1, proyecto.getSemanasEstimadasFase2() == null ? 1 : proyecto.getSemanasEstimadasFase2());
                startOffset = Math.max(1, proyecto.getSemanasEstimadasFase1() == null ? 1 : proyecto.getSemanasEstimadasFase1()) * 7L;
            }
            case DETALLE_Y_ENTREGA_FINAL -> {
                weeks = 1;
                startOffset = (Math.max(1, proyecto.getSemanasEstimadasFase1() == null ? 1 : proyecto.getSemanasEstimadasFase1())
                        + Math.max(1, proyecto.getSemanasEstimadasFase2() == null ? 1 : proyecto.getSemanasEstimadasFase2())) * 7L;
            }
            default -> throw new IllegalStateException("Fase no soportada: " + fase);
        }
        if (days < startOffset) {
            return 0;
        }
        return Math.min(weeks, (int) ((days - startOffset) / 7L) + 1);
    }

    private FaseObra faseEquipo(Proyecto proyecto, EquipoTrabajo team) {
        return team.getFase() == null
                ? proyecto.getFaseActual() == null ? FaseObra.PLANEACION : proyecto.getFaseActual()
                : team.getFase();
    }

    private RiskFactor factorSinDatos(String name, int weight) {
        return new RiskFactor(name, weight, null, 0.0, false, "Sin datos suficientes.");
    }

    private RiskFactor factor(String name, int weight, double subscore, boolean available, String detail) {
        double normalized = clamp(subscore);
        return new RiskFactor(
                name,
                weight,
                normalized,
                weight * normalized / 100.0,
                available,
                detail);
    }

    private double clamp(double value) {
        return Math.max(0.0, Math.min(100.0, value));
    }

    private record ScheduleResult(RiskFactor factor, LocalDate deliveryDate, Integer delayDays) {}

    public record RiskFactor(
            String nombre,
            int peso,
            Double score,
            double aporte,
            boolean disponible,
            String detalle) {}

    public record RiskAssessment(
            Double score,
            String nivel,
            List<RiskFactor> factores,
            LocalDate entregaProyectada,
            Integer diasRetrasoProyectado,
            long factoresConDatos,
            long totalFactores) {}

    public record TeamProgress(
            String id,
            String nombre,
            String actividad,
            String fase,
            double porcentajeAvance) {}

    public record ProjectIndicators(
            double avanceObra,
            String faseActual,
            double exposicionClimatica,
            List<ProyectoFaseService.AvanceFase> avanceFase,
            List<TeamProgress> avanceEquipos,
            PronosticoClimaService.Pronostico pronosticoLluvia,
            RiskAssessment riesgo) {}
}
