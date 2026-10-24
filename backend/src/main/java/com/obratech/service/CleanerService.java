package com.obratech.service;

import java.util.Arrays;
import java.util.List;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.mongodb.core.BulkOperations;
import org.springframework.data.mongodb.core.BulkOperations.BulkMode;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import com.obratech.entity.Perfil;

@Service
public class CleanerService implements ApplicationRunner {

    private static final String[] CLEANABLE_FIELDS = {
            "nombre", "apellido", "email", "telefono", "especialidad", "oficio"
    };

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
        Criteria[] invalidFields = Arrays.stream(CLEANABLE_FIELDS)
                .map(field -> Criteria.where(field).regex("^(?:\\s*|null|undefined)$", "i"))
                .toArray(Criteria[]::new);
        Query query = Query.query(new Criteria().orOperator(invalidFields));
        List<Perfil> list = mongoTemplate.find(query, Perfil.class);
        BulkOperations updates = mongoTemplate.bulkOps(BulkMode.UNORDERED, Perfil.class);
        int updated = 0;

        for (Perfil p : list) {
            Update update = new Update();
            boolean changed = setNullIfInvalid(update, "nombre", p.getNombre());
            changed |= setNullIfInvalid(update, "apellido", p.getApellido());
            changed |= setNullIfInvalid(update, "email", p.getEmail());
            changed |= setNullIfInvalid(update, "telefono", p.getTelefono());
            changed |= setNullIfInvalid(update, "especialidad", p.getEspecialidad());
            changed |= setNullIfInvalid(update, "oficio", p.getOficio());

            if (changed) {
                updates.updateOne(
                        Query.query(Criteria.where("id").is(p.getId())),
                        update);
                updated++;
            }
        }

        if (updated > 0) {
            updates.execute();
        }

        System.out.println("[Cleaner] Perfiles actualizados: " + updated);
    }

    private boolean setNullIfInvalid(Update update, String field, String value) {
        if (value == null || (!value.trim().isEmpty()
                && !value.equalsIgnoreCase("null")
                && !value.equalsIgnoreCase("undefined"))) {
            return false;
        }

        update.set(field, null);
        return true;
    }
}
