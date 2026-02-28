package com.goaltracker.controller;

import com.goaltracker.dto.ApiResponse;
import com.goaltracker.dto.response.LeaderboardEntryResponse;
import com.goaltracker.model.enums.GoalCategory;
import com.goaltracker.service.LeaderboardService;
import com.goaltracker.util.SecurityUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/leaderboard")
public class LeaderboardController {

    private final LeaderboardService leaderboardService;
    private final SecurityUtils securityUtils;

    public LeaderboardController(LeaderboardService leaderboardService, SecurityUtils securityUtils) {
        this.leaderboardService = leaderboardService;
        this.securityUtils = securityUtils;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<LeaderboardEntryResponse>>> getLeaderboard(
            @RequestParam(value = "category", required = false) GoalCategory category) {
        Long userId = securityUtils.getCurrentUserId();
        List<LeaderboardEntryResponse> leaderboard = leaderboardService.getLeaderboard(userId, category);
        return ResponseEntity.ok(ApiResponse.ok(leaderboard));
    }
}

