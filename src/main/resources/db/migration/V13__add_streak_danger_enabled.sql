-- V13: Add streak_danger_enabled to notification_settings
ALTER TABLE notification_settings ADD COLUMN streak_danger_enabled BOOLEAN NOT NULL DEFAULT TRUE;

