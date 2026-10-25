package com.obratech.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.obratech.entity.CuotaProyecto;
import com.obratech.entity.EquipoTrabajo;
import com.obratech.entity.Proyecto;
import com.obratech.entity.ReporteAvanceTrabajador;
import com.obratech.entity.enums.EstadoFaseObra;
import com.obratech.entity.enums.FaseObra;
import com.obratech.repository.CuotaProyectoRepository;
import com.obratech.repository.EquipoTrabajoRepository;
import com.obratech.repository.ReporteAvanceTrabajadorRepository;

class ProyectoFaseServiceTest {

    private final EquipoTrabajoRepository teamRepository = mock(EquipoTrabajoRepository.class);
    private final CuotaProyectoRepository installmentRepository = mock(CuotaProyectoRepository.class);
    private final ReporteAvanceTrabajadorRepository reportRepository = mock(ReporteAvanceTrabajadorRepository.class);
    private final ProyectoFaseService service = new ProyectoFaseService(
            teamRepository, installmentRepository, reportRepository);

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
        proyecto.setId("project-1");
        proyecto.setEstadoFaseActual(EstadoFaseObra.EN_VALIDACION);
        proyecto.setEvidenciasFotograficas(List.of("terreno-antes.jpg", "terreno-despues.jpg"));
        proyecto.setAprobacionClienteFase(true);
        proyecto.setCuotasSemanaPagadas(2);
        proyecto.setCuotasTotalesEsperadas(3);
        proyecto.setTareasFaseCompletadas(true);
        when(teamRepository.findByProyectoId("project-1")).thenReturn(List.of(teamWithEvidence()));
        when(reportRepository.findByProyectoIdOrderByCreadoDesc("project-1")).thenReturn(List.of(reportWithEvidence()));
        when(installmentRepository.findByProyectoId("project-1")).thenReturn(cuotas(2));

        assertFalse(service.puedeDesbloquearSiguienteFase(proyecto));

        when(installmentRepository.findByProyectoId("project-1")).thenReturn(cuotas(3));
        assertTrue(service.puedeDesbloquearSiguienteFase(proyecto));
    }

    @Test
    void avanzaLaFaseSoloCuandoSeCumplenLasCondicionesPrevias() {
        Proyecto proyecto = new Proyecto();
        proyecto.setPresupuesto(50_000_000.0);
        proyecto.setSemanasEstimadasFase1(3);
        proyecto.setSemanasEstimadasFase2(10);
        proyecto.setFaseActual(FaseObra.PLANEACION);
        proyecto.setId("project-1");
        proyecto.setEstadoFaseActual(EstadoFaseObra.EN_VALIDACION);
        proyecto.setEvidenciasFotograficas(List.of("terreno-antes.jpg", "terreno-despues.jpg", "huecos.jpg", "replanteo.jpg"));
        proyecto.setAprobacionClienteFase(true);
        proyecto.setCuotasSemanaPagadas(3);
        proyecto.setCuotasTotalesEsperadas(3);
        proyecto.setTareasFaseCompletadas(true);
        when(teamRepository.findByProyectoId("project-1")).thenReturn(List.of(teamWithEvidence()));
        when(reportRepository.findByProyectoIdOrderByCreadoDesc("project-1")).thenReturn(List.of(reportWithEvidence()));
        when(installmentRepository.findByProyectoId("project-1")).thenReturn(cuotas(3));

        assertTrue(service.avanzarFase(proyecto));
        assertEquals(FaseObra.DESARROLLO, proyecto.getFaseActual());
        assertEquals(EstadoFaseObra.BLOQUEADA, proyecto.getEstadoFaseActual());
        assertFalse(proyecto.getTareasFaseCompletadas());
        assertFalse(proyecto.getAprobacionClienteFase());
    }

    @Test
    void calculaElAvancePonderadoConAvanceCompletoEnFasesPrevias() {
        Proyecto proyecto = new Proyecto();
        proyecto.setId("project-1");
        proyecto.setFaseActual(FaseObra.DESARROLLO);
        EquipoTrabajo planeacion = new EquipoTrabajo();
        planeacion.setFase(FaseObra.PLANEACION);
        planeacion.setPorcentajeAvance(100.0);
        EquipoTrabajo desarrollo = new EquipoTrabajo();
        desarrollo.setFase(FaseObra.DESARROLLO);
        desarrollo.setPorcentajeAvance(50.0);
        when(teamRepository.findByProyectoId("project-1")).thenReturn(List.of(planeacion, desarrollo));

        assertEquals(55.0, service.calcularAvanceObra(proyecto));
    }

    private EquipoTrabajo teamWithEvidence() {
        EquipoTrabajo team = new EquipoTrabajo();
        team.setId("team-1");
        team.setFase(FaseObra.PLANEACION);
        return team;
    }

    private ReporteAvanceTrabajador reportWithEvidence() {
        ReporteAvanceTrabajador report = new ReporteAvanceTrabajador();
        report.setEquipoId("team-1");
        report.setEvidencias(List.of(new com.obratech.entity.EvidenciaAdjunta()));
        return report;
    }

    private List<CuotaProyecto> cuotas(int cantidad) {
        return java.util.stream.IntStream.range(0, cantidad).mapToObj(index -> {
            CuotaProyecto cuota = new CuotaProyecto();
            cuota.setFase(FaseObra.PLANEACION);
            cuota.setNumeroCuota(index + 1);
            return cuota;
        }).toList();
    }
}
