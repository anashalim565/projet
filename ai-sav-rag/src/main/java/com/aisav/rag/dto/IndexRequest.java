package com.aisav.rag.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class IndexRequest {

    @NotBlank(message = "Title is required")
    @Size(max = 300, message = "Title must not exceed 300 characters")
    private String title;

    @NotBlank(message = "Content is required")
    private String content;

    @NotBlank(message = "Type is required")
    @Size(max = 100, message = "Type must not exceed 100 characters")
    private String type;

    @NotBlank(message = "Source is required")
    @Size(max = 500, message = "Source must not exceed 500 characters")
    private String source;

    @NotBlank(message = "Language is required")
    @Size(max = 20, message = "Language must not exceed 20 characters")
    private String language;
}