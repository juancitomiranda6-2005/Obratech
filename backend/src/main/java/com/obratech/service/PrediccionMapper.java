package com.obratech.service;

import java.text.Normalizer;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.obratech.dto.ml.PrediccionRetrasoRequest;
import com.obratech.dto.ml.PrediccionSeleccionRequest;
import com.obratech.entity.Perfil;
import com.obratech.entity.Proyecto;
import com.obratech.entity.enums.EstadoEjecucion;
import com.obratech.repository.PerfilRepository;
import com.obratech.repository.PostulacionRepository;
import com.obratech.repository.ProyectoRepository;

@Component
public class PrediccionMapper {

    private static final Logger log = LoggerFactory.getLogger(PrediccionMapper.class);
    private static final double COP_POR_MILLON = 1_000_000.0;
    private static final Set<String> MUNICIPIOS_CONOCIDOS = Set.of(
            "barranquilla", "soledad", "malambo", "puerto colombia", "sabanalarga",
            "baranoa", "galapa", "sabanagrande", "santo tomas", "juan de acosta",
            "tubara", "luruaco", "repelon", "campo de la cruz", "candelaria",
            "manati", "palmar de varela", "polonuevo", "ponedera", "santa lucia",
            "suan", "usiacuri", "piojo");
    private static final Map<String, Coordenadas> COORDENADAS_ATLANTICO = Map.ofEntries(
            Map.entry("barranquilla", new Coordenadas(10.9685, -74.7813)),
            Map.entry("soledad", new Coordenadas(10.9184, -74.7646)),
            Map.entry("malambo", new Coordenadas(10.8595, -74.7739)),
            Map.entry("puerto colombia", new Coordenadas(11.0136, -74.8860)),
            Map.entry("sabanalarga", new Coordenadas(10.6378, -74.9200)),
            Map.entry("baranoa", new Coordenadas(10.7947, -74.9160)),
            Map.entry("galapa", new Coordenadas(10.8969, -74.8860)),
            Map.entry("sabanagrande", new Coordenadas(10.7917, -74.7556)),
            Map.entry("santo tomas", new Coordenadas(10.7578, -74.7544)),
            Map.entry("juan de acosta", new Coordenadas(10.8293, -75.0347)),
            Map.entry("tubara", new Coordenadas(10.8744, -74.9781)),
            Map.entry("luruaco", new Coordenadas(10.6086, -75.1511)),
            Map.entry("repelon", new Coordenadas(10.4930, -75.1286)),
            Map.entry("campo de la cruz", new Coordenadas(10.3789, -74.8839)),
            Map.entry("candelaria", new Coordenadas(10.4594, -74.8797)),
            Map.entry("manati", new Coordenadas(10.4453, -74.9581)),
            Map.entry("palmar de varela", new Coordenadas(10.7406, -74.7547)),
            Map.entry("polonuevo", new Coordenadas(10.7761, -74.8539)),
            Map.entry("ponedera", new Coordenadas(10.6428, -74.7533)),
            Map.entry("santa lucia", new Coordenadas(10.3247, -74.9608)),
            Map.entry("suan", new Coordenadas(10.3331, -74.8803)),
            Map.entry("usiacuri", new Coordenadas(10.7422, -75.0106)),
            Map.entry("piojo", new Coordenadas(10.7492, -75.1078)));
    private final ProyectoRepository proyectoRepository;
    private final PostulacionRepository postulacionRepository;
    private final PerfilRepository perfilRepository;
    private final ProjectFitSimulationService projectFitSimulationService;

    public PrediccionMapper(
            ProyectoRepository proyectoRepository,
            PostulacionRepository postulacionRepository,
            PerfilRepository perfilRepository,
            ProjectFitSimulationService projectFitSimulationService) {
        this.proyectoRepository = proyectoRepository;
        this.postulacionRepository = postulacionRepository;
        this.perfilRepository = perfilRepository;
        this.projectFitSimulationService = projectFitSimulationService;
    }

