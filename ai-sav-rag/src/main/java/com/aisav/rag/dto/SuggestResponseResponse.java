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
public class SuggestResponseResponse {

    private String question;
    private String answer;
    private int sourceCount;
    private List<SourceDto> sources;
}