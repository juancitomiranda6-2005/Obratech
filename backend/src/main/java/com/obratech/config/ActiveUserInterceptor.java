// ActiveUserInterceptor
package com.obratech.config;

import com.obratech.entity.Usuario;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class ActiveUserInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return true;
        }

        Usuario usuario = (Usuario) session.getAttribute("usuario");
        if (usuario == null || usuario.isActivo()) {
            return true;
        }

        String uri = request.getRequestURI();
        if (uri.startsWith(request.getContextPath() + "/login")
                || uri.startsWith(request.getContextPath() + "/registro")
                || uri.startsWith(request.getContextPath() + "/completar-registro-oauth2")
                || uri.startsWith(request.getContextPath() + "/oauth2")
                || uri.startsWith(request.getContextPath() + "/logout")
                || uri.startsWith(request.getContextPath() + "/error")
                || uri.startsWith(request.getContextPath() + "/uploads/")
                || uri.startsWith(request.getContextPath() + "/css/")
                || uri.startsWith(request.getContextPath() + "/js/")
                || uri.startsWith(request.getContextPath() + "/images/")) {
            return true;
        }

        response.sendRedirect(request.getContextPath() + "/login?pending=true");
        return false;
    }
}
