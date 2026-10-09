package com.obratech.service;

import java.util.List;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

import com.obratech.entity.Perfil;

@Service
public class CleanerService implements ApplicationRunner {

    private final MongoTemplate mongoTemplate;

    public CleanerService(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        System.out.println("[Cleaner] iniciando limpieza...");
        cleanPerfiles();
        System.out.println("[Cleaner] limpieza completada");
    }

    private void cleanPerfiles() {
        List<Perfil> list = mongoTemplate.findAll(Perfil.class);
        int updated = 0;

        for (Perfil p : list) {
            boolean changed = false;

            if (isInvalid(p.getNombre()))     { p.setNombre(null);     changed = true; }
            if (isInvalid(p.getApellido()))   { p.setApellido(null);   changed = true; }
            if (isInvalid(p.getEmail()))      { p.setEmail(null);      changed = true; }
            if (isInvalid(p.getTelefono()))   { p.setTelefono(null);   changed = true; }
            if (isInvalid(p.getEspecialidad())) { p.setEspecialidad(null); changed = true; }
            if (isInvalid(p.getOficio()))     { p.setOficio(null);     changed = true; }

            if (changed) {
                mongoTemplate.save(p);
                updated++;
            }
        }

        System.out.println("[Cleaner] Perfiles actualizados: " + updated);
    }

    private boolean isInvalid(String value) {
        return value == null ||
               value.trim().isEmpty() ||
               value.equalsIgnoreCase("null") ||
               value.equalsIgnoreCase("undefined");
    }
}
