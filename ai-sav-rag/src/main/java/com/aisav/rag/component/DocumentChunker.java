package com.aisav.rag.component;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Component
public class DocumentChunker {

    private static final int DEFAULT_CHUNK_SIZE = 800;
    private static final int DEFAULT_CHUNK_OVERLAP = 100;

    public List<String> chunk(String content) {
        return chunk(content, DEFAULT_CHUNK_SIZE, DEFAULT_CHUNK_OVERLAP);
    }

    public List<String> chunk(String content, int chunkSize, int chunkOverlap) {
        String normalized = normalize(content);

        if (normalized.isBlank()) {
            return Collections.emptyList();
        }

        if (chunkSize <= 0) {
            throw new IllegalArgumentException("chunkSize must be > 0");
        }

        if (chunkOverlap < 0) {
            throw new IllegalArgumentException("chunkOverlap must be >= 0");
        }

        if (chunkOverlap >= chunkSize) {
            throw new IllegalArgumentException("chunkOverlap must be smaller than chunkSize");
        }

        List<String> chunks = new ArrayList<>();
        int start = 0;
        int step = chunkSize - chunkOverlap;

        while (start < normalized.length()) {
            int end = Math.min(start + chunkSize, normalized.length());
            String chunk = normalized.substring(start, end).trim();

            if (!chunk.isBlank()) {
                chunks.add(chunk);
            }

            if (end == normalized.length()) {
                break;
            }

            start += step;
        }

        return chunks;
    }

    private String normalize(String content) {
        if (content == null) {
            return "";
        }

        return content
                .replace("\r\n", "\n")
                .replace("\r", "\n")
                .replaceAll("[ \\t]+", " ")
                .replaceAll("\\n{3,}", "\n\n")
                .trim();
    }
}