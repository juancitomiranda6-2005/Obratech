package com.obratech.entity;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "observaciones_predictivas_proyecto")
public class ProyectoObservacionPredictiva {

    @Id
    private String id;

    @Indexed
    private String proyectoId;

    @Indexed
    private LocalDateTime observadoEn;

    private Double presupuesto;
    private Double avanceActual;
    private Integer diasSinActividad;
    private Integer equiposActivos;
    private Integer equiposTotales;
    private Integer pagosPendientes;
    private Integer aprobacionesPendientes;
    private Double probabilidadLluvia;
    private String faseActual;
    private Boolean terminoConRetraso;
    private Integer diasRetraso;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getProyectoId() { return proyectoId; }
    public void setProyectoId(String proyectoId) { this.proyectoId = proyectoId; }
    public LocalDateTime getObservadoEn() { return observadoEn; }
    public void setObservadoEn(LocalDateTime observadoEn) { this.observadoEn = observadoEn; }
    public Double getPresupuesto() { return presupuesto; }
    public void setPresupuesto(Double presupuesto) { this.presupuesto = presupuesto; }
    public Double getAvanceActual() { return avanceActual; }
    public void setAvanceActual(Double avanceActual) { this.avanceActual = avanceActual; }
    public Integer getDiasSinActividad() { return diasSinActividad; }
    public void setDiasSinActividad(Integer diasSinActividad) { this.diasSinActividad = diasSinActividad; }
    public Integer getEquiposActivos() { return equiposActivos; }
    public void setEquiposActivos(Integer equiposActivos) { this.equiposActivos = equiposActivos; }
    public Integer getEquiposTotales() { return equiposTotales; }
    public void setEquiposTotales(Integer equiposTotales) { this.equiposTotales = equiposTotales; }
    public Integer getPagosPendientes() { return pagosPendientes; }
    public void setPagosPendientes(Integer pagosPendientes) { this.pagosPendientes = pagosPendientes; }
    public Integer getAprobacionesPendientes() { return aprobacionesPendientes; }
    public void setAprobacionesPendientes(Integer aprobacionesPendientes) { this.aprobacionesPendientes = aprobacionesPendientes; }
    public Double getProbabilidadLluvia() { return probabilidadLluvia; }
    public void setProbabilidadLluvia(Double probabilidadLluvia) { this.probabilidadLluvia = probabilidadLluvia; }
    public String getFaseActual() { return faseActual; }
    public void setFaseActual(String faseActual) { this.faseActual = faseActual; }
    public Boolean getTerminoConRetraso() { return terminoConRetraso; }
    public void setTerminoConRetraso(Boolean terminoConRetraso) { this.terminoConRetraso = terminoConRetraso; }
    public Integer getDiasRetraso() { return diasRetraso; }
    public void setDiasRetraso(Integer diasRetraso) { this.diasRetraso = diasRetraso; }
}
