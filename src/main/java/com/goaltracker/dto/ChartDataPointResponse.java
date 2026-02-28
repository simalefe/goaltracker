package com.goaltracker.dto;

import java.math.BigDecimal;

public record ChartDataPointResponse(
        String date,
        BigDecimal planned,
        BigDecimal actual,
        BigDecimal dailyActual
) {}

