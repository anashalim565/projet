package com.aisav.api.controller;

import com.aisav.api.client.TicketClient;
import com.aisav.api.dto.PagedTicketsDto;
import com.aisav.api.dto.TicketDto;
import com.aisav.api.dto.TicketStatsDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/claims")
@RequiredArgsConstructor
public class ClaimsController {

    private final TicketClient ticketClient;

    @GetMapping
    public ResponseEntity<?> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String statut
    ) {
        try {
            return ResponseEntity.ok(ticketClient.getAll(page, size, statut));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body("Batch service unavailable: " + e.getMessage());
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<TicketDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ticketClient.getById(id));
    }

    @GetMapping("/stats")
    public ResponseEntity<TicketStatsDto> getStats() {
        return ResponseEntity.ok(ticketClient.getStats());
    }
}