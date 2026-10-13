package com.obratech.controllers;

import org.bson.types.ObjectId;
import org.springframework.core.io.Resource;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mongodb.client.gridfs.model.GridFSFile;
import com.obratech.entity.Perfil;
import com.obratech.repository.PerfilRepository;

@RestController
@RequestMapping("/api/public/profiles")
public class PublicProfilePhotoApiController {

    private final PerfilRepository perfilRepository;
    private final GridFsTemplate gridFsTemplate;

    public PublicProfilePhotoApiController(PerfilRepository perfilRepository, GridFsTemplate gridFsTemplate) {
        this.perfilRepository = perfilRepository;
        this.gridFsTemplate = gridFsTemplate;
    }

    @GetMapping("/{profileId}/photo")
    public ResponseEntity<Resource> getPhoto(@PathVariable String profileId) {
        Perfil perfil = perfilRepository.findById(profileId)
                .orElseGet(() -> perfilRepository.findByUsernameIgnoreCase(profileId).orElse(null));
        if (perfil == null || perfil.getFotoPerfilUrl() == null || perfil.getFotoPerfilUrl().isBlank()) {
            return ResponseEntity.notFound().build();
        }

        try {
            ObjectId photoId = new ObjectId(perfil.getFotoPerfilUrl());
            GridFSFile file = gridFsTemplate.findOne(new Query(Criteria.where("_id").is(photoId)));
            if (file == null) return ResponseEntity.notFound().build();
            GridFsResource resource = gridFsTemplate.getResource(file);
            String contentType = resource.getContentType();
            if (!MediaType.IMAGE_JPEG_VALUE.equals(contentType) && !MediaType.IMAGE_PNG_VALUE.equals(contentType)) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok()
                    .header(HttpHeaders.CACHE_CONTROL, "public, max-age=300")
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(resource);
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.notFound().build();
        }
    }
}