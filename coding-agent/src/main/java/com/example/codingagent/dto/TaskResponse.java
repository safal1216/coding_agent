package com.example.codingagent.dto;

import com.example.codingagent.domain.TaskStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskResponse {

    private UUID taskId;
    private String goal;
    private TaskStatus status;
    private Integer currentIteration;
    private Integer maxIterations;
    private String generatedCode;
    private String errorMessage;
    private Instant createdAt;
    private Instant completedAt;
}