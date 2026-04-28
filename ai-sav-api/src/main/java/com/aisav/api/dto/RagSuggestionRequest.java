package com.aisav.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RagSuggestionRequest(
        @NotBlank String ticketId,
        @NotBlank String title,
        @NotBlank String description,
        @NotNull @Min(1) Integer maxSuggestions
) {
}