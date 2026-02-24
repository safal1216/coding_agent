package com.example.codingagent.controller;

import com.example.codingagent.dto.TaskRequest;
import com.example.codingagent.dto.TaskResponse;
import com.example.codingagent.service.AsyncTaskProcessor;
import com.example.codingagent.service.TaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/tasks")
@RequiredArgsConstructor
@Slf4j
public class TaskController {

    private final TaskService taskService;

    @PostMapping
    public ResponseEntity<TaskResponse> createTask(@Valid @RequestBody TaskRequest request) {
        log.info("Received task request: {}", request.getGoal());

        TaskResponse response = taskService.createTask(request);

        log.info("Created task with ID: {}", response.getTaskId());

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    @GetMapping("/{taskId}")
    public ResponseEntity<TaskResponse> getTask(@PathVariable UUID taskId) {
        log.info("Fetching task: {}", taskId);

        TaskResponse response = taskService.getTask(taskId);

        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<TaskResponse>> listTasks(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        log.info("Listing tasks - status: {}, page: {}, size: {}", status, page, size);

        List<TaskResponse> tasks = taskService.listTasks(status, page, size);

        return ResponseEntity.ok(tasks);
    }

    @DeleteMapping("/{taskId}")
    public ResponseEntity<Void> cancelTask(@PathVariable UUID taskId) {
        log.info("Cancelling task: {}", taskId);

        taskService.cancelTask(taskId);

        return ResponseEntity.noContent().build();
    }
}