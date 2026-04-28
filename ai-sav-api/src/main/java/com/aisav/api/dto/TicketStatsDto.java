package com.aisav.api.dto;

import lombok.Data;

@Data
public class TicketStatsDto {
    private long total;
    private long pending;
    private long classified;
    private long critique;
    private long nonCritique;
}