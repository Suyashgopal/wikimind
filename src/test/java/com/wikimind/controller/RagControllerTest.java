package com.wikimind.controller;

import com.wikimind.exception.GlobalExceptionHandler;
import com.wikimind.service.RagService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RagController.class)
@Import(GlobalExceptionHandler.class)
class RagControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RagService ragService;

    @Test
    void addReturnsNumberOfStoredChunks() throws Exception {
        when(ragService.ingest(anyString(), anyString())).thenReturn(2);

        mockMvc.perform(post("/api/rag/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Policy\",\"content\":\"Friday is Pizza Day.\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Policy"))
                .andExpect(jsonPath("$.chunksStored").value(2));
    }

    @Test
    void addRejectsBlankContent() throws Exception {
        mockMvc.perform(post("/api/rag/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Policy\",\"content\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("content must not be blank"))
                .andExpect(jsonPath("$.path").value("/api/rag/add"));
    }

    @Test
    void askReturnsAnswer() throws Exception {
        when(ragService.answer(anyString())).thenReturn("Pizza Day");

        mockMvc.perform(post("/api/rag/ask")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\":\"What happens on Friday?\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer").value("Pizza Day"));
    }

    @Test
    void askRejectsMalformedJson() throws Exception {
        mockMvc.perform(post("/api/rag/ask")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("not json"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void unexpectedFailuresBecomeCleanServerErrors() throws Exception {
        when(ragService.answer(anyString())).thenThrow(new RuntimeException("gemini down"));

        mockMvc.perform(post("/api/rag/ask")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\":\"hi\"}"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500));
    }
}
