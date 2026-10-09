package com.obratech.entity;

// ESTA CLASE UNIRLA CON CONTRATISTA
import java.util.HashSet;
import java.util.Set;
import org.springframework.data.mongodb.core.mapping.DBRef;

public class Contratista {

    private String especialidad;
    private String ubicacion;
    private String descripcion;
    private Integer experiencia;
    private Double calificacionPromedio;
    private String cvUrl;
    private String estatutosUrl;
    private String fotoPerfilUrl;
    private String ciudad;
    private String departamento;
    private String matriculaProfesional;
    private String matriculaDocumentoUrl;
    private Set<String> subespecialidades = new HashSet<>();

    @DBRef(lazy = true)
    private Set<Proyecto> proyectos = new HashSet<>();

    public Contratista() {
    }

    public String getEspecialidad() {
        return especialidad;
    }

    public void setEspecialidad(String especialidad) {
        this.especialidad = especialidad;
    }

    public String getUbicacion() {
        return ubicacion;
    }

    public void setUbicacion(String ubicacion) {
        this.ubicacion = ubicacion;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public Integer getExperiencia() {
        return experiencia;
    }

    public void setExperiencia(Integer experiencia) {
        this.experiencia = experiencia;
    }

    public Double getCalificacionPromedio() {
        return calificacionPromedio;
    }

    public void setCalificacionPromedio(Double calificacionPromedio) {
        this.calificacionPromedio = calificacionPromedio;
    }

    public String getCvUrl() {
        return cvUrl;
    }

    public void setCvUrl(String cvUrl) {
        this.cvUrl = cvUrl;
    }

    public String getEstatutosUrl() {
        return estatutosUrl;
    }

    public void setEstatutosUrl(String estatutosUrl) {
        this.estatutosUrl = estatutosUrl;
    }

    public String getFotoPerfilUrl() {
        return fotoPerfilUrl;
    }

    public void setFotoPerfilUrl(String fotoPerfilUrl) {
        this.fotoPerfilUrl = fotoPerfilUrl;
    }

    public String getCiudad() {
        return ciudad;
    }

    public void setCiudad(String ciudad) {
        this.ciudad = ciudad;
    }

    public String getDepartamento() {
        return departamento;
    }

    public void setDepartamento(String departamento) {
        this.departamento = departamento;
    }

    public String getMatriculaProfesional() {
        return matriculaProfesional;
    }

    public void setMatriculaProfesional(String matriculaProfesional) {
        this.matriculaProfesional = matriculaProfesional;
    }

    public String getMatriculaDocumentoUrl() {
        return matriculaDocumentoUrl;
    }

    public void setMatriculaDocumentoUrl(String matriculaDocumentoUrl) {
        this.matriculaDocumentoUrl = matriculaDocumentoUrl;
    }

    public Set<String> getSubespecialidades() {
        return subespecialidades;
    }

    public void setSubespecialidades(Set<String> subespecialidades) {
        this.subespecialidades = subespecialidades != null ? subespecialidades : new HashSet<>();
    }

    public Set<Proyecto> getProyectos() {
        return proyectos;
    }

    public void setProyectos(Set<Proyecto> proyectos) {
        this.proyectos = proyectos;
    }
}
