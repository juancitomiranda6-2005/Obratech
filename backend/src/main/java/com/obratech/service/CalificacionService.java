package com.obratech.service;

import com.obratech.entity.Calificacion;
import com.obratech.repository.CalificacionRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CalificacionService {

    private final CalificacionRepository repo;

    public CalificacionService(CalificacionRepository repo) {
        this.repo = repo;
    }

    public List<Calificacion> findAll() { return repo.findAll(); }

    public Optional<Calificacion> findById(String id) { 
        @SuppressWarnings("null")
        String safeId = id != null ? id : "";
        return repo.findById(safeId); 
    }

    public Calificacion save(Calificacion c) { 
        @SuppressWarnings("null")
        Calificacion safeC = c;
        return repo.save(safeC); 
    }

    public void deleteById(String id) { 
        @SuppressWarnings("null")
        String safeId = id != null ? id : "";
        repo.deleteById(safeId); 
    }
}
