package com.example.codingagent.exception;

/**
 * Exception thrown when code execution fails.
 *
 * This includes:
 * - Compilation failures
 * - Runtime errors
 * - Sandbox errors
 */
public class CodeExecutionException extends RuntimeException {

    /**
     * Standard output from the execution.
     */
    private final String stdout;

    /**
     * Standard error from the execution.
     */
    private final String stderr;

    /**
     * Exit code from the process.
     */
    private final int exitCode;

    public CodeExecutionException(String message) {
        super(message);
        this.stdout = "";
        this.stderr = "";
        this.exitCode = -1;
    }

    public CodeExecutionException(String message, Throwable cause) {
        super(message, cause);
        this.stdout = "";
        this.stderr = "";
        this.exitCode = -1;
    }

    public CodeExecutionException(String message, String stdout, String stderr, int exitCode) {
        super(message);
        this.stdout = stdout;
        this.stderr = stderr;
        this.exitCode = exitCode;
    }

    public String getStdout() {
        return stdout;
    }

    public String getStderr() {
        return stderr;
    }

    public int getExitCode() {
        return exitCode;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(super.toString());
        if (stderr != null && !stderr.isEmpty()) {
            sb.append("\nStderr: ").append(stderr);
        }
        if (stdout != null && !stdout.isEmpty()) {
            sb.append("\nStdout: ").append(stdout);
        }
        if (exitCode != -1) {
            sb.append("\nExit code: ").append(exitCode);
        }
        return sb.toString();
    }
}