package com.obratech.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.DBRef;

import com.obratech.entity.enums.EstadoAsignacion;
import com.obratech.entity.enums.EstadoEjecucion;
import com.obratech.entity.enums.EstadoFaseObra;
import com.obratech.entity.enums.EstadoValidacion;
import com.obratech.entity.enums.FaseObra;

@Document(collection = "proyectos")
public class Proyecto {

    @Id
    private String id;

    private String titulo;

    private String descripcion;

    private String tipoProyecto;
    private String ubicacion;
    private Double latitud;
    private Double longitud;
    private Double presupuesto;
    private Integer plazoEstimado;
    private LocalDate fechaInicio;
    private LocalDate fechaEntrega;
    private LocalDate fechaLimitePostulacion;
    private LocalDateTime fechaLimite;

    private EstadoAsignacion estadoAsignacion = EstadoAsignacion.SIN_ASIGNAR;
    @Indexed
    private EstadoEjecucion estadoEjecucion = EstadoEjecucion.PENDIENTE;

    private FaseObra faseActual = FaseObra.PLANEACION;
    private EstadoFaseObra estadoFaseActual = EstadoFaseObra.BLOQUEADA;
    private Integer semanasEstimadasFase1 = 3;
    private Integer semanasEstimadasFase2 = 10;
    private Integer cuotasSemanaPagadas = 0;
    private Integer cuotasTotalesEsperadas = 0;
    private Boolean tareasFaseCompletadas = false;
    private Boolean aprobacionClienteFase = false;
    private Boolean limpiezaProfundaCompletada = false;
    private List<String> evidenciasFotograficas = new ArrayList<>();

    private Double areaTotal;
    private Integer numeroPisos;

    private String tipoContratacion;

    private String documentoLegalUrl;
    private String documentoLegalNombre;

    @DBRef
    @Indexed
    private Usuario cliente;

    private LocalDateTime fechaCreacion = LocalDateTime.now();

    @Indexed
    private EstadoValidacion estadoValidacion = EstadoValidacion.PENDIENTE;

    private Boolean postulacionesAbiertas = true;

    private String observaciones;

    @DBRef
    private List<Perfil> equipoTrabajo = new ArrayList<>();

    private Map<String, String> actividadesAsignadas = new HashMap<>();

    @DBRef
    @Indexed
    private Perfil contratistaAsignado;

    public Proyecto() {
    }

