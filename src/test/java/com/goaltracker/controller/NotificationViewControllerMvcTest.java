package com.goaltracker.controller;

import com.goaltracker.dto.response.NotificationResponse;
import com.goaltracker.dto.response.NotificationSettingsResponse;
import com.goaltracker.model.enums.NotificationType;
import com.goaltracker.service.NotificationService;
import com.goaltracker.util.SecurityUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class NotificationViewControllerMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private NotificationService notificationService;

    @MockBean
    private SecurityUtils securityUtils;

    private NotificationResponse sampleNotification() {
        return new NotificationResponse(
                1L, NotificationType.DAILY_REMINDER, "Günlük Hatırlatma",
                "Bugün hedefinize giriş yapmayı unutmayın!", false, null, Instant.now());
    }

    // ─── MVC (Thymeleaf) Tests ───────────────────────────────────────

    @Test
    @WithMockUser(username = "test@test.com")
    @DisplayName("GET /notifications → 200 bildirim listesi sayfası")
    void shouldRenderNotificationsList() throws Exception {
        given(securityUtils.getCurrentUserId()).willReturn(1L);
        Page<NotificationResponse> page = new PageImpl<>(
                List.of(sampleNotification()), PageRequest.of(0, 20), 1);
        given(notificationService.getNotifications(eq(1L), any())).willReturn(page);
        given(notificationService.getUnreadCount(1L)).willReturn(3L);

        mockMvc.perform(get("/notifications"))
                .andExpect(status().isOk())
                .andExpect(view().name("notifications/list"))
                .andExpect(model().attributeExists("notifications"))
                .andExpect(model().attribute("unreadCount", 3L));
    }

    @Test
    @WithMockUser(username = "test@test.com")
    @DisplayName("POST /notifications/{id}/read → redirect")
    void shouldMarkAsReadAndRedirect() throws Exception {
        given(securityUtils.getCurrentUserId()).willReturn(1L);

        mockMvc.perform(post("/notifications/1/read"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/notifications"));

        verify(notificationService).markAsRead(1L, 1L);
    }

    @Test
    @WithMockUser(username = "test@test.com")
    @DisplayName("POST /notifications/read-all → redirect")
    void shouldMarkAllAsReadAndRedirect() throws Exception {
        given(securityUtils.getCurrentUserId()).willReturn(1L);

        mockMvc.perform(post("/notifications/read-all"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/notifications"));

        verify(notificationService).markAllAsRead(1L);
    }

    @Test
    @WithMockUser(username = "test@test.com")
    @DisplayName("GET /settings/notifications → 200 ayarlar sayfası")
    void shouldRenderSettingsPage() throws Exception {
        given(securityUtils.getCurrentUserId()).willReturn(1L);
        given(notificationService.getSettings(1L)).willReturn(
                new NotificationSettingsResponse(true, true, LocalTime.of(20, 0), 1, true, true));

        mockMvc.perform(get("/settings/notifications"))
                .andExpect(status().isOk())
                .andExpect(view().name("settings/notifications"))
                .andExpect(model().attributeExists("settings"));
    }

    // ─── REST API Tests ──────────────────────────────────────────────

    @Test
    @WithMockUser(username = "test@test.com")
    @DisplayName("GET /api/notifications → 200 JSON")
    void shouldReturnNotificationsApi() throws Exception {
        given(securityUtils.getCurrentUserId()).willReturn(1L);
        Page<NotificationResponse> page = new PageImpl<>(
                List.of(sampleNotification()), PageRequest.of(0, 20), 1);
        given(notificationService.getNotifications(eq(1L), any())).willReturn(page);

        mockMvc.perform(get("/api/notifications")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray());
    }

    @Test
    @WithMockUser(username = "test@test.com")
    @DisplayName("GET /api/notifications/unread-count → 200")
    void shouldReturnUnreadCount() throws Exception {
        given(securityUtils.getCurrentUserId()).willReturn(1L);
        given(notificationService.getUnreadCount(1L)).willReturn(5L);

        mockMvc.perform(get("/api/notifications/unread-count")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.count").value(5));
    }

    @Test
    @WithMockUser(username = "test@test.com")
    @DisplayName("PUT /api/notifications/{id}/read → 200")
    void shouldMarkAsReadApi() throws Exception {
        given(securityUtils.getCurrentUserId()).willReturn(1L);

        mockMvc.perform(put("/api/notifications/1/read")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    // ─── Auth Tests ──────────────────────────────────────────────────

    @Test
    @DisplayName("Yetkilendirilmemiş erişim → redirect")
    void shouldRedirectWhenNotAuthenticated() throws Exception {
        mockMvc.perform(get("/notifications"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @DisplayName("API yetkilendirilmemiş erişim → redirect")
    void shouldRedirectApiWhenNotAuthenticated() throws Exception {
        mockMvc.perform(get("/api/notifications")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().is3xxRedirection());
    }
}

