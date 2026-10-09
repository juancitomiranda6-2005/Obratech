package com.obratech.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/**
 * Controlador para gestionar las pantallas de error personalizadas HTTP (403, 404, 500).
 */
@Controller
@RequestMapping("/error")
public class ErrorController {

    /**
     * Muestra la pantalla de error de acceso denegado (HTTP 403).
     */
    @GetMapping("/403")
    public String error403(HttpServletResponse response, HttpSession session, Model model) {
        response.setStatus(403);
        String rutaDenegada = (String) session.getAttribute("rutaDenegada");
        session.removeAttribute("rutaDenegada");

        model.addAttribute("codigoError", 403);
        model.addAttribute("errorTitle", "Acceso Denegado");
        model.addAttribute("errorIcon", "lock");

        if (rutaDenegada != null) {
            model.addAttribute("errorMessage",
                    "No tienes permiso para acceder a: " + rutaDenegada + ". Esta seccion requiere un rol diferente al tuyo.");
        } else {
            model.addAttribute("errorMessage", "No tienes los permisos necesarios para acceder a este recurso.");
        }

        return "error";
    }

    /**
     * Muestra la pantalla de recurso no encontrado (HTTP 404).
     */
    @GetMapping("/404")
    public String error404(HttpServletResponse response, Model model) {
        response.setStatus(404);
        model.addAttribute("codigoError", 404);
        model.addAttribute("errorTitle", "Pgina No Encontrada");
        model.addAttribute("errorMessage", "La pgina que buscas no existe o fue movida a otra direccin.");
        model.addAttribute("errorIcon", "search_off");
        return "error";
    }

    /**
     * Muestra la pantalla de error interno del servidor (HTTP 500).
     */
    @GetMapping("/500")
    public String error500(HttpServletResponse response, Model model) {
        response.setStatus(500);
        model.addAttribute("codigoError", 500);
        model.addAttribute("errorTitle", "Error Interno del Servidor");
        model.addAttribute("errorMessage", "Ocurri un problema inesperado. Por favor intenta de nuevo ms tarde.");
        model.addAttribute("errorIcon", "warning");
        return "error";
    }
}
