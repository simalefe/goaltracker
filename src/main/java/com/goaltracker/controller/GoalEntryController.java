package com.goaltracker.controller;

import com.goaltracker.dto.ApiResponse;
import com.goaltracker.dto.ChartDataResponse;
import com.goaltracker.service.GoalEntryService;
import com.goaltracker.util.SecurityUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST endpoint'leri — sadece ön yüzden (charts.js) AJAX ile çağrılan
 * chart-data endpoint'ini sunar.
 */
@RestController
@RequestMapping("/api")
public class GoalEntryController {

    private final GoalEntryService goalEntryService;
    private final SecurityUtils securityUtils;

    public GoalEntryController(GoalEntryService goalEntryService, SecurityUtils securityUtils) {
        this.goalEntryService = goalEntryService;
        this.securityUtils = securityUtils;
    }

    @GetMapping("/goals/{id}/chart-data")
    public ResponseEntity<ApiResponse<ChartDataResponse>> getChartData(
            @PathVariable("id") Long goalId) {
        Long userId = securityUtils.getCurrentUserId();
        ChartDataResponse chart = goalEntryService.getChartData(goalId, userId);
        return ResponseEntity.ok(ApiResponse.ok(chart));
    }
}
