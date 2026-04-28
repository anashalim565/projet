package com.aisav.api.dto;

import java.util.List;

public record RagSuggestionResponse(
        List<String> suggestions,
        String model
) {
}