    public Proyecto(String titulo, String descripcion) {
        this.titulo = titulo;
        this.descripcion = descripcion;
        this.estadoAsignacion = EstadoAsignacion.SIN_ASIGNAR;
        this.estadoEjecucion = EstadoEjecucion.PENDIENTE;
        this.fechaCreacion = LocalDateTime.now();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getTipoProyecto() {
        return tipoProyecto;
    }

    public void setTipoProyecto(String tipoProyecto) {
        this.tipoProyecto = tipoProyecto;
    }

    public String getUbicacion() {
        return ubicacion;
    }

    public void setUbicacion(String ubicacion) {
        this.ubicacion = ubicacion;
    }

    public Double getLatitud() {
        return latitud;
    }

    public void setLatitud(Double latitud) {
        this.latitud = latitud;
    }

    public Double getLongitud() {
        return longitud;
    }

    public void setLongitud(Double longitud) {
        this.longitud = longitud;
    }

    public Double getPresupuesto() {
        return presupuesto;
    }

    public void setPresupuesto(Double presupuesto) {
        this.presupuesto = presupuesto;
    }

    public Integer getPlazoEstimado() {
        return plazoEstimado;
    }

    public void setPlazoEstimado(Integer plazoEstimado) {
        this.plazoEstimado = plazoEstimado;
    }

    public LocalDate getFechaInicio() {
        return fechaInicio;
    }

    public void setFechaInicio(LocalDate fechaInicio) {
        this.fechaInicio = fechaInicio;
    }

    public LocalDate getFechaEntrega() {
        return fechaEntrega;
    }

    public void setFechaEntrega(LocalDate fechaEntrega) {
        this.fechaEntrega = fechaEntrega;
    }

    public LocalDate getFechaLimitePostulacion() {
        return fechaLimitePostulacion;
    }

    public void setFechaLimitePostulacion(LocalDate fechaLimitePostulacion) {
        this.fechaLimitePostulacion = fechaLimitePostulacion;
    }

    public LocalDateTime getFechaLimite() {
        return fechaLimite;
    }

    public void setFechaLimite(LocalDateTime fechaLimite) {
        this.fechaLimite = fechaLimite;
    }

    public EstadoAsignacion getEstadoAsignacion() {
        return estadoAsignacion;
    }

    public void setEstadoAsignacion(EstadoAsignacion estadoAsignacion) {
        this.estadoAsignacion = estadoAsignacion;
    }

    public EstadoEjecucion getEstadoEjecucion() {
        return estadoEjecucion;
    }

    public void setEstadoEjecucion(EstadoEjecucion estadoEjecucion) {
        this.estadoEjecucion = estadoEjecucion;
    }

    public FaseObra getFaseActual() {
        return faseActual;
    }

    public void setFaseActual(FaseObra faseActual) {
        this.faseActual = faseActual;
    }

    public EstadoFaseObra getEstadoFaseActual() {
        return estadoFaseActual;
    }

    public void setEstadoFaseActual(EstadoFaseObra estadoFaseActual) {
        this.estadoFaseActual = estadoFaseActual;
    }

    public Integer getSemanasEstimadasFase1() {
        return semanasEstimadasFase1;
    }

    public void setSemanasEstimadasFase1(Integer semanasEstimadasFase1) {
        this.semanasEstimadasFase1 = semanasEstimadasFase1;
    }

    public Integer getSemanasEstimadasFase2() {
        return semanasEstimadasFase2;
    }

    public void setSemanasEstimadasFase2(Integer semanasEstimadasFase2) {
        this.semanasEstimadasFase2 = semanasEstimadasFase2;
    }

    public Integer getCuotasSemanaPagadas() {
        return cuotasSemanaPagadas;
    }

    public void setCuotasSemanaPagadas(Integer cuotasSemanaPagadas) {
        this.cuotasSemanaPagadas = cuotasSemanaPagadas;
    }

    public Integer getCuotasTotalesEsperadas() {
        return cuotasTotalesEsperadas;
    }

    public void setCuotasTotalesEsperadas(Integer cuotasTotalesEsperadas) {
        this.cuotasTotalesEsperadas = cuotasTotalesEsperadas;
    }

    public Boolean getTareasFaseCompletadas() {
        return tareasFaseCompletadas;
    }

    public void setTareasFaseCompletadas(Boolean tareasFaseCompletadas) {
        this.tareasFaseCompletadas = tareasFaseCompletadas;
    }

    public Boolean getAprobacionClienteFase() {
        return aprobacionClienteFase;
    }

    public void setAprobacionClienteFase(Boolean aprobacionClienteFase) {
        this.aprobacionClienteFase = aprobacionClienteFase;
    }

    public Boolean getLimpiezaProfundaCompletada() {
        return limpiezaProfundaCompletada;
    }

    public void setLimpiezaProfundaCompletada(Boolean limpiezaProfundaCompletada) {
        this.limpiezaProfundaCompletada = limpiezaProfundaCompletada;
    }

    public List<String> getEvidenciasFotograficas() {
        return evidenciasFotograficas;
    }

    public void setEvidenciasFotograficas(List<String> evidenciasFotograficas) {
        this.evidenciasFotograficas = evidenciasFotograficas == null ? new ArrayList<>() : evidenciasFotograficas;
    }

    public Double getAreaTotal() {
        return areaTotal;
    }

    public void setAreaTotal(Double areaTotal) {
        this.areaTotal = areaTotal;
    }

    public Integer getNumeroPisos() {
        return numeroPisos;
    }

    public void setNumeroPisos(Integer numeroPisos) {
        this.numeroPisos = numeroPisos;
    }

    public String getTipoContratacion() {
        return tipoContratacion;
    }

    public void setTipoContratacion(String tipoContratacion) {
        this.tipoContratacion = tipoContratacion;
    }

    public Usuario getCliente() {
        return cliente;
    }

    public void setCliente(Usuario cliente) {
        this.cliente = cliente;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(LocalDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public EstadoValidacion getEstadoValidacion() {
        return estadoValidacion;
    }

    public void setEstadoValidacion(EstadoValidacion estadoValidacion) {
        this.estadoValidacion = estadoValidacion;
    }

    public boolean isPostulacionesAbiertas() {
        return postulacionesAbiertas == null || postulacionesAbiertas;
    }

    public void setPostulacionesAbiertas(Boolean postulacionesAbiertas) {
        this.postulacionesAbiertas = postulacionesAbiertas;
    }

    public String getObservaciones() {
        return observaciones;
    }

    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
    }

    public List<Perfil> getEquipoTrabajo() {
        return equipoTrabajo;
    }

    public void setEquipoTrabajo(List<Perfil> equipoTrabajo) {
        this.equipoTrabajo = equipoTrabajo;
    }

    public Map<String, String> getActividadesAsignadas() {
        return actividadesAsignadas;
    }

    public void setActividadesAsignadas(Map<String, String> actividadesAsignadas) {
        this.actividadesAsignadas = actividadesAsignadas;
    }

    public Perfil getContratistaAsignado() {
        return contratistaAsignado;
    }

    public void setContratistaAsignado(Perfil contratistaAsignado) {
        this.contratistaAsignado = contratistaAsignado;
    }

    public String getDocumentoLegalUrl() {
        return documentoLegalUrl;
    }

    public void setDocumentoLegalUrl(String documentoLegalUrl) {
        this.documentoLegalUrl = documentoLegalUrl;
    }

    public String getDocumentoLegalNombre() {
        return documentoLegalNombre;
    }

    public void setDocumentoLegalNombre(String documentoLegalNombre) {
        this.documentoLegalNombre = documentoLegalNombre;
    }
}
