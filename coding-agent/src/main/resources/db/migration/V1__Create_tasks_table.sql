-- Table: tasks
CREATE TABLE tasks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    goal TEXT NOT NULL,
    description TEXT,
    language VARCHAR(50) NOT NULL DEFAULT 'java',

    test_cases JSONB NOT NULL,

    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',

    current_iteration INTEGER DEFAULT 0,
    max_iterations INTEGER NOT NULL DEFAULT 10,

    generated_code TEXT,
    error_message TEXT,

    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    started_at TIMESTAMP WITH TIME ZONE,
    completed_at TIMESTAMP WITH TIME ZONE,

    CONSTRAINT chk_status CHECK (status IN ('PENDING', 'IN_PROGRESS', 'COMPLETED', 'FAILED', 'CANCELLED'))
);

CREATE INDEX idx_tasks_status ON tasks(status);
CREATE INDEX idx_tasks_created_at ON tasks(created_at DESC);