package com.aisav.rag.service;

import com.aisav.rag.component.AnswerGenerationService;
import com.aisav.rag.component.PromptAssemblyService;
import com.aisav.rag.dto.*;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RagSuggestionService {

    private final RagSearchService ragSearchService;
    private final PromptAssemblyService promptAssemblyService;
    private final AnswerGenerationService answerGenerationService;

    public RagSuggestionService(RagSearchService ragSearchService,
                                PromptAssemblyService promptAssemblyService,
                                AnswerGenerationService answerGenerationService) {
        this.ragSearchService = ragSearchService;
        this.promptAssemblyService = promptAssemblyService;
        this.answerGenerationService = answerGenerationService;
    }

    public SuggestResponseResponse suggest(SuggestResponseRequest request) {
        SearchRequest searchRequest = new SearchRequest();
        searchRequest.setQuery(request.getQuestion());
        searchRequest.setTopK(request.getTopK());

        SearchResponse searchResponse = ragSearchService.search(searchRequest);
        List<RetrievedChunkDto> retrievedChunks = searchResponse.getResults();

        String prompt = promptAssemblyService.buildPrompt(request.getQuestion(), retrievedChunks);
        String answer = answerGenerationService.generate(prompt);

        List<SourceDto> sources = retrievedChunks.stream()
                .map(chunk -> new SourceDto(
                        chunk.getTitle(),
                        chunk.getType(),
                        chunk.getSource(),
                        chunk.getLanguage()
                ))
                .toList();

        return new SuggestResponseResponse(
                request.getQuestion(),
                answer,
                sources.size(),
                sources
        );
    }
}