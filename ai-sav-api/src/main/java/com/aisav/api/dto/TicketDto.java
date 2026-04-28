package com.aisav.api.dto;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class TicketDto {
    private Long id;
    private String ticketRef;
    private String agence;
    private String commentaire;
    private String ancienCoupleFamille;
    private String nouveauCoupleFamille;
    private String retourCgr;
    private Boolean classification;
    private LocalDate dateQualification;
    private String statut;
    private Double aiScore;
    private Boolean isCritique;
    private String sla;
    private LocalDateTime createdAt;
}