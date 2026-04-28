package com.aisav.api.client;

import com.aisav.api.dto.CriticalityRequest;
import com.aisav.api.dto.CriticalityResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class CriticalityClient {

    private final WebClient internalWebClient;
    private final String criticalityBaseUrl;

    public CriticalityClient(WebClient internalWebClient,
                             @Value("${services.criticality.url}") String criticalityBaseUrl) {
        this.internalWebClient = internalWebClient;
        this.criticalityBaseUrl = criticalityBaseUrl;
    }

    public CriticalityResponse classify(CriticalityRequest request) {
        return internalWebClient.post()
                .uri(criticalityBaseUrl + "/api/v1/classify")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(CriticalityResponse.class)
                .block();
    }
}