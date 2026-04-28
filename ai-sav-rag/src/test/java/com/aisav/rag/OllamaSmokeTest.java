package com.aisav.rag;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class OllamaSmokeTest implements CommandLineRunner {

    private final ChatClient chatClient;

    public OllamaSmokeTest(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    @Override
    public void run(String... args) {
        String response = chatClient.prompt()
                .user("Say hello in one short sentence.")
                .call()
                .content();

        System.out.println("=== OLLAMA RESPONSE ===");
        System.out.println(response);
    }
}