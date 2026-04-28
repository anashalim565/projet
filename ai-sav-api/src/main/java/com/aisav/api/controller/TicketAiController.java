package com.aisav.api.controller;

import com.aisav.api.dto.CriticalityRequest;
import com.aisav.api.dto.CriticalityResponse;
import com.aisav.api.dto.ForecastRequest;
import com.aisav.api.dto.ForecastResponse;
import com.aisav.api.dto.RagSuggestionRequest;
import com.aisav.api.dto.RagSuggestionResponse;
import com.aisav.api.service.TicketAiService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tickets")
public class TicketAiController {

    private final TicketAiService ticketAiService;

    public TicketAiController(TicketAiService ticketAiService) {
        this.ticketAiService = ticketAiService;
    }

    @PostMapping("/classify")
    public ResponseEntity<CriticalityResponse> classify(@Valid @RequestBody CriticalityRequest request) {
        return ResponseEntity.ok(ticketAiService.classify(request));
    }

    @PostMapping("/forecast")
    public ResponseEntity<ForecastResponse> forecast(@Valid @RequestBody ForecastRequest request) {
        return ResponseEntity.ok(ticketAiService.forecast(request));
    }

    @PostMapping("/suggest")
    public ResponseEntity<RagSuggestionResponse> suggest(@Valid @RequestBody RagSuggestionRequest request) {
        return ResponseEntity.ok(ticketAiService.suggest(request));
    }
}