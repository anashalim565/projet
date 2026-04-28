package com.aisav.rag.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class SuggestResponseRequest {

    @NotBlank(message = "Question is required")
    private String question;

    @Min(value = 1, message = "topK must be greater than or equal to 1")
    private int topK = 3;
}