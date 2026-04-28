package com.aisav.rag;

import com.aisav.rag.dto.IndexRequest;
import com.aisav.rag.dto.IndexResponse;
import com.aisav.rag.service.RagIngestionService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(1)
public class RagIngestionSmokeTest implements CommandLineRunner {

    private final RagIngestionService ragIngestionService;

    public RagIngestionSmokeTest(RagIngestionService ragIngestionService) {
        this.ragIngestionService = ragIngestionService;
    }

    @Override
    public void run(String... args) {
        IndexRequest request = new IndexRequest();
        request.setTitle("Guide virement non reçu");
        request.setType("procedure");
        request.setSource("local-test");
        request.setLanguage("fr");
        request.setContent("""
                Procédure de traitement d’un virement non reçu.

                Étape 1 : vérifier si le virement a bien été initié dans le système.
                Étape 2 : contrôler le statut interbancaire de l’opération.
                Étape 3 : informer le client que certains virements peuvent subir un délai de compensation.
                Étape 4 : si nécessaire, ouvrir une investigation interne.
                Étape 5 : tracer l’échange avec le client dans le système SAV.

                Si le client insiste, il faut aussi vérifier l’existence d’un rejet ou d’un retour de fonds.
                En cas d’anomalie persistante, escalader vers l’équipe support monétique ou paiements.
                """);

        IndexResponse response = ragIngestionService.indexDocument(request);

        System.out.println("=== RAG INGESTION SMOKE TEST ===");
        System.out.println("Success: " + response.isSuccess());
        System.out.println("Chunks indexed: " + response.getChunksIndexed());
        System.out.println("Message: " + response.getMessage());
    }
}