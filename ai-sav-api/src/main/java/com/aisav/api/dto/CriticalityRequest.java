package com.aisav.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CriticalityRequest(
        @NotBlank String commentaire,
        @NotBlank String ticket_number,
        @NotNull Boolean use_distlbert
) {
}