package com.aisav.rag.controller;

import com.aisav.rag.dto.IndexRequest;
import com.aisav.rag.dto.IndexResponse;
import com.aisav.rag.service.RagIngestionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/internal/rag")
public class RagIndexController {

    private final RagIngestionService ragIngestionService;

    public RagIndexController(RagIngestionService ragIngestionService) {
        this.ragIngestionService = ragIngestionService;
    }

    @PostMapping("/index")
    public ResponseEntity<IndexResponse> indexDocument(@Valid @RequestBody IndexRequest request) {
        IndexResponse response = ragIngestionService.indexDocument(request);
        return ResponseEntity.ok(response);
    }
}