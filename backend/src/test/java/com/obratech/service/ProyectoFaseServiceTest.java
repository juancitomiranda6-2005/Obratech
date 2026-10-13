package com.obratech.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.obratech.entity.Proyecto;
import com.obratech.entity.enums.EstadoFaseObra;
import com.obratech.entity.enums.FaseObra;

class ProyectoFaseServiceTest {

    private final ProyectoFaseService service = new ProyectoFaseService();

    @Test
    void calculaPlanDePagosSegunDistribucionDePresupuesto() {
        Proyecto proyecto = new Proyecto();
        proyecto.setPresupuesto(50_000_000.0);
        proyecto.setSemanasEstimadasFase1(3);
        proyecto.setSemanasEstimadasFase2(10);

        var plan = service.calcularPlanPagos(proyecto);

        assertEquals(15_000_000.0, plan.montoFase1());
        assertEquals(5_000_000.0, plan.cuotaSemanalFase1());
        assertEquals(25_000_000.0, plan.montoFase2());
        assertEquals(2_500_000.0, plan.cuotaSemanalFase2());
        assertEquals(10_000_000.0, plan.montoFase3());
    }

    @Test
    void bloqueaElDesbloqueoDeFaseSiFaltanEvidenciasOPagos() {
        Proyecto proyecto = new Proyecto();
        proyecto.setPresupuesto(50_000_000.0);
        proyecto.setSemanasEstimadasFase1(3);
        proyecto.setSemanasEstimadasFase2(10);
        proyecto.setFaseActual(FaseObra.PLANEACION);
        proyecto.setEstadoFaseActual(EstadoFaseObra.EN_VALIDACION);
        proyecto.setEvidenciasFotograficas(List.of("terreno-antes.jpg", "terreno-despues.jpg"));
        proyecto.setAprobacionClienteFase(true);
        proyecto.setCuotasSemanaPagadas(2);
        proyecto.setCuotasTotalesEsperadas(3);
        proyecto.setTareasFaseCompletadas(true);

        assertFalse(service.puedeDesbloquearSiguienteFase(proyecto));

        proyecto.setCuotasSemanaPagadas(3);
        assertTrue(service.puedeDesbloquearSiguienteFase(proyecto));
    }

    @Test
    void avanzaLaFaseSoloCuandoSeCumplenLasCondicionesPrevias() {
        Proyecto proyecto = new Proyecto();
        proyecto.setPresupuesto(50_000_000.0);
        proyecto.setSemanasEstimadasFase1(3);
        proyecto.setSemanasEstimadasFase2(10);
        proyecto.setFaseActual(FaseObra.PLANEACION);
        proyecto.setEstadoFaseActual(EstadoFaseObra.EN_VALIDACION);
        proyecto.setEvidenciasFotograficas(List.of("terreno-antes.jpg", "terreno-despues.jpg", "huecos.jpg", "replanteo.jpg"));
        proyecto.setAprobacionClienteFase(true);
        proyecto.setCuotasSemanaPagadas(3);
        proyecto.setCuotasTotalesEsperadas(3);
        proyecto.setTareasFaseCompletadas(true);

        assertTrue(service.avanzarFase(proyecto));
        assertEquals(FaseObra.DESARROLLO, proyecto.getFaseActual());
        assertEquals(EstadoFaseObra.BLOQUEADA, proyecto.getEstadoFaseActual());
    }
}
