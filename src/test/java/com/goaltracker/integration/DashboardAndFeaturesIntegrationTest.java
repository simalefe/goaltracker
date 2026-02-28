package com.goaltracker.integration;

import com.goaltracker.dto.CreateEntryRequest;
import com.goaltracker.dto.StatusUpdateRequest;
import com.goaltracker.model.enums.GoalCategory;
import com.goaltracker.model.enums.GoalStatus;
import com.goaltracker.model.enums.GoalType;
import org.junit.jupiter.api.*;
import org.springframework.http.MediaType;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Comprehensive integration tests for Dashboard, Notifications, and Export features.
 */
class DashboardAndFeaturesIntegrationTest extends BaseIntegrationTest {

    private String token;

    @BeforeEach
    void setUp() throws Exception {
        token = registerAndGetToken("dashboard@test.com", "dashuser", STRONG_PASSWORD);
    }

    // ========================================================================
    // DASHBOARD
    // ========================================================================

    @Nested
    @DisplayName("Dashboard (GET /api/dashboard)")
    class DashboardTests {

        @Test
        @DisplayName("Boş dashboard — sıfır değerler")
        void dashboard_empty() throws Exception {
            mockMvc.perform(get("/api/dashboard")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.activeGoalCount").value(0))
                    .andExpect(jsonPath("$.data.todayEntryCount").value(0))
                    .andExpect(jsonPath("$.data.topGoals").isArray())
                    .andExpect(jsonPath("$.data.recentEntries").isArray());
        }

        @Test
        @DisplayName("Dashboard — hedef ve entry sonrası doğru veriler")
        void dashboard_withData() throws Exception {
            // Create 2 active goals
            Long goalId1 = createGoalAndGetId(token, "Dashboard Hedef 1");
            Long goalId2 = createGoalAndGetId(token, "Dashboard Hedef 2");

            // Add entries today
            mockMvc.perform(post("/api/goals/" + goalId1 + "/entries")
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(
                            new CreateEntryRequest(LocalDate.now(), new BigDecimal("10.0"), null))));

            mockMvc.perform(post("/api/goals/" + goalId2 + "/entries")
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(
                            new CreateEntryRequest(LocalDate.now(), new BigDecimal("5.0"), null))));

            mockMvc.perform(get("/api/dashboard")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.activeGoalCount").value(2))
                    .andExpect(jsonPath("$.data.todayEntryCount").value(2))
                    .andExpect(jsonPath("$.data.topGoals").isNotEmpty())
                    .andExpect(jsonPath("$.data.recentEntries").isNotEmpty());
        }

        @Test
        @DisplayName("Dashboard — farklı kullanıcılar birbirini etkilemez")
        void dashboard_userIsolation() throws Exception {
            String otherToken = registerAndGetToken("other@test.com", "otheruser", STRONG_PASSWORD);
            createGoalAndGetId(otherToken, "Diğer Kullanıcı Hedefi");

            // Current user should see 0 goals
            mockMvc.perform(get("/api/dashboard")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(jsonPath("$.data.activeGoalCount").value(0));
        }
    }

    // ========================================================================
    // STREAK
    // ========================================================================

    @Nested
    @DisplayName("Streak (Seri)")
    class StreakTests {

        @Test
        @DisplayName("Hedef streak bilgisi alınır")
        void getStreak() throws Exception {
            Long goalId = createGoalAndGetId(token, "Streak Hedefi");

            mockMvc.perform(get("/api/goals/" + goalId + "/streak")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.goalId").value(goalId))
                    .andExpect(jsonPath("$.data.currentStreak").isNumber())
                    .andExpect(jsonPath("$.data.longestStreak").isNumber());
        }
    }

    // ========================================================================
    // EXPORT
    // ========================================================================

    @Nested
    @DisplayName("Dışa Aktarma (Export)")
    class ExportTests {

