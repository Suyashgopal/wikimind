package com.wikimind.security;

import com.wikimind.controller.RagController;
import com.wikimind.exception.GlobalExceptionHandler;
import com.wikimind.model.AppUser;
import com.wikimind.model.Role;
import com.wikimind.service.RagService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RagController.class)
@Import({SecurityConfig.class, JwtService.class, GlobalExceptionHandler.class})
@TestPropertySource(properties = "jwt.secret=test-secret-that-is-at-least-32-characters-long")
class SecurityRulesTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private RagService ragService;

    private static final String ASK_BODY = "{\"question\":\"hi\"}";

    @Test
    void requestWithoutTokenIsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/rag/ask").contentType(MediaType.APPLICATION_JSON).content(ASK_BODY))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void requestWithGarbageTokenIsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/rag/ask")
                        .header("Authorization", "Bearer not-a-real-token")
                        .contentType(MediaType.APPLICATION_JSON).content(ASK_BODY))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void requestWithValidMemberTokenIsAllowed() throws Exception {
        when(ragService.answer(anyString())).thenReturn("ok");
        String token = jwtService.generateToken(new AppUser("alice", "hash", Role.MEMBER, "acme"));

        mockMvc.perform(post("/api/rag/ask")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(ASK_BODY))
                .andExpect(status().isOk());
    }
}
