package com.goaltracker.dto;

import java.math.BigDecimal;
import java.util.List;

public record ChartDataResponse(
        List<ChartDataPointResponse> dataPoints,
        BigDecimal dailyTarget
) {}

