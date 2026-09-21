package com.wikimind.service;

import com.wikimind.model.WikiDocument;
import com.wikimind.repository.WikiDocumentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RagServiceTest {

    @Mock
    private EmbeddingModel embeddingModel;

    @Mock
    private ChatModel chatModel;

    @Mock
    private WikiDocumentRepository repository;

    @InjectMocks
    private RagService ragService;

    @Test
    void ingestSplitsLongTextIntoOverlappingChunks() {
        when(embeddingModel.embed(anyString())).thenReturn(new float[]{0.1f, 0.2f});

        int stored = ragService.ingest("Policy", "a".repeat(1200));

        // chunk size 500, overlap 50 -> starts at 0, 450, 900
        assertEquals(3, stored);
        verify(repository, times(3)).save(any(WikiDocument.class));
    }

    @Test
    void ingestOfBlankTextStoresNothing() {
        int stored = ragService.ingest("Empty", "   ");

        assertEquals(0, stored);
        verify(repository, never()).save(any());
    }

    @Test
    void answerTellsCallerWhenNothingIsStored() {
        when(embeddingModel.embed(anyString())).thenReturn(new float[]{0.1f});
        when(repository.searchSimilar(anyString(), anyInt())).thenReturn(List.of());

        String answer = ragService.answer("What is Friday?");

        assertTrue(answer.contains("ingest"));
        verify(chatModel, never()).call(anyString());
    }

    @Test
    void answerPutsRetrievedContextAndQuestionIntoPrompt() {
        when(embeddingModel.embed(anyString())).thenReturn(new float[]{0.1f, 0.2f});
        when(repository.searchSimilar(anyString(), anyInt()))
                .thenReturn(List.of(new WikiDocument("Policy", "Friday is Pizza Day.", new float[]{0.1f})));
        when(chatModel.call(anyString())).thenReturn("Pizza Day");

        String answer = ragService.answer("What happens on Friday?");

        assertEquals("Pizza Day", answer);
        ArgumentCaptor<String> prompt = ArgumentCaptor.forClass(String.class);
        verify(chatModel).call(prompt.capture());
        assertTrue(prompt.getValue().contains("Friday is Pizza Day."));
        assertTrue(prompt.getValue().contains("What happens on Friday?"));
    }

    @Test
    void answerSendsQueryVectorAsPgVectorLiteral() {
        when(embeddingModel.embed(anyString())).thenReturn(new float[]{0.5f, -1.0f});
        when(repository.searchSimilar(anyString(), anyInt())).thenReturn(List.of());

        ragService.answer("anything");

        verify(repository).searchSimilar("[0.5,-1.0]", 3);
    }
}
