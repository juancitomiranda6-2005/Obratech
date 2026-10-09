package com.obratech.repository;

import com.obratech.entity.Calificacion;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface CalificacionRepository extends MongoRepository<Calificacion, String> {
    List<Calificacion> findByContratistaId(String contratistaId);
    long countByClienteId(String clienteId);
    boolean existsByProyectoIdAndContratistaId(String proyectoId, String contratistaId);
    List<Calificacion> findByProyectoIdAndContratistaId(String proyectoId, String contratistaId);
}
