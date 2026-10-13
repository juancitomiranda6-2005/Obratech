package com.obratech.entity;
// COMO SERIA MEJOR ESTRUCTURAR ESTO ENVIANDOLE UNA  NOTIFICACION Y UNA VENTANITA EMERGENTE COMO LA QUE TIENE AHORA MISMO 
import java.time.LocalDateTime;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.DBRef;

@Document(collection = "invitaciones_trabajo")
public class InvitacionTrabajo {

    @Id
    private String id;

    @DBRef
    @Indexed
    private Proyecto proyecto;

    private String proyectoId;

    private String proyectoTitulo;

    private String proyectoUbicacion;

    private String proyectoDescripcion;

    private String clientePerfilId;

    private String contratistaPerfilId;

    private Double pagoOfrecido;

    @DBRef
    @Indexed
    private Perfil trabajador;

    @DBRef
    @Indexed
    private Perfil contratista;

    private String estado = "PENDIENTE"; // PENDIENTE, ACEPTADA, RECHAZADA

    private LocalDateTime fechaCreacion = LocalDateTime.now();

    private LocalDateTime fechaExpiracion;

    public InvitacionTrabajo() {}

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Proyecto getProyecto() {
        return proyecto;
    }

    public void setProyecto(Proyecto proyecto) {
        this.proyecto = proyecto;
    }

    public String getProyectoId() {
        return proyectoId != null ? proyectoId : proyecto == null ? null : proyecto.getId();
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

    public String getProyectoUbicacion() {
        return proyectoUbicacion;
    }

    public void setProyectoUbicacion(String proyectoUbicacion) {
        this.proyectoUbicacion = proyectoUbicacion;
    }

    public String getProyectoDescripcion() {
        return proyectoDescripcion;
    }

    public void setProyectoDescripcion(String proyectoDescripcion) {
        this.proyectoDescripcion = proyectoDescripcion;
    }

    public String getClientePerfilId() {
        return clientePerfilId;
    }

    public void setClientePerfilId(String clientePerfilId) {
        this.clientePerfilId = clientePerfilId;
    }

    public String getContratistaPerfilId() {
        return contratistaPerfilId != null ? contratistaPerfilId : contratista == null ? null : contratista.getId();
    }

    public void setContratistaPerfilId(String contratistaPerfilId) {
        this.contratistaPerfilId = contratistaPerfilId;
    }

    public Double getPagoOfrecido() {
        return pagoOfrecido;
    }

    public void setPagoOfrecido(Double pagoOfrecido) {
        this.pagoOfrecido = pagoOfrecido;
    }

    public Perfil getTrabajador() {
        return trabajador;
    }

    public void setTrabajador(Perfil trabajador) {
        this.trabajador = trabajador;
    }

    public Perfil getContratista() {
        return contratista;
    }

    public void setContratista(Perfil contratista) {
        this.contratista = contratista;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(LocalDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public LocalDateTime getFechaExpiracion() {
        return fechaExpiracion;
    }

    public void setFechaExpiracion(LocalDateTime fechaExpiracion) {
        this.fechaExpiracion = fechaExpiracion;
    }
}
