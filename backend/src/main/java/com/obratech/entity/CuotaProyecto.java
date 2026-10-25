package com.obratech.entity;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import com.obratech.entity.enums.FaseObra;

@Document(collection = "cuotas_proyecto")
@CompoundIndex(name = "project_phase_installment_unique", def = "{'proyectoId': 1, 'fase': 1, 'numeroCuota': 1}", unique = true)
public class CuotaProyecto {

    @Id
    private String id;

    private String proyectoId;
    private FaseObra fase;
    private int numeroCuota;
    private double monto;
    private String registradoPor;
    private LocalDateTime registradoEn = LocalDateTime.now();

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

    public FaseObra getFase() {
        return fase;
    }

    public void setFase(FaseObra fase) {
        this.fase = fase;
    }

    public int getNumeroCuota() {
        return numeroCuota;
    }

    public void setNumeroCuota(int numeroCuota) {
        this.numeroCuota = numeroCuota;
    }

    public double getMonto() {
        return monto;
    }

    public void setMonto(double monto) {
        this.monto = monto;
    }

    public String getRegistradoPor() {
        return registradoPor;
    }

    public void setRegistradoPor(String registradoPor) {
        this.registradoPor = registradoPor;
    }

    public LocalDateTime getRegistradoEn() {
        return registradoEn;
    }

    public void setRegistradoEn(LocalDateTime registradoEn) {
        this.registradoEn = registradoEn;
    }
}
