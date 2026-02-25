package com.example.codingagent.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Configuration for asynchronous task processing.
 *
 * WHY CUSTOM THREAD POOL?
 * - Control number of concurrent tasks
 * - Set queue capacity for pending tasks
 * - Name threads for debugging
 * - Set rejection policy
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    /**
     * Thread pool for async task processing.
     *
     * Configuration:
     * - Core pool: 5 threads (always alive)
     * - Max pool: 10 threads (scale up when needed)
     * - Queue: 100 tasks (waiting tasks)
     * - Keep-alive: 60s (for extra threads)
     */
    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // Core pool size: always running
        executor.setCorePoolSize(5);

        // Max pool size: scale up to this
        executor.setMaxPoolSize(10);

        // Queue capacity: pending tasks
        executor.setQueueCapacity(100);

        // Thread name prefix (for debugging)
        executor.setThreadNamePrefix("agent-task-");

        // Keep-alive time for extra threads
        executor.setKeepAliveSeconds(60);

        // What to do when queue is full
        // CALLER_RUNS: Execute in caller's thread (backpressure)
        executor.setRejectedExecutionHandler(
                new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy()
        );

        // Initialize
        executor.initialize();

        return executor;
    }
}