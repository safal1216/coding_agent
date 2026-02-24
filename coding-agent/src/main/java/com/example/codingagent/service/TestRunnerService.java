package com.example.codingagent.service;

import com.example.codingagent.dto.ExecutionResult;
import com.example.codingagent.dto.TestResult;
import com.example.codingagent.util.CodeSanitizer;
import com.example.codingagent.util.TestCaseParser;
import com.example.codingagent.util.TestCaseParser.ParsedTestCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;


@Service
@Slf4j
@RequiredArgsConstructor
public class TestRunnerService {

    private final CodeExecutionService codeExecutionService;
    private final TestCaseParser testCaseParser;
    private final CodeSanitizer codeSanitizer;

    /**
     * Run all test cases against the generated code.
     *
     * @param code The generated code to test
     * @param testCases List of test case strings
     * @return Test results
     */

    public TestResult runTests(String code, List<String> testCases) {
        log.info("Running {} test cases", testCases.size());

        // Parse test cases
        List<ParsedTestCase> parsedTests = testCaseParser.parseAll(testCases);

        if (parsedTests.isEmpty()) {
            log.warn("No valid test cases to run");
            return TestResult.builder()
                    .allPassed(false)
                    .passedCount(0)
                    .failedCount(testCases.size())
                    .failureSummary("Could not parse any test cases")
                    .build();
        }

        // Extract method name from code (assume first public method)
        String methodName = extractMethodName(code);

        // Generate test code
        String testCode = generateTestCode(code, parsedTests, methodName);

        log.info("Final test code being executed:\n{}", testCode);
        // Execute test code
        ExecutionResult executionResult = codeExecutionService.execute(testCode);

        // If execution failed, all tests fail
        if (!executionResult.isSuccess()) {
            return TestResult.builder()
                    .allPassed(false)
                    .passedCount(0)
                    .failedCount(parsedTests.size())
                    .failureSummary("Execution failed: " + executionResult.getErrorMessage())
                    .build();
        }

        // Parse output and compare with expected
        return compareResults(parsedTests, executionResult.getStdout());
    }

    /**
     * Extract the first public method name from code.
     * Used to call the method in tests.
     */
    private String extractMethodName(String code) {
        // Simple regex to find first public method
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
                "public\\s+\\w+\\s+(\\w+)\\s*\\("
        );
        java.util.regex.Matcher matcher = pattern.matcher(code);

        if (matcher.find()) {
            return matcher.group(1);
        }

        // Default
        return "solution";
    }

    /**
     * Generate test code that executes all test cases and prints results.
     *
     * Example output:
     * public class Solution {
     *     public boolean isPrime(int n) { ... }
     *
     *     public static void main(String[] args) {
     *         Solution sol = new Solution();
     *         System.out.println(sol.isPrime(2));   // Test case 1
     *         System.out.println(sol.isPrime(4));   // Test case 2
     *     }
     * }
     */
    private String generateTestCode(String code, List<ParsedTestCase> tests, String methodName) {
        String className = codeSanitizer.extractClassName(code);

        // Build test main method
        StringBuilder testMain = new StringBuilder();
        testMain.append("    public static void main(String[] args) {\n");
        testMain.append("        ").append(className).append(" sol = new ").append(className).append("();\n");

        for (int i = 0; i < tests.size(); i++) {
            ParsedTestCase test = tests.get(i);
            testMain.append("        // Test case ").append(i + 1).append(": ").append(test.getOriginal()).append("\n");
            testMain.append("        System.out.println(sol.").append(methodName).append("(").append(test.getInput()).append("));\n");
        }

        testMain.append("    }\n");

        // Insert test main into code
        // Remove existing main method if present
        code = code.replaceAll("public\\s+static\\s+void\\s+main\\s*\\([^)]*\\)[^}]*\\{[^}]*\\}", "");

        // Insert before last closing brace
        int lastBrace = code.lastIndexOf("}");
        if (lastBrace == -1) {
            log.error("Could not find closing brace in code");
            return code;
        }

        return code.substring(0, lastBrace) + "\n" + testMain.toString() + "}\n";
    }

    /**
     * Compare actual output with expected output for each test case.
     */
    private TestResult compareResults(List<ParsedTestCase> tests, String stdout) {
        String[] outputLines = stdout.trim().split("\n");

        List<TestResult.TestCaseResult> results = new ArrayList<>();
        int passedCount = 0;
        int failedCount = 0;

        for (int i = 0; i < tests.size(); i++) {
            ParsedTestCase test = tests.get(i);
            String expected = test.getExpectedOutput().trim();

            // Get actual output (ith line of stdout)
            String actual = "";
            if (i < outputLines.length) {
                actual = outputLines[i].trim();
            }

            // Compare
            boolean passed = actual.equals(expected);

            if (passed) {
                passedCount++;
            } else {
                failedCount++;
            }

            results.add(TestResult.TestCaseResult.builder()
                    .testCase(test.getOriginal())
                    .passed(passed)
                    .input(test.getInput())
                    .expected(expected)
                    .actual(actual)
                    .errorMessage(passed ? null : "Expected '" + expected + "' but got '" + actual + "'")
                    .build());
        }

        // Build failure summary
        StringBuilder failureSummary = new StringBuilder();
        for (TestResult.TestCaseResult result : results) {
            if (!result.isPassed()) {
                failureSummary.append("Test: ").append(result.getTestCase()).append("\n");
                failureSummary.append("  Expected: ").append(result.getExpected()).append("\n");
                failureSummary.append("  Actual: ").append(result.getActual()).append("\n");
            }
        }

        return TestResult.builder()
                .testCases(results)
                .allPassed(failedCount == 0)
                .passedCount(passedCount)
                .failedCount(failedCount)
                .failureSummary(failureSummary.toString())
                .build();
    }
}