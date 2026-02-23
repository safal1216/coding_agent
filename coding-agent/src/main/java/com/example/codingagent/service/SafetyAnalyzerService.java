package com.example.codingagent.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Service for analyzing code safety.
 *
 * Checks for dangerous patterns:
 * - System command execution
 * - File system access
 * - Network access
 * - Reflection
 * - System.exit()
 * - Infinite loops (basic detection)
 */
@Service
@Slf4j
public class SafetyAnalyzerService {

    // Dangerous patterns
    private static final List<Pattern> FORBIDDEN_PATTERNS = List.of(
            Pattern.compile("Runtime\\.getRuntime\\(\\)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("ProcessBuilder", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bexec\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("FileWriter|FileReader|FileOutputStream|FileInputStream", Pattern.CASE_INSENSITIVE),
            Pattern.compile("Socket|ServerSocket|URL|HttpURLConnection", Pattern.CASE_INSENSITIVE),
            Pattern.compile("System\\.exit", Pattern.CASE_INSENSITIVE),
            Pattern.compile("Class\\.forName", Pattern.CASE_INSENSITIVE),
            Pattern.compile("ClassLoader", Pattern.CASE_INSENSITIVE),
            Pattern.compile("Thread\\.sleep", Pattern.CASE_INSENSITIVE)
    );

    /**
     * Analyze code for safety violations.
     *
     * @param code The code to analyze
     * @return Analysis result with violations found
     */
    public SafetyAnalysisResult analyze(String code) {
        log.debug("Analyzing code safety");

        List<String> violations = new ArrayList<>();

        // Check each forbidden pattern
        for (Pattern pattern : FORBIDDEN_PATTERNS) {
            if (pattern.matcher(code).find()) {
                violations.add("Forbidden pattern detected: " + pattern.pattern());
            }
        }

        // Check for potential infinite loops (basic)
        if (hasLikeLyInfiniteLoop(code)) {
            violations.add("Potential infinite loop detected");
        }

        boolean safe = violations.isEmpty();

        return new SafetyAnalysisResult(safe, violations);
    }

    /**
     * Basic check for infinite loops.
     * Looks for while(true) or for loops without clear termination.
     */
    private boolean hasLikeLyInfiniteLoop(String code) {
        // Check for while(true) without break
        if (code.contains("while(true)") || code.contains("while (true)")) {
            // Only flag if there's no break statement nearby
            String[] lines = code.split("\n");
            for (int i = 0; i < lines.length; i++) {
                if (lines[i].contains("while") && lines[i].contains("true")) {
                    // Check next few lines for break
                    boolean hasBreak = false;
                    for (int j = i; j < Math.min(i + 10, lines.length); j++) {
                        if (lines[j].contains("break") || lines[j].contains("return")) {
                            hasBreak = true;
                            break;
                        }
                    }
                    if (!hasBreak) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    /**
     * Result of safety analysis.
     */
    public static class SafetyAnalysisResult {
        private final boolean safe;
        private final List<String> violations;

        public SafetyAnalysisResult(boolean safe, List<String> violations) {
            this.safe = safe;
            this.violations = violations;
        }

        public boolean isSafe() {
            return safe;
        }

        public List<String> getViolations() {
            return violations;
        }

        public String getViolationsSummary() {
            if (violations.isEmpty()) {
                return "No safety violations";
            }
            return String.join("; ", violations);
        }
    }
}