package com.example.codingagent.util;

import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * Utility for sanitizing and validating generated code.
 *
 * Performs:
 * - Remove markdown formatting
 * - Extract class name
 * - Validate basic syntax
 * - Clean whitespace
 */
@Component
public class CodeSanitizer {

    /**
     * Sanitize generated code.
     * Removes markdown, cleans whitespace, validates basic structure.
     */
    public String sanitize(String code) {
        if (code == null || code.isEmpty()) {
            return code;
        }

        // Remove markdown code fences
        code = removeMarkdown(code);

        // Trim whitespace
        code = code.trim();

        // Ensure it has at least a class or method
        if (!hasValidStructure(code)) {
            throw new IllegalArgumentException("Code does not contain a valid class or method");
        }

        return code;
    }

    /**
     * Remove markdown code fences.
     */
    private String removeMarkdown(String code) {
        // Remove ```java\n at start
        code = code.replaceAll("^```java\\s*\n", "");

        // Remove ``` at end
        code = code.replaceAll("\n?```\\s*$", "");

        // Remove any remaining ```
        code = code.replaceAll("```", "");

        return code;
    }

    /**
     * Check if code has valid structure (class, method, or interface).
     */
    private boolean hasValidStructure(String code) {
        // Must have at least one of: class, interface, enum, or method signature
        return code.contains("class ") ||
                code.contains("interface ") ||
                code.contains("enum ") ||
                code.contains("public ") ||
                code.contains("private ") ||
                code.contains("protected ");
    }

    /**
     * Extract the main class name from code.
     * Used for creating the Java file.
     */
    public String extractClassName(String code) {
        // Pattern: public class ClassName
        Pattern pattern = Pattern.compile("public\\s+class\\s+(\\w+)");
        var matcher = pattern.matcher(code);

        if (matcher.find()) {
            return matcher.group(1);
        }

        // Pattern: class ClassName (without public)
        pattern = Pattern.compile("class\\s+(\\w+)");
        matcher = pattern.matcher(code);

        if (matcher.find()) {
            return matcher.group(1);
        }

        // Default name if no class found
        return "Solution";
    }

}