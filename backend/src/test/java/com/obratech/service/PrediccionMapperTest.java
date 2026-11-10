package com.obratech.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.obratech.dto.ml.PrediccionRetrasoRequest;
import com.obratech.dto.ml.PrediccionSeleccionRequest;
import com.obratech.entity.Contratista;
import com.obratech.entity.Perfil;
import com.obratech.entity.Proyecto;
import com.obratech.entity.enums.EstadoEjecucion;
import com.obratech.repository.PerfilRepository;
import com.obratech.repository.PostulacionRepository;
import com.obratech.repository.ProyectoRepository;

class PrediccionMapperTest {

    private ProyectoRepository proyectoRepository;
    private PostulacionRepository postulacionRepository;
    private PerfilRepository perfilRepository;
    private ProjectFitSimulationService fitService;
    private PrediccionMapper mapper;

    @BeforeEach
    void setUp() {
        proyectoRepository = mock(ProyectoRepository.class);
        postulacionRepository = mock(PostulacionRepository.class);
        perfilRepository = mock(PerfilRepository.class);
        fitService = mock(ProjectFitSimulationService.class);
        mapper = new PrediccionMapper(proyectoRepository, postulacionRepository, perfilRepository, fitService);
    }

    @Test
    void construyeRetrasoConFechasPresupuestoYHistorial() {
        Proyecto proyecto = proyecto("Residencial", "Barranquilla");
        Perfil contratista = perfil("perfil-1", "contratista@demo.test");
        proyecto.setContratistaAsignado(contratista);

        Proyecto terminado = proyecto("Vial", "Barranquilla");
        terminado.setEstadoEjecucion(EstadoEjecucion.COMPLETADO);
        terminado.setFechaEntrega(LocalDate.of(2025, 6, 10));
        terminado.setFechaFinReal(LocalDate.of(2025, 6, 10));
        when(proyectoRepository.findByContratistaAsignadoIdAndEstadoEjecucion(
                "perfil-1", EstadoEjecucion.COMPLETADO)).thenReturn(List.of(terminado));
        when(proyectoRepository.findByContratistaAsignadoIdAndEstadoEjecucionNot(
                "perfil-1", EstadoEjecucion.COMPLETADO)).thenReturn(List.of());

        PrediccionRetrasoRequest request = mapper.construirRetraso(proyecto);

        assertEquals(2.5, request.presupuesto());
        assertEquals(30, request.duracionPlanificadaDias());
        assertEquals("barranquilla", request.ubicacion());
        assertEquals(8.0, request.factores().get("experiencia_contratista_anios"));
        assertEquals(1.0, request.factores().get("pct_obras_a_tiempo_contratista"));
    }

    @Test
    void construyeSeleccionConCompatibilidadEHistorial() {
        Proyecto proyecto = proyecto("Infraestructura", "Soledad");
        proyecto.setId("proyecto-1");
        Perfil contratista = perfil("perfil-2", "postulante@demo.test");
        contratista.getContratista().setCalificacionPromedio(4.5);
        when(postulacionRepository.findByProyectoId("proyecto-1")).thenReturn(List.of());
        when(proyectoRepository.findByContratistaAsignadoIdAndEstadoEjecucion(
                "perfil-2", EstadoEjecucion.COMPLETADO)).thenReturn(List.of());
        when(fitService.simulate(proyecto, contratista))
                .thenReturn(new ProjectFitSimulationService.Simulation(
                        0.8, 0.7, 0.9, 0.82, 0.8, 0, 0.0, 0, 0.6));

        PrediccionSeleccionRequest request = mapper.construirSeleccion(proyecto, contratista);

        assertEquals(82.0, request.compatibilidad());
        assertEquals(2.5, request.presupuesto());
        assertEquals(1, request.factores().get("num_postulantes"));
        assertNotNull(request.factores().get("municipio_proyecto"));
    }

    private Proyecto proyecto(String tipo, String municipio) {
        Proyecto proyecto = new Proyecto();
        proyecto.setTipoProyecto(tipo);
        proyecto.setUbicacion(municipio);
        proyecto.setPresupuesto(2_500_000.0);
        proyecto.setFechaInicio(LocalDate.of(2026, 1, 1));
        proyecto.setFechaEntrega(LocalDate.of(2026, 1, 31));
        return proyecto;
    }

    private Perfil perfil(String id, String username) {
        Perfil perfil = new Perfil();
        perfil.setId(id);
        perfil.setUsername(username);
        Contratista contratista = new Contratista();
        contratista.setExperiencia(8);
        contratista.setCalificacionPromedio(4.2);
        perfil.setDetallesContratista(contratista);
        return perfil;
    }
}
