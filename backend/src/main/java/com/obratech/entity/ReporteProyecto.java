package com.obratech.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "reportes_proyecto")
public class ReporteProyecto {

    @Id
    private String id;

    @Indexed
    private String proyectoId;

    private String proyectoTitulo;

    @Indexed
    private String contratistaId;

    private String contratistaUsername;

    private String contenido;

    private LocalDateTime creado;

    private List<String> reportesTrabajadorIds = new ArrayList<>();

    private List<EvidenciaAdjunta> evidencias = new ArrayList<>();

    private List<ReporteAvanceTrabajador> avancesTrabajador = new ArrayList<>();

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getProyectoId() {
        return proyectoId;
    }

    public void setProyectoId(String proyectoId) {
        this.proyectoId = proyectoId;
    }

    public String getProyectoTitulo() {
        return proyectoTitulo;
    }

    public void setProyectoTitulo(String proyectoTitulo) {
        this.proyectoTitulo = proyectoTitulo;
    }

    public String getContratistaId() {
        return contratistaId;
    }

    public void setContratistaId(String contratistaId) {
        this.contratistaId = contratistaId;
    }

    public String getContratistaUsername() {
        return contratistaUsername;
    }

    public void setContratistaUsername(String contratistaUsername) {
        this.contratistaUsername = contratistaUsername;
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

    public List<String> getReportesTrabajadorIds() {
        return reportesTrabajadorIds;
    }

    public void setReportesTrabajadorIds(List<String> reportesTrabajadorIds) {
        this.reportesTrabajadorIds = reportesTrabajadorIds;
    }

    public List<EvidenciaAdjunta> getEvidencias() {
        return evidencias == null ? List.of() : evidencias;
    }

    public void setEvidencias(List<EvidenciaAdjunta> evidencias) {
        this.evidencias = evidencias == null ? new ArrayList<>() : evidencias;
    }

    public List<ReporteAvanceTrabajador> getAvancesTrabajador() {
        return avancesTrabajador == null ? List.of() : avancesTrabajador;
    }

    public void setAvancesTrabajador(List<ReporteAvanceTrabajador> avancesTrabajador) {
        this.avancesTrabajador = avancesTrabajador == null ? new ArrayList<>() : avancesTrabajador;
    }
}