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

    @DBRef
    @Indexed
    private Perfil trabajador;

    @DBRef
    @Indexed
    private Perfil contratista;

    private String estado = "PENDIENTE"; // PENDIENTE, ACEPTADA, RECHAZADA

    private LocalDateTime fechaCreacion = LocalDateTime.now();

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
}
