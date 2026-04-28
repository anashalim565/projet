package com.aisav.rag.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SearchResponse {

    private String query;
    private int resultCount;
    private List<RetrievedChunkDto> results;
}