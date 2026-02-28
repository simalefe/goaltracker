package com.goaltracker.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record RecentEntryResponse(
        Long entryId,
        Long goalId,
        String goalTitle,
        String unit,
        BigDecimal actualValue,
        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDate entryDate,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
        Instant createdAt
) {}

