-- V3: Goal Entries tablosu
CREATE TABLE goal_entries (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    goal_id       BIGINT          NOT NULL REFERENCES goals(id) ON DELETE CASCADE,
    entry_date    DATE            NOT NULL,
    actual_value  NUMERIC(12,2)   NOT NULL,
    note          VARCHAR(500),
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(goal_id, entry_date),
    CONSTRAINT chk_entry_value CHECK (actual_value >= 0)
);

CREATE INDEX idx_goal_entries_goal_id    ON goal_entries(goal_id);
CREATE INDEX idx_goal_entries_entry_date ON goal_entries(entry_date);
CREATE INDEX idx_goal_entries_goal_date  ON goal_entries(goal_id, entry_date DESC);

