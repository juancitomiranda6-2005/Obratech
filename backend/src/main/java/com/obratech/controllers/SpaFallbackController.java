package com.obratech.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SpaFallbackController {

    @GetMapping({
            "/clientes/mis-postulaciones",
            "/mis-postulaciones",
            "/mis-postulaciones/{applicationId}",
            "/perfil-contratista/editar",
            "/perfil-trabajador",
            "/perfil-trabajador/editar",
            "/admin/perfiles/{id}",
            "/proyectos/{projectId}/postulantes",
            "/oauth/callback",
            "/recuperar-contrasena",
            "/restablecer-contrasena"
    })
    public String forwardToReact() {
        return "forward:/index.html";
    }
}