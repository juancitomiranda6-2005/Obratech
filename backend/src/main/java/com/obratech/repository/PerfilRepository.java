package com.obratech.repository;

import com.obratech.entity.Perfil;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;
import java.util.Optional;

public interface PerfilRepository extends MongoRepository<Perfil, String> {

    Optional<Perfil> findByUsername(String username);

    Optional<Perfil> findByUsernameIgnoreCase(String username);

    Optional<Perfil> findByEmailIgnoreCase(String email);

    @Query("{ 'roles': ?0 }")
    List<Perfil> findByRoles(String rol);

    @Query("{ 'roles': ?0, 'activo': { $ne: false } }")
    List<Perfil> findByRolesAndActivoTrue(String rol);

    @Query("{ 'roles': ?0, $or: [{ 'verificado': false }, { 'verificado': { $exists: false } }] }")
    List<Perfil> findByRolesAndVerificadoFalse(String rol);

    @Query("{ 'detallesContratista.especialidad': { $regex: ?0, $options: 'i' }, 'roles': ?1 }")
    List<Perfil> findByEspecialidadContainingIgnoreCaseAndRoles(String especialidad, String rol);

    @Query("{ $or: [ { 'detallesTrabajador.disponibilidad': true }, { 'detallesTrabajador.disponibilidad': { $exists: false } } ], 'roles': ?0 }")
    List<Perfil> findByDisponibilidadTrueAndRoles(String rol);

    @Query("{ 'roles': ?0, 'activo': false }")
    List<Perfil> findByRolesAndActivoFalse(String rol);

    @Query(value = "{ 'roles': ?0, $or: [{ 'activo': false }, { 'activo': { $exists: false } }] }", count = true)
    long countByRolesAndActivoFalse(String rol);
}
