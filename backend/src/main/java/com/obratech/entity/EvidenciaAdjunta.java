package com.obratech.entity;

public class EvidenciaAdjunta {

    private String fileId;
    private String filename;
    private String contentType;
    private String reporteTrabajadorId;

    public EvidenciaAdjunta() {
    }

    public EvidenciaAdjunta(String fileId, String filename, String contentType, String reporteTrabajadorId) {
        this.fileId = fileId;
        this.filename = filename;
        this.contentType = contentType;
        this.reporteTrabajadorId = reporteTrabajadorId;
    }

    public String getFileId() {
        return fileId;
    }

    public void setFileId(String fileId) {
        this.fileId = fileId;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getReporteTrabajadorId() {
        return reporteTrabajadorId;
    }

    public void setReporteTrabajadorId(String reporteTrabajadorId) {
        this.reporteTrabajadorId = reporteTrabajadorId;
    }
}