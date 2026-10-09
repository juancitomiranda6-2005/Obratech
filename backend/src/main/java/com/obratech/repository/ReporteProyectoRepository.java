package com.obratech.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.obratech.entity.ReporteProyecto;

public interface ReporteProyectoRepository extends MongoRepository<ReporteProyecto, String> {
    List<ReporteProyecto> findByContratistaIdOrderByCreadoDesc(String contratistaId);
    List<ReporteProyecto> findByProyectoIdInOrderByCreadoDesc(List<String> proyectoIds);
}