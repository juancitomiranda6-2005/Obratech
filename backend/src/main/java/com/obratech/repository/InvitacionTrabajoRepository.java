package com.obratech.repository;

import com.obratech.entity.InvitacionTrabajo;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface InvitacionTrabajoRepository extends MongoRepository<InvitacionTrabajo, String> {
    List<InvitacionTrabajo> findByTrabajadorIdAndEstado(String trabajadorId, String estado);
    List<InvitacionTrabajo> findByContratistaIdAndEstado(String contratistaId, String estado);
    List<InvitacionTrabajo> findByProyectoIdAndEstado(String proyectoId, String estado);
    List<InvitacionTrabajo> findByContratistaId(String contratistaId);
    List<InvitacionTrabajo> findByTrabajadorId(String trabajadorId);
    List<InvitacionTrabajo> findByProyectoId(String proyectoId);
}
