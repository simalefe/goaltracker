package com.goaltracker.controller;

import com.goaltracker.dto.ApiResponse;
import com.goaltracker.dto.request.*;
import com.goaltracker.dto.response.AuthResponse;
import com.goaltracker.dto.response.UserBadgeResponse;
import com.goaltracker.dto.response.UserResponse;
import com.goaltracker.dto.response.UserStatsResponse;
import com.goaltracker.model.enums.GoalStatus;
import com.goaltracker.repository.GoalEntryRepository;
import com.goaltracker.repository.GoalRepository;
import com.goaltracker.service.AuthService;
import com.goaltracker.service.BadgeService;
import com.goaltracker.service.StreakService;
import com.goaltracker.service.UserService;
import com.goaltracker.util.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ApiAuthController {

    private final AuthService authService;
    private final UserService userService;
    private final BadgeService badgeService;
    private final StreakService streakService;
    private final SecurityUtils securityUtils;
    private final GoalEntryRepository goalEntryRepository;
    private final GoalRepository goalRepository;

    public ApiAuthController(AuthService authService, UserService userService,
                             BadgeService badgeService, StreakService streakService,
                             SecurityUtils securityUtils,
                             GoalEntryRepository goalEntryRepository,
                             GoalRepository goalRepository) {
        this.authService = authService;
        this.userService = userService;
        this.badgeService = badgeService;
        this.streakService = streakService;
        this.securityUtils = securityUtils;
        this.goalEntryRepository = goalEntryRepository;
        this.goalRepository = goalRepository;
    }

    @PostMapping("/auth/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletResponse response) {
        AuthResponse auth = authService.register(request, response);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(auth, "Kayıt başarılı. E-posta doğrulama linki gönderildi."));
    }

    @PostMapping("/auth/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletResponse response) {
        AuthResponse auth = authService.login(request, response);
        return ResponseEntity.ok(ApiResponse.ok(auth));
    }

    @PostMapping("/auth/refresh")
    public ResponseEntity<ApiResponse<Map<String, String>>> refresh(
            HttpServletRequest request,
            HttpServletResponse response) {
        String accessToken = authService.refreshToken(request, response);
        return ResponseEntity.ok(ApiResponse.ok(Map.of("accessToken", accessToken)));
    }

    @PostMapping("/auth/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            HttpServletRequest request,
            HttpServletResponse response) {
        authService.logout(request, response);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @PostMapping("/auth/verify-email")
    public ResponseEntity<ApiResponse<Void>> verifyEmail(@RequestBody Map<String, String> body) {
        authService.verifyEmail(body.get("token"));
        return ResponseEntity.ok(ApiResponse.ok(null, "E-posta adresiniz başarıyla doğrulandı."));
    }

    @PostMapping("/auth/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request.email());
        return ResponseEntity.ok(ApiResponse.ok(null, "İşlem tamamlandı."));
    }

    @PostMapping("/auth/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request.token(), request.newPassword());
        return ResponseEntity.ok(ApiResponse.ok(null, "Şifreniz başarıyla sıfırlandı."));
    }

    // ---- User Profile ----
    @GetMapping("/users/me")
    public ResponseEntity<ApiResponse<UserResponse>> getProfile(
            @AuthenticationPrincipal UserDetails userDetails) {
        UserResponse user = userService.getProfile(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.ok(user));
    }

    @PutMapping("/users/me")
    public ResponseEntity<ApiResponse<UserResponse>> updateProfile(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody UpdateProfileRequest request) {
        UserResponse user = userService.updateProfile(userDetails.getUsername(), request);
        return ResponseEntity.ok(ApiResponse.ok(user));
    }

    @PutMapping("/users/me/password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(userDetails.getUsername(), request);
        return ResponseEntity.ok(ApiResponse.ok(null, "Şifre başarıyla değiştirildi."));
    }

    @DeleteMapping("/users/me")
    public ResponseEntity<ApiResponse<Void>> deleteAccount(
            @AuthenticationPrincipal UserDetails userDetails) {
        userService.deleteAccount(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.ok(null, "Hesabınız devre dışı bırakıldı."));
    }

    @GetMapping("/users/search")
    public ResponseEntity<ApiResponse<List<UserResponse>>> searchUsers(
            @RequestParam("q") String query) {
        List<UserResponse> users = userService.searchUsers(query);
        return ResponseEntity.ok(ApiResponse.ok(users));
    }

    // ---- Badges & Stats (Phase 5) ----
    @GetMapping("/users/me/badges")
    public ResponseEntity<ApiResponse<List<UserBadgeResponse>>> getUserBadges() {
        Long userId = securityUtils.getCurrentUserId();
        List<UserBadgeResponse> badges = badgeService.getUserBadges(userId);
        return ResponseEntity.ok(ApiResponse.ok(badges));
    }

    @GetMapping("/users/me/stats")
    public ResponseEntity<ApiResponse<UserStatsResponse>> getUserStats() {
        Long userId = securityUtils.getCurrentUserId();
        long totalEntries = goalEntryRepository.countByGoal_User_Id(userId);
        long completedGoals = goalRepository.countByUserIdAndStatus(userId, GoalStatus.COMPLETED);
        int totalStreakDays = streakService.getTotalStreakDays(userId);
        long earnedBadgeCount = badgeService.getUserBadgeCount(userId);
        UserStatsResponse stats = new UserStatsResponse(totalEntries, completedGoals, totalStreakDays, earnedBadgeCount);
        return ResponseEntity.ok(ApiResponse.ok(stats));
    }
}

