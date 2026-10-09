package com.obratech.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.DBRef;
import java.time.LocalDateTime;

import com.obratech.entity.enums.EstadoEjecucion;

@Document(collection = "historial_proyectos")
public class HistorialProyecto {

    @Id
    private String id;

    private EstadoEjecucion estadoAnterior;
    private EstadoEjecucion estadoNuevo;
    private LocalDateTime fechaCambio;

    @DBRef
    private Proyecto proyecto;

    public HistorialProyecto() {
    }

    public HistorialProyecto(EstadoEjecucion estadoAnterior, EstadoEjecucion estadoNuevo, Proyecto proyecto) {
        this.estadoAnterior = estadoAnterior;
        this.estadoNuevo = estadoNuevo;
        this.fechaCambio = LocalDateTime.now();
        this.proyecto = proyecto;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public EstadoEjecucion getEstadoAnterior() {
        return estadoAnterior;
    }

    public void setEstadoAnterior(EstadoEjecucion estadoAnterior) {
        this.estadoAnterior = estadoAnterior;
    }

    public EstadoEjecucion getEstadoNuevo() {
        return estadoNuevo;
    }

    public void setEstadoNuevo(EstadoEjecucion estadoNuevo) {
        this.estadoNuevo = estadoNuevo;
    }

    public LocalDateTime getFechaCambio() {
        return fechaCambio;
    }

    public void setFechaCambio(LocalDateTime fechaCambio) {
        this.fechaCambio = fechaCambio;
    }

    public Proyecto getProyecto() {
        return proyecto;
    }

    public void setProyecto(Proyecto proyecto) {
        this.proyecto = proyecto;
    }

}
