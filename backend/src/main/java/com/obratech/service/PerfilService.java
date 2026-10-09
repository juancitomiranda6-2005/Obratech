package com.obratech.service;

import com.obratech.entity.Perfil;
import com.obratech.repository.PerfilRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class PerfilService {

    private final PerfilRepository repo;

    public PerfilService(PerfilRepository repo) {
        this.repo = repo;
    }

    public List<Perfil> findAll() {
        return repo.findAll();
    }

    public Optional<Perfil> findById(String id) {
        @SuppressWarnings("null")
        String safeId = id != null ? id : "";
        return repo.findById(safeId);
    }

    public Perfil update(String id, Perfil p) {
        @SuppressWarnings("null")
        String safeId = id != null ? id : "";
        Optional<Perfil> existing = repo.findById(safeId);
        if (existing.isPresent()) {
            p.setId(id);
            @SuppressWarnings("null")
            Perfil safeP = p;
            return repo.save(safeP);
        }
        return null;
    }

    public Perfil save(Perfil p) {
        @SuppressWarnings("null")
        Perfil safeP = p;
        return repo.save(safeP);
    }

    public void deleteById(String id) {
        @SuppressWarnings("null")
        String safeId = id != null ? id : "";
        repo.deleteById(safeId);
    }
}
