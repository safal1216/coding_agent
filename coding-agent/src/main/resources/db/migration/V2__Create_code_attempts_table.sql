-- Table: code_attempts
CREATE TABLE code_attempts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    task_id UUID NOT NULL REFERENCES tasks(id) ON DELETE CASCADE,

    iteration_number INTEGER NOT NULL,

    generated_code TEXT NOT NULL,

    test_passed BOOLEAN NOT NULL DEFAULT false,
    test_output TEXT,
    error_message TEXT,
    error_type VARCHAR(100),

    reflection_analysis TEXT,
    root_cause TEXT,
    suggested_fix TEXT,

    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    execution_time_ms INTEGER,

    CONSTRAINT unique_task_iteration UNIQUE (task_id, iteration_number)
);

CREATE INDEX idx_attempts_task_id ON code_attempts(task_id);
CREATE INDEX idx_attempts_error_type ON code_attempts(error_type) WHERE error_type IS NOT NULL;
CREATE INDEX idx_attempts_created_at ON code_attempts(created_at DESC);