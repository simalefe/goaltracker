package com.goaltracker.dto.response;

public record BadgeResponse(
        Long id,
        String code,
        String name,
        String description,
        String icon,
        String conditionType,
        int conditionValue
) {}

