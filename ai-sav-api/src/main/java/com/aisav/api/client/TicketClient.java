package com.aisav.api.client;

import com.aisav.api.dto.PagedTicketsDto;
import com.aisav.api.dto.TicketDto;
import com.aisav.api.dto.TicketStatsDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import java.util.Optional;

@Component
public class TicketClient {

    private final WebClient webClient;

    public TicketClient(@Value("${services.batch.url}") String batchUrl,
                        @Value("${internal.api-key}") String apiKey) {
        this.webClient = WebClient.builder()
                .baseUrl(batchUrl)
                .defaultHeader("X-Internal-Api-Key", apiKey)
                .build();
    }

    public PagedTicketsDto getAll(int page, int size, String statut) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/internal/tickets")
                        .queryParam("page", page)
                        .queryParam("size", size)
                        .queryParamIfPresent("statut", Optional.ofNullable(statut))
                        .build())
                .retrieve()
                .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                        response -> response.bodyToMono(String.class)
                                .map(body -> new RuntimeException(
                                        "Batch service error " + response.statusCode() + ": " + body)))
                .bodyToMono(PagedTicketsDto.class)
                .block();
    }

    public TicketDto getById(Long id) {
        return webClient.get()
                .uri("/internal/tickets/{id}", id)
                .retrieve()
                .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                        response -> response.bodyToMono(String.class)
                                .map(body -> new RuntimeException(
                                        "Batch service error " + response.statusCode() + ": " + body)))
                .bodyToMono(TicketDto.class)
                .block();
    }

    public TicketStatsDto getStats() {
        return webClient.get()
                .uri("/internal/tickets/stats")
                .retrieve()
                .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                        response -> response.bodyToMono(String.class)
                                .map(body -> new RuntimeException(
                                        "Batch service error " + response.statusCode() + ": " + body)))
                .bodyToMono(TicketStatsDto.class)
                .block();
    }
}