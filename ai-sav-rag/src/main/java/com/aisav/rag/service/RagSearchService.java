package com.aisav.rag.service;

import com.aisav.rag.component.RetrievalService;
import com.aisav.rag.dto.RetrievedChunkDto;
import com.aisav.rag.dto.SearchRequest;
import com.aisav.rag.dto.SearchResponse;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RagSearchService {

    private final RetrievalService retrievalService;

    public RagSearchService(RetrievalService retrievalService) {
        this.retrievalService = retrievalService;
    }

    public SearchResponse search(SearchRequest request) {
        List<Document> documents = retrievalService.retrieve(request);

        List<RetrievedChunkDto> results = documents.stream()
                .map(this::toDto)
                .toList();

        return new SearchResponse(
                request.getQuery(),
                results.size(),
                results
        );
    }

    private RetrievedChunkDto toDto(Document document) {
        return new RetrievedChunkDto(
                document.getText(),
                getMetadataValue(document, "title"),
                getMetadataValue(document, "type"),
                getMetadataValue(document, "source"),
                getMetadataValue(document, "language")
        );
    }

    private String getMetadataValue(Document document, String key) {
        Object value = document.getMetadata().get(key);
        return value != null ? value.toString() : null;
    }
}