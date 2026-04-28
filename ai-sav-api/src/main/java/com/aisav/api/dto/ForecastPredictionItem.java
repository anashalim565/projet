package com.aisav.api.dto;

public record ForecastPredictionItem(
        String date,
        Double yhat,
        Double yhat_lower,
        Double yhat_upper,
        Boolean is_weekend,
        Boolean is_holiday
) {
}