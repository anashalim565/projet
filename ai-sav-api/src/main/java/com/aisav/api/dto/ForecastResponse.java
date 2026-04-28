package com.aisav.api.dto;

import java.util.List;

public record ForecastResponse(
        String agence,
        String granularity,
        String confidence,
        Double mape,
        Double pct_critique_prevu,
        List<ForecastPredictionItem> predictions_total,
        List<ForecastPredictionItem> predictions_critique,
        String generated_at,
        String warning
) {
}