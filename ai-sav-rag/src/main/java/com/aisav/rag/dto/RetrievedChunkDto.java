package com.aisav.rag.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RetrievedChunkDto {

    private String content;
    private String title;
    private String type;
    private String source;
    private String language;
}