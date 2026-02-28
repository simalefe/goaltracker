package com.goaltracker.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record GoalEntryResponse(
        Long id,
        Long goalId,
        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDate entryDate,
        BigDecimal actualValue,
        String note,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
        Instant createdAt
) {}

