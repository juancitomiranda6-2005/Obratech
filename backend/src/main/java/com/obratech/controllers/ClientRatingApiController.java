package com.obratech.controllers;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.obratech.entity.Calificacion;
import com.obratech.entity.Perfil;
import com.obratech.entity.Proyecto;
import com.obratech.entity.Usuario;
import com.obratech.repository.CalificacionRepository;
import com.obratech.repository.PerfilRepository;
import com.obratech.repository.ProyectoRepository;
import com.obratech.repository.UsuarioRepository;

@RestController
@RequestMapping("/api/client/ratings")
public class ClientRatingApiController {

    private final CalificacionRepository calificacionRepository;
    private final PerfilRepository perfilRepository;
    private final ProyectoRepository proyectoRepository;
    private final UsuarioRepository usuarioRepository;

    public ClientRatingApiController(
            CalificacionRepository calificacionRepository,
            PerfilRepository perfilRepository,
            ProyectoRepository proyectoRepository,
            UsuarioRepository usuarioRepository) {
        this.calificacionRepository = calificacionRepository;
        this.perfilRepository = perfilRepository;
        this.proyectoRepository = proyectoRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @GetMapping("/{projectId}/{contractorId}")
    public ResponseEntity<?> getRating(
            @PathVariable String projectId,
            @PathVariable String contractorId,
            Authentication authentication) {
        RatingContext context = findContext(projectId, contractorId, authentication.getName());
        if (context == null) {
            return ResponseEntity.notFound().build();
        }

        Calificacion existing = findOwnedRating(projectId, contractorId, context.usuario().getId());
        return ResponseEntity.ok(toRatingResponse(context.proyecto(), context.contratista(), existing));
    }

    @GetMapping("/project/{projectId}")
    public ResponseEntity<?> getProjectRating(
            @PathVariable String projectId,
            Authentication authentication) {
        Usuario usuario = usuarioRepository.findByUsernameIgnoreCase(authentication.getName()).orElse(null);
        Proyecto proyecto = proyectoRepository.findById(projectId).orElse(null);
        if (usuario == null || proyecto == null || proyecto.getCliente() == null
                || !usuario.getId().equals(proyecto.getCliente().getId())
                || proyecto.getContratistaAsignado() == null) {
            return ResponseEntity.notFound().build();
        }
        Perfil contratista = proyecto.getContratistaAsignado();
        Calificacion existing = findOwnedRating(projectId, contratista.getId(), usuario.getId());
        return ResponseEntity.ok(toRatingResponse(proyecto, contratista, existing));
    }

    @GetMapping("/by-id/{ratingId}")
    public ResponseEntity<?> getOwnedRatingById(
            @PathVariable String ratingId,
            Authentication authentication) {
        Usuario usuario = usuarioRepository.findByUsernameIgnoreCase(authentication.getName()).orElse(null);
        Calificacion rating = calificacionRepository.findById(ratingId).orElse(null);
        if (usuario == null || rating == null || rating.getProyecto() == null || rating.getContratista() == null
                || rating.getCliente() == null || !usuario.getId().equals(rating.getCliente().getId())) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(toRatingResponse(rating.getProyecto(), rating.getContratista(), rating));
    }

    @PutMapping("/{projectId}/{contractorId}")
    public ResponseEntity<?> saveRating(
            @PathVariable String projectId,
            @PathVariable String contractorId,
            @RequestBody RatingRequest request,
            Authentication authentication) {
        if (request == null || request.puntuacion() == null || request.puntuacion() < 1 || request.puntuacion() > 5) {
            return ResponseEntity.badRequest().body(Map.of("error", "La puntuación debe estar entre 1 y 5."));
        }

        RatingContext context = findContext(projectId, contractorId, authentication.getName());
        if (context == null) {
            return ResponseEntity.notFound().build();
        }

        List<Calificacion> matching = calificacionRepository.findByProyectoIdAndContratistaId(projectId, contractorId);
        Calificacion rating = matching.stream()
                .filter(item -> item.getCliente() != null && context.usuario().getId().equals(item.getCliente().getId()))
                .findFirst()
                .orElseGet(() -> new Calificacion());
        if (matching.stream().anyMatch(item -> item.getCliente() == null
                || !context.usuario().getId().equals(item.getCliente().getId()))) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "Ya existe una calificación para este proyecto y contratista."));
        }

        rating.setProyecto(context.proyecto());
        rating.setContratista(context.contratista());
        rating.setCliente(context.usuario());
        rating.setPuntuacion(request.puntuacion());
        rating.setComentario(request.comentario() == null ? "" : request.comentario().trim());
        if (rating.getFecha() == null) {
            rating.setFecha(LocalDateTime.now());
        }
        Calificacion saved = calificacionRepository.save(rating);
        List<Calificacion> contractorRatings = calificacionRepository.findByContratistaId(contractorId);
        double average = contractorRatings.stream().mapToInt(Calificacion::getPuntuacion).average().orElse(0.0);
        context.contratista().setCalificacionPromedio(average);
        perfilRepository.save(context.contratista());
        return ResponseEntity.ok(toRatingResponse(context.proyecto(), context.contratista(), saved));
    }

    private RatingContext findContext(String projectId, String contractorId, String username) {
        Usuario usuario = usuarioRepository.findByUsernameIgnoreCase(username).orElse(null);
        Proyecto proyecto = proyectoRepository.findById(projectId).orElse(null);
        Perfil contratista = perfilRepository.findById(contractorId).orElse(null);
        if (usuario == null || proyecto == null || contratista == null
            || proyecto.getCliente() == null || proyecto.getCliente().getId() == null
            || !usuario.getId().equals(proyecto.getCliente().getId())
                || proyecto.getContratistaAsignado() == null
                || !contractorId.equals(proyecto.getContratistaAsignado().getId())) {
            return null;
        }
        return new RatingContext(usuario, proyecto, contratista);
    }

    private Calificacion findOwnedRating(String projectId, String contractorId, String clientId) {
        return calificacionRepository.findByProyectoIdAndContratistaId(projectId, contractorId).stream()
                .filter(item -> item.getCliente() != null && clientId.equals(item.getCliente().getId()))
                .findFirst()
                .orElse(null);
    }

    private RatingResponse toRatingResponse(Proyecto proyecto, Perfil contratista, Calificacion rating) {
        return new RatingResponse(
                proyecto.getId(), proyecto.getTitulo(),
                contratista.getId(), contratista.getNombre(), contratista.getEspecialidad(),
                rating == null ? null : rating.getPuntuacion(),
                rating == null ? "" : rating.getComentario());
    }

    private record RatingContext(Usuario usuario, Proyecto proyecto, Perfil contratista) {}

    public record RatingRequest(Integer puntuacion, String comentario) {}

    public record RatingResponse(
            String projectId,
            String projectTitle,
            String contractorId,
            String contractorName,
            String specialty,
            Integer puntuacion,
            String comentario) {}
}
