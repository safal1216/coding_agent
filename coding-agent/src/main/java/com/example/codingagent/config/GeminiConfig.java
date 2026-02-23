package com.example.codingagent.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Configuration for Google Gemini API.
 *
 * WHY: We use Gemini API (FREE) for code generation instead of paid APIs.
 * This configures the WebClient to call Gemini's REST API.
 */
@Configuration
public class GeminiConfig {

    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.api.base-url}")
    private String baseUrl;

    /**
     * WebClient for calling Gemini API.
     *
     * WHY: Gemini uses REST API, so we use WebClient for HTTP calls.
     */
    @Bean
    public WebClient geminiWebClient() {
        return WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Content-Type", "application/json")
                .defaultHeader("x-goog-api-key", apiKey)
                .build();
    }

    /**
     * ObjectMapper for JSON parsing.
     *
     * WHY: Used to parse Gemini API responses.
     * Spring Boot usually auto-configures this, but we're making it explicit.
     */
    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}