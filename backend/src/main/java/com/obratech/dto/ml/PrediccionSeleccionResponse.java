package com.obratech.dto.ml;

import java.util.List;
import java.util.Map;

public record PrediccionSeleccionResponse(
        Double probabilidad,
        String recomendacion,
        String modelo,
        Boolean mock,
        List<String> camposPorDefecto,
        Double porcentaje,
        String nivel,
        List<Map<String, Object>> factores) {
    public PrediccionSeleccionResponse(
            Double probabilidad,
            String recomendacion,
            String modelo,
            Boolean mock) {
        this(probabilidad, recomendacion, modelo, mock, List.of(), null, null, List.of());
    }

    public PrediccionSeleccionResponse(
            Double probabilidad,
            String recomendacion,
            String modelo,
            Boolean mock,
            List<String> camposPorDefecto) {
        this(probabilidad, recomendacion, modelo, mock, camposPorDefecto, null, null, List.of());
    }

    // TODO: Agregar ranking, razones de recomendación y versión del modelo.
}
