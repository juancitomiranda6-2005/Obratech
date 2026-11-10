package com.obratech.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class MlServiceConfig {

    @Bean
    public RestTemplate mlRestTemplate() {
        // TODO: Configurar timeouts, reintentos y observabilidad para producción.
        return new RestTemplate();
    }

    @Bean
    public String mlServiceUrl(@Value("${ml.service.url:http://localhost:8000}") String url) {
        // TODO: Reemplazar este bean auxiliar por una clase de propiedades tipada si crece la configuración.
        return url;
    }
}
