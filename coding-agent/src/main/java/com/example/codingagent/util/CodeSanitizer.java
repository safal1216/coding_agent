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

    /**
     * Wrap code in a class if it's just a method.
     * Example: "public int add(int a, int b)" -> full class with main method
     */
    public String wrapIfNeeded(String code) {
        // If code already has a class declaration, return as-is
        if (code.contains("class ")) {
            return code;
        }

        // Wrap in a class
        return String.format("""
            public class Solution {
                %s
                
                public static void main(String[] args) {
                    // Test the method
                    Solution sol = new Solution();
                    System.out.println("Code is ready");
                }
            }
            """, code);
    }

    /**
     * Ensure code has a main method for execution.
     * If not, add one that tests the code.
     */
    public String ensureMainMethod(String code) {
        // If already has main method, return as-is
        if (code.contains("public static void main")) {
            return code;
        }

        // Extract class name
        String className = extractClassName(code);

        // Add main method before the closing brace
        int lastBrace = code.lastIndexOf("}");
        if (lastBrace == -1) {
            return code; // Invalid code, let compilation fail
        }

        String mainMethod = """
            
            public static void main(String[] args) {
                System.out.println("Solution ready for testing");
            }
        """;

        return code.substring(0, lastBrace) + mainMethod + "\n}";
    }

    /**
     * Validate that code doesn't contain dangerous patterns.
     * Returns null if safe, error message if unsafe.
     */
    public String validateSafety(String code) {
        if (code == null) {
            return null;
        }

        String lowerCode = code.toLowerCase();

        // Check for dangerous patterns
        if (lowerCode.contains("runtime.getruntime()") ||
                lowerCode.contains("processbuilder")) {
            return "Code contains forbidden system command execution";
        }

        if (lowerCode.contains("file") ||
                lowerCode.contains("filewriter") ||
                lowerCode.contains("filereader")) {
            return "Code contains forbidden file system access";
        }

        if (lowerCode.contains("socket") ||
                lowerCode.contains("url") ||
                lowerCode.contains("httpurlconnection")) {
            return "Code contains forbidden network access";
        }

        if (lowerCode.contains("system.exit")) {
            return "Code contains forbidden System.exit()";
        }

        if (lowerCode.contains("reflection") ||
                lowerCode.contains("class.forname")) {
            return "Code contains forbidden reflection";
        }

        return null; // Safe
    }
}