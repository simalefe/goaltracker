package com.goaltracker.controller;

import com.goaltracker.dto.ApiResponse;
import com.goaltracker.dto.request.NotificationSettingsRequest;
import com.goaltracker.dto.response.NotificationResponse;
import com.goaltracker.dto.response.NotificationSettingsResponse;
import com.goaltracker.model.enums.NotificationType;
import com.goaltracker.service.NotificationService;
import com.goaltracker.util.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Map;

@Controller
public class NotificationViewController {

    private final NotificationService notificationService;
    private final SecurityUtils securityUtils;

    public NotificationViewController(NotificationService notificationService,
                                       SecurityUtils securityUtils) {
        this.notificationService = notificationService;
        this.securityUtils = securityUtils;
    }

    // ========================
    // MVC Endpoints (Thymeleaf)
    // ========================

    @GetMapping("/notifications")
    public String listNotifications(
            @RequestParam(value = "type", required = false) NotificationType type,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            Model model) {

        Long userId = securityUtils.getCurrentUserId();
        Pageable pageable = PageRequest.of(page, Math.min(size, 50), Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<NotificationResponse> notifications;
        if (type != null) {
            notifications = notificationService.getNotificationsByType(userId, type, pageable);
        } else {
            notifications = notificationService.getNotifications(userId, pageable);
        }

        long unreadCount = notificationService.getUnreadCount(userId);

        model.addAttribute("notifications", notifications);
        model.addAttribute("unreadCount", unreadCount);
        model.addAttribute("currentType", type);
        model.addAttribute("notificationTypes", NotificationType.values());
        model.addAttribute("pageTitle", "Bildirimler");
        model.addAttribute("activePage", "notifications");
        return "notifications/list";
    }

    @PostMapping("/notifications/{id}/read")
    public String markAsRead(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        Long userId = securityUtils.getCurrentUserId();
        notificationService.markAsRead(id, userId);
        redirectAttributes.addFlashAttribute("successMessage", "Bildirim okundu olarak işaretlendi.");
        return "redirect:/notifications";
    }

    @PostMapping("/notifications/read-all")
    public String markAllAsRead(RedirectAttributes redirectAttributes) {
        Long userId = securityUtils.getCurrentUserId();
        notificationService.markAllAsRead(userId);
        redirectAttributes.addFlashAttribute("successMessage", "Tüm bildirimler okundu olarak işaretlendi.");
        return "redirect:/notifications";
    }

    @GetMapping("/settings/notifications")
    public String notificationSettings(Model model) {
        Long userId = securityUtils.getCurrentUserId();
        NotificationSettingsResponse settings = notificationService.getSettings(userId);
        model.addAttribute("settings", settings);
        model.addAttribute("pageTitle", "Bildirim Ayarları");
        model.addAttribute("activePage", "settings");
        return "settings/notifications";
    }

    @PostMapping("/settings/notifications")
    public String updateNotificationSettings(
            @Valid @ModelAttribute NotificationSettingsRequest request,
            RedirectAttributes redirectAttributes) {
        Long userId = securityUtils.getCurrentUserId();
        // HTML checkboxes: unchecked = null → treat as false
        if (request.getEmailEnabled() == null) request.setEmailEnabled(false);
        if (request.getPushEnabled() == null) request.setPushEnabled(false);
        if (request.getStreakDangerEnabled() == null) request.setStreakDangerEnabled(false);
        if (request.getWeeklySummaryEnabled() == null) request.setWeeklySummaryEnabled(false);
        notificationService.updateSettings(userId, request);
        redirectAttributes.addFlashAttribute("successMessage", "Bildirim ayarları güncellendi.");
        return "redirect:/settings/notifications";
    }

    @GetMapping("/settings/appearance")
    public String appearanceSettings(Model model) {
        model.addAttribute("pageTitle", "Görünüm Ayarları");
        model.addAttribute("activePage", "settings");
        return "settings/appearance";
    }

    // ========================
    // REST API Endpoints
    // ========================

    @GetMapping("/api/notifications")
    @ResponseBody
    public ResponseEntity<ApiResponse<Map<String, Object>>> getNotificationsApi(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {

        Long userId = securityUtils.getCurrentUserId();
        Pageable pageable = PageRequest.of(page, Math.min(size, 50), Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<NotificationResponse> notifications = notificationService.getNotifications(userId, pageable);

        Map<String, Object> response = Map.of(
                "content", notifications.getContent(),
                "page", notifications.getNumber(),
                "size", notifications.getSize(),
                "totalElements", notifications.getTotalElements(),
                "totalPages", notifications.getTotalPages(),
                "first", notifications.isFirst(),
                "last", notifications.isLast()
        );
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/api/notifications/unread-count")
    @ResponseBody
    public ResponseEntity<ApiResponse<Map<String, Long>>> getUnreadCount() {
        Long userId = securityUtils.getCurrentUserId();
        long count = notificationService.getUnreadCount(userId);
        return ResponseEntity.ok(ApiResponse.ok(Map.of("count", count)));
    }

    @PutMapping("/api/notifications/{id}/read")
    @ResponseBody
    public ResponseEntity<ApiResponse<Void>> markAsReadApi(@PathVariable("id") Long id) {
        Long userId = securityUtils.getCurrentUserId();
        notificationService.markAsRead(id, userId);
        return ResponseEntity.ok(ApiResponse.ok(null, "Bildirim okundu olarak işaretlendi."));
    }

    @PutMapping("/api/notifications/read-all")
    @ResponseBody
    public ResponseEntity<ApiResponse<Void>> markAllAsReadApi() {
        Long userId = securityUtils.getCurrentUserId();
        notificationService.markAllAsRead(userId);
        return ResponseEntity.ok(ApiResponse.ok(null, "Tüm bildirimler okundu olarak işaretlendi."));
    }

    @GetMapping("/api/notification-settings")
    @ResponseBody
    public ResponseEntity<ApiResponse<NotificationSettingsResponse>> getSettingsApi() {
        Long userId = securityUtils.getCurrentUserId();
        NotificationSettingsResponse settings = notificationService.getSettings(userId);
        return ResponseEntity.ok(ApiResponse.ok(settings));
    }

    @PutMapping("/api/notification-settings")
    @ResponseBody
    public ResponseEntity<ApiResponse<NotificationSettingsResponse>> updateSettingsApi(
            @Valid @RequestBody NotificationSettingsRequest request) {
        Long userId = securityUtils.getCurrentUserId();
        NotificationSettingsResponse settings = notificationService.updateSettings(userId, request);
        return ResponseEntity.ok(ApiResponse.ok(settings, "Bildirim ayarları güncellendi."));
    }
}

