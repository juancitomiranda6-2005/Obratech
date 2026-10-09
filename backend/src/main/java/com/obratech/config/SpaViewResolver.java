package com.obratech.config;

import java.util.Locale;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.ViewResolver;
import org.springframework.web.servlet.view.InternalResourceView;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SpaViewResolver implements ViewResolver {

    @Override
    public View resolveViewName(String viewName, Locale locale) {
        if (viewName == null || viewName.startsWith("redirect:")
                || viewName.startsWith("forward:") || viewName.startsWith("error:")
                || viewName.contains("::")) {
            return null;
        }
        return new InternalResourceView("/index.html");
    }
}