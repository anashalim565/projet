package com.aisav.api.dto;

import jakarta.validation.constraints.NotBlank;

public record ForecastRequest(
        @NotBlank String agence
) {
}