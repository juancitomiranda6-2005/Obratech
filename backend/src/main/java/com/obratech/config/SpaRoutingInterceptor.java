package com.obratech.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class SpaRoutingInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        String accept = request.getHeader("Accept");
        if ("/".equals(path) || "/index.html".equals(path)
            || !"GET".equals(request.getMethod()) || accept == null || !accept.contains(MediaType.TEXT_HTML_VALUE)
                || isServerResource(path)) {
            return true;
        }
        request.getRequestDispatcher("/index.html").forward(request, response);
        return false;
    }

    private boolean isServerResource(String path) {
        return path.startsWith("/api/")
                || path.startsWith("/oauth2/")
                || path.startsWith("/login/oauth2/")
                || path.startsWith("/uploads/")
                || path.startsWith("/css/")
                || path.startsWith("/js/")
                || path.startsWith("/img/")
                || path.startsWith("/images/")
                || path.startsWith("/styles/")
                || path.startsWith("/assets/")
                || path.startsWith("/webjars/")
                || path.startsWith("/actuator/")
                || path.startsWith("/proyectos/documento/")
                || path.startsWith("/trabajadores/cv/");
    }
}