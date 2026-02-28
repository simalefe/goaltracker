package com.goaltracker.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.goaltracker.dto.*;
import com.goaltracker.exception.DuplicateEntryException;
import com.goaltracker.exception.EntryOutOfRangeException;
import com.goaltracker.exception.GoalNotActiveException;
import com.goaltracker.service.GoalEntryService;
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

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class GoalEntryControllerMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private GoalEntryService goalEntryService;

    @MockBean
    private SecurityUtils securityUtils;

    private GoalEntryResponse sampleEntryResponse() {
        return new GoalEntryResponse(1L, 10L, LocalDate.of(2026, 3, 5),
                new BigDecimal("25.00"), "Test note", Instant.now());
    }

    @Test
    @WithMockUser(username = "test@test.com")
    @DisplayName("POST /api/goals/{id}/entries → 201 entry oluşturma")
    void createEntry_shouldReturn201() throws Exception {
        given(securityUtils.getCurrentUserId()).willReturn(1L);
        given(goalEntryService.createEntry(eq(10L), eq(1L), any(CreateEntryRequest.class)))
                .willReturn(sampleEntryResponse());

        String body = """
                {
                  "entryDate": "2026-03-05",
                  "actualValue": 25.00,
                  "note": "Test note"
                }
                """;

        mockMvc.perform(post("/api/goals/10/entries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.goalId").value(10))
                .andExpect(jsonPath("$.data.actualValue").value(25.00));
    }

    @Test
    @WithMockUser(username = "test@test.com")
    @DisplayName("POST aynı gün tekrar → 409 DUPLICATE_ENTRY")
    void createEntry_duplicate_shouldReturn409() throws Exception {
        given(securityUtils.getCurrentUserId()).willReturn(1L);
        given(goalEntryService.createEntry(eq(10L), eq(1L), any(CreateEntryRequest.class)))
                .willThrow(new DuplicateEntryException("2026-03-05"));

        String body = """
                {
                  "entryDate": "2026-03-05",
                  "actualValue": 25.00
                }
                """;

        mockMvc.perform(post("/api/goals/10/entries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("DUPLICATE_ENTRY"));
    }

    @Test
    @WithMockUser(username = "test@test.com")
    @DisplayName("POST tarih aralığı dışında → 400 ENTRY_OUT_OF_RANGE")
    void createEntry_outOfRange_shouldReturn400() throws Exception {
        given(securityUtils.getCurrentUserId()).willReturn(1L);
        given(goalEntryService.createEntry(eq(10L), eq(1L), any(CreateEntryRequest.class)))
                .willThrow(new EntryOutOfRangeException(LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31)));

        String body = """
                {
                  "entryDate": "2026-04-05",
                  "actualValue": 25.00
                }
                """;

        mockMvc.perform(post("/api/goals/10/entries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("ENTRY_OUT_OF_RANGE"));
    }

    @Test
    @WithMockUser(username = "test@test.com")
    @DisplayName("POST ARCHIVED hedefe → 400 GOAL_NOT_ACTIVE")
    void createEntry_goalNotActive_shouldReturn400() throws Exception {
        given(securityUtils.getCurrentUserId()).willReturn(1L);
        given(goalEntryService.createEntry(eq(10L), eq(1L), any(CreateEntryRequest.class)))
                .willThrow(new GoalNotActiveException("ARCHIVED"));

        String body = """
                {
                  "entryDate": "2026-03-05",
                  "actualValue": 25.00
                }
                """;

        mockMvc.perform(post("/api/goals/10/entries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("GOAL_NOT_ACTIVE"));
    }

    @Test
    @WithMockUser(username = "test@test.com")
    @DisplayName("GET /api/goals/{id}/entries → 200 entry listesi")
    void getEntries_shouldReturn200() throws Exception {
        given(securityUtils.getCurrentUserId()).willReturn(1L);
        given(goalEntryService.getEntries(10L, 1L)).willReturn(List.of(sampleEntryResponse()));

        mockMvc.perform(get("/api/goals/10/entries"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].goalId").value(10));
    }

    @Test
    @WithMockUser(username = "test@test.com")
    @DisplayName("PUT /api/entries/{id} → 200 entry güncelleme")
    void updateEntry_shouldReturn200() throws Exception {
        given(securityUtils.getCurrentUserId()).willReturn(1L);
        given(goalEntryService.updateEntry(eq(1L), eq(1L), any(UpdateEntryRequest.class)))
                .willReturn(sampleEntryResponse());

        String body = """
                {
                  "actualValue": 30.00,
                  "note": "Güncellendi"
                }
                """;

        mockMvc.perform(put("/api/entries/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @WithMockUser(username = "test@test.com")
    @DisplayName("DELETE /api/entries/{id} → 204")
    void deleteEntry_shouldReturn204() throws Exception {
        given(securityUtils.getCurrentUserId()).willReturn(1L);
        doNothing().when(goalEntryService).deleteEntry(1L, 1L);

        mockMvc.perform(delete("/api/entries/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(username = "test@test.com")
    @DisplayName("GET /api/goals/{id}/stats → 200 istatistikler")
    void getStats_shouldReturn200() throws Exception {
        given(securityUtils.getCurrentUserId()).willReturn(1L);
        GoalStatsResponse stats = new GoalStatsResponse(
                new BigDecimal("125.00"), new BigDecimal("300.00"),
                new BigDecimal("41.67"), new BigDecimal("16.13"),
                new BigDecimal("76.52"), "AHEAD",
                new BigDecimal("7.29"), "sayfa",
                24, 31, 7, 5, 0, 0);
        given(goalEntryService.getStats(10L, 1L)).willReturn(stats);

        mockMvc.perform(get("/api/goals/10/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.trackingStatus").value("AHEAD"))
                .andExpect(jsonPath("$.data.completionPct").value(41.67));
    }

    @Test
    @WithMockUser(username = "test@test.com")
    @DisplayName("GET /api/goals/{id}/chart-data → 200 grafik verisi")
    void getChartData_shouldReturn200() throws Exception {
        given(securityUtils.getCurrentUserId()).willReturn(1L);
        ChartDataResponse chart = new ChartDataResponse(
                List.of(new ChartDataPointResponse("2026-03-01", new BigDecimal("9.68"),
                        new BigDecimal("15.00"), new BigDecimal("15.00"))),
                new BigDecimal("9.68"));
        given(goalEntryService.getChartData(10L, 1L)).willReturn(chart);

        mockMvc.perform(get("/api/goals/10/chart-data"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.dailyTarget").value(9.68))
                .andExpect(jsonPath("$.data.dataPoints[0].date").value("2026-03-01"));
    }
}

