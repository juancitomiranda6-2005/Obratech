package com.obratech.repository;

import com.obratech.entity.EquipoTrabajo;
import com.obratech.entity.Perfil;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface EquipoTrabajoRepository extends MongoRepository<EquipoTrabajo, String> {
    List<EquipoTrabajo> findByProyectoId(String proyectoId);
    List<EquipoTrabajo> findByIntegrantesContaining(Perfil trabajador);
}
