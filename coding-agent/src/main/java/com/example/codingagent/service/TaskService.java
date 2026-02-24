package com.example.codingagent.service;

import com.example.codingagent.domain.Task;
import com.example.codingagent.domain.TaskStatus;
import com.example.codingagent.dto.TaskRequest;
import com.example.codingagent.dto.TaskResponse;
import com.example.codingagent.exception.TaskNotFoundException;
import com.example.codingagent.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TaskService {

    private final TaskRepository taskRepository;
    private final AsyncTaskProcessor asyncTaskProcessor;

    /**
     * Create a new task and trigger async processing.
     */
    @Transactional
    public TaskResponse createTask(TaskRequest request) {
        log.info("Creating new task: {}", request.getGoal());

        // Create task entity
        Task task = Task.builder()
                .goal(request.getGoal())
                .description(request.getDescription())
                .language(request.getLanguage())
                .testCases(request.getTestCases())
                .status(TaskStatus.PENDING)
                .maxIterations(10)
                .build();

        // Save to database
        Task saved = taskRepository.save(task);
        log.info("Created task with ID: {}", saved.getId());

        // Trigger async processing AFTER transaction commits
        // This prevents "Task not found" errors
        UUID taskId = saved.getId();
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        log.info("Transaction committed, triggering async processing for task: {}", taskId);
                        asyncTaskProcessor.processTaskAsync(taskId);
                    }
                }
        );

        return toResponse(saved);
    }

    /**
     * Get a task by ID.
     */
    @Transactional(readOnly = true)
    public TaskResponse getTask(UUID taskId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new TaskNotFoundException(taskId));
        return toResponse(task);
    }

    /**
     * List tasks with optional filtering.
     */
    @Transactional(readOnly = true)
    public List<TaskResponse> listTasks(String statusFilter, int page, int size) {
        if (statusFilter != null && !statusFilter.isEmpty()) {
            TaskStatus status = TaskStatus.valueOf(statusFilter.toUpperCase());
            return taskRepository.findByStatus(status, PageRequest.of(page, size))
                    .map(this::toResponse)
                    .getContent();
        }

        return taskRepository.findAll(PageRequest.of(page, size))
                .map(this::toResponse)
                .getContent();
    }

    /**
     * Cancel a running task.
     */
    @Transactional
    public void cancelTask(UUID taskId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new TaskNotFoundException(taskId));

        if (task.getStatus() == TaskStatus.IN_PROGRESS ||
                task.getStatus() == TaskStatus.PENDING) {
            task.setStatus(TaskStatus.CANCELLED);
            taskRepository.save(task);
            log.info("Cancelled task: {}", taskId);
        }
    }

    /**
     * Convert entity to response DTO.
     */
    private TaskResponse toResponse(Task task) {
        return TaskResponse.builder()
                .taskId(task.getId())
                .goal(task.getGoal())
                .status(task.getStatus())
                .currentIteration(task.getCurrentIteration())
                .maxIterations(task.getMaxIterations())
                .generatedCode(task.getGeneratedCode())
                .errorMessage(task.getErrorMessage())
                .createdAt(task.getCreatedAt())
                .completedAt(task.getCompletedAt())
                .build();
    }
}