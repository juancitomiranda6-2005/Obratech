package com.obratech.util;

import org.springframework.boot.CommandLineRunner;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

import com.obratech.entity.Perfil;

@Component
public class CleanupRunner implements CommandLineRunner {

    private final MongoTemplate mongoTemplate;

    public CleanupRunner(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public void run(String... args) {
        Query query = Query.query(new Criteria().orOperator(
                Criteria.where("username").isNull(),
                Criteria.where("username").regex("^\\s*$")));
        long removed = mongoTemplate.remove(query, Perfil.class).getDeletedCount();

        if (removed > 0) {
            System.out.println("[CleanupRunner] Eliminados " + removed + " documentos sin username de `perfiles`");
        } else {
            System.out.println("[CleanupRunner] No se encontraron documentos vacos en `perfiles`");
        }
    }
}
