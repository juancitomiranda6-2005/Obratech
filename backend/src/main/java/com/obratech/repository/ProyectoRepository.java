package com.obratech.repository;

import com.obratech.entity.Proyecto;
import com.obratech.entity.Perfil;
import com.obratech.entity.enums.EstadoEjecucion;
import com.obratech.entity.enums.EstadoValidacion;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import java.time.LocalDate;
import java.util.List;

public interface ProyectoRepository extends MongoRepository<Proyecto, String> {
    List<Proyecto> findByClienteId(String clienteId);

    List<Proyecto> findByEquipoTrabajoContaining(Perfil trabajador);

    long countByClienteId(String clienteId);

    long countByClienteIdAndEstadoEjecucion(String clienteId, EstadoEjecucion estadoEjecucion);

    long countByClienteIdAndContratistaAsignadoIsNotNull(String clienteId);

    List<Proyecto> findByFechaLimitePostulacionIsNullOrFechaLimitePostulacionGreaterThanEqual(LocalDate date);

    List<Proyecto> findByContratistaAsignadoId(String contratistaId);

    List<Proyecto> findByContratistaAsignadoIdAndEstadoEjecucionNot(String contratistaId,
            EstadoEjecucion estadoEjecucion);

    List<Proyecto> findByContratistaAsignadoIdAndEstadoEjecucion(String contratistaId, EstadoEjecucion estadoEjecucion);

    long countByContratistaAsignadoIdAndEstadoEjecucion(String contratistaId, EstadoEjecucion estadoEjecucion);

    long countByContratistaAsignadoIdAndEstadoEjecucionNot(String contratistaId, EstadoEjecucion estadoEjecucion);

    List<Proyecto> findByClienteIdAndContratistaAsignadoIsNotNull(String clienteId);

    List<Proyecto> findByClienteIdAndEstadoEjecucion(String clienteId, EstadoEjecucion estadoEjecucion);

    List<Proyecto> findByClienteIdAndEstadoValidacion(String clienteId, EstadoValidacion estadoValidacion);

    List<Proyecto> findByEstadoValidacion(EstadoValidacion estadoValidacion);
}
