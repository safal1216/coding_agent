package com.example.codingagent.util;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility for parsing natural language test cases.
 *
 * Converts:
 * "Input: 2, Output: true" -> TestCase{input="2", output="true"}
 * "Input: [1,2,3], Expected: 6" -> TestCase{input="[1,2,3]", output="6"}
 */
@Component
public class TestCaseParser {

    // Pattern: "Input: X, Output: Y" or "Input: X, Expected: Y"
    private static final Pattern PATTERN_1 = Pattern.compile(
            "(?i)input\\s*:\\s*(.+?)\\s*,\\s*(?:output|expected)\\s*:\\s*(.+)",
            Pattern.CASE_INSENSITIVE
    );

    // Pattern: "X -> Y" or "X => Y"
    private static final Pattern PATTERN_2 = Pattern.compile(
            "(.+?)\\s*(?:->|=>)\\s*(.+)"
    );

    /**
     * Parse a test case string into input and expected output.
     */
    public ParsedTestCase parse(String testCase) {
        if (testCase == null || testCase.trim().isEmpty()) {
            throw new IllegalArgumentException("Test case is empty");
        }

        testCase = testCase.trim();

        // pattern 1: "Input: X, Output: Y"
        Matcher matcher = PATTERN_1.matcher(testCase);
        if (matcher.find()) {
            String input = matcher.group(1).trim();
            String output = matcher.group(2).trim();
            return new ParsedTestCase(testCase, input, output);
        }

        // pattern 2: "X -> Y"
        matcher = PATTERN_2.matcher(testCase);
        if (matcher.find()) {
            String input = matcher.group(1).trim();
            String output = matcher.group(2).trim();
            return new ParsedTestCase(testCase, input, output);
        }

        // Could not parse
        throw new IllegalArgumentException("Could not parse test case: " + testCase);
    }

    /**
     * Parse multiple test cases.
     */
    public List<ParsedTestCase> parseAll(List<String> testCases) {
        List<ParsedTestCase> parsed = new ArrayList<>();

        for (String testCase : testCases) {
            try {
                parsed.add(parse(testCase));
            } catch (IllegalArgumentException e) {
                // Log warning but continue with other test cases
                System.err.println("Warning: Could not parse test case: " + testCase);
            }
        }

        return parsed;
    }

    /**
     * A parsed test case.
     */
    public static class ParsedTestCase {
        private final String original;
        private final String input;
        private final String expectedOutput;

        public ParsedTestCase(String original, String input, String expectedOutput) {
            this.original = original;
            this.input = input;
            this.expectedOutput = expectedOutput;
        }

        public String getOriginal() {
            return original;
        }

        public String getInput() {
            return input;
        }

        public String getExpectedOutput() {
            return expectedOutput;
        }

        /**
         * Generate Java code to test this case.
         * Example: System.out.println(isPrime(2));
         */
        public String toJavaTestCode(String methodName) {
            return String.format("System.out.println(%s(%s));", methodName, input);
        }

        @Override
        public String toString() {
            return String.format("Input: %s, Expected: %s", input, expectedOutput);
        }
    }
}