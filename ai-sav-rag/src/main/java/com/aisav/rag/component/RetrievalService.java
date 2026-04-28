package com.aisav.rag.component;

import com.aisav.rag.dto.SearchRequest;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RetrievalService {

    private final VectorStore vectorStore;

    public RetrievalService(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    public List<Document> retrieve(SearchRequest request) {
        org.springframework.ai.vectorstore.SearchRequest searchRequest =
                org.springframework.ai.vectorstore.SearchRequest.builder()
                        .query(request.getQuery())
                        .topK(request.getTopK())
                        .build();

        return vectorStore.similaritySearch(searchRequest);
    }
}