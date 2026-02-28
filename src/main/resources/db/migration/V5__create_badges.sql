-- V5: Badges & User Badges tabloları
CREATE TABLE badges (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    code             VARCHAR(50)  NOT NULL UNIQUE,
    name             VARCHAR(100) NOT NULL,
    description      TEXT         NOT NULL,
    icon             VARCHAR(10)  NOT NULL,
    condition_type   VARCHAR(50)  NOT NULL,
    condition_value  INT          NOT NULL,
    created_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE user_badges (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id    BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    badge_id   BIGINT NOT NULL REFERENCES badges(id),
    earned_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user_id, badge_id)
);

