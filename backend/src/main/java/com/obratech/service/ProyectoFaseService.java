package com.obratech.service;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.obratech.entity.EquipoTrabajo;
import com.obratech.entity.CuotaProyecto;
import com.obratech.entity.Proyecto;
import com.obratech.entity.enums.EstadoFaseObra;
import com.obratech.entity.enums.FaseObra;
import com.obratech.repository.EquipoTrabajoRepository;
import com.obratech.repository.CuotaProyectoRepository;
import com.obratech.repository.ReporteAvanceTrabajadorRepository;

@Service
public class ProyectoFaseService {

    public record PlanPagos(
            double montoFase1,
            double cuotaSemanalFase1,
            double montoFase2,
            double cuotaSemanalFase2,
            double montoFase3,
            int semanasFase1,
            int semanasFase2) {
    }

    public record AvanceFase(String fase, double avance, int equipos) {}

    private static final Map<FaseObra, Double> PESOS_FASES = Map.of(
            FaseObra.PLANEACION, 30.0,
            FaseObra.DESARROLLO, 50.0,
            FaseObra.DETALLE_Y_ENTREGA_FINAL, 20.0);

    private final EquipoTrabajoRepository equipoTrabajoRepository;
    private final CuotaProyectoRepository cuotaProyectoRepository;
    private final ReporteAvanceTrabajadorRepository reporteAvanceRepository;

    public ProyectoFaseService(
            EquipoTrabajoRepository equipoTrabajoRepository,
            CuotaProyectoRepository cuotaProyectoRepository,
            ReporteAvanceTrabajadorRepository reporteAvanceRepository) {
        this.equipoTrabajoRepository = equipoTrabajoRepository;
        this.cuotaProyectoRepository = cuotaProyectoRepository;
        this.reporteAvanceRepository = reporteAvanceRepository;
    }

    public PlanPagos calcularPlanPagos(Proyecto proyecto) {
        if (proyecto == null) {
            return new PlanPagos(0.0, 0.0, 0.0, 0.0, 0.0, 0, 0);
        }

        double presupuesto = proyecto.getPresupuesto() == null ? 0.0 : proyecto.getPresupuesto();
        int n1 = Math.max(1, proyecto.getSemanasEstimadasFase1() == null ? 1 : proyecto.getSemanasEstimadasFase1());
        int n2 = Math.max(1, proyecto.getSemanasEstimadasFase2() == null ? 1 : proyecto.getSemanasEstimadasFase2());

        double montoFase1 = redondear(presupuesto * 0.30);
        double montoFase2 = redondear(presupuesto * 0.50);
        double montoFase3 = redondear(presupuesto * 0.20);
        double cuotaFase1 = redondear(montoFase1 / n1);
        double cuotaFase2 = redondear(montoFase2 / n2);

        return new PlanPagos(montoFase1, cuotaFase1, montoFase2, cuotaFase2, montoFase3, n1, n2);
    }

    public double calcularAvanceFase(Proyecto proyecto, FaseObra fase) {
        return calcularAvanceFase(proyecto, fase, equiposDelProyecto(proyecto));
    }

    public double calcularAvanceFase(Proyecto proyecto, FaseObra fase, List<EquipoTrabajo> equipos) {
        if (proyecto == null || fase == null) {
            return 0.0;
        }
        return equiposDeFase(proyecto, fase, equipos).stream()
                .map(EquipoTrabajo::getPorcentajeAvance)
                .filter(java.util.Objects::nonNull)
                .mapToDouble(value -> Math.max(0.0, Math.min(100.0, value)))
                .average()
                .orElse(0.0);
    }

    public List<AvanceFase> calcularAvanceFases(Proyecto proyecto) {
        return calcularAvanceFases(proyecto, equiposDelProyecto(proyecto));
    }

    public List<AvanceFase> calcularAvanceFases(Proyecto proyecto, List<EquipoTrabajo> equipos) {
        List<AvanceFase> result = new ArrayList<>();
        for (FaseObra fase : FaseObra.values()) {
            List<EquipoTrabajo> equiposFase = equiposDeFase(proyecto, fase, equipos);
            double avance = equiposFase.stream()
                    .map(EquipoTrabajo::getPorcentajeAvance)
                    .filter(java.util.Objects::nonNull)
                    .mapToDouble(value -> Math.max(0.0, Math.min(100.0, value)))
                    .average()
                    .orElse(0.0);
            result.add(new AvanceFase(fase.name(), avance, equiposFase.size()));
        }
        return result;
    }

