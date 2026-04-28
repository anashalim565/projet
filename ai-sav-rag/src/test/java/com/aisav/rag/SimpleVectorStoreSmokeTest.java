package com.aisav.rag;

import java.util.List;
import java.util.Map;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class SimpleVectorStoreSmokeTest implements CommandLineRunner {

    private final VectorStore vectorStore;

    public SimpleVectorStoreSmokeTest(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    @Override
    public void run(String... args) {
        List<Document> documents = List.of(
                new Document(
                        "Procédure : en cas de virement non reçu, vérifier le statut de l'opération et informer le client du délai interbancaire.",
                        Map.of("title", "Guide virement non reçu", "type", "procedure", "language", "fr")
                ),
                new Document(
                        "FAQ fraude carte : en cas d'opération suspecte, bloquer immédiatement la carte et déclencher la procédure d'opposition.",
                        Map.of("title", "FAQ fraude carte", "type", "faq", "language", "fr")
                ),
                new Document(
                        "Procédure double débit : vérifier le journal monétique et confirmer si l'écriture est en attente d'extourne.",
                        Map.of("title", "Procédure double débit", "type", "procedure", "language", "fr")
                )
        );

        vectorStore.add(documents);

        List<Document> results = vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query("Le client signale un virement non reçu")
                        .topK(2)
                        .build()
        );

        System.out.println("=== SIMPLE VECTOR STORE SMOKE TEST ===");
        System.out.println("Results count: " + results.size());

        for (int i = 0; i < results.size(); i++) {
            Document doc = results.get(i);
            System.out.println("Result #" + (i + 1));
            System.out.println("Title: " + doc.getMetadata().get("title"));
            System.out.println("Content: " + doc.getText());
            System.out.println("---");
        }
    }
}