package com.aisav.rag;

import java.util.Arrays;
import java.util.List;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class EmbeddingSmokeTest implements CommandLineRunner {

    private final EmbeddingModel embeddingModel;

    public EmbeddingSmokeTest(EmbeddingModel embeddingModel) {
        this.embeddingModel = embeddingModel;
    }

    @Override
    public void run(String... args) {
        List<String> texts = Arrays.asList(
                "Le client signale un virement non reçu.",
                "Le client demande une opposition sur sa carte bancaire."
        );

        EmbeddingResponse response = embeddingModel.embedForResponse(texts);

        System.out.println("=== EMBEDDING SMOKE TEST ===");
        System.out.println("Embeddings count: " + response.getResults().size());

        if (!response.getResults().isEmpty()) {
            float[] vector = response.getResults().get(0).getOutput();
            System.out.println("First embedding dimension: " + vector.length);
            System.out.println("First values sample: "
                    + vector[0] + ", "
                    + vector[1] + ", "
                    + vector[2]);
        }
    }
}