    public double calcularAvanceObra(Proyecto proyecto) {
        return calcularAvanceObra(proyecto, equiposDelProyecto(proyecto));
    }

    public double calcularAvanceObra(Proyecto proyecto, List<EquipoTrabajo> equipos) {
        if (proyecto == null) {
            return 0.0;
        }
        FaseObra faseActual = proyecto.getFaseActual() == null
                ? FaseObra.PLANEACION : proyecto.getFaseActual();
        if (faseActual == FaseObra.DETALLE_Y_ENTREGA_FINAL
                && proyecto.getEstadoFaseActual() == EstadoFaseObra.APROBADA) {
            return 100.0;
        }
        double avance = 0.0;
        for (FaseObra fase : FaseObra.values()) {
            if (fase.ordinal() < faseActual.ordinal()) {
                avance += PESOS_FASES.get(fase);
            } else if (fase == faseActual) {
                avance += PESOS_FASES.get(fase) * calcularAvanceFase(proyecto, fase, equipos) / 100.0;
                break;
            }
        }
        return Math.max(0.0, Math.min(100.0, avance));
    }

    public int cuotasRequeridasHastaFase(Proyecto proyecto) {
        PlanPagos plan = calcularPlanPagos(proyecto);
        FaseObra faseActual = proyecto == null || proyecto.getFaseActual() == null
                ? FaseObra.PLANEACION : proyecto.getFaseActual();
        return switch (faseActual) {
            case PLANEACION -> plan.semanasFase1();
            case DESARROLLO -> plan.semanasFase1() + plan.semanasFase2();
            case DETALLE_Y_ENTREGA_FINAL -> plan.semanasFase1() + plan.semanasFase2() + 1;
        };
    }

    public int cuotasPagadasHastaFase(Proyecto proyecto) {
        if (proyecto == null || proyecto.getId() == null) {
            return 0;
        }
        FaseObra faseActual = proyecto.getFaseActual() == null
                ? FaseObra.PLANEACION : proyecto.getFaseActual();
        return (int) cuotaProyectoRepository.findByProyectoId(proyecto.getId()).stream()
                .filter(cuota -> cuota.getFase() != null && cuota.getFase().ordinal() <= faseActual.ordinal())
                .count();
    }

    public int cuotasPagadasEnFase(Proyecto proyecto, FaseObra fase) {
        if (proyecto == null || proyecto.getId() == null || fase == null) {
            return 0;
        }
        return (int) cuotaProyectoRepository.findByProyectoId(proyecto.getId()).stream()
                .filter(cuota -> cuota.getFase() == fase)
                .count();
    }

    public int cuotasEsperadasEnFase(Proyecto proyecto, FaseObra fase) {
        PlanPagos plan = calcularPlanPagos(proyecto);
        return switch (fase) {
            case PLANEACION -> plan.semanasFase1();
            case DESARROLLO -> plan.semanasFase2();
            case DETALLE_Y_ENTREGA_FINAL -> 1;
        };
    }

    public CuotaProyecto registrarSiguienteCuota(Proyecto proyecto, String username) {
        FaseObra faseActual = proyecto.getFaseActual() == null
                ? FaseObra.PLANEACION : proyecto.getFaseActual();
        int pagadas = cuotasPagadasEnFase(proyecto, faseActual);
        if (pagadas >= cuotasEsperadasEnFase(proyecto, faseActual)) {
            return null;
        }
        PlanPagos plan = calcularPlanPagos(proyecto);
        CuotaProyecto cuota = new CuotaProyecto();
        cuota.setProyectoId(proyecto.getId());
        cuota.setFase(faseActual);
        cuota.setNumeroCuota(pagadas + 1);
        cuota.setMonto(switch (faseActual) {
            case PLANEACION -> plan.cuotaSemanalFase1();
            case DESARROLLO -> plan.cuotaSemanalFase2();
            case DETALLE_Y_ENTREGA_FINAL -> plan.montoFase3();
        });
        cuota.setRegistradoPor(username);
        CuotaProyecto saved = cuotaProyectoRepository.save(cuota);
        proyecto.setCuotasSemanaPagadas((int) cuotaProyectoRepository.findByProyectoId(proyecto.getId()).size());
        proyecto.setCuotasTotalesEsperadas(
                plan.semanasFase1() + plan.semanasFase2() + 1);
        return saved;
    }

    public int contarEvidenciasFaseActual(Proyecto proyecto) {
        if (proyecto == null || proyecto.getId() == null) {
            return 0;
        }
        return contarEvidenciasFaseActual(
                proyecto,
                equiposDelProyecto(proyecto),
                reporteAvanceRepository.findByProyectoIdOrderByCreadoDesc(proyecto.getId()));
    }

