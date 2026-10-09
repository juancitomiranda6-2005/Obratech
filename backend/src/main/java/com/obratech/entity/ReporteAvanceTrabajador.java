package com.obratech.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "reportes_avance_trabajador")
public class ReporteAvanceTrabajador {

    @Id
    private String id;

    @Indexed
    private String equipoId;

    @Indexed
    private String proyectoId;

    @Indexed
    private String trabajadorId;

    private String trabajadorUsername;
    private String trabajadorNombre;
    private String contenido;
    private LocalDateTime creado;
    private List<EvidenciaAdjunta> evidencias = new ArrayList<>();

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getEquipoId() {
        return equipoId;
    }

    public void setEquipoId(String equipoId) {
        this.equipoId = equipoId;
    }

    public String getProyectoId() {
        return proyectoId;
    }

    public void setProyectoId(String proyectoId) {
        this.proyectoId = proyectoId;
    }

    public String getTrabajadorId() {
        return trabajadorId;
    }

    public void setTrabajadorId(String trabajadorId) {
        this.trabajadorId = trabajadorId;
    }

    public String getTrabajadorUsername() {
        return trabajadorUsername;
    }

    public void setTrabajadorUsername(String trabajadorUsername) {
        this.trabajadorUsername = trabajadorUsername;
    }

    public String getTrabajadorNombre() {
        return trabajadorNombre;
    }

    public void setTrabajadorNombre(String trabajadorNombre) {
        this.trabajadorNombre = trabajadorNombre;
    }

    public String getContenido() {
        return contenido;
    }

    public void setContenido(String contenido) {
        this.contenido = contenido;
    }

    public LocalDateTime getCreado() {
        return creado;
    }

    public void setCreado(LocalDateTime creado) {
        this.creado = creado;
    }

    public List<EvidenciaAdjunta> getEvidencias() {
        return evidencias;
    }

    public void setEvidencias(List<EvidenciaAdjunta> evidencias) {
        this.evidencias = evidencias;
    }
}