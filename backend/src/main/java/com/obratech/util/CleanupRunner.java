package com.obratech.util;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.obratech.entity.Perfil;
import com.obratech.repository.PerfilRepository;

import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Component
public class CleanupRunner implements CommandLineRunner {

    private final PerfilRepository perfilRepository;

    public CleanupRunner(PerfilRepository perfilRepository) {
        this.perfilRepository = perfilRepository;
    }

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        List<Perfil> perfiles = perfilRepository.findAll();
        long removed = perfiles.stream()
                .filter(p -> p.getUsername() == null || p.getUsername().trim().isEmpty())
                .peek(p -> {
                    String id = p.getId();
                    if (id != null)
                        perfilRepository.deleteById(id);
                })
                .count();

        if (removed > 0) {
            System.out.println("[CleanupRunner] Eliminados " + removed + " documentos sin username de `perfiles`");
        } else {
            System.out.println("[CleanupRunner] No se encontraron documentos vacos en `perfiles`");
        }
    }
}
