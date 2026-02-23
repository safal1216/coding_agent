package com.example.codingagent.service;

import com.example.codingagent.dto.ExecutionResult;
import com.example.codingagent.util.CodeSanitizer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

/**
 * FIXED VERSION - Properly checks exit codes instead of stderr content
 */
@Service
@Slf4j
public class CodeExecutionService {

    private final CodeSanitizer codeSanitizer;

    @Value("${agent.execution.timeout-seconds:30}")
    private int timeoutSeconds;

    @Autowired
    public CodeExecutionService(CodeSanitizer codeSanitizer) {
        this.codeSanitizer = codeSanitizer;
    }

    public ExecutionResult execute(String code) {
        log.info("Executing code ({} chars)", code.length());

        long startTime = System.currentTimeMillis();

        try {
            // Sanitize code
            code = codeSanitizer.sanitize(code);

            // Create temporary directory
            Path tempDir = Files.createTempDirectory("code-execution-");
            try {
                // Extract class name
                String className = codeSanitizer.extractClassName(code);
                log.debug("Extracted class name: {}", className);

                // Write code to file
                Path javaFile = tempDir.resolve(className + ".java");
                Files.writeString(javaFile, code);

                // List files for debugging
                log.debug("Files in tempDir before compilation:");
                Files.list(tempDir).forEach(f -> log.debug(" - {}", f.getFileName()));

                // Compile
                ExecutionResult compileResult = compile(tempDir, javaFile);
                if (!compileResult.isCompiled()) {
                    log.error("Compilation failed. Exit code: {}, Stderr: {}",
                            compileResult.getExitCode(), compileResult.getStderr());
                    compileResult.setExecutionTimeMs(System.currentTimeMillis() - startTime);
                    return compileResult;
                }

                log.info("Compilation succeeded!");

                // Execute
                ExecutionResult executeResult = run(tempDir, className);
                executeResult.setExecutionTimeMs(System.currentTimeMillis() - startTime);

                log.info("Execution completed. Exit code: {}", executeResult.getExitCode());
                log.debug("Stdout: {}", executeResult.getStdout());
                log.debug("Stderr: {}", executeResult.getStderr());

                return executeResult;

            } finally {
                // Clean up temp directory
                deleteDirectory(tempDir);
            }

        } catch (IOException e) {
            log.error("I/O error during code execution", e);
            return ExecutionResult.builder()
                    .compiled(false)
                    .executed(false)
                    .stderr("I/O error: " + e.getMessage())
                    .exceptionMessage(e.getMessage())
                    .executionTimeMs(System.currentTimeMillis() - startTime)
                    .build();
        }
    }

    /**
     * Compile Java file - FIXED to check exit code properly
     */
    private ExecutionResult compile(Path workDir, Path javaFile) {
        log.debug("Compiling: {}", javaFile.getFileName());

        try {
            ProcessBuilder pb = new ProcessBuilder(
                    "javac",
                    javaFile.getFileName().toString()  // Use just filename
            );
            pb.directory(workDir.toFile());
            pb.redirectErrorStream(false);  // Keep stdout and stderr separate

            Process process = pb.start();

            // Read stdout and stderr separately
            String stdout = readStream(process.getInputStream());
            String stderr = readStream(process.getErrorStream());

            // Wait for compilation
            boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);

            if (!finished) {
                process.destroyForcibly();
                return ExecutionResult.builder()
                        .compiled(false)
                        .executed(false)
                        .stderr("Compilation timed out")
                        .timedOut(true)
                        .exitCode(-1)
                        .build();
            }

            int exitCode = process.exitValue();

            // FIX: Check exit code, not stderr content!
            boolean compiledSuccessfully = (exitCode == 0);

            log.debug("Compilation exit code: {}", exitCode);
            log.debug("Compilation stdout: {}", stdout);
            log.debug("Compilation stderr: {}", stderr);

            if (compiledSuccessfully) {
                // Check that .class file was created
                String className = javaFile.getFileName().toString().replace(".java", "");
                Path classFile = workDir.resolve(className + ".class");
                boolean classFileExists = Files.exists(classFile);

                log.debug("Class file exists: {}", classFileExists);

                if (!classFileExists) {
                    return ExecutionResult.builder()
                            .compiled(false)
                            .executed(false)
                            .stdout(stdout)
                            .stderr("Compilation reported success but .class file not found")
                            .exitCode(exitCode)
                            .build();
                }

                return ExecutionResult.builder()
                        .compiled(true)
                        .executed(false)
                        .stdout(stdout)
                        .stderr(stderr)
                        .exitCode(exitCode)
                        .build();
            } else {
                return ExecutionResult.builder()
                        .compiled(false)
                        .executed(false)
                        .stdout(stdout)
                        .stderr(stderr.isEmpty() ? "Compilation failed with exit code " + exitCode : stderr)
                        .exitCode(exitCode)
                        .build();
            }

        } catch (Exception e) {
            log.error("Compilation failed with exception", e);
            return ExecutionResult.builder()
                    .compiled(false)
                    .executed(false)
                    .stderr("Compilation error: " + e.getMessage())
                    .exceptionMessage(e.getMessage())
                    .exitCode(-1)
                    .build();
        }
    }

    /**
     * Run compiled code - FIXED to check exit code properly
     */
    private ExecutionResult run(Path workDir, String className) {
        log.debug("Executing: {}", className);

        try {
            ProcessBuilder pb = new ProcessBuilder(
                    "java",
                    className
            );
            pb.directory(workDir.toFile());
            pb.redirectErrorStream(false);

            Process process = pb.start();

            // Read stdout and stderr separately
            String stdout = readStream(process.getInputStream());
            String stderr = readStream(process.getErrorStream());

            // Wait for execution
            boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);

            if (!finished) {
                process.destroyForcibly();
                return ExecutionResult.builder()
                        .compiled(true)
                        .executed(false)
                        .stdout(stdout)
                        .stderr("Execution timed out")
                        .timedOut(true)
                        .exitCode(-1)
                        .build();
            }

            int exitCode = process.exitValue();

            // FIX: Check exit code for success
            boolean executedSuccessfully = (exitCode == 0);

            log.debug("Execution exit code: {}", exitCode);
            log.debug("Execution stdout: {}", stdout);
            log.debug("Execution stderr: {}", stderr);

            return ExecutionResult.builder()
                    .compiled(true)
                    .executed(executedSuccessfully)
                    .stdout(stdout)
                    .stderr(stderr)
                    .exitCode(exitCode)
                    .build();

        } catch (Exception e) {
            log.error("Execution failed with exception", e);
            return ExecutionResult.builder()
                    .compiled(true)
                    .executed(false)
                    .stderr("Execution error: " + e.getMessage())
                    .exceptionMessage(e.getMessage())
                    .exitCode(-1)
                    .build();
        }
    }

    /**
     * Read all content from a stream
     */
    private String readStream(java.io.InputStream inputStream) {
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(inputStream))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
        } catch (IOException e) {
            log.error("Error reading stream", e);
        }
        return output.toString().trim();
    }

    /**
     * Recursively delete directory
     */
    private void deleteDirectory(Path dir) {
        try {
            if (Files.exists(dir)) {
                Files.walk(dir)
                        .sorted((a, b) -> -a.compareTo(b))
                        .forEach(path -> {
                            try {
                                Files.delete(path);
                            } catch (IOException e) {
                                log.warn("Failed to delete {}", path);
                            }
                        });
            }
        } catch (IOException e) {
            log.error("Error cleaning up directory: {}", dir, e);
        }
    }
}