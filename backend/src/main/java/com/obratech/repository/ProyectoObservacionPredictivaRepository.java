package com.obratech.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.obratech.entity.ProyectoObservacionPredictiva;

public interface ProyectoObservacionPredictivaRepository
        extends MongoRepository<ProyectoObservacionPredictiva, String> {

    List<ProyectoObservacionPredictiva> findByProyectoIdOrderByObservadoEnAsc(String proyectoId);
}
