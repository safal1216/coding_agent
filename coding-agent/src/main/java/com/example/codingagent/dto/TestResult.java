package com.example.codingagent.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Result of running test cases against generated code.
 *
 * Contains:
 * - Which tests passed/failed
 * - Expected vs actual outputs
 * - Overall pass rate
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestResult {

    /**
     * Individual test case results.
     */
    @Builder.Default
    private List<TestCaseResult> testCases = new ArrayList<>();

    /**
     * Whether all tests passed.
     */
    private boolean allPassed;

    /**
     * Number of tests that passed.
     */
    private int passedCount;

    /**
     * Number of tests that failed.
     */
    private int failedCount;

    /**
     * Summary of failures.
     */
    private String failureSummary;

    /**
     * Result for a single test case.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TestCaseResult {
        /**
         * Original test case description.
         * Example: "Input: 2, Output: true"
         */
        private String testCase;

        /**
         * Whether this test passed.
         */
        private boolean passed;

        /**
         * Input that was tested.
         * Example: "2"
         */
        private String input;

        /**
         * Expected output.
         * Example: "true"
         */
        private String expected;

        /**
         * Actual output from code.
         * Example: "false" (if test failed)
         */
        private String actual;

        /**
         * Error message if test failed.
         */
        private String errorMessage;
    }

    /**
     * Calculate pass rate (0.0 to 1.0).
     */
    public double getPassRate() {
        int total = passedCount + failedCount;
        return total == 0 ? 0.0 : (double) passedCount / total;
    }

    /**
     * Get a summary string of test results.
     */
    public String getSummary() {
        return String.format("%d/%d tests passed (%.1f%%)",
                passedCount, passedCount + failedCount, getPassRate() * 100);
    }
}