        @Test
        @DisplayName("Excel export — binary data döner")
        void exportExcel() throws Exception {
            Long goalId = createGoalAndGetId(token, "Export Excel Hedefi");

            // Add an entry
            mockMvc.perform(post("/api/goals/" + goalId + "/entries")
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(
                            new CreateEntryRequest(LocalDate.now(), new BigDecimal("5.0"), null))));

            mockMvc.perform(get("/api/goals/" + goalId + "/export/excel")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(header().exists("Content-Disposition"))
                    .andExpect(content().contentTypeCompatibleWith(
                            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        }

        @Test
        @DisplayName("PDF export — binary data döner")
        void exportPdf() throws Exception {
            Long goalId = createGoalAndGetId(token, "Export PDF Hedefi");

            mockMvc.perform(get("/api/goals/" + goalId + "/export/pdf")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(header().exists("Content-Disposition"))
                    .andExpect(content().contentType("application/pdf"));
        }

        @Test
        @DisplayName("CSV export — text data döner")
        void exportCsv() throws Exception {
            Long goalId = createGoalAndGetId(token, "Export CSV Hedefi");

            // Add some entries
            for (int i = 0; i < 3; i++) {
                mockMvc.perform(post("/api/goals/" + goalId + "/entries")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CreateEntryRequest(LocalDate.now().plusDays(i),
                                        new BigDecimal(String.valueOf(i + 1)), "Gün " + (i + 1)))));
            }

            mockMvc.perform(get("/api/goals/" + goalId + "/export/csv")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(header().exists("Content-Disposition"));
        }

        @Test
        @DisplayName("Başka kullanıcının hedefini export etme — 403/404")
        void exportExcel_otherUser() throws Exception {
            String otherToken = registerAndGetToken("exporter@test.com", "exporter", STRONG_PASSWORD);
            Long otherGoalId = createGoalAndGetId(otherToken, "Diğer Hedef");

            mockMvc.perform(get("/api/goals/" + otherGoalId + "/export/excel")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().is4xxClientError());
        }
    }

    // ========================================================================
    // NOTIFICATIONS
    // ========================================================================

    @Nested
    @DisplayName("Bildirimler")
    class NotificationTests {

        @Test
        @DisplayName("Bildirim listesi — 200")
        void getNotifications() throws Exception {
            mockMvc.perform(get("/api/notifications")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.content").isArray());
        }

        @Test
        @DisplayName("Okunmamış bildirim sayısı — 200")
        void getUnreadCount() throws Exception {
            mockMvc.perform(get("/api/notifications/unread-count")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("Bildirim ayarları getirme — 200")
        void getNotificationSettings() throws Exception {
            mockMvc.perform(get("/api/notification-settings")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.emailEnabled").isBoolean())
                    .andExpect(jsonPath("$.data.pushEnabled").isBoolean());
        }
    }

    // ========================================================================
    // GOAL LIMIT TEST
    // ========================================================================

    @Test
    @DisplayName("Aktif hedef limiti (50) aşıldığında — 400")
    void goalLimit_exceeded() throws Exception {
        // Create 50 active goals
        for (int i = 0; i < 50; i++) {
            createGoalAndGetId(token, "Limit Hedef " + i);
        }

        // 51st goal should fail
        try {
            createGoalAndGetId(token, "Limit Hedef 51");
            // If no exception, check the status manually
        } catch (AssertionError e) {
            // This is expected — the createGoalAndGetId helper expects 201
            // Let's verify with direct call
        }

        // Direct call to verify
        var goalRequest = new com.goaltracker.dto.CreateGoalRequest();
        goalRequest.setTitle("51. Hedef");
        goalRequest.setUnit("birim");
        goalRequest.setGoalType(GoalType.DAILY);
        goalRequest.setTargetValue(new BigDecimal("10.00"));
        goalRequest.setStartDate(LocalDate.now());
        goalRequest.setEndDate(LocalDate.now().plusDays(30));

        mockMvc.perform(post("/api/goals")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(goalRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("GOAL_LIMIT_EXCEEDED"));
    }
}

