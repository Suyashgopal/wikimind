package com.wikimind;

import com.google.genai.Client;
import com.google.genai.types.HttpOptions;
import com.wikimind.repository.AppUserRepository;
import com.wikimind.repository.WikiDocumentRepository;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Boots the whole application except the database, so wiring problems
 * (missing properties, conflicting beans) fail here instead of at startup.
 */
@SpringBootTest
@TestPropertySource(properties = {
        "spring.autoconfigure.exclude="
                + "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,"
                + "org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration,"
                + "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration,"
                + "org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration",
        "jwt.secret=test-secret-that-is-at-least-32-characters-long",
        "GEMINI_API_KEY=test-key"})
class WikiMindApplicationTests {

    @MockitoBean
    private AppUserRepository appUserRepository;

    @MockitoBean
    private WikiDocumentRepository wikiDocumentRepository;

    @Autowired
    private Client geminiClient;

    @Autowired
    private ChatModel chatModel;

    @Autowired
    private EmbeddingModel embeddingModel;

    @Test
    void chatAndEmbeddingShareTheConfiguredGeminiClient() {
        assertSame(geminiClient, ReflectionTestUtils.getField(chatModel, "genAiClient"));
        assertSame(geminiClient, ReflectionTestUtils.getField(embeddingModel, "genAiClient"));
    }

    // Spring AI builds this client during config parsing, before property placeholders are normally ready.
    @Test
    void geminiClientGetsResolvedKeyTimeoutAndRetryAttempts() {
        assertEquals("test-key", geminiClient.apiKey());

        Object apiClient = ReflectionTestUtils.getField(geminiClient, "apiClient");
        HttpOptions options = (HttpOptions) ReflectionTestUtils.getField(apiClient, "httpOptions");
        assertEquals(20000, options.timeout().orElseThrow());
        assertEquals(3, options.retryOptions().orElseThrow().attempts().orElseThrow());
    }
}
