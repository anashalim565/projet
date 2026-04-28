package com.aisav.rag.controller;

import com.aisav.rag.dto.SearchRequest;
import com.aisav.rag.dto.SearchResponse;
import com.aisav.rag.service.RagSearchService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/internal/rag")
public class RagSearchController {

    private final RagSearchService ragSearchService;

    public RagSearchController(RagSearchService ragSearchService) {
        this.ragSearchService = ragSearchService;
    }

    @PostMapping("/search")
    public ResponseEntity<SearchResponse> search(@Valid @RequestBody SearchRequest request) {
        SearchResponse response = ragSearchService.search(request);
        return ResponseEntity.ok(response);
    }
}
