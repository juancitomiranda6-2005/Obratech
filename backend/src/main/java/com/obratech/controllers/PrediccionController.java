package com.obratech.controllers;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import com.obratech.dto.ml.PrediccionRetrasoRequest;
import com.obratech.dto.ml.PrediccionRetrasoResponse;
import com.obratech.dto.ml.PrediccionSeleccionRequest;
import com.obratech.dto.ml.PrediccionSeleccionResponse;
import com.obratech.entity.Perfil;
import com.obratech.entity.Proyecto;
import com.obratech.repository.PerfilRepository;
import com.obratech.repository.ProyectoRepository;
import com.obratech.service.PrediccionService;

@RestController
@RequestMapping("/api/predicciones")
public class PrediccionController {

    private final PrediccionService prediccionService;
    private final ProyectoRepository proyectoRepository;
    private final PerfilRepository perfilRepository;

    public PrediccionController(
            PrediccionService prediccionService,
            ProyectoRepository proyectoRepository,
            PerfilRepository perfilRepository) {
        this.prediccionService = prediccionService;
        this.proyectoRepository = proyectoRepository;
        this.perfilRepository = perfilRepository;
    }

    @GetMapping("/health")
    public String health() {
        // TODO: Reemplazar por un estado agregado del backend y del servicio ML.
        return "predicciones-ok";
    }

    @PostMapping(value = "/retraso", consumes = MediaType.APPLICATION_JSON_VALUE)
    public PrediccionRetrasoResponse predecirRetraso(@RequestBody PrediccionRetrasoRequest request) {
        return prediccionService.predecirRetraso(request);
    }

    @PostMapping(value = "/seleccion", consumes = MediaType.APPLICATION_JSON_VALUE)
    public PrediccionSeleccionResponse predecirSeleccion(@RequestBody PrediccionSeleccionRequest request) {
        return prediccionService.predecirSeleccion(request);
    }

    @GetMapping("/proyectos/{id}/retraso")
    public ResponseEntity<?> predecirRetrasoDeProyecto(
            @PathVariable String id,
            @RequestParam(required = false) String perfilId) {
        return proyectoRepository.findById(id)
                .<ResponseEntity<?>>map(proyecto -> ResponseEntity.ok(
                        prediccionService.predecirRetraso(proyecto, perfilId)))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).body("Proyecto no encontrado"));
    }

    @GetMapping("/proyectos/{id}/probabilidad-seleccion")
    public ResponseEntity<?> predecirSeleccionDeProyecto(
            @PathVariable String id,
            Authentication authentication) {
        if (authentication == null || authentication.getAuthorities().stream()
                .noneMatch(authority -> "ROLE_CONTRACTOR".equals(authority.getAuthority()))) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Se requiere rol CONTRACTOR");
        }
        Proyecto proyecto = proyectoRepository.findById(id).orElse(null);
        Perfil contratista = perfilRepository.findByUsernameIgnoreCase(authentication.getName()).orElse(null);
        if (proyecto == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Proyecto no encontrado");
        }
        if (contratista == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Perfil de contratista no encontrado");
        }
        return ResponseEntity.ok(prediccionService.predecirSeleccion(proyecto, contratista));
    }
}
