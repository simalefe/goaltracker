package com.goaltracker.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.goaltracker.dto.*;
import com.goaltracker.exception.GoalAccessDeniedException;
import com.goaltracker.exception.GoalNotFoundException;
import com.goaltracker.exception.InvalidStatusTransitionException;
import com.goaltracker.model.enums.*;
import com.goaltracker.service.GoalService;
import com.goaltracker.service.GoalEntryService;
import com.goaltracker.service.ExportService;
import com.goaltracker.service.StreakService;
import com.goaltracker.util.SecurityUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class GoalControllerMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private GoalService goalService;

    @MockBean
    private GoalEntryService goalEntryService;

    @MockBean
    private com.goaltracker.service.GoalShareService goalShareService;

    @MockBean
    private StreakService streakService;

    @MockBean
    private ExportService exportService;

    @MockBean
    private SecurityUtils securityUtils;

    private GoalResponse sampleGoalResponse() {
        GoalResponse r = new GoalResponse();
        r.setId(1L);
        r.setTitle("Test Hedef");
        r.setUnit("sayfa");
        r.setGoalType(GoalType.CUMULATIVE);
        r.setFrequency(GoalFrequency.DAILY);
        r.setTargetValue(new BigDecimal("300.00"));
        r.setStartDate(LocalDate.of(2026, 3, 1));
        r.setEndDate(LocalDate.of(2026, 3, 31));
        r.setCategory(GoalCategory.EDUCATION);
        r.setColor("#3B82F6");
        r.setStatus(GoalStatus.ACTIVE);
        r.setCompletionPct(BigDecimal.ZERO);
        r.setCurrentProgress(BigDecimal.ZERO);
        r.setDaysLeft(31);
        r.setCurrentStreak(0);
        r.setVersion(0L);
        r.setCreatedAt(Instant.now());
        r.setUpdatedAt(Instant.now());
        return r;
    }

    @Test
    @WithMockUser(username = "test@test.com")
    @DisplayName("GET /api/goals → 200 sayfalı liste")
    void listGoals_shouldReturnOk() throws Exception {
        given(securityUtils.getCurrentUserId()).willReturn(1L);
        Page<GoalSummaryResponse> emptyPage = Page.empty();
        given(goalService.getGoals(eq(1L), any(), any(), any(), any(), any())).willReturn(emptyPage);

        mockMvc.perform(get("/api/goals")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @WithMockUser(username = "test@test.com")
    @DisplayName("POST /api/goals → 201 hedef oluşturma")
    void createGoal_shouldReturn201() throws Exception {
        given(securityUtils.getCurrentUserId()).willReturn(1L);
        given(goalService.createGoal(any(CreateGoalRequest.class), eq(1L))).willReturn(sampleGoalResponse());

        String json = """
                {
                    "title": "Kitap Okuma",
                    "unit": "sayfa",
                    "goalType": "CUMULATIVE",
                    "targetValue": 300,
                    "startDate": "2026-03-01",
                    "endDate": "2026-03-31"
                }
                """;

        mockMvc.perform(post("/api/goals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    @WithMockUser(username = "test@test.com")
    @DisplayName("POST /api/goals validasyon hatası → 400")
    void createGoal_shouldReturn400ForInvalidRequest() throws Exception {
        String invalidJson = """
                {
                    "title": "",
                    "goalType": "CUMULATIVE",
                    "targetValue": 0,
                    "startDate": "2026-03-01",
                    "endDate": "2026-02-01"
                }
                """;

        mockMvc.perform(post("/api/goals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "test@test.com")
    @DisplayName("GET /api/goals/{id} → 200")
    void getGoal_shouldReturnOk() throws Exception {
        given(securityUtils.getCurrentUserId()).willReturn(1L);
        given(goalService.getGoal(1L, 1L)).willReturn(sampleGoalResponse());

        mockMvc.perform(get("/api/goals/1")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.title").value("Test Hedef"));
    }

    @Test
    @WithMockUser(username = "test@test.com")
    @DisplayName("GET /api/goals/{id} var olmayan → 404")
    void getGoal_shouldReturn404() throws Exception {
        given(securityUtils.getCurrentUserId()).willReturn(1L);
        given(goalService.getGoal(999L, 1L)).willThrow(new GoalNotFoundException(999L));

        mockMvc.perform(get("/api/goals/999")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("GOAL_NOT_FOUND"));
    }

    @Test
    @WithMockUser(username = "test@test.com")
    @DisplayName("PUT /api/goals/{id} başka kullanıcı → 403")
    void updateGoal_shouldReturn403() throws Exception {
        given(securityUtils.getCurrentUserId()).willReturn(2L);
        given(goalService.updateGoal(eq(1L), any(UpdateGoalRequest.class), eq(2L)))
                .willThrow(new GoalAccessDeniedException(1L));

        String json = """
                { "title": "Hack Attempt" }
                """;

        mockMvc.perform(put("/api/goals/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("GOAL_ACCESS_DENIED"));
    }

    @Test
    @WithMockUser(username = "test@test.com")
    @DisplayName("DELETE /api/goals/{id} → 204")
    void deleteGoal_shouldReturn204() throws Exception {
        given(securityUtils.getCurrentUserId()).willReturn(1L);
        doNothing().when(goalService).deleteGoal(1L, 1L);

        mockMvc.perform(delete("/api/goals/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(username = "test@test.com")
    @DisplayName("PATCH /api/goals/{id}/status geçerli geçiş → 200")
    void updateStatus_shouldReturn200() throws Exception {
        given(securityUtils.getCurrentUserId()).willReturn(1L);
        GoalResponse response = sampleGoalResponse();
        response.setStatus(GoalStatus.PAUSED);
        given(goalService.updateStatus(1L, 1L, GoalStatus.PAUSED)).willReturn(response);

        mockMvc.perform(patch("/api/goals/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"newStatus\": \"PAUSED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PAUSED"));
    }

    @Test
    @WithMockUser(username = "test@test.com")
    @DisplayName("PATCH /api/goals/{id}/status geçersiz geçiş → 400")
    void updateStatus_shouldReturn400ForInvalidTransition() throws Exception {
        given(securityUtils.getCurrentUserId()).willReturn(1L);
        given(goalService.updateStatus(1L, 1L, GoalStatus.ACTIVE))
                .willThrow(new InvalidStatusTransitionException("Geçersiz durum geçişi: COMPLETED → ACTIVE"));

        mockMvc.perform(patch("/api/goals/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"newStatus\": \"ACTIVE\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_STATUS_TRANSITION"));
    }

    @Test
    @DisplayName("Yetkilendirilmemiş erişim → 302 login redirect")
    void shouldRedirectToLoginWhenNotAuthenticated() throws Exception {
        mockMvc.perform(get("/api/goals")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().is3xxRedirection());
    }
}