    public PrediccionRetrasoRequest construirRetraso(Proyecto proyecto) {
        return construirRetraso(proyecto, null);
    }

    public PrediccionRetrasoRequest construirRetraso(Proyecto proyecto, String perfilId) {
        Perfil contratista = proyecto.getContratistaAsignado();
        if (contratista == null && perfilId != null && !perfilId.isBlank()) {
            contratista = perfilRepository.findById(perfilId).orElse(null);
        }

        Map<String, Object> factores = factoresBase(proyecto);
        if (contratista != null) {
            factores.put("experiencia_contratista_anios",
                    contratista.getExperiencia() == null ? 0.0 : contratista.getExperiencia().doubleValue());
            Historial historial = historial(contratista);
            factores.put("pct_obras_a_tiempo_contratista", historial.porcentajeATiempo());
            factores.put("obras_simultaneas_contratista", proyectosActivos(contratista));
        }

        return new PrediccionRetrasoRequest(
                proyecto.getTitulo(),
                presupuestoMillones(proyecto),
                duracionDias(proyecto),
                municipio(proyecto),
                factores);
    }

    public PrediccionSeleccionRequest construirSeleccion(Proyecto proyecto, Perfil contratista) {
        if (contratista == null) {
            throw new IllegalArgumentException("El perfil del contratista es obligatorio");
        }

        ProjectFitSimulationService.Simulation simulacion =
                projectFitSimulationService.simulate(proyecto, contratista);
        Historial historial = historial(contratista);
        Map<String, Object> factores = factoresBase(proyecto);
        factores.put("tipo_obra", tipoObra(proyecto.getTipoProyecto()));
        factores.put("municipio_proyecto", municipio(proyecto));
        factores.put("pct_obras_a_tiempo", historial.porcentajeATiempo());
        factores.put("obras_completadas", historial.obrasCompletadas());
        factores.put("num_postulantes", postulacionRepository.findByProyectoId(proyecto.getId()).size() + 1);
        factores.put("ciudadContratista", normalizar(contratista.getCiudad()));
        factores.put("calificacion",
                contratista.getCalificacionPromedio() == null ? 0.0 : contratista.getCalificacionPromedio());

        return new PrediccionSeleccionRequest(
                proyecto.getTitulo(),
                contratista.getUsername(),
                presupuestoMillones(proyecto),
                duracionDias(proyecto),
                contratista.getExperiencia() == null ? null : contratista.getExperiencia().doubleValue(),
                contratista.getCalificacionPromedio(),
                simulacion.compatibilidadGeneral() * 100.0,
                factores);
    }

    private Map<String, Object> factoresBase(Proyecto proyecto) {
        Map<String, Object> factores = new LinkedHashMap<>();
        factores.put("tipo_obra", tipoObra(proyecto.getTipoProyecto()));
        String municipio = municipio(proyecto);
        if (municipio != null) {
            factores.put("municipio", municipio);
        }
        factores.put("fecha_inicio",
                proyecto.getFechaInicio() == null ? null : proyecto.getFechaInicio().toString());
        factores.put("latitud", proyecto.getLatitud());
        factores.put("longitud", proyecto.getLongitud());
        factores.put("num_trabajadores",
                proyecto.getEquipoTrabajo() == null ? 0 : proyecto.getEquipoTrabajo().size());
        return factores;
    }

    private double presupuestoMillones(Proyecto proyecto) {
        // Proyecto.presupuesto se valida y persiste en pesos COP; el modelo recibe millones de COP.
        return proyecto.getPresupuesto() == null ? 0.0 : proyecto.getPresupuesto() / COP_POR_MILLON;
    }

    private int duracionDias(Proyecto proyecto) {
        if (proyecto.getFechaInicio() != null && proyecto.getFechaEntrega() != null) {
            return (int) ChronoUnit.DAYS.between(proyecto.getFechaInicio(), proyecto.getFechaEntrega());
        }
        return proyecto.getPlazoEstimado() == null ? 0 : proyecto.getPlazoEstimado();
    }

