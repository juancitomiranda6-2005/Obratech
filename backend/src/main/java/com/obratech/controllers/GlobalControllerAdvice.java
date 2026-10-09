package com.obratech.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import com.obratech.entity.Usuario;
import com.obratech.repository.PerfilRepository;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;


@ControllerAdvice
public class GlobalControllerAdvice {

    private static final Logger log = LoggerFactory.getLogger(GlobalControllerAdvice.class);

    private final PerfilRepository perfilRepository;

    public GlobalControllerAdvice(PerfilRepository perfilRepository) {
        this.perfilRepository = perfilRepository;
    }

    @ModelAttribute("perfilGlobal")
    public Object addProfileToModel(HttpSession session) {
        try {
            Usuario usuario = (Usuario) session.getAttribute("usuario");
            if (usuario == null || usuario.getUsername() == null) return null;
            return perfilRepository.findByUsernameIgnoreCase(usuario.getUsername()).orElse(null);
        } catch (Exception e) {
            log.error("Error al cargar perfil global en el advice: {}", e.getMessage());
            return null;
        }
    }

    @ExceptionHandler(AccessDeniedException.class)
    public String handleAccessDenied(AccessDeniedException ex,
                                     HttpServletResponse response, Model model) {
        log.warn(" Acceso denegado: {}", ex.getMessage());
        response.setStatus(HttpStatus.FORBIDDEN.value());
        model.addAttribute("codigoError", 403);
        model.addAttribute("errorTitle", "Acceso Denegado");
        model.addAttribute("errorMessage",
            "No tienes los permisos necesarios para acceder a este recurso. " +
            "Si crees que es un error, contacta al administrador.");
        model.addAttribute("errorIcon", "lock");
        return "error";
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public String handleNotFound(NoHandlerFoundException ex,
                                 HttpServletResponse response, Model model) {
        log.warn(" Pgina no encontrada: {}", ex.getRequestURL());
        response.setStatus(HttpStatus.NOT_FOUND.value());
        model.addAttribute("codigoError", 404);
        model.addAttribute("errorTitle", "Pgina No Encontrada");
        model.addAttribute("errorMessage",
            "La pgina que buscas no existe o fue movida a otra direccin.");
        model.addAttribute("errorIcon", "search_off");
        return "error";
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public String handleNoResource(NoResourceFoundException ex,
                                   HttpServletResponse response, Model model) {
        log.warn(" Recurso no encontrado: {}", ex.getMessage());
        response.setStatus(HttpStatus.NOT_FOUND.value());
        model.addAttribute("codigoError", 404);
        model.addAttribute("errorTitle", "Recurso No Encontrado");
        model.addAttribute("errorMessage", "El recurso solicitado no existe en el servidor.");
        model.addAttribute("errorIcon", "search_off");
        return "error";
    }

    @ExceptionHandler(UsernameNotFoundException.class)
    public String handleUsernameNotFound(UsernameNotFoundException ex,
                                         HttpServletResponse response, Model model) {
        log.warn(" Autenticacin fallida: {}", ex.getMessage());
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        model.addAttribute("codigoError", 401);
        model.addAttribute("errorTitle", "Autenticacin Fallida");
        model.addAttribute("errorMessage",
            "No encontramos una cuenta con las credenciales proporcionadas.");
        model.addAttribute("errorIcon", "person_off");
        return "error";
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public String handleIllegalArgument(IllegalArgumentException ex,
                                        HttpServletResponse response, Model model) {
        log.warn(" Error de validacin: {}", ex.getMessage());
        response.setStatus(HttpStatus.BAD_REQUEST.value());
        model.addAttribute("codigoError", 400);
        model.addAttribute("errorTitle", "Datos Invlidos");
        model.addAttribute("errorMessage", ex.getMessage());
        model.addAttribute("errorIcon", "error_outline");
        return "error";
    }

    @ExceptionHandler(Exception.class)
    public String handleGeneral(Exception ex, HttpServletResponse response, Model model) {
        log.error(" Error inesperado: {}", ex.getMessage(), ex);
        response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
        model.addAttribute("codigoError", 500);
        model.addAttribute("errorTitle", "Error Interno del Servidor");
        model.addAttribute("errorMessage",
            "Ocurri un problema inesperado. Por favor intenta de nuevo ms tarde.");
        model.addAttribute("errorIcon", "warning");
        return "error";
    }
}
