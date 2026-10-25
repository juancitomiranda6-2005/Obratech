package com.obratech.service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.obratech.entity.Proyecto;
import com.obratech.repository.ProyectoRepository;

@Service
public class PronosticoClimaService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PronosticoClimaService.class);
    private static final Duration CACHE_DURATION = Duration.ofHours(3);
    private static final String GEOCODING_URL = "https://geocoding-api.open-meteo.com/v1/search";
    private static final String FORECAST_URL = "https://api.open-meteo.com/v1/forecast";

    private final RestClient restClient;
    private final ProyectoRepository proyectoRepository;
    private final ConcurrentHashMap<String, CacheEntry> cache = new ConcurrentHashMap<>();

    public PronosticoClimaService(ProyectoRepository proyectoRepository) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(2));
        requestFactory.setReadTimeout(Duration.ofSeconds(2));
        this.restClient = RestClient.builder().requestFactory(requestFactory).build();
        this.proyectoRepository = proyectoRepository;
    }

    public Pronostico obtenerPronostico(Proyecto proyecto) {
        if (proyecto == null || proyecto.getId() == null || proyecto.getUbicacion() == null
                || proyecto.getUbicacion().isBlank()) {
            return Pronostico.sinDatos();
        }
        CacheEntry cached = cache.get(proyecto.getId());
        if (cached != null && Duration.between(cached.cachedAt(), Instant.now()).compareTo(CACHE_DURATION) < 0) {
            return cached.pronostico();
        }

        try {
            if (proyecto.getLatitud() == null || proyecto.getLongitud() == null) {
                geocodificar(proyecto);
            }
            if (proyecto.getLatitud() == null || proyecto.getLongitud() == null) {
                return cachearSinDatos(proyecto.getId());
            }
            var uri = UriComponentsBuilder.fromUriString(FORECAST_URL)
                    .queryParam("latitude", proyecto.getLatitud())
                    .queryParam("longitude", proyecto.getLongitud())
                    .queryParam("daily", "precipitation_sum,precipitation_probability_max")
                    .queryParam("forecast_days", 14)
                    .queryParam("timezone", "auto")
                    .build().encode().toUri();
            ForecastResponse response = restClient.get().uri(uri).retrieve().body(ForecastResponse.class);
            Pronostico pronostico = convertir(response);
            cache.put(proyecto.getId(), new CacheEntry(Instant.now(), pronostico));
            return pronostico;
        } catch (RestClientException | IllegalArgumentException exception) {
            LOGGER.warn("No fue posible obtener el pronóstico Open-Meteo del proyecto {}: {}",
                    proyecto.getId(), exception.getMessage());
            return cachearSinDatos(proyecto.getId());
        }
    }

    private void geocodificar(Proyecto proyecto) {
        var uri = UriComponentsBuilder.fromUriString(GEOCODING_URL)
                .queryParam("name", proyecto.getUbicacion())
                .queryParam("count", 1)
                .queryParam("language", "es")
                .queryParam("format", "json")
                .build().encode().toUri();
        GeocodingResponse response = restClient.get().uri(uri).retrieve().body(GeocodingResponse.class);
        if (response == null || response.results() == null || response.results().isEmpty()) {
            return;
        }
        GeocodingResult result = response.results().get(0);
        proyecto.setLatitud(result.latitude());
        proyecto.setLongitud(result.longitude());
        proyectoRepository.save(proyecto);
    }

    private Pronostico convertir(ForecastResponse response) {
        if (response == null || response.daily() == null || response.daily().time() == null
                || response.daily().precipitationSum() == null
                || response.daily().precipitationProbabilityMax() == null) {
            return Pronostico.sinDatos();
        }
        DailyForecast daily = response.daily();
        int count = Math.min(14, Math.min(daily.time().size(),
                Math.min(daily.precipitationSum().size(), daily.precipitationProbabilityMax().size())));
        List<DiaPronostico> dias = java.util.stream.IntStream.range(0, count)
                .mapToObj(index -> new DiaPronostico(
                        daily.time().get(index),
                        daily.precipitationSum().get(index),
                        daily.precipitationProbabilityMax().get(index)))
                .toList();
        return dias.size() == 14 ? new Pronostico(true, null, dias) : Pronostico.sinDatos();
    }

    private Pronostico cachearSinDatos(String projectId) {
        Pronostico sinDatos = Pronostico.sinDatos();
        cache.put(projectId, new CacheEntry(Instant.now(), sinDatos));
        return sinDatos;
    }

    private record CacheEntry(Instant cachedAt, Pronostico pronostico) {}

    public record Pronostico(boolean disponible, String mensaje, List<DiaPronostico> dias) {
        static Pronostico sinDatos() {
            return new Pronostico(false, "Pronóstico no disponible para esta ubicación.", List.of());
        }
    }

    public record DiaPronostico(String fecha, double precipitacionMm, double probabilidadMaxima) {}

    private record GeocodingResponse(List<GeocodingResult> results) {}

    private record GeocodingResult(double latitude, double longitude) {}

    private record ForecastResponse(DailyForecast daily) {}

    private record DailyForecast(
            List<String> time,
            @JsonProperty("precipitation_sum") List<Double> precipitationSum,
            @JsonProperty("precipitation_probability_max") List<Double> precipitationProbabilityMax) {}
}
