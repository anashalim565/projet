package com.aisav.rag.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Objects;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
        name = "knowledge_documents",
        indexes = {
                @Index(name = "idx_knowledge_document_title", columnList = "title"),
                @Index(name = "idx_knowledge_document_type", columnList = "document_type"),
                @Index(name = "idx_knowledge_document_language", columnList = "language"),
                @Index(name = "idx_knowledge_document_active", columnList = "active"),
                @Index(name = "idx_knowledge_document_checksum", columnList = "checksum", unique = true)
        }
)
public class KnowledgeDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "title", nullable = false, length = 300)
    private String title;

    @Column(name = "document_type", nullable = false, length = 100)
    private String type;

    @Column(name = "source", nullable = false, length = 500)
    private String source;

    @Column(name = "language", nullable = false, length = 20)
    private String language;

    @Column(name = "checksum", nullable = false, length = 128, unique = true)
    private String checksum;

    @Column(name = "indexed_at", nullable = false)
    private LocalDateTime indexedAt;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    public KnowledgeDocument(
            String title,
            String type,
            String source,
            String language,
            String checksum,
            LocalDateTime indexedAt,
            boolean active
    ) {
        this.title = title;
        this.type = type;
        this.source = source;
        this.language = language;
        this.checksum = checksum;
        this.indexedAt = indexedAt;
        this.active = active;
    }

}