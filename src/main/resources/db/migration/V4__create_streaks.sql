-- V4: Streaks tablosu
CREATE TABLE streaks (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    goal_id         BIGINT  NOT NULL UNIQUE REFERENCES goals(id) ON DELETE CASCADE,
    current_streak  INT     NOT NULL DEFAULT 0,
    longest_streak  INT     NOT NULL DEFAULT 0,
    last_entry_date DATE,
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

