package com.example.codingagent.repository;

import com.example.codingagent.domain.CodeAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CodeAttemptRepository extends JpaRepository<CodeAttempt, UUID> {

    List<CodeAttempt> findByTaskIdOrderByIterationNumberAsc(UUID taskId);
}