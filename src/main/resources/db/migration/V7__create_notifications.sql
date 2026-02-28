-- V7: Notifications & Notification Settings tabloları
CREATE TABLE notifications (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id       BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type          VARCHAR(50)  NOT NULL,
    title         VARCHAR(200) NOT NULL,
    message       TEXT         NOT NULL,
    is_read       BOOLEAN      NOT NULL DEFAULT FALSE,
    metadata      VARCHAR(2000),
    scheduled_at  TIMESTAMP WITH TIME ZONE,
    sent_at       TIMESTAMP WITH TIME ZONE,
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_notifications_user_id   ON notifications(user_id);
CREATE INDEX idx_notifications_unread    ON notifications(user_id, is_read);
CREATE INDEX idx_notifications_scheduled ON notifications(scheduled_at);

CREATE TABLE notification_settings (
    id                      BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id                 BIGINT  NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    email_enabled           BOOLEAN NOT NULL DEFAULT TRUE,
    push_enabled            BOOLEAN NOT NULL DEFAULT TRUE,
    daily_reminder_time     TIME    NOT NULL DEFAULT TIME '20:00:00',
    weekly_summary_day      SMALLINT NOT NULL DEFAULT 1,
    weekly_summary_enabled  BOOLEAN NOT NULL DEFAULT TRUE
);

