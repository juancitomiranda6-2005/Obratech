package com.obratech.controllers;


import java.util.List;
import java.util.Optional;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.obratech.entity.Calificacion;
import com.obratech.entity.Perfil;
import com.obratech.entity.Proyecto;
import com.obratech.service.CalificacionService;
import com.obratech.service.PerfilService;
import com.obratech.service.ProyectoService;

@RestController
@RequestMapping("/api")
public class ApiController {

    private final PerfilService perfilService;
    private final ProyectoService proyectoService;
    private final CalificacionService calificacionService;

    public ApiController(
            PerfilService perfilService,
            ProyectoService proyectoService,
            CalificacionService calificacionService) {

        this.perfilService = perfilService;
        this.proyectoService = proyectoService;
        this.calificacionService = calificacionService;
    }


    @GetMapping("/perfiles")
    public List<Perfil> listPerfiles() {
        return perfilService.findAll();
    }

    @GetMapping("/perfiles/{id}")
    public ResponseEntity<Perfil> getPerfil(@PathVariable String id) {

        Optional<Perfil> perfilOpt = perfilService.findById(id);

        return perfilOpt
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/perfiles")
    public ResponseEntity<Perfil> crearPerfil(@RequestBody Perfil perfil) {

        if (perfil == null) {
            return ResponseEntity.badRequest().build();
        }

        Perfil saved = perfilService.save(perfil);

        if (saved == null || saved.getId() == null) {
            return ResponseEntity.badRequest().build();
        }

        return ResponseEntity
                .created(java.net.URI.create("/api/perfiles/" + saved.getId()))
                .body(saved);
    }

    @PutMapping("/perfiles/{id}")
    public ResponseEntity<Perfil> updatePerfil(
            @PathVariable String id,
            @RequestBody Perfil perfil) {

        if (perfil == null) {
            return ResponseEntity.badRequest().build();
        }

        Perfil updated = perfilService.update(id, perfil);

        if (updated == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/perfiles/{id}")
    public ResponseEntity<Void> deletePerfil(@PathVariable String id) {

        Optional<Perfil> existing = perfilService.findById(id);

        if (existing.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        perfilService.deleteById(id);

        return ResponseEntity.noContent().build();
    }


    @GetMapping("/proyectos")
    public List<Proyecto> listProyectos() {
        return proyectoService.findAll();
    }

    @GetMapping("/proyectos/{id}")
    public ResponseEntity<Proyecto> getProyecto(@PathVariable String id) {

        Optional<Proyecto> proyectoOpt = proyectoService.findById(id);

        return proyectoOpt
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/proyectos")
    public ResponseEntity<Proyecto> crearProyecto(@RequestBody Proyecto proyecto) {

        if (proyecto == null) {
            return ResponseEntity.badRequest().build();
        }

        Proyecto saved = proyectoService.save(proyecto);

        if (saved == null || saved.getId() == null) {
            return ResponseEntity.badRequest().build();
        }

        return ResponseEntity
                .created(java.net.URI.create("/api/proyectos/" + saved.getId()))
                .body(saved);
    }

    @PutMapping("/proyectos/{id}")
    public ResponseEntity<Proyecto> updateProyecto(
            @PathVariable String id,
            @RequestBody Proyecto proyecto) {

        if (proyecto == null) {
            return ResponseEntity.badRequest().build();
        }

        Proyecto updated = proyectoService.update(id, proyecto);

        if (updated == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/proyectos/{id}")
    public ResponseEntity<Void> deleteProyecto(@PathVariable String id) {

        Optional<Proyecto> existing = proyectoService.findById(id);

        if (existing.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        proyectoService.deleteById(id);

        return ResponseEntity.noContent().build();
    }


    @GetMapping("/calificaciones")
    public List<Calificacion> listCalificaciones() {
        return calificacionService.findAll();
    }

    @PostMapping("/calificaciones")
    public ResponseEntity<Calificacion> crearCalificacion(
            @RequestBody Calificacion calificacion) {

        if (calificacion == null) {
            return ResponseEntity.badRequest().build();
        }

        Calificacion saved = calificacionService.save(calificacion);

        if (saved == null || saved.getId() == null) {
            return ResponseEntity.badRequest().build();
        }

        return ResponseEntity
                .created(java.net.URI.create("/api/calificaciones/" + saved.getId()))
                .body(saved);
    }


    @PostMapping("/proyectos/{proyectoId}/asignar/{perfilId}")
    public ResponseEntity<Proyecto> asignarContratista(
            @PathVariable String proyectoId,
            @PathVariable String perfilId) {

        Optional<Proyecto> proyectoOpt =
                proyectoService.findById(proyectoId);

        Optional<Perfil> perfilOpt =
                perfilService.findById(perfilId);

        if (proyectoOpt.isEmpty() || perfilOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Proyecto proyecto = proyectoOpt.get();
        Perfil contratista = perfilOpt.get();

        if (proyecto.getContratistaAsignado() != null) {
            return ResponseEntity.badRequest().build();
        }

        List<Proyecto> proyectosActivos =
                proyectoService.findByContratistaAsignadoId(
                        contratista.getId());

        if (proyectosActivos != null
                && proyectosActivos.size() >= 5) {

            return ResponseEntity.badRequest().build();
        }

        proyecto.setContratistaAsignado(contratista);

        Proyecto saved = proyectoService.save(proyecto);

        return ResponseEntity.ok(saved);
    }
}