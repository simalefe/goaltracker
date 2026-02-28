package com.goaltracker.controller;

import com.goaltracker.dto.*;
import com.goaltracker.service.GoalEntryService;
import com.goaltracker.util.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class GoalEntryController {

    private final GoalEntryService goalEntryService;
    private final SecurityUtils securityUtils;

    public GoalEntryController(GoalEntryService goalEntryService, SecurityUtils securityUtils) {
        this.goalEntryService = goalEntryService;
        this.securityUtils = securityUtils;
    }

    @GetMapping("/goals/{goalId}/entries")
    public ResponseEntity<ApiResponse<List<GoalEntryResponse>>> getEntries(
            @PathVariable("goalId") Long goalId) {
        Long userId = securityUtils.getCurrentUserId();
        List<GoalEntryResponse> entries = goalEntryService.getEntries(goalId, userId);
        return ResponseEntity.ok(ApiResponse.ok(entries));
    }

    @PostMapping("/goals/{goalId}/entries")
    public ResponseEntity<ApiResponse<GoalEntryResponse>> createEntry(
            @PathVariable("goalId") Long goalId,
            @Valid @RequestBody CreateEntryRequest request) {
        Long userId = securityUtils.getCurrentUserId();
        GoalEntryResponse response = goalEntryService.createEntry(goalId, userId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(response, "İlerleme kaydedildi."));
    }

    @PutMapping("/entries/{entryId}")
    public ResponseEntity<ApiResponse<GoalEntryResponse>> updateEntry(
            @PathVariable("entryId") Long entryId,
            @Valid @RequestBody UpdateEntryRequest request) {
        Long userId = securityUtils.getCurrentUserId();
        GoalEntryResponse response = goalEntryService.updateEntry(entryId, userId, request);
        return ResponseEntity.ok(ApiResponse.ok(response, "Kayıt güncellendi."));
    }

    @DeleteMapping("/entries/{entryId}")
    public ResponseEntity<Void> deleteEntry(@PathVariable("entryId") Long entryId) {
        Long userId = securityUtils.getCurrentUserId();
        goalEntryService.deleteEntry(entryId, userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/goals/{id}/stats")
    public ResponseEntity<ApiResponse<GoalStatsResponse>> getStats(
            @PathVariable("id") Long goalId) {
        Long userId = securityUtils.getCurrentUserId();
        GoalStatsResponse stats = goalEntryService.getStats(goalId, userId);
        return ResponseEntity.ok(ApiResponse.ok(stats));
    }

    @GetMapping("/goals/{id}/chart-data")
    public ResponseEntity<ApiResponse<ChartDataResponse>> getChartData(
            @PathVariable("id") Long goalId) {
        Long userId = securityUtils.getCurrentUserId();
        ChartDataResponse chart = goalEntryService.getChartData(goalId, userId);
        return ResponseEntity.ok(ApiResponse.ok(chart));
    }
}

