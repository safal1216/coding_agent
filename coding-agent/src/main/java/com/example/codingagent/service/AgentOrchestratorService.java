package com.example.codingagent.service;

import com.example.codingagent.domain.CodeAttempt;
import com.example.codingagent.domain.ErrorType;
import com.example.codingagent.domain.Task;
import com.example.codingagent.domain.TaskStatus;
import com.example.codingagent.dto.ExecutionResult;
import com.example.codingagent.dto.ReflectionResult;
import com.example.codingagent.dto.TestResult;
import com.example.codingagent.repository.CodeAttemptRepository;
import com.example.codingagent.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class AgentOrchestratorService {

    private final TaskRepository taskRepository;
    private final CodeAttemptRepository codeAttemptRepository;
    private final CodeGenerationService codeGenerationService;
    private final CodeExecutionService codeExecutionService;
    private final TestRunnerService testRunnerService;
    private final ReflectionService reflectionService;
    private final SafetyAnalyzerService safetyAnalyzerService;

    @Value("${agent.max-iterations:10}")
    private int maxIterations;

    /**
     * Run the agent loop for a task.
     *
     * This is the main entry point.
     */
    public void processTask(Task task) {
        log.info("Starting agent loop for task: {}", task.getId());

        try {
            // Update task status
            task.setStatus(TaskStatus.IN_PROGRESS);
            task.setStartedAt(Instant.now());
            taskRepository.save(task);

            // Run the loop
            boolean success = runAgentLoop(task);

            // Update final status
            if (success) {
                task.setStatus(TaskStatus.COMPLETED);
                log.info("Task {} completed successfully!", task.getId());
            } else {
                task.setStatus(TaskStatus.FAILED);
                task.setErrorMessage("Failed after " + task.getCurrentIteration() + " iterations");
                log.warn("Task {} failed after {} iterations", task.getId(), task.getCurrentIteration());
            }

            task.setCompletedAt(Instant.now());
            taskRepository.save(task);

        } catch (Exception e) {
            log.error("Error processing task {}", task.getId(), e);
            task.setStatus(TaskStatus.FAILED);
            task.setErrorMessage("Internal error: " + e.getMessage());
            task.setCompletedAt(Instant.now());
            taskRepository.save(task);
        }
    }

    /**
     * The main agent loop.
     *
     * Returns true if successful, false if max iterations reached.
     */
    private boolean runAgentLoop(Task task) {
        String currentPrompt = buildInitialPrompt(task);
        List<CodeAttempt> attempts = new ArrayList<>();

        for (int iteration = 0; iteration < maxIterations; iteration++) {
            log.info("=== Iteration {} / {} ===", iteration + 1, maxIterations);

            task.setCurrentIteration(iteration + 1);
            taskRepository.save(task);

            // STEP 1: GENERATE CODE
            log.info("Step 1: Generating code");
            String generatedCode;
            try {
                generatedCode = codeGenerationService.generateCode(currentPrompt);
                log.info("Generated {} chars of code", generatedCode.length());
            } catch (Exception e) {
                log.error("Code generation failed", e);
                continue; // Try again
            }

            // STEP 2: SAFETY CHECK
            log.info("Step 2: Safety check");
            SafetyAnalyzerService.SafetyAnalysisResult safetyResult =
                    safetyAnalyzerService.analyze(generatedCode);

            if (!safetyResult.isSafe()) {
                log.warn("Code failed safety check: {}", safetyResult.getViolationsSummary());

                // Save this attempt
                CodeAttempt attempt = createAttempt(task, iteration, generatedCode, null, null);
                attempt.setErrorType(ErrorType.SAFETY_VIOLATION.name());
                attempt.setErrorMessage(safetyResult.getViolationsSummary());
                attempt.setReflectionAnalysis("Code contains forbidden operations");
                codeAttemptRepository.save(attempt);
                attempts.add(attempt);

                // Update prompt to avoid safety violations
                currentPrompt = currentPrompt + "\n\nIMPORTANT: Do NOT use: " +
                        safetyResult.getViolationsSummary();

                continue; // Try again
            }

            // STEP 3: EXECUTE CODE
            log.info("Step 3: Executing code");
            ExecutionResult executionResult = codeExecutionService.execute(generatedCode);

            // STEP 4: RUN TESTS
            log.info("Step 4: Running tests");
            TestResult testResult = testRunnerService.runTests(generatedCode, task.getTestCases());

            log.info("Test results: {} passed, {} failed",
                    testResult.getPassedCount(), testResult.getFailedCount());

            // STEP 5: CHECK SUCCESS
            if (testResult.isAllPassed()) {
                log.info("🎉 All tests passed! Task completed.");

                // Save successful attempt
                CodeAttempt attempt = createAttempt(task, iteration, generatedCode, executionResult, testResult);
                attempt.setTestPassed(true);
                codeAttemptRepository.save(attempt);

                // Save solution to task
                task.setGeneratedCode(generatedCode);

                return true; // SUCCESS!
            }

            // STEP 6: REFLECT ON FAILURE
            log.info("Step 5: Reflecting on failure");
            ReflectionResult reflection = reflectionService.reflect(
                    generatedCode,
                    executionResult,
                    testResult,
                    task.getGoal(),
                    attempts
            );

            log.info("Reflection: {} - {}", reflection.getErrorType(), reflection.getRootCause());

            // Save attempt with reflection
            CodeAttempt attempt = createAttempt(task, iteration, generatedCode, executionResult, testResult);
            attempt.setTestPassed(false);
            attempt.setErrorType(reflection.getErrorType().name());
            attempt.setErrorMessage(executionResult.getErrorMessage());
            attempt.setReflectionAnalysis(reflection.getAnalysis());
            attempt.setRootCause(reflection.getRootCause());
            attempt.setSuggestedFix(reflection.getSuggestedFix());
            codeAttemptRepository.save(attempt);
            attempts.add(attempt);

            // STEP 7: UPDATE PROMPT FOR NEXT ITERATION
            currentPrompt = reflection.getEnhancedPrompt();

            log.info("Will retry with enhanced prompt");
        }

        // Max iterations reached without success
        log.warn("Max iterations ({}) reached without success", maxIterations);
        return false;
    }

    /**
     * Build initial prompt from task.
     */
    private String buildInitialPrompt(Task task) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("You are an expert Java programmer. Generate clean, well-documented Java code.\n\n");
        prompt.append("TASK: ").append(task.getGoal()).append("\n\n");

        if (task.getDescription() != null && !task.getDescription().isEmpty()) {
            prompt.append("DESCRIPTION: ").append(task.getDescription()).append("\n\n");
        }

        prompt.append("TEST CASES:\n");
        for (String testCase : task.getTestCases()) {
            prompt.append("- ").append(testCase).append("\n");
        }

        prompt.append("\nREQUIREMENTS:\n");
        prompt.append("1. Write complete, runnable Java code\n");
        prompt.append("2. Include a main method that tests the code\n");
        prompt.append("3. Add comments explaining the logic\n");
        prompt.append("4. Handle ALL edge cases from test cases\n");
        prompt.append("5. Return ONLY the code, wrapped in ```java code blocks\n");
        prompt.append("6. Do NOT use file I/O, network, or system commands\n");
        prompt.append("\nGenerate the code now:");

        return prompt.toString();
    }

    /**
     * Create a CodeAttempt entity.
     */
    private CodeAttempt createAttempt(
            Task task,
            int iteration,
            String code,
            ExecutionResult executionResult,
            TestResult testResult
    ) {
        CodeAttempt attempt = new CodeAttempt();
        attempt.setTask(task);
        attempt.setIterationNumber(iteration);
        attempt.setGeneratedCode(code);

        if (executionResult != null) {
            attempt.setTestOutput(executionResult.getStdout());
            attempt.setExecutionTimeMs((int) executionResult.getExecutionTimeMs());
        }

        return attempt;
    }
}