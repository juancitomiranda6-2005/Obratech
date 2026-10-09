package com.obratech.entity;

import java.util.ArrayList;
import java.util.List;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.DBRef;

@Document(collection = "equipos_trabajo")
public class EquipoTrabajo {

    @Id
    private String id;

    private String nombre;

    private String actividad;

    private Double porcentajeAvance = 0.0; // 0.0 to 100.0

    @DBRef
    @Indexed
    private Proyecto proyecto;

    @DBRef
    private List<Perfil> integrantes = new ArrayList<>();

    public EquipoTrabajo() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getActividad() {
        return actividad;
    }

    public void setActividad(String actividad) {
        this.actividad = actividad;
    }

    public Double getPorcentajeAvance() {
        return porcentajeAvance;
    }

    public void setPorcentajeAvance(Double porcentajeAvance) {
        this.porcentajeAvance = porcentajeAvance;
    }

    public Proyecto getProyecto() {
        return proyecto;
    }

    public void setProyecto(Proyecto proyecto) {
        this.proyecto = proyecto;
    }

    public List<Perfil> getIntegrantes() {
        return integrantes;
    }

    public void setIntegrantes(List<Perfil> integrantes) {
        this.integrantes = integrantes;
    }
}
