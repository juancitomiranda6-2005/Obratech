package com.obratech.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.obratech.entity.Postulacion;
import com.obratech.entity.Proyecto;

public interface PostulacionRepository extends MongoRepository<Postulacion, String> {
    List<Postulacion> findByProyectoId(String proyectoId);
    List<Postulacion> findByProyectoIn(List<Proyecto> proyectos);
    List<Postulacion> findByUsuarioId(String usuarioId);
    boolean existsByProyectoIdAndUsuarioId(String id, String usuarioId);
}
