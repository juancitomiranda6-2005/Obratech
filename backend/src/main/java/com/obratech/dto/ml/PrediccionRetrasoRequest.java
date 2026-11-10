package com.obratech.dto.ml;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record PrediccionRetrasoRequest(
        String obra,
        Double presupuesto,
        Integer duracionPlanificadaDias,
        String ubicacion,
        Map<String, Object> factores) {
    // TODO: Completar los campos de clima, materiales, permisos y avance histórico.
}
