package com.example.codingagent.domain;

/**
 * Categorizes different types of errors that can occur during code generation and execution.
 *
 * WHY: Helps the reflection engine understand what went wrong and suggest appropriate fixes.
 * Similar errors can be grouped and learned from.
 */
public enum ErrorType {
    /**
     * Compilation errors (syntax errors, undefined variables, etc.)
     * Example: "cannot find symbol", "';' expected"
     */
    COMPILATION_ERROR,

    /**
     * Runtime exceptions during execution
     * Example: NullPointerException, ArrayIndexOutOfBoundsException
     */
    RUNTIME_ERROR,

    /**
     * Test assertions failed (code runs but wrong output)
     * Example: Expected 'true' but got 'false'
     */
    TEST_FAILURE,

    /**
     * Code execution timed out
     * Example: Infinite loop, very slow algorithm
     */
    TIMEOUT,

    /**
     * Code violates safety constraints
     * Example: Tries to access file system, network, or execute system commands
     */
    SAFETY_VIOLATION,

    /**
     * Logic errors (wrong algorithm, off-by-one, etc.)
     * Example: Code runs and compiles but produces wrong results
     */
    LOGIC_ERROR,

    /**
     * Unable to parse or understand test cases
     * Example: Malformed test case format
     */
    TEST_PARSE_ERROR,

    /**
     * AI generated incomplete or malformed code
     * Example: Missing closing braces, incomplete functions
     */
    MALFORMED_CODE,

    /**
     * Unknown or uncategorized error
     */
    UNKNOWN;

    /**
     * Infer error type from error message.
     *
     * @param errorMessage The error message from compilation/execution
     * @return The most likely ErrorType
     */
    public static ErrorType fromErrorMessage(String errorMessage) {
        if (errorMessage == null || errorMessage.isEmpty()) {
            return UNKNOWN;
        }

        String lowerMsg = errorMessage.toLowerCase();

        // Compilation errors
        if (lowerMsg.contains("cannot find symbol") ||
                lowerMsg.contains("expected") ||
                lowerMsg.contains("illegal start") ||
                lowerMsg.contains("class, interface, or enum expected") ||
                lowerMsg.contains("';' expected")) {
            return COMPILATION_ERROR;
        }

        // Runtime errors
        if (lowerMsg.contains("exception") ||
                lowerMsg.contains("nullpointerexception") ||
                lowerMsg.contains("arrayindexoutofbounds") ||
                lowerMsg.contains("arithmeticexception")) {
            return RUNTIME_ERROR;
        }

        // Test failures
        if (lowerMsg.contains("expected") && lowerMsg.contains("but was") ||
                lowerMsg.contains("assertion failed") ||
                lowerMsg.contains("test failed")) {
            return TEST_FAILURE;
        }

        // Timeout
        if (lowerMsg.contains("timeout") ||
                lowerMsg.contains("timed out") ||
                lowerMsg.contains("time limit exceeded")) {
            return TIMEOUT;
        }

        // Safety violations
        if (lowerMsg.contains("security") ||
                lowerMsg.contains("access denied") ||
                lowerMsg.contains("permission") ||
                lowerMsg.contains("forbidden")) {
            return SAFETY_VIOLATION;
        }

        return UNKNOWN;
    }
}