package com.aisav.api.dto;

import lombok.Data;
import java.util.List;

@Data
public class PagedTicketsDto {
    private List<TicketDto> content;
    private int totalPages;
    private long totalElements;
    private int number;
    private int size;
}