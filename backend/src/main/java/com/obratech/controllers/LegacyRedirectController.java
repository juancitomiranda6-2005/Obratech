package com.obratech.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class LegacyRedirectController {

    @GetMapping({"/proyectos/postular", "/proyectos/postular/"})
    public String redirectProyectosPostular() {
        return "redirect:/postulaciones";
    }

    @GetMapping("/proyectos/postular/{id}")
    public String redirectProyectosPostularWithId(@PathVariable String id) {
        return "redirect:/postulaciones/" + id;
    }
}
