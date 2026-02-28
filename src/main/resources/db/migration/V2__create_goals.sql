-- V2: Goals tablosu
CREATE TABLE goals (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id       BIGINT        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title         VARCHAR(200)  NOT NULL,
    description   TEXT,
    unit          VARCHAR(50)   NOT NULL,
    goal_type     VARCHAR(20)   NOT NULL,
    frequency     VARCHAR(20)   NOT NULL DEFAULT 'DAILY',
    target_value  NUMERIC(10,2) NOT NULL,
    start_date    DATE          NOT NULL,
    end_date      DATE          NOT NULL,
    category      VARCHAR(50),
    color         VARCHAR(7),
    status        VARCHAR(20)   NOT NULL DEFAULT 'ACTIVE',
    version       BIGINT        NOT NULL DEFAULT 0,
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_goals_dates  CHECK (end_date > start_date),
    CONSTRAINT chk_goals_target CHECK (target_value > 0)
);

CREATE INDEX idx_goals_user_id     ON goals(user_id);
CREATE INDEX idx_goals_status      ON goals(status);
CREATE INDEX idx_goals_user_status ON goals(user_id, status);