    public int contarEvidenciasFaseActual(
            Proyecto proyecto,
            List<EquipoTrabajo> equipos,
            List<com.obratech.entity.ReporteAvanceTrabajador> reportes) {
        if (proyecto == null || proyecto.getId() == null) {
            return 0;
        }
        FaseObra faseActual = proyecto.getFaseActual() == null
                ? FaseObra.PLANEACION : proyecto.getFaseActual();
        List<String> teamIds = equiposDeFase(proyecto, faseActual, equipos).stream()
                .map(EquipoTrabajo::getId)
                .filter(java.util.Objects::nonNull)
                .toList();
        long reportEvidence = reportes.stream()
                .filter(reporte -> teamIds.contains(reporte.getEquipoId()))
                .filter(reporte -> reporte.getEvidencias() != null && !reporte.getEvidencias().isEmpty())
                .count();
        return (int) reportEvidence;
    }

    public boolean puedeDesbloquearSiguienteFase(Proyecto proyecto) {
        if (proyecto == null) {
            return false;
        }
        if (proyecto.getFaseActual() == FaseObra.DETALLE_Y_ENTREGA_FINAL
                && proyecto.getEstadoFaseActual() == EstadoFaseObra.APROBADA) {
            return false;
        }

        if (!Boolean.TRUE.equals(proyecto.getTareasFaseCompletadas())) {
            return false;
        }

        if (contarEvidenciasFaseActual(proyecto) == 0) {
            return false;
        }

        if (!Boolean.TRUE.equals(proyecto.getAprobacionClienteFase())) {
            return false;
        }

        int esperadas = cuotasRequeridasHastaFase(proyecto);
        int pagadas = cuotasPagadasHastaFase(proyecto);
        if (pagadas < esperadas) {
            return false;
        }

        if (proyecto.getFaseActual() == FaseObra.DETALLE_Y_ENTREGA_FINAL
                && !Boolean.TRUE.equals(proyecto.getLimpiezaProfundaCompletada())) {
            return false;
        }

        return true;
    }

    public boolean avanzarFase(Proyecto proyecto) {
        if (proyecto == null || !puedeDesbloquearSiguienteFase(proyecto)) {
            return false;
        }

        if (proyecto.getFaseActual() == null) {
            proyecto.setFaseActual(FaseObra.PLANEACION);
        }

        switch (proyecto.getFaseActual()) {
            case PLANEACION -> {
                proyecto.setFaseActual(FaseObra.DESARROLLO);
                proyecto.setEstadoFaseActual(EstadoFaseObra.BLOQUEADA);
                limpiarRequisitosFase(proyecto);
                return true;
            }
            case DESARROLLO -> {
                proyecto.setFaseActual(FaseObra.DETALLE_Y_ENTREGA_FINAL);
                proyecto.setEstadoFaseActual(EstadoFaseObra.BLOQUEADA);
                limpiarRequisitosFase(proyecto);
                return true;
            }
            case DETALLE_Y_ENTREGA_FINAL -> {
                proyecto.setEstadoFaseActual(EstadoFaseObra.APROBADA);
                return true;
            }
            default -> {
                return false;
            }
        }
    }

    public double redondear(double valor) {
        return Math.round(valor * 100.0) / 100.0;
    }

    private List<EquipoTrabajo> equiposDeFase(Proyecto proyecto, FaseObra fase) {
        return equiposDeFase(proyecto, fase, equiposDelProyecto(proyecto));
    }

    private List<EquipoTrabajo> equiposDelProyecto(Proyecto proyecto) {
        return proyecto == null || proyecto.getId() == null
                ? List.of() : equipoTrabajoRepository.findByProyectoId(proyecto.getId());
    }

    private List<EquipoTrabajo> equiposDeFase(
            Proyecto proyecto,
            FaseObra fase,
            List<EquipoTrabajo> equipos) {
        if (proyecto == null || fase == null) return List.of();
        FaseObra faseActual = proyecto.getFaseActual() == null
                ? FaseObra.PLANEACION : proyecto.getFaseActual();
        return equipos.stream()
                .filter(equipo -> (equipo.getFase() == null ? faseActual : equipo.getFase()) == fase)
                .toList();
    }

    private void limpiarRequisitosFase(Proyecto proyecto) {
        proyecto.setTareasFaseCompletadas(false);
        proyecto.setAprobacionClienteFase(false);
        proyecto.setLimpiezaProfundaCompletada(false);
    }
}
