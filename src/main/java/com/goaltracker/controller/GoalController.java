package com.goaltracker.controller;

import com.goaltracker.dto.GoalResponse;
import com.goaltracker.service.ExportService;
import com.goaltracker.service.GoalService;
import com.goaltracker.util.SecurityUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;

/**
 * REST endpoint'leri — sadece ön yüzden (Thymeleaf template'lerinden) doğrudan çağrılan
 * export endpoint'lerini sunar.
 */
@RestController
@RequestMapping("/api/goals")
public class GoalController {

    private final GoalService goalService;
    private final ExportService exportService;
    private final SecurityUtils securityUtils;

    public GoalController(GoalService goalService,
                          ExportService exportService,
                          SecurityUtils securityUtils) {
        this.goalService = goalService;
        this.exportService = exportService;
        this.securityUtils = securityUtils;
    }

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
