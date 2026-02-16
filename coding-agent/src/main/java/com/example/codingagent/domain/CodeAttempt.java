package com.example.codingagent.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "code_attempts")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CodeAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "task_id", nullable = false)
    @ToString.Exclude
    private Task task;

    @Column(name = "iteration_number", nullable = false)
    private Integer iterationNumber;

    @Column(name = "generated_code", nullable = false, columnDefinition = "TEXT")
    private String generatedCode;

    @Column(name = "test_passed", nullable = false)
    @Builder.Default
    private Boolean testPassed = false;

    @Column(name = "test_output", columnDefinition = "TEXT")
    private String testOutput;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "error_type", length = 100)
    private String errorType;

    @Column(name = "reflection_analysis", columnDefinition = "TEXT")
    private String reflectionAnalysis;

    @Column(name = "root_cause", columnDefinition = "TEXT")
    private String rootCause;

    @Column(name = "suggested_fix", columnDefinition = "TEXT")
    private String suggestedFix;

    @Column(name = "execution_time_ms")
    private Integer executionTimeMs;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}