package com.aisav.rag;

import com.aisav.rag.dto.RetrievedChunkDto;
import com.aisav.rag.dto.SearchRequest;
import com.aisav.rag.dto.SearchResponse;
import com.aisav.rag.service.RagSearchService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(2)
public class RagSearchSmokeTest implements CommandLineRunner {

    private final RagSearchService ragSearchService;

    public RagSearchSmokeTest(RagSearchService ragSearchService) {
        this.ragSearchService = ragSearchService;
    }

    @Override
    public void run(String... args) {
        SearchRequest request = new SearchRequest();
        request.setQuery("Le client signale un virement non reçu");
        request.setTopK(3);

        SearchResponse response = ragSearchService.search(request);

        System.out.println("=== RAG SEARCH SMOKE TEST ===");
        System.out.println("Query: " + response.getQuery());
        System.out.println("Result count: " + response.getResultCount());

        int index = 1;
        for (RetrievedChunkDto result : response.getResults()) {
            System.out.println("Result #" + index++);
            System.out.println("Title: " + result.getTitle());
            System.out.println("Type: " + result.getType());
            System.out.println("Source: " + result.getSource());
            System.out.println("Language: " + result.getLanguage());
            System.out.println("Content: " + result.getContent());
            System.out.println("---");
        }
    }
}