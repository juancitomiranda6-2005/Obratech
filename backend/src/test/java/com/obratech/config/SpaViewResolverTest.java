package com.obratech.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Locale;

import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.view.InternalResourceView;

class SpaViewResolverTest {

    private final SpaViewResolver resolver = new SpaViewResolver();

    @Test
    void mapsLegacyViewNamesToTheReactEntryPoint() throws Exception {
        var view = resolver.resolveViewName("desboard-cliente", Locale.ROOT);

        assertInstanceOf(InternalResourceView.class, view);
        assertEquals("/index.html", ((InternalResourceView) view).getUrl());
    }

    @Test
    void leavesRedirectsToSpringMvc() throws Exception {
        assertNull(resolver.resolveViewName("redirect:/login", Locale.ROOT));
    }
}