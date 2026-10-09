package com.obratech.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.obratech.entity.ReporteAvanceTrabajador;

public interface ReporteAvanceTrabajadorRepository extends MongoRepository<ReporteAvanceTrabajador, String> {
    List<ReporteAvanceTrabajador> findByProyectoIdOrderByCreadoDesc(String proyectoId);
    List<ReporteAvanceTrabajador> findByProyectoIdInOrderByCreadoDesc(List<String> proyectoIds);
    List<ReporteAvanceTrabajador> findByEquipoIdAndTrabajadorIdOrderByCreadoDesc(String equipoId, String trabajadorId);
    Optional<ReporteAvanceTrabajador> findByIdAndEquipoId(String id, String equipoId);
    Optional<ReporteAvanceTrabajador> findByIdAndEquipoIdAndTrabajadorId(String id, String equipoId, String trabajadorId);
}