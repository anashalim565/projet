package com.aisav.rag;

import com.aisav.rag.dto.SourceDto;
import com.aisav.rag.dto.SuggestResponseRequest;
import com.aisav.rag.dto.SuggestResponseResponse;
import com.aisav.rag.service.RagSuggestionService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(3)
public class RagSuggestionSmokeTest implements CommandLineRunner {

    private final RagSuggestionService ragSuggestionService;

    public RagSuggestionSmokeTest(RagSuggestionService ragSuggestionService) {
        this.ragSuggestionService = ragSuggestionService;
    }

    @Override
    public void run(String... args) {
        SuggestResponseRequest request = new SuggestResponseRequest();
        request.setQuestion("Le client signale un virement non reçu. Quelle réponse peut-on lui proposer ?");
        request.setTopK(3);

        SuggestResponseResponse response = ragSuggestionService.suggest(request);

        System.out.println("=== RAG SUGGESTION SMOKE TEST ===");
        System.out.println("Question: " + response.getQuestion());
        System.out.println("Answer: " + response.getAnswer());
        System.out.println("Source count: " + response.getSourceCount());

        int index = 1;
        for (SourceDto source : response.getSources()) {
            System.out.println("Source #" + index++);
            System.out.println("Title: " + source.getTitle());
            System.out.println("Type: " + source.getType());
            System.out.println("Source: " + source.getSource());
            System.out.println("Language: " + source.getLanguage());
            System.out.println("---");
        }
    }
}