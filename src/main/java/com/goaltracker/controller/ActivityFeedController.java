package com.goaltracker.controller;

import com.goaltracker.dto.ApiResponse;
import com.goaltracker.dto.response.ActivityFeedItemResponse;
import com.goaltracker.service.ActivityFeedService;
import com.goaltracker.util.SecurityUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/social")
public class ActivityFeedController {

    private final ActivityFeedService activityFeedService;
    private final SecurityUtils securityUtils;

    public ActivityFeedController(ActivityFeedService activityFeedService, SecurityUtils securityUtils) {
        this.activityFeedService = activityFeedService;
        this.securityUtils = securityUtils;
    }

    @GetMapping("/activity-feed")
    public ResponseEntity<ApiResponse<List<ActivityFeedItemResponse>>> getActivityFeed() {
        Long userId = securityUtils.getCurrentUserId();
        List<ActivityFeedItemResponse> feed = activityFeedService.getFeed(userId);
        return ResponseEntity.ok(ApiResponse.ok(feed));
    }
}

