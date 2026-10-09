package com.obratech.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Redirige la ruta legacy /mis-proyectos al endpoint oficial en /proyectos/mis-proyectos.
 */
@Controller
public class MisProyectosController {

    @GetMapping("/mis-proyectos")
    public String verMisProyectos() {
        return "redirect:/proyectos/mis-proyectos";
    }
}
