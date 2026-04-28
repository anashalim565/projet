package com.aisav.rag.config;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EmbeddingConfig {

    @Bean
    public EmbeddingModel embeddingModel(EmbeddingModel embeddingModel) {
        return embeddingModel;
    }
}