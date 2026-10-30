package com.wikimind.config;

import com.google.genai.Client;
import com.google.genai.types.HttpOptions;
import com.google.genai.types.HttpRetryOptions;
import org.springframework.ai.google.genai.GoogleGenAiEmbeddingConnectionDetails;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration(proxyBeanMethods = false)
public class GeminiClientConfig {

    @Bean
    public Client geminiClient(@Value("${spring.ai.google.genai.api-key}") String apiKey,
                               @Value("${wikimind.ai.timeout-ms}") int timeoutMs,
                               @Value("${wikimind.ai.max-attempts}") int maxAttempts) {
        HttpOptions options = HttpOptions.builder()
                .timeout(timeoutMs)
                .retryOptions(HttpRetryOptions.builder().attempts(maxAttempts).build())
                .build();
        return Client.builder().apiKey(apiKey).httpOptions(options).build();
    }

    @Bean
    public GoogleGenAiEmbeddingConnectionDetails geminiEmbeddingConnection(
            Client geminiClient, @Value("${spring.ai.google.genai.api-key}") String apiKey) {
        return GoogleGenAiEmbeddingConnectionDetails.builder()
                .apiKey(apiKey)
                .genAiClient(geminiClient)
                .build();
    }
}
