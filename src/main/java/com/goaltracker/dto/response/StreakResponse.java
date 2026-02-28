package com.goaltracker.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDate;

public record StreakResponse(
        Long goalId,
        int currentStreak,
        int longestStreak,
        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDate lastEntryDate
) {}

