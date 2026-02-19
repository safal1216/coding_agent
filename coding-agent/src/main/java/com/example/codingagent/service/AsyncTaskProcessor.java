package com.example.codingagent.service;

import com.example.codingagent.domain.Task;
import com.example.codingagent.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;


@Service
@Slf4j
@RequiredArgsConstructor
public class AsyncTaskProcessor {

    private final TaskRepository taskRepository;
    private final AgentOrchestratorService agentOrchestratorService;

    /**
     * Process a task asynchronously.
     *
     * @param taskId ID of task to process
     * @return CompletableFuture that completes when task is done
     */
    @Async("taskExecutor") // Uses our custom thread pool
    public CompletableFuture<Void> processTaskAsync(UUID taskId) {
        log.info("Starting async processing for task: {}", taskId);

        try {
            // Load task
            Task task = taskRepository.findById(taskId)
                    .orElseThrow(() -> new RuntimeException("Task not found: " + taskId));

            // Process task (this runs the agent loop)
            agentOrchestratorService.processTask(task);

            log.info("Async processing completed for task: {}", taskId);

        } catch (Exception e) {
            log.error("Error in async task processing for task: {}", taskId, e);
            // Error handling is done in orchestrator
        }

        return CompletableFuture.completedFuture(null);
    }
}