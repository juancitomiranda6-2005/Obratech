package com.obratech.entity;

import java.io.Serializable;
import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "usuarios")
public class Usuario implements Serializable {

    @Id
    private String id;

    @Indexed
    private String username;

    private String password;

    private java.util.Set<String> roles = new java.util.HashSet<>(java.util.Collections.singleton("ROLE_USER"));

    private boolean activo = true;

    private boolean verificado = false;

    private int intentosFallidos = 0;

    private LocalDateTime bloqueadoHasta;

    private LocalDateTime creado;

    private LocalDateTime ultimoAcceso;

    public Usuario() {
        if (this.creado == null) {
            this.creado = LocalDateTime.now();
        }
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public java.util.Set<String> getRoles() {
        return roles;
    }

    public void setRoles(java.util.Set<String> roles) {
        this.roles = roles;
    }

    public String getRole() {
        if (roles == null || roles.isEmpty())
            return "ROLE_USER";
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

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }

    public boolean isVerificado() {
        return verificado;
    }

    public void setVerificado(boolean verificado) {
        this.verificado = verificado;
    }

    public int getIntentosFallidos() {
        return intentosFallidos;
    }

    public void setIntentosFallidos(int intentosFallidos) {
        this.intentosFallidos = intentosFallidos;
    }

    public LocalDateTime getBloqueadoHasta() {
        return bloqueadoHasta;
    }

    public void setBloqueadoHasta(LocalDateTime bloqueadoHasta) {
        this.bloqueadoHasta = bloqueadoHasta;
    }

    public LocalDateTime getCreado() {
        return creado;
    }

    public void setCreado(LocalDateTime creado) {
        this.creado = creado;
    }

    public LocalDateTime getUltimoAcceso() {
        return ultimoAcceso;
    }

    public void setUltimoAcceso(LocalDateTime ultimoAcceso) {
        this.ultimoAcceso = ultimoAcceso;
    }

    @Override
    public String toString() {
        return "Usuario{id=" + id + ", username='" + username + "', role='" + getRole() + "', activo=" + activo
                + ", verificado=" + verificado + "}";
    }
}
