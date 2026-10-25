package com.obratech.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.obratech.entity.CuotaProyecto;

public interface CuotaProyectoRepository extends MongoRepository<CuotaProyecto, String> {
    List<CuotaProyecto> findByProyectoId(String proyectoId);
}
