package com.obratech.dto.ml;

import java.util.Map;

public record PrediccionSeleccionRequest(
        String obra,
        String contratista,
        Double presupuesto,
        Integer duracionPlanificadaDias,
        Double experienciaAnios,
        Double calificacionPromedio,
        Double compatibilidad,
        Map<String, Object> factores) {
    // TODO: Alinear estos campos con el esquema final de entrenamiento y las entidades existentes.

    public PrediccionSeleccionRequest(
            String obra,
            String contratista,
            Double presupuesto,
            Integer duracionPlanificadaDias,
            Double experienciaAnios,
            Double calificacionPromedio,
            Double compatibilidad) {
        this(obra, contratista, presupuesto, duracionPlanificadaDias,
                experienciaAnios, calificacionPromedio, compatibilidad, Map.of());
    }
}
