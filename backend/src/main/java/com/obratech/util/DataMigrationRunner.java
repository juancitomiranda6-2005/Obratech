package com.obratech.util;

import org.springframework.boot.CommandLineRunner;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;
import org.bson.Document;

import java.util.List;

@Component
public class DataMigrationRunner implements CommandLineRunner {

    private final MongoTemplate mongoTemplate;

    public DataMigrationRunner(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public void run(String... args) throws Exception {
        System.out.println("--- Iniciando Migración de Datos a 'perfiles' y Saneamiento ---");

        migrarColeccion("trabajador");
        migrarColeccion("contratista");
        migrarColeccion("cliente");
        migrarColeccion("clientes");
        migrarColeccion("contratistas");
        migrarColeccion("trabajadores");
        migrarColeccion("personas");

        sanearColeccionUsuarios();

        System.out.println("--- Migración y Saneamiento Completados ---");
    }

    private void migrarColeccion(String oldCollectionName) {
        if (mongoTemplate.collectionExists(oldCollectionName != null ? oldCollectionName : "")) {
            List<Document> documentos = mongoTemplate.findAll(Document.class, oldCollectionName != null ? oldCollectionName : "");
            if (!documentos.isEmpty()) {
                int migrados = 0;
                for (Document doc : documentos) {
                    doc.put("_class", "com.obratech.entity.Perfil");

                    if (doc.containsKey("role") && doc.get("role") instanceof String) {
                        java.util.List<String> rolesList = new java.util.ArrayList<>();
                        rolesList.add(doc.getString("role"));
                        doc.put("roles", rolesList);
                        doc.remove("role");
                    } else if (!doc.containsKey("roles")) {
                        java.util.List<String> rolesList = new java.util.ArrayList<>();
                        rolesList.add("ROLE_USER");
                        doc.put("roles", rolesList);
                    }

                    doc.put("activo", true);
                    if (!doc.containsKey("verificado")) {
                        doc.put("verificado", false);
                    }

                    
                    if (doc.containsKey("empresa")) {
                        Document detallesCliente = new Document();
                        detallesCliente.put("empresa", doc.get("empresa"));
                        doc.put("detallesCliente", detallesCliente);
                        doc.remove("empresa");
                    }
                    
                    if (doc.containsKey("especialidad") || doc.containsKey("ubicacion")) {
                        Document detallesContratista = new Document();
                        if (doc.containsKey("especialidad")) detallesContratista.put("especialidad", doc.remove("especialidad"));
                        if (doc.containsKey("ubicacion")) detallesContratista.put("ubicacion", doc.remove("ubicacion"));
                        if (doc.containsKey("descripcion")) detallesContratista.put("descripcion", doc.remove("descripcion"));
                        if (doc.containsKey("experiencia")) detallesContratista.put("experiencia", doc.remove("experiencia"));
                        if (doc.containsKey("calificacionPromedio")) detallesContratista.put("calificacionPromedio", doc.remove("calificacionPromedio"));
                        if (doc.containsKey("cvUrl")) detallesContratista.put("cvUrl", doc.remove("cvUrl"));
                        doc.put("detallesContratista", detallesContratista);
                    }
                    
                    if (doc.containsKey("oficio") || doc.containsKey("disponibilidad")) {
                        Document detallesTrabajador = new Document();
                        if (doc.containsKey("oficio")) detallesTrabajador.put("oficio", doc.remove("oficio"));
                        if (doc.containsKey("disponibilidad")) detallesTrabajador.put("disponibilidad", doc.remove("disponibilidad"));
                        doc.put("detallesTrabajador", detallesTrabajador);
                    }
                    
                    try {
                        mongoTemplate.save(doc, "perfiles");
                        migrados++;
                    } catch (Exception e) {
                        System.err.println("Error migrando documento de " + oldCollectionName + ": " + e.getMessage());
                    }
                }
                System.out.println("[Migracin] " + migrados + " documentos migrados de '" + oldCollectionName + "' a 'perfiles'.");
                
            }
        }
    }

    private void sanearColeccionUsuarios() {
        if (mongoTemplate.collectionExists("usuarios")) {
            List<Document> documentos = mongoTemplate.findAll(Document.class, "usuarios");
            int actualizados = 0;
            for (Document doc : documentos) {
                boolean modificado = false;

                if (doc.containsKey("role") && doc.get("role") instanceof String) {
                    String roleStr = doc.getString("role");
                    java.util.List<String> rolesList = new java.util.ArrayList<>();
                    rolesList.add(roleStr);
                    doc.put("roles", rolesList);
                    modificado = true;
                }

                if (!doc.containsKey("activo") || Boolean.FALSE.equals(doc.get("activo"))) {
                    doc.put("activo", true);
                    modificado = true;
                }

                if (modificado) {
                    mongoTemplate.save(doc, "usuarios");
                    actualizados++;
                }
            }
            System.out.println("[Migración] " + actualizados + " usuarios saneados (convertidos a 'roles' y activados).");
        }
    }
}
