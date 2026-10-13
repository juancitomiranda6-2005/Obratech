package com.obratech.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.obratech.entity.Proyecto;
import com.obratech.entity.enums.EstadoFaseObra;
import com.obratech.entity.enums.FaseObra;

@Service
public class ProyectoFaseService {

    public record PlanPagos(
            double montoFase1,
            double cuotaSemanalFase1,
            double montoFase2,
            double cuotaSemanalFase2,
            double montoFase3) {
    }

    public PlanPagos calcularPlanPagos(Proyecto proyecto) {
        if (proyecto == null) {
            return new PlanPagos(0.0, 0.0, 0.0, 0.0, 0.0);
        }

        double presupuesto = proyecto.getPresupuesto() == null ? 0.0 : proyecto.getPresupuesto();
        int n1 = Math.max(1, proyecto.getSemanasEstimadasFase1() == null ? 1 : proyecto.getSemanasEstimadasFase1());
        int n2 = Math.max(1, proyecto.getSemanasEstimadasFase2() == null ? 1 : proyecto.getSemanasEstimadasFase2());

        double montoFase1 = redondear(presupuesto * 0.30);
        double montoFase2 = redondear(presupuesto * 0.50);
        double montoFase3 = redondear(presupuesto * 0.20);
        double cuotaFase1 = redondear(montoFase1 / n1);
        double cuotaFase2 = redondear(montoFase2 / n2);

        return new PlanPagos(montoFase1, cuotaFase1, montoFase2, cuotaFase2, montoFase3);
    }

    public boolean puedeDesbloquearSiguienteFase(Proyecto proyecto) {
        if (proyecto == null) {
            return false;
        }

        if (Boolean.FALSE.equals(proyecto.getTareasFaseCompletadas())) {
            return false;
        }

        List<String> evidencias = proyecto.getEvidenciasFotograficas() == null
                ? List.of()
                : proyecto.getEvidenciasFotograficas();

        if (evidencias.isEmpty()) {
            return false;
        }

        if (Boolean.FALSE.equals(proyecto.getAprobacionClienteFase())) {
            return false;
        }

        int esperadas = proyecto.getCuotasTotalesEsperadas() == null ? 0 : proyecto.getCuotasTotalesEsperadas();
        int pagadas = proyecto.getCuotasSemanaPagadas() == null ? 0 : proyecto.getCuotasSemanaPagadas();
        if (esperadas > 0 && pagadas < esperadas) {
            return false;
        }

        if (proyecto.getFaseActual() == FaseObra.DETALLE_Y_ENTREGA_FINAL
                && Boolean.FALSE.equals(proyecto.getLimpiezaProfundaCompletada())) {
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
                return true;
            }
            case DESARROLLO -> {
                proyecto.setFaseActual(FaseObra.DETALLE_Y_ENTREGA_FINAL);
                proyecto.setEstadoFaseActual(EstadoFaseObra.BLOQUEADA);
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
}
