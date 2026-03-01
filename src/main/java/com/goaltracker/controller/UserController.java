package com.goaltracker.controller;

import com.goaltracker.dto.ApiResponse;
import com.goaltracker.dto.request.ChangePasswordRequest;
import com.goaltracker.dto.request.UpdateProfileRequest;
import com.goaltracker.dto.response.UserResponse;
import com.goaltracker.service.ExportService;
import com.goaltracker.service.UserService;
import com.goaltracker.util.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final ExportService exportService;
    private final UserService userService;
    private final SecurityUtils securityUtils;

    public UserController(ExportService exportService,
                          UserService userService,
                          SecurityUtils securityUtils) {
        this.exportService = exportService;
        this.userService = userService;
        this.securityUtils = securityUtils;
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getProfile(
            @AuthenticationPrincipal UserDetails userDetails) {
        UserResponse profile = userService.getProfile(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.ok(profile));
    }

    @PutMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> updateProfile(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody UpdateProfileRequest request) {
        UserResponse updated = userService.updateProfile(userDetails.getUsername(), request);
        return ResponseEntity.ok(ApiResponse.ok(updated));
    }

    @PutMapping("/me/password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(userDetails.getUsername(), request);
        return ResponseEntity.ok(ApiResponse.ok(null, "Şifre başarıyla değiştirildi."));
    }

    @DeleteMapping("/me")
    public ResponseEntity<ApiResponse<Void>> deleteAccount(
            @AuthenticationPrincipal UserDetails userDetails) {
        userService.deleteAccount(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.ok(null, "Hesap başarıyla devre dışı bırakıldı."));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<UserResponse>>> searchUsers(
            @RequestParam("q") String query) {
        List<UserResponse> results = userService.searchUsers(query);
        return ResponseEntity.ok(ApiResponse.ok(results));
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

