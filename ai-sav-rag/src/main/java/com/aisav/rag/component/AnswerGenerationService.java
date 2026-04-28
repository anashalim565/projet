package com.aisav.rag.component;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

@Component
public class AnswerGenerationService {

    private final ChatClient chatClient;

    public AnswerGenerationService(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    public String generate(String prompt) {
        return chatClient.prompt()
                .user(prompt)
                .call()
                .content();
    }
}