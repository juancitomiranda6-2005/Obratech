package com.obratech.entity;

// ESTA CLASE UNIRLA CON CLIENTE
import java.util.HashSet;
import java.util.Set;
import org.springframework.data.mongodb.core.mapping.DBRef;

public class Cliente {

    private String empresa;

    @DBRef(lazy = true)
    private Set<Proyecto> proyectos = new HashSet<>();

    public Cliente() {}

    public String getEmpresa() {
        return empresa;
    }

    public void setEmpresa(String empresa) {
        this.empresa = empresa;
    }

    public Set<Proyecto> getProyectos() {
        return proyectos;
    }

    public void setProyectos(Set<Proyecto> proyectos) {
        this.proyectos = proyectos;
    }
}
