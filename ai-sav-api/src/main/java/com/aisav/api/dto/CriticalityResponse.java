package com.aisav.api.dto;

import java.util.List;

public record CriticalityResponse(
        String ticket_number,
        String label,
        Integer label_value,
        Double confiance,
        Boolean is_critique,
        String sentiment,
        Double sentiment_score,
        Integer urgence,
        String emotion,
        List<String> signaux,
        String modele_utilise,
        String model_version
) {
}