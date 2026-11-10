package com.obratech.dto.ml;

import java.util.List;

public record PrediccionRetrasoResponse(
        Boolean retrasoProbable,
        Double probabilidad,
        Integer diasRetrasoEstimados,
        String modelo,
        Boolean mock,
        List<String> camposPorDefecto) {
    public PrediccionRetrasoResponse(
            Boolean retrasoProbable,
            Double probabilidad,
            Integer diasRetrasoEstimados,
            String modelo,
            Boolean mock) {
        this(retrasoProbable, probabilidad, diasRetrasoEstimados, modelo, mock, List.of());
    }

    // TODO: Agregar intervalo de confianza y explicación de las variables más influyentes.
}
