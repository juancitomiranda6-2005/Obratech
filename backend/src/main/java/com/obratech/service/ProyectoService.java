package com.obratech.service;

import com.obratech.entity.HistorialProyecto;
import com.obratech.entity.Proyecto;
import com.obratech.entity.Usuario;
import com.obratech.entity.enums.EstadoAsignacion;
import com.obratech.entity.enums.EstadoEjecucion;
import com.obratech.entity.enums.EstadoValidacion;
import com.obratech.repository.HistorialProyectoRepository;
import com.obratech.repository.ProyectoRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class ProyectoService {

    private final ProyectoRepository repo;
    private final HistorialProyectoRepository historialRepo;
    @Autowired
    private ProyectoRepository proyectoRepository;

    public ProyectoService(ProyectoRepository repo, HistorialProyectoRepository historialRepo) {
        this.repo = repo;
        this.historialRepo = historialRepo;
    }

    public List<Proyecto> findAll() {
        return repo.findAll();
    }

    public Optional<Proyecto> findById(String id) {
        if (id == null)
            return Optional.empty();
        return repo.findById(id);
    }

    public Proyecto publicarProyecto(Proyecto proyecto, Usuario cliente) {
        proyecto.setCliente(cliente);
        proyecto.setEstadoAsignacion(EstadoAsignacion.SIN_ASIGNAR);
        proyecto.setEstadoEjecucion(EstadoEjecucion.PENDIENTE);
        proyecto.setEstadoValidacion(EstadoValidacion.PENDIENTE);
        proyecto.setFechaCreacion(LocalDateTime.now());

        Proyecto guardado = repo.save(proyecto);

        HistorialProyecto historial = new HistorialProyecto(
                null,
                EstadoEjecucion.PENDIENTE,
                guardado);
        historialRepo.save(historial);

        return guardado;
    }

    public void cambiarEstado(Proyecto proyecto, EstadoEjecucion nuevoEstado) {
        EstadoEjecucion anterior = proyecto.getEstadoEjecucion();
        proyecto.setEstadoEjecucion(nuevoEstado);
        repo.save(proyecto);

        historialRepo.save(
                new HistorialProyecto(anterior, nuevoEstado, proyecto));
    }

    public Proyecto update(String id, Proyecto proyecto) {
        if (id == null || proyecto == null)
            return null;
        Optional<Proyecto> existente = repo.findById(id);
        if (existente.isPresent()) {
            proyecto.setId(id);
            return repo.save(proyecto);
        }
        return null;
    }

    public Proyecto save(Proyecto proyecto) {
        if (proyecto == null)
            return null;
        return repo.save(proyecto);
    }

    public void deleteById(String id) {
        if (id != null)
            repo.deleteById(id);
    }

    public List<Proyecto> findByContratistaAsignadoId(String contratistaId) {
        return proyectoRepository.findByContratistaAsignadoId(contratistaId);
    }
}
