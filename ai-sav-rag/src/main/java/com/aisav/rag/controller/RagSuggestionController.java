package com.aisav.rag.controller;

import com.aisav.rag.dto.SuggestResponseRequest;
import com.aisav.rag.dto.SuggestResponseResponse;
import com.aisav.rag.service.RagSuggestionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@CrossOrigin(origins = "http://localhost:5173")
@RequestMapping("/internal/rag")
public class RagSuggestionController {

    private final RagSuggestionService ragSuggestionService;

    public RagSuggestionController(RagSuggestionService ragSuggestionService) {
        this.ragSuggestionService = ragSuggestionService;
    }

    @PostMapping("/suggest-response")
    public ResponseEntity<SuggestResponseResponse> suggestResponse(
            @Valid @RequestBody SuggestResponseRequest request
    ) {
        SuggestResponseResponse response = ragSuggestionService.suggest(request);
        return ResponseEntity.ok(response);
    }
}