    private String tipoObra(String valor) {
        String normalizado = normalizar(valor);
        return switch (normalizado) {
            case "residencial", "vivienda", "casa", "casas" -> "vivienda";
            case "infraestructura", "vial", "carretera", "carreteras" -> "vial";
            case "acueducto", "agua", "saneamiento" -> "acueducto";
            case "edificio", "edificios", "comercial", "industrial", "institucional" -> "edificio";
            case "remodelacion", "remodelación", "remodelaciones" -> "remodelacion";
            case "espacio publico", "espacio_publico", "parque", "parques" -> "espacio_publico";
            default -> desconocido("tipoProyecto", valor, normalizado);
        };
    }

    private String municipio(Proyecto proyecto) {
        String ubicacion = proyecto.getUbicacion();
        String normalizada = normalizar(ubicacion);
        String municipioConocido = MUNICIPIOS_CONOCIDOS.stream()
                .filter(normalizada::contains)
                .findFirst().orElse(null);
        if (municipioConocido != null) {
            return municipioConocido;
        }
        if (proyecto.getUbicacionGeografica() != null) {
            String ciudad = normalizar(proyecto.getUbicacionGeografica().getMunicipio());
            if (MUNICIPIOS_CONOCIDOS.contains(ciudad)) {
                return ciudad;
            }
        }
        if (proyecto.getLatitud() != null && proyecto.getLongitud() != null) {
            return COORDENADAS_ATLANTICO.entrySet().stream()
                    .min((a, b) -> Double.compare(distancia(proyecto, a.getValue()), distancia(proyecto, b.getValue())))
                    .map(Map.Entry::getKey)
                    .orElse(null);
        }
        log.warn("No se pudo determinar municipio para ML: {}", ubicacion);
        return null;
    }

    private String desconocido(String campo, String original, String normalizado) {
        log.warn("Valor de {} no mapeado para ML: {}", campo, original);
        return original == null || original.isBlank() ? normalizado : original.trim();
    }

    private double distancia(Proyecto proyecto, Coordenadas municipio) {
        double lat = proyecto.getLatitud() - municipio.latitud();
        double lon = proyecto.getLongitud() - municipio.longitud();
        return lat * lat + lon * lon;
    }

    private Historial historial(Perfil contratista) {
        if (contratista.getId() == null) {
            return new Historial(0, 0);
        }
        long completados = proyectoRepository.findByContratistaAsignadoIdAndEstadoEjecucion(
                        contratista.getId(), EstadoEjecucion.COMPLETADO).stream()
                .filter(p -> p.getFechaFinReal() != null)
                .count();
        long aTiempo = proyectoRepository.findByContratistaAsignadoIdAndEstadoEjecucion(
                        contratista.getId(), EstadoEjecucion.COMPLETADO).stream()
                .filter(p -> p.getFechaFinReal() != null && p.getFechaEntrega() != null)
                .filter(p -> !p.getFechaFinReal().isAfter(p.getFechaEntrega()))
                .count();
        return new Historial(completados, aTiempo);
    }

    private int proyectosActivos(Perfil contratista) {
        if (contratista.getId() == null) {
            return 0;
        }
        return proyectoRepository.findByContratistaAsignadoIdAndEstadoEjecucionNot(
                        contratista.getId(), EstadoEjecucion.COMPLETADO).size();
    }

    private String normalizar(String valor) {
        if (valor == null) {
            return "";
        }
        return Normalizer.normalize(valor, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .trim()
                .replaceAll("\\s+", " ");
    }

    private record Historial(long obrasCompletadas, long obrasATiempo) {
        double porcentajeATiempo() {
            return obrasCompletadas == 0 ? 0.0 : (double) obrasATiempo / obrasCompletadas;
        }
    }

    private record Coordenadas(double latitud, double longitud) {
    }
}
