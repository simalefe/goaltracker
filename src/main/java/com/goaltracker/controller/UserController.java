package com.goaltracker.controller;

import com.goaltracker.service.ExportService;
import com.goaltracker.util.SecurityUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final ExportService exportService;
    private final SecurityUtils securityUtils;

    public UserController(ExportService exportService, SecurityUtils securityUtils) {
        this.exportService = exportService;
        this.securityUtils = securityUtils;
    }

    @GetMapping("/me/reports/monthly")
    public ResponseEntity<byte[]> monthlyReport(
            @RequestParam("year") int year,
            @RequestParam("month") int month) {
        Long userId = securityUtils.getCurrentUserId();
        byte[] data = exportService.generateMonthlyReport(userId, year, month);
        String filename = "aylik-rapor-" + YearMonth.of(year, month).format(DateTimeFormatter.ofPattern("yyyy-MM")) + ".pdf";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(data.length)
                .body(data);
    }
}

