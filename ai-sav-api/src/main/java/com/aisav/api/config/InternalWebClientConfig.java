package com.aisav.api.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.UUID;

@Configuration
public class InternalWebClientConfig {

    @Bean
    public WebClient internalWebClient(WebClient.Builder builder,
                                       @Value("${internal.api-key}") String internalApiKey) {
        return builder
                .defaultHeader("X-Internal-Api-Key", internalApiKey)
                .filter((request, next) -> {
                    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                    String userId = (auth != null && auth.getName() != null) ? auth.getName() : "system";

                    ClientRequest enriched = ClientRequest.from(request)
                            .header("X-User-Id", userId)
                            .header("X-Request-Id", UUID.randomUUID().toString())
                            .build();

                    return next.exchange(enriched);
                })
                .build();
    }
}