package com.obratech.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.obratech.dto.ml.PrediccionRetrasoRequest;
import com.obratech.dto.ml.PrediccionRetrasoResponse;
import com.obratech.dto.ml.PrediccionSeleccionRequest;
import com.obratech.dto.ml.PrediccionSeleccionResponse;

@Component
public class MlServiceClient {

    private static final Logger log = LoggerFactory.getLogger(MlServiceClient.class);

    private final RestTemplate restTemplate;
    private final String mlServiceUrl;
    private final ObjectMapper objectMapper;

    public MlServiceClient(
            RestTemplate mlRestTemplate,
            @Qualifier("mlServiceUrl") String mlServiceUrl,
            ObjectMapper objectMapper) {
        this.restTemplate = mlRestTemplate;
        this.mlServiceUrl = mlServiceUrl;
        this.objectMapper = objectMapper;
    }

    public PrediccionRetrasoResponse predecirRetraso(PrediccionRetrasoRequest request) {
        try {
            return restTemplate.postForObject(
                    mlServiceUrl + "/predecir-retraso",
                    request,
                    PrediccionRetrasoResponse.class);
        } catch (RestClientException exception) {
            // TODO: Sustituir el mock por una política configurable de disponibilidad y circuit breaker.
            return new PrediccionRetrasoResponse(true, 50.0, 7, "mock-sin-modelo", true);
        }
    }

    public PrediccionSeleccionResponse predecirSeleccion(PrediccionSeleccionRequest request) {
        String url = mlServiceUrl + "/predecir-seleccion";
        try {
            log.debug("JSON enviado al servicio ML para selección: {}", json(request));
            ResponseEntity<PrediccionSeleccionResponse> entity = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    new HttpEntity<>(request),
                    PrediccionSeleccionResponse.class);
            PrediccionSeleccionResponse response = entity.getBody();
            log.info("ML selección URL={} HTTP={} porcentaje={} mock={} camposPorDefecto={}",
                    url,
                    entity.getStatusCode().value(),
                    response == null ? null : response.porcentaje(),
                    response == null ? null : response.mock(),
                    response == null ? null : response.camposPorDefecto());
            log.debug("JSON recibido del servicio ML para selección: {}", json(response));
            return response;
        } catch (HttpStatusCodeException exception) {
            log.warn("Fallo HTTP del servicio ML para selección URL={} HTTP={} cuerpo={}",
                    url, exception.getStatusCode().value(), exception.getResponseBodyAsString());
            return new PrediccionSeleccionResponse(50.0, "evaluar-manualmente", "mock-sin-modelo", true);
        } catch (RestClientException exception) {
            log.warn("Excepción del servicio ML para selección URL={} mensaje={}", url, exception.getMessage(), exception);
            return new PrediccionSeleccionResponse(50.0, "evaluar-manualmente", "mock-sin-modelo", true);
        }
    }

    private String json(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            log.warn("No se pudo serializar JSON para el log del servicio ML", exception);
            return "<no serializable>";
        }
    }
}
