package com.aisav.api.client;

import com.aisav.api.dto.RagSuggestionRequest;
import com.aisav.api.dto.RagSuggestionResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class RagClient {

    private final WebClient internalWebClient;
    private final String ragBaseUrl;

    public RagClient(WebClient internalWebClient,
                     @Value("${services.rag.url}") String ragBaseUrl) {
        this.internalWebClient = internalWebClient;
        this.ragBaseUrl = ragBaseUrl;
    }

    public RagSuggestionResponse suggest(RagSuggestionRequest request) {
        return internalWebClient.post()
                .uri(ragBaseUrl + "/api/suggest")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(RagSuggestionResponse.class)
                .block();
    }
}