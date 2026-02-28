-- V6: Social (Friendships & Goal Shares) tabloları
CREATE TABLE friendships (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    requester_id  BIGINT      NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    receiver_id   BIGINT      NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    status        VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(requester_id, receiver_id),
    CONSTRAINT chk_no_self_friend CHECK (requester_id != receiver_id)
);

CREATE INDEX idx_friendships_receiver ON friendships(receiver_id);
CREATE INDEX idx_friendships_status   ON friendships(requester_id, status);

CREATE TABLE goal_shares (
    id                   BIGINT AUTO_INCREMENT PRIMARY KEY,
    goal_id              BIGINT      NOT NULL REFERENCES goals(id) ON DELETE CASCADE,
    shared_with_user_id  BIGINT      NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    permission           VARCHAR(20) NOT NULL DEFAULT 'READ',
    created_at           TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(goal_id, shared_with_user_id)
);

