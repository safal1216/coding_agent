package com.example.codingagent.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

/**
 * Service for generating code using Google Gemini API (FREE).
 *
 * WHY: This is the EXECUTE phase of the agent loop.
 * Uses Gemini Pro model to generate Java code.
 */
@Service
@Slf4j
public class CodeGenerationService {

    private final WebClient geminiWebClient;
    private final ObjectMapper objectMapper;

    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.api.model:gemini-pro}")
    private String model;

    @Value("${gemini.api.temperature:0.7}")
    private Double temperature;

    public CodeGenerationService(WebClient geminiWebClient, ObjectMapper objectMapper) {
        this.geminiWebClient = geminiWebClient;
        this.objectMapper = objectMapper;
    }

    /**
     * Generate code for a given goal using Gemini API.
     *
     * @param goal Natural language description
     * @return Generated Java code
     */
    public String generateCode(String goal) {
        log.info("Generating code for goal: {}", goal);

        String prompt = buildPrompt(goal);

        try {
            String response = callGeminiAPI(prompt);
            String generatedCode = extractCode(response);

            log.info("Generated {} characters of code", generatedCode.length());
            return generatedCode;

        } catch (Exception e) {
            log.error("Failed to generate code", e);
            throw new RuntimeException("Code generation failed: " + e.getMessage(), e);
        }
    }

    /**
     * Call Gemini API.
     */
    private String callGeminiAPI(String prompt) {
        String endpoint = String.format("/models/%s:generateContent?key=%s", model, apiKey);

        // Build request body
        Map<String, Object> requestBody = Map.of(
                "contents", new Object[] {
                        Map.of("parts", new Object[] {
                                Map.of("text", prompt)
                        })
                },
                "generationConfig", Map.of(
                        "temperature", temperature,
                        "maxOutputTokens", 2048
                )
        );

        // Call API
        String response = geminiWebClient.post()
                .uri(endpoint)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        // Extract text from response
        try {
            JsonNode root = objectMapper.readTree(response);
            JsonNode candidates = root.get("candidates");
            if (candidates != null && candidates.size() > 0) {
                JsonNode content = candidates.get(0).get("content");
                JsonNode parts = content.get("parts");
                if (parts != null && parts.size() > 0) {
                    return parts.get(0).get("text").asText();
                }
            }
        } catch (Exception e) {
            log.error("Failed to parse Gemini response", e);
        }

        return response;
    }

    /**
     * Build prompt for code generation.
     */
    private String buildPrompt(String goal) {
        return """
            You are an expert Java programmer. Generate clean, well-documented Java code
            for the following task:
            
            TASK: %s
            
            Requirements:
            1. Write complete, runnable Java code
            2. Include a main method if applicable
            3. Add comments explaining the logic
            4. Handle edge cases
            5. Return ONLY the code, wrapped in ```java code blocks
            
            Generate the code now:
            """.formatted(goal);
    }

    /**
     * Extract code from markdown blocks.
     */
    private String extractCode(String response) {
        // Remove ```java and ``` markers
        return response
                .replaceAll("```java\\n", "")
                .replaceAll("```", "")
                .trim();
    }
}