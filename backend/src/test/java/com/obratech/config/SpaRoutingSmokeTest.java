package com.obratech.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class SpaRoutingSmokeTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void deepReactRouteServesTheSpaEntryPoint() throws Exception {
        mockMvc.perform(get("/clientes/mis-postulaciones").accept("text/html"))
                .andExpect(status().isOk())
                .andExpect(forwardedUrl("/index.html"));
    }

    @Test
    void apiRouteDoesNotFallThroughToTheSpa() throws Exception {
        mockMvc.perform(get("/api/client/applications").accept("application/json"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void legacyDocumentRouteStillRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/proyectos/documento/000000000000000000000000/preview").accept("text/html"))
                .andExpect(status().is3xxRedirection());
    }
}