package com.goaltracker.controller;

import com.goaltracker.dto.response.DashboardGoalSummary;
import com.goaltracker.dto.response.DashboardResponse;
import com.goaltracker.dto.response.RecentEntryResponse;
import com.goaltracker.model.enums.GoalCategory;
import com.goaltracker.model.enums.GoalStatus;
import com.goaltracker.service.DashboardService;
import com.goaltracker.service.StreakService;
import com.goaltracker.util.SecurityUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class DashboardControllerMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DashboardService dashboardService;

    @MockBean
    private StreakService streakService;

    @MockBean
    private SecurityUtils securityUtils;

    private DashboardResponse sampleDashboard() {
        return new DashboardResponse(
                3, 2, 0, 2, 1,
                List.of(
                        new DashboardGoalSummary(1L, "Kitap Okuma", "sayfa",
                                new BigDecimal("85.50"), new BigDecimal("15.30"), "AHEAD",
                                GoalStatus.ACTIVE, GoalCategory.EDUCATION, "#3B82F6", 0, 10),
                        new DashboardGoalSummary(2L, "Koşu", "km",
                                new BigDecimal("65.00"), new BigDecimal("-5.00"), "BEHIND",
                                GoalStatus.ACTIVE, GoalCategory.FITNESS, "#10B981", 0, 45)
                ),
                List.of(
                        new RecentEntryResponse(10L, 1L, "Kitap Okuma", "sayfa",
                                new BigDecimal("25.00"), LocalDate.of(2026, 2, 28),
                                Instant.parse("2026-02-28T20:30:00Z"))
                )
        );
    }

    @Test
    @DisplayName("GET /api/dashboard — 200 DashboardResponse döner")
    @WithMockUser(username = "test@test.com")
    void shouldReturnDashboardResponse() throws Exception {
        given(securityUtils.getCurrentUserId()).willReturn(1L);
        given(dashboardService.getDashboard(1L)).willReturn(sampleDashboard());

        mockMvc.perform(get("/api/dashboard")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.activeGoalCount").value(3))
                .andExpect(jsonPath("$.data.todayEntryCount").value(2))
                .andExpect(jsonPath("$.data.totalStreakDays").value(0))
                .andExpect(jsonPath("$.data.goalsOnTrack").value(2))
                .andExpect(jsonPath("$.data.goalsBehind").value(1))
                .andExpect(jsonPath("$.data.topGoals").isArray())
                .andExpect(jsonPath("$.data.topGoals[0].goalId").value(1))
                .andExpect(jsonPath("$.data.topGoals[0].title").value("Kitap Okuma"))
                .andExpect(jsonPath("$.data.topGoals[0].trackingStatus").value("AHEAD"))
                .andExpect(jsonPath("$.data.recentEntries").isArray())
                .andExpect(jsonPath("$.data.recentEntries[0].entryId").value(10));
    }

    @Test
    @DisplayName("GET /api/dashboard — boş dashboard")
    @WithMockUser(username = "test@test.com")
    void shouldReturnEmptyDashboard() throws Exception {
        given(securityUtils.getCurrentUserId()).willReturn(1L);
        given(dashboardService.getDashboard(1L)).willReturn(
                new DashboardResponse(0, 0, 0, 0, 0, List.of(), List.of())
        );

        mockMvc.perform(get("/api/dashboard")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.activeGoalCount").value(0))
                .andExpect(jsonPath("$.data.topGoals").isEmpty())
                .andExpect(jsonPath("$.data.recentEntries").isEmpty());
    }

    @Test
    @DisplayName("GET /api/dashboard — authentication gerekli")
    void shouldRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/dashboard")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @DisplayName("GET /dashboard — MVC dashboard sayfası yükleniyor")
    @WithMockUser(username = "test@test.com")
    void shouldRenderDashboardPage() throws Exception {
        given(securityUtils.getCurrentUserId()).willReturn(1L);
        given(dashboardService.getDashboard(1L)).willReturn(sampleDashboard());

        mockMvc.perform(get("/dashboard"))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard/index"))
                .andExpect(model().attribute("activeGoalCount", 3))
                .andExpect(model().attribute("todayEntryCount", 2))
                .andExpect(model().attribute("goalsOnTrack", 2))
                .andExpect(model().attribute("goalsBehind", 1))
                .andExpect(model().attributeExists("topGoals"))
                .andExpect(model().attributeExists("recentEntries"));
    }

    @Test
    @DisplayName("GET /dashboard — boş dashboard sayfası")
    @WithMockUser(username = "test@test.com")
    void shouldRenderEmptyDashboardPage() throws Exception {
        given(securityUtils.getCurrentUserId()).willReturn(1L);
        given(dashboardService.getDashboard(1L)).willReturn(
                new DashboardResponse(0, 0, 0, 0, 0, List.of(), List.of())
        );

        mockMvc.perform(get("/dashboard"))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard/index"))
                .andExpect(model().attribute("activeGoalCount", 0));
    }
}

