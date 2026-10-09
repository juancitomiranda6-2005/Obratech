package com.obratech.entity;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "perfiles")
public class Perfil {

    @Id
    private String id;

    private String nombre;
    private String apellido;

    @Indexed
    private String username;

    private String email;

    private String telefono;

    private java.util.Set<String> roles = new java.util.HashSet<>(); // ROLE_CLIENT | ROLE_CONTRACTOR | ROLE_WORKER

    private Boolean activo = true;
    private Boolean verificado = false;

    private LocalDateTime creado;

    private Cliente cliente;
    private Contratista contratista;
    private Trabajador trabajador;

    public Perfil() {
        if (this.creado == null)
            this.creado = LocalDateTime.now();
    }

    public Perfil(String nombre, String username) {
        this.nombre = nombre;
        this.username = username;
        this.creado = LocalDateTime.now();
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

    public String getApellido() {
        return apellido;
    }

    public void setApellido(String apellido) {
        this.apellido = apellido;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public java.util.Set<String> getRoles() {
        return roles;
    }

    public void setRoles(java.util.Set<String> roles) {
        this.roles = roles;
    }

    public String getRole() {
        if (roles == null || roles.isEmpty())
            return null;
        if (roles.contains("ROLE_ADMIN"))
            return "ROLE_ADMIN";
        if (roles.contains("ROLE_CLIENT"))
            return "ROLE_CLIENT";
        if (roles.contains("ROLE_CONTRACTOR"))
            return "ROLE_CONTRACTOR";
        if (roles.contains("ROLE_WORKER"))
            return "ROLE_WORKER";
        return roles.iterator().next();
    }

    public void setRole(String role) {
        if (this.roles == null)
            this.roles = new java.util.HashSet<>();
        this.roles.add(role);
    }

    public Boolean getActivo() {
        return activo != null && activo;
    }

    public void setActivo(Boolean activo) {
        this.activo = activo;
    }

    public Boolean getVerificado() {
        return verificado;
    }

    public void setVerificado(Boolean verificado) {
        this.verificado = verificado;
    }

    public LocalDateTime getCreado() {
        return creado;
    }

    public void setCreado(LocalDateTime creado) {
        this.creado = creado;
    }

    public Cliente getCliente() {
        return cliente;
    }

    public void setDetallesCliente(Cliente cliente) {
        this.cliente = cliente;
    }

    public Contratista getContratista() {
        return contratista;
    }

    public void setDetallesContratista(Contratista contratista) {
        this.contratista = contratista;
    }

    public Trabajador getTrabajador() {
        return trabajador;
    }

    public void setTrabajador(Trabajador trabajador) {
        this.trabajador = trabajador;
    }

    public String getEmpresa() {
        return cliente != null ? cliente.getEmpresa() : null;
    }

    public void setEmpresa(String empresa) {
        if (cliente == null)
            cliente = new Cliente();
        cliente.setEmpresa(empresa);
    }

    public String getEspecialidad() {
        return contratista != null ? contratista.getEspecialidad() : null;
    }

    public void setEspecialidad(String especialidad) {
        if (contratista == null)
            contratista = new Contratista();
        contratista.setEspecialidad(especialidad);
    }

    public String getUbicacion() {
        return contratista != null ? contratista.getUbicacion() : null;
    }

    public void setUbicacion(String ubicacion) {
        if (contratista == null)
            contratista = new Contratista();
        contratista.setUbicacion(ubicacion);
    }

    public String getDescripcion() {
        return contratista != null ? contratista.getDescripcion() : null;
    }

    public void setDescripcion(String descripcion) {
        if (contratista == null)
            contratista = new Contratista();
        contratista.setDescripcion(descripcion);
    }

    public Integer getExperiencia() {
        return contratista != null ? contratista.getExperiencia() : null;
    }

    public void setExperiencia(Integer experiencia) {
        if (contratista == null)
            contratista = new Contratista();
        contratista.setExperiencia(experiencia);
    }

    public Double getCalificacionPromedio() {
        return contratista != null ? contratista.getCalificacionPromedio() : null;
    }

    public void setCalificacionPromedio(Double calificacionPromedio) {
        if (contratista == null)
            contratista = new Contratista();
        contratista.setCalificacionPromedio(calificacionPromedio);
    }

    public String getCvUrl() {
        return contratista != null ? contratista.getCvUrl() : null;
    }

    public void setCvUrl(String cvUrl) {
        if (contratista == null)
            contratista = new Contratista();
        contratista.setCvUrl(cvUrl);
    }

    public String getEstatutosUrl() {
        return contratista != null ? contratista.getEstatutosUrl() : null;
    }

    public void setEstatutosUrl(String estatutosUrl) {
        if (contratista == null)
            contratista = new Contratista();
        contratista.setEstatutosUrl(estatutosUrl);
    }

    public String getFotoPerfilUrl() {
        return contratista != null ? contratista.getFotoPerfilUrl() : null;
    }

    public void setFotoPerfilUrl(String fotoPerfilUrl) {
        if (contratista == null)
            contratista = new Contratista();
        contratista.setFotoPerfilUrl(fotoPerfilUrl);
    }

    public String getCiudad() {
        return contratista != null ? contratista.getCiudad() : null;
    }

    public void setCiudad(String ciudad) {
        if (contratista == null)
            contratista = new Contratista();
        contratista.setCiudad(ciudad);
    }

    public String getDepartamento() {
        return contratista != null ? contratista.getDepartamento() : null;
    }

    public void setDepartamento(String departamento) {
        if (contratista == null)
            contratista = new Contratista();
        contratista.setDepartamento(departamento);
    }

    public String getMatriculaProfesional() {
        return contratista != null ? contratista.getMatriculaProfesional() : null;
    }

    public void setMatriculaProfesional(String matriculaProfesional) {
        if (contratista == null)
            contratista = new Contratista();
        contratista.setMatriculaProfesional(matriculaProfesional);
    }

    public String getMatriculaDocumentoUrl() {
        return contratista != null ? contratista.getMatriculaDocumentoUrl() : null;
    }

    public void setMatriculaDocumentoUrl(String matriculaDocumentoUrl) {
        if (contratista == null)
            contratista = new Contratista();
        contratista.setMatriculaDocumentoUrl(matriculaDocumentoUrl);
    }

    public java.util.Set<String> getSubespecialidades() {
        return contratista != null ? contratista.getSubespecialidades() : new java.util.HashSet<>();
    }

    public void setSubespecialidades(java.util.Set<String> subespecialidades) {
        if (contratista == null)
            contratista = new Contratista();
        contratista.setSubespecialidades(subespecialidades);
    }

    public String getOficio() {
        return trabajador != null ? trabajador.getOficio() : null;
    }

    public void setOficio(String oficio) {
        if (trabajador == null)
            trabajador = new Trabajador();
        trabajador.setOficio(oficio);
    }

    public Boolean getDisponibilidad() {
        return trabajador != null && Boolean.TRUE.equals(trabajador.getDisponibilidad());
    }

    public void setDisponibilidad(Boolean disponibilidad) {
        if (trabajador == null)
            trabajador = new Trabajador();
        trabajador.setDisponibilidad(disponibilidad);
    }

    @Override
    public String toString() {
        return "Perfil{id=" + id + ", username='" + username + "', role='" + getRole() + "', activo=" + activo + "}";
    }
}
