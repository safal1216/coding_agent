package com.example.codingagent.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaskRequest {

    @NotBlank(message = "Goal is required")
    private String goal;

    private String description;

    @NotEmpty(message = "At least one test case is required")
    private List<String> testCases;

    private String language = "java";
}