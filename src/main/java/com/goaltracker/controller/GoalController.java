package com.goaltracker.controller;

import com.goaltracker.dto.*;
import com.goaltracker.dto.request.ShareGoalRequest;
import com.goaltracker.dto.response.FriendResponse;
import com.goaltracker.dto.response.StreakResponse;
import com.goaltracker.model.enums.GoalCategory;
import com.goaltracker.model.enums.GoalStatus;
import com.goaltracker.model.enums.GoalType;
import com.goaltracker.service.ExportService;
import com.goaltracker.service.GoalService;
import com.goaltracker.service.GoalShareService;
import com.goaltracker.service.StreakService;
import com.goaltracker.util.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/goals")
public class GoalController {

    private final GoalService goalService;
    private final GoalShareService goalShareService;
    private final StreakService streakService;
    private final ExportService exportService;
    private final SecurityUtils securityUtils;

    public GoalController(GoalService goalService, GoalShareService goalShareService,
                          StreakService streakService, ExportService exportService,
                          SecurityUtils securityUtils) {
        this.goalService = goalService;
        this.goalShareService = goalShareService;
        this.streakService = streakService;
        this.exportService = exportService;
        this.securityUtils = securityUtils;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<GoalResponse>> createGoal(
            @Valid @RequestBody CreateGoalRequest req) {
        Long userId = securityUtils.getCurrentUserId();
        GoalResponse r = goalService.createGoal(req, userId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(r, "Hedef başarıyla oluşturuldu."));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> listGoals(
            @RequestParam(value = "status", required = false) GoalStatus status,
            @RequestParam(value = "category", required = false) GoalCategory category,
            @RequestParam(value = "goalType", required = false) GoalType goalType,
            @RequestParam(value = "query", required = false) String query,
            @PageableDefault(size = 10, sort = "createdAt", direction = org.springframework.data.domain.Sort.Direction.DESC) Pageable pageable) {
        Long userId = securityUtils.getCurrentUserId();
        Page<GoalSummaryResponse> page = goalService.getGoals(userId, status, category, goalType, query, pageable);
        Map<String, Object> response = Map.of(
                "content", page.getContent(),
                "page", page.getNumber(),
                "size", page.getSize(),
                "totalElements", page.getTotalElements(),
                "totalPages", page.getTotalPages(),
                "first", page.isFirst(),
                "last", page.isLast()
        );
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<GoalResponse>> getGoal(@PathVariable("id") Long id) {
        Long userId = securityUtils.getCurrentUserId();
        GoalResponse r = goalService.getGoal(id, userId);
        return ResponseEntity.ok(ApiResponse.ok(r));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<GoalResponse>> updateGoal(
            @PathVariable("id") Long id,
            @Valid @RequestBody UpdateGoalRequest req) {
        Long userId = securityUtils.getCurrentUserId();
        GoalResponse r = goalService.updateGoal(id, req, userId);
        return ResponseEntity.ok(ApiResponse.ok(r, "Hedef başarıyla güncellendi."));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteGoal(@PathVariable("id") Long id) {
        Long userId = securityUtils.getCurrentUserId();
        goalService.deleteGoal(id, userId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<GoalResponse>> updateStatus(
            @PathVariable("id") Long id,
            @Valid @RequestBody StatusUpdateRequest req) {
        Long userId = securityUtils.getCurrentUserId();
        GoalResponse r = goalService.updateStatus(id, userId, req.getNewStatus());
        return ResponseEntity.ok(ApiResponse.ok(r, "Hedef durumu güncellendi."));
    }

    @GetMapping("/{id}/streak")
    public ResponseEntity<ApiResponse<StreakResponse>> getStreak(@PathVariable("id") Long id) {
        securityUtils.getCurrentUserId(); // ownership check — goal endpoint
        StreakResponse streak = streakService.getStreak(id);
        return ResponseEntity.ok(ApiResponse.ok(streak));
    }

    // --- Goal Sharing ---

    @PostMapping("/{id}/share")
    public ResponseEntity<ApiResponse<Void>> shareGoal(
            @PathVariable("id") Long id,
            @Valid @RequestBody ShareGoalRequest request) {
        Long userId = securityUtils.getCurrentUserId();
        goalShareService.shareGoal(id, userId, request.getUserId(), request.getPermission());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(null, "Hedef başarıyla paylaşıldı."));
    }

    @DeleteMapping("/{id}/share/{targetUserId}")
    public ResponseEntity<Void> removeShare(
            @PathVariable("id") Long id,
            @PathVariable("targetUserId") Long targetUserId) {
        Long userId = securityUtils.getCurrentUserId();
        goalShareService.removeShare(id, userId, targetUserId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/shared-with")
    public ResponseEntity<ApiResponse<List<FriendResponse>>> getSharedWithUsers(
            @PathVariable("id") Long id) {
        Long userId = securityUtils.getCurrentUserId();
        List<FriendResponse> sharedWith = goalShareService.getSharedWithUsers(id, userId);
        return ResponseEntity.ok(ApiResponse.ok(sharedWith));
    }

    @GetMapping("/shared-with-me")
    public ResponseEntity<ApiResponse<List<GoalResponse>>> getSharedWithMe() {
        Long userId = securityUtils.getCurrentUserId();
        List<GoalResponse> sharedGoals = goalShareService.getSharedGoals(userId);
        return ResponseEntity.ok(ApiResponse.ok(sharedGoals));
    }

    // --- Export Endpoints ---

    @GetMapping("/{id}/export/excel")
    public ResponseEntity<byte[]> exportExcel(@PathVariable("id") Long id) {
        Long userId = securityUtils.getCurrentUserId();
        GoalResponse goal = goalService.getGoal(id, userId);
        byte[] data = exportService.exportGoalToExcel(id, userId);
        String filename = exportService.normalizeFilename(goal.getTitle())
                + "-" + YearMonth.now().format(DateTimeFormatter.ofPattern("yyyy-MM")) + ".xlsx";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .contentLength(data.length)
                .body(data);
    }

    @GetMapping("/{id}/export/pdf")
    public ResponseEntity<byte[]> exportPdf(@PathVariable("id") Long id) {
        Long userId = securityUtils.getCurrentUserId();
        GoalResponse goal = goalService.getGoal(id, userId);
        byte[] data = exportService.exportGoalToPdf(id, userId);
        String filename = exportService.normalizeFilename(goal.getTitle())
                + "-" + YearMonth.now().format(DateTimeFormatter.ofPattern("yyyy-MM")) + ".pdf";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(data.length)
                .body(data);
    }

    @GetMapping("/{id}/export/csv")
    public ResponseEntity<byte[]> exportCsv(@PathVariable("id") Long id) {
        Long userId = securityUtils.getCurrentUserId();
        GoalResponse goal = goalService.getGoal(id, userId);
        byte[] data = exportService.exportGoalToCsv(id, userId);
        String filename = exportService.normalizeFilename(goal.getTitle())
                + "-" + YearMonth.now().format(DateTimeFormatter.ofPattern("yyyy-MM")) + ".csv";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(new MediaType("text", "csv", java.nio.charset.StandardCharsets.UTF_8))
                .contentLength(data.length)
                .body(data);
    }
}

