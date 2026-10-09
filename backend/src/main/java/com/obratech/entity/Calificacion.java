package com.obratech.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.DBRef;
import java.time.LocalDateTime;

@Document(collection = "calificaciones")
public class Calificacion {

    @Id
    private String id;

    @DBRef
    @Indexed
    private Perfil contratista;

    @DBRef
    private Usuario cliente;

    @DBRef
    @Indexed
    private Proyecto proyecto;

    private int puntuacion; // 1-5

    private String comentario;

    private LocalDateTime fecha;

    public Calificacion() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Perfil getContratista() {
        return contratista;
    }

    public void setContratista(Perfil contratista) {
        this.contratista = contratista;
    }

    public Usuario getCliente() {
        return cliente;
    }

    public void setCliente(Usuario cliente) {
        this.cliente = cliente;
    }

    public Proyecto getProyecto() {
        return proyecto;
    }

    public void setProyecto(Proyecto proyecto) {
        this.proyecto = proyecto;
    }

    public int getPuntuacion() {
        return puntuacion;
    }

    public void setPuntuacion(int puntuacion) {
        this.puntuacion = puntuacion;
    }

    public String getComentario() {
        return comentario;
    }

    public void setComentario(String comentario) {
        this.comentario = comentario;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public void setFecha(LocalDateTime fecha) {
        this.fecha = fecha;
    }

    {
        if (this.fecha == null)
            this.fecha = LocalDateTime.now();
    }
}
