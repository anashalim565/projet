package com.aisav.api.client;

import com.aisav.api.dto.ForecastRequest;
import com.aisav.api.dto.ForecastResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class ForecastClient {

    private final WebClient internalWebClient;
    private final String forecastBaseUrl;

    public ForecastClient(WebClient internalWebClient,
                          @Value("${services.forecast.url}") String forecastBaseUrl) {
        this.internalWebClient = internalWebClient;
        this.forecastBaseUrl = forecastBaseUrl;
    }

    public ForecastResponse forecast(ForecastRequest request) {
        return internalWebClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(forecastBaseUrl + "/api/v1/forecast/result")
                        .queryParam("agence", request.agence())
                        .build())
                .retrieve()
                .bodyToMono(ForecastResponse.class)
                .block();
    }
}