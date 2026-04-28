package com.aisav.rag.service;

import com.aisav.rag.component.DocumentChunker;
import com.aisav.rag.dto.IndexRequest;
import com.aisav.rag.dto.IndexResponse;
import com.aisav.rag.entity.KnowledgeDocument;
import com.aisav.rag.repository.KnowledgeDocumentRepository;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class RagIngestionService {

    private final KnowledgeDocumentRepository knowledgeDocumentRepository;
    private final DocumentChunker documentChunker;
    private final VectorStore vectorStore;

    public RagIngestionService(KnowledgeDocumentRepository knowledgeDocumentRepository,
                               DocumentChunker documentChunker,
                               VectorStore vectorStore) {
        this.knowledgeDocumentRepository = knowledgeDocumentRepository;
        this.documentChunker = documentChunker;
        this.vectorStore = vectorStore;
    }

    @Transactional
    public IndexResponse indexDocument(IndexRequest request) {
        String checksum = calculateChecksum(request.getContent());
        System.out.println("START indexDocument checksum=" + checksum);

        if (knowledgeDocumentRepository.existsByChecksum(checksum)) {
            System.out.println("ALREADY INDEXED");
            return new IndexResponse(false, 0, "Document already indexed");
        }

        KnowledgeDocument knowledgeDocument = new KnowledgeDocument(
                request.getTitle(),
                request.getType(),
                request.getSource(),
                request.getLanguage(),
                checksum,
                LocalDateTime.now(),
                true
        );

        KnowledgeDocument savedDocument = knowledgeDocumentRepository.save(knowledgeDocument);
        System.out.println("AFTER SAVE id=" + savedDocument.getId());

        List<String> chunks = documentChunker.chunk(request.getContent());
        System.out.println("CHUNKS=" + chunks.size());

        if (chunks.isEmpty()) {
            System.out.println("NO CHUNKS");
            return new IndexResponse(false, 0, "Document content is empty after chunking");
        }

        List<Document> vectorDocuments = chunks.stream()
                .map(chunk -> new Document(chunk, buildMetadata(savedDocument)))
                .toList();


            System.out.println("BEFORE VECTOR STORE ADD");
            vectorStore.add(vectorDocuments);
            System.out.println("AFTER VECTOR STORE ADD");


        return new IndexResponse(true, vectorDocuments.size(), "Document indexed successfully");
    }

    private Map<String, Object> buildMetadata(KnowledgeDocument knowledgeDocument) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("documentId", knowledgeDocument.getId());
        metadata.put("title", knowledgeDocument.getTitle());
        metadata.put("type", knowledgeDocument.getType());
        metadata.put("source", knowledgeDocument.getSource());
        metadata.put("language", knowledgeDocument.getLanguage());
        metadata.put("indexedAt", knowledgeDocument.getIndexedAt().toString());
        return metadata;
    }

    private String calculateChecksum(String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(content.getBytes(StandardCharsets.UTF_8));

            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();

        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Unable to calculate checksum", e);
        }
    }
}