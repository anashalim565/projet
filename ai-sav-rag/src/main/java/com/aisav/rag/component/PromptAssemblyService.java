package com.aisav.rag.component;

import com.aisav.rag.dto.RetrievedChunkDto;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PromptAssemblyService {

    public String buildPrompt(String question, List<RetrievedChunkDto> chunks) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("""
                Tu es un assistant SAV bancaire.
                Réponds de manière professionnelle, claire et concise.
                Utilise uniquement les informations du contexte fourni.
                Si le contexte est insuffisant, dis-le clairement sans inventer.
                
                CONTEXTE :
                """);

        if (chunks == null || chunks.isEmpty()) {
            prompt.append("Aucun contexte documentaire disponible.\n");
        } else {
            int index = 1;
            for (RetrievedChunkDto chunk : chunks) {
                prompt.append("Source ").append(index++).append(":\n");
                prompt.append("Titre: ").append(nullSafe(chunk.getTitle())).append("\n");
                prompt.append("Type: ").append(nullSafe(chunk.getType())).append("\n");
                prompt.append("Source: ").append(nullSafe(chunk.getSource())).append("\n");
                prompt.append("Langue: ").append(nullSafe(chunk.getLanguage())).append("\n");
                prompt.append("Contenu: ").append(nullSafe(chunk.getContent())).append("\n\n");
            }
        }

        prompt.append("QUESTION UTILISATEUR :\n");
        prompt.append(question).append("\n\n");

        prompt.append("""
                INSTRUCTIONS DE RÉPONSE :
                - Réponds en français.
                - Sois utile et structuré.
                - Maximum 4 phrases.
                - Ne cite pas d’informations absentes du contexte.
                - Si nécessaire, propose les prochaines vérifications à faire.
                
                RÉPONSE :
                """);

        return prompt.toString();
    }

    private String nullSafe(String value) {
        return value == null ? "" : value;
    }
}