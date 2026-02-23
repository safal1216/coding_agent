package com.example.codingagent.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Service
@Slf4j
@RequiredArgsConstructor
public class CodeGenerationService {

    private final WebClient geminiWebClient;
    private final ObjectMapper objectMapper;

    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.api.model}")
    private String model;

    @Value("${gemini.api.temperature:0.4}")
    private Double temperature;

    @Value("${gemini.api.max-tokens:2048}")
    private Integer maxTokens;


    public String generateCode(String prompt) {
        log.info("Loaded Gemini API Key: {}", apiKey);
        log.info("Generating code ({} chars prompt)", prompt.length());

        try {
            // Call Gemini API
            String response = callGeminiAPI(prompt);

            // Extract and clean code
            String generatedCode = extractCode(response);

            log.info("Generated {} characters of code", generatedCode.length());
            return generatedCode;

        } catch (Exception e) {
            log.error("Failed to generate code", e);
            throw new RuntimeException("Code generation failed: " + e.getMessage(), e);
        }
    }

    /**
     * Call Gemini API with prompt.
     * FIXED: Using HashMap for better control and added error logging
     *
     * @param prompt The prompt to send
     * @return Raw response from Gemini
     */
    private String callGeminiAPI(String prompt) {
        String endpoint = String.format("/models/%s:generateContent", model);

        // ✅ Use HashMap for better control over structure
        Map<String, Object> partsMap = new HashMap<>();
        partsMap.put("text", prompt);

        Map<String, Object> contentMap = new HashMap<>();
        contentMap.put("role", "user");
        contentMap.put("parts", List.of(partsMap));

        Map<String, Object> configMap = new HashMap<>();
        configMap.put("temperature", temperature);
        configMap.put("maxOutputTokens", maxTokens);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("contents", List.of(contentMap));
        requestBody.put("generationConfig", configMap);

        try {
            log.info("Calling Gemini API - Endpoint: {}", endpoint);
            log.debug("Request body: {}", requestBody);

            // Call API
            String response = geminiWebClient.post()
                    .uri(endpoint)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            log.debug("Received response: {}", response);

            // Parse response
            return parseGeminiResponse(response);

        } catch (WebClientResponseException e) {
            // ✅ LOG THE ACTUAL ERROR RESPONSE FROM GEMINI
            log.error("Gemini API call failed with status: {}", e.getStatusCode());
            log.error("Response body: {}", e.getResponseBodyAsString());
            log.error("Full error:", e);
            throw new RuntimeException("Failed to call Gemini API: " + e.getMessage() +
                    " | Response: " + e.getResponseBodyAsString(), e);
        } catch (Exception e) {
            log.error("Gemini API call failed", e);
            throw new RuntimeException("Failed to call Gemini API: " + e.getMessage(), e);
        }
    }

    /**
     * Parse Gemini API response to extract generated text.
     *
     * Gemini response format:
     * {
     *   "candidates": [{
     *     "content": {
     *       "parts": [{"text": "...generated code..."}]
     *     }
     *   }]
     * }
     */
    private String parseGeminiResponse(String response) {
        try {
            JsonNode root = objectMapper.readTree(response);

            // Navigate: candidates[0].content.parts[0].text
            JsonNode candidates = root.get("candidates");
            if (candidates != null && candidates.size() > 0) {
                JsonNode content = candidates.get(0).get("content");
                if (content != null) {
                    JsonNode parts = content.get("parts");
                    if (parts != null && parts.size() > 0) {
                        JsonNode textNode = parts.get(0).get("text");
                        if (textNode != null) {
                            return textNode.asText();
                        }
                    }
                }
            }

            // Fallback: return raw response
            log.warn("Could not parse Gemini response structure, returning raw");
            return response;

        } catch (Exception e) {
            log.error("Failed to parse Gemini response", e);
            return response; // Return raw on parse error
        }
    }

    /**
     * Extract code from markdown blocks.
     *
     * Gemini often returns code wrapped in:
     * ```java
     * public class Solution { ... }
     * ```
     *
     * This method removes those markers.
     */
    private String extractCode(String response) {
        if (response == null || response.isEmpty()) {
            return "";
        }

        // Remove markdown code fences
        String code = response
                .replaceAll("```java\\s*\n", "")  // Remove opening ```java
                .replaceAll("\n?```\\s*$", "")     // Remove closing ```
                .replaceAll("```", "")             // Remove any remaining ```
                .trim();

        // Additional cleanup
        code = cleanupCode(code);

        return code;
    }

    /**
     * Additional code cleanup.
     *
     * Sometimes Gemini adds explanatory text before the code.
     * This tries to extract just the Java code.
     */
    private String cleanupCode(String code) {
        // If there's a class definition, extract from there
        if (code.contains("public class") || code.contains("class ")) {
            int classIndex = code.indexOf("public class");
            if (classIndex == -1) {
                classIndex = code.indexOf("class ");
            }
            if (classIndex > 0) {
                code = code.substring(classIndex);
            }
        }

        return code.trim();
    }
}