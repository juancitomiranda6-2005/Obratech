package com.obratech.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.obratech.entity.EquipoTrabajo;
import com.obratech.entity.Proyecto;
import com.obratech.entity.enums.FaseObra;
import com.obratech.repository.EquipoTrabajoRepository;
import com.obratech.repository.ReporteAvanceTrabajadorRepository;

class ProjectRiskServiceTest {

    private final ProyectoFaseService proyectoFaseService = mock(ProyectoFaseService.class);
    private final EquipoTrabajoRepository equipoTrabajoRepository = mock(EquipoTrabajoRepository.class);
    private final ReporteAvanceTrabajadorRepository reporteAvanceRepository =
            mock(ReporteAvanceTrabajadorRepository.class);
    private final PronosticoClimaService pronosticoClimaService = mock(PronosticoClimaService.class);
    private final ProjectRiskService service = new ProjectRiskService(
            proyectoFaseService,
            equipoTrabajoRepository,
            reporteAvanceRepository,
            pronosticoClimaService);

    @Test
    void leavesRiskWithoutScoreWhenEveryFactorLacksData() {
        when(pronosticoClimaService.obtenerPronostico(null))
                .thenReturn(new PronosticoClimaService.Pronostico(false, "Sin datos", List.of()));

        var indicators = service.calcularIndicadores(null);

        assertNull(indicators.riesgo().score());
        assertEquals("Sin datos", indicators.riesgo().nivel());
        assertEquals(0, indicators.riesgo().factoresConDatos());
    }

    @Test
    void normalizesRiskAgainstOnlyAvailableFactors() {
        Proyecto proyecto = new Proyecto();
        proyecto.setId("project-1");
        proyecto.setFaseActual(FaseObra.PLANEACION);
        proyecto.setFechaInicio(LocalDate.now().minusDays(20));
        proyecto.setFechaEntrega(LocalDate.now().plusDays(30));
        proyecto.setSemanasEstimadasFase1(2);
        proyecto.setSemanasEstimadasFase2(4);

        EquipoTrabajo team = new EquipoTrabajo();
        team.setFase(FaseObra.PLANEACION);
        team.setActividad("Estructura");
        when(equipoTrabajoRepository.findByProyectoId("project-1")).thenReturn(List.of(team));
        when(reporteAvanceRepository.findByProyectoIdOrderByCreadoDesc("project-1")).thenReturn(List.of());
        when(pronosticoClimaService.obtenerPronostico(proyecto))
                .thenReturn(new PronosticoClimaService.Pronostico(false, "Sin datos", List.of()));
        when(proyectoFaseService.calcularAvanceObra(proyecto)).thenReturn(10.0);
        when(proyectoFaseService.cuotasPagadasEnFase(proyecto, FaseObra.PLANEACION)).thenReturn(0);
        when(proyectoFaseService.contarEvidenciasFaseActual(proyecto)).thenReturn(0);

        var indicators = service.calcularIndicadores(proyecto);
        var risk = indicators.riesgo();
        double expectedScore = risk.factores().stream()
                .filter(ProjectRiskService.RiskFactor::disponible)
                .mapToDouble(ProjectRiskService.RiskFactor::aporte)
                .sum() * 100.0
                / risk.factores().stream()
                        .filter(ProjectRiskService.RiskFactor::disponible)
                        .mapToInt(ProjectRiskService.RiskFactor::peso)
                        .sum();

        assertEquals(3, risk.factoresConDatos());
        assertTrue(risk.factores().stream().anyMatch(factor -> !factor.disponible() && factor.nombre().equals("Clima")));
        assertEquals(Math.round(expectedScore * 10.0) / 10.0, risk.score());
    }
}
