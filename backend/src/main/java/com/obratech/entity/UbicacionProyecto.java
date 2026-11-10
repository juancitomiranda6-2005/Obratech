package com.obratech.entity;

import java.util.Objects;

public class UbicacionProyecto {

    private String departamento;
    private String municipio;
    private String detalle;

    public UbicacionProyecto() {
    }

    public UbicacionProyecto(String departamento, String municipio, String detalle) {
        this.departamento = departamento;
        this.municipio = municipio;
        this.detalle = detalle;
    }

    public String getDepartamento() {
        return departamento;
    }

    public void setDepartamento(String departamento) {
        this.departamento = departamento;
    }

    public String getMunicipio() {
        return municipio;
    }

    public void setMunicipio(String municipio) {
        this.municipio = municipio;
    }

    public String getDetalle() {
        return detalle;
    }

    public void setDetalle(String detalle) {
        this.detalle = detalle;
    }

    public String toDisplayString() {
        return String.join(", ", java.util.stream.Stream.of(municipio, departamento, detalle)
                .filter(value -> value != null && !value.isBlank())
                .map(String::trim)
                .toList());
    }

    public boolean hasRequiredValues() {
        return StreamValues.present(departamento) && StreamValues.present(municipio)
                && StreamValues.present(detalle);
    }

    private static final class StreamValues {
        private StreamValues() {
        }

        private static boolean present(String value) {
            return Objects.nonNull(value) && !value.isBlank();
        }
    }
}
