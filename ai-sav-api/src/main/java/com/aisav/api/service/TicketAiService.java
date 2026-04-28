package com.aisav.api.service;

import com.aisav.api.client.CriticalityClient;
import com.aisav.api.client.ForecastClient;
import com.aisav.api.client.RagClient;
import com.aisav.api.dto.CriticalityRequest;
import com.aisav.api.dto.CriticalityResponse;
import com.aisav.api.dto.ForecastRequest;
import com.aisav.api.dto.ForecastResponse;
import com.aisav.api.dto.RagSuggestionRequest;
import com.aisav.api.dto.RagSuggestionResponse;
import org.springframework.stereotype.Service;

@Service
public class TicketAiService {

    private final CriticalityClient criticalityClient;
    private final ForecastClient forecastClient;
    private final RagClient ragClient;

    public TicketAiService(CriticalityClient criticalityClient,
                           ForecastClient forecastClient,
                           RagClient ragClient) {
        this.criticalityClient = criticalityClient;
        this.forecastClient = forecastClient;
        this.ragClient = ragClient;
    }

    public CriticalityResponse classify(CriticalityRequest request) {
        return criticalityClient.classify(request);
    }

    public ForecastResponse forecast(ForecastRequest request) {
        return forecastClient.forecast(request);
    }

    public RagSuggestionResponse suggest(RagSuggestionRequest request) {
        return ragClient.suggest(request);
    }
}