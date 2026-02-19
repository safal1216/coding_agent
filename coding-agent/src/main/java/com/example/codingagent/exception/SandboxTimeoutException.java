package com.example.codingagent.exception;

/**
 * Exception thrown when code execution exceeds the timeout limit.
 *
 * Common causes:
 * - Infinite loops
 * - Very slow algorithms
 * - Excessive computation
 */
public class SandboxTimeoutException extends RuntimeException {

    /**
     * How long the code ran before timeout (milliseconds).
     */
    private final long executionTimeMs;

    /**
     * The timeout limit that was exceeded (milliseconds).
     */
    private final long timeoutLimitMs;

    public SandboxTimeoutException(long executionTimeMs, long timeoutLimitMs) {
        super(String.format(
                "Code execution timed out after %d ms (limit: %d ms)",
                executionTimeMs, timeoutLimitMs
        ));
        this.executionTimeMs = executionTimeMs;
        this.timeoutLimitMs = timeoutLimitMs;
    }

    public SandboxTimeoutException(String message, long executionTimeMs, long timeoutLimitMs) {
        super(message);
        this.executionTimeMs = executionTimeMs;
        this.timeoutLimitMs = timeoutLimitMs;
    }

    public long getExecutionTimeMs() {
        return executionTimeMs;
    }

    public long getTimeoutLimitMs() {
        return timeoutLimitMs;
    }
}