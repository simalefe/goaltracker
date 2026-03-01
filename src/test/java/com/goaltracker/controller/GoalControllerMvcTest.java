package com.goaltracker.controller;

import com.goaltracker.dto.GoalResponse;
import com.goaltracker.model.enums.*;
import com.goaltracker.service.ExportService;
import com.goaltracker.service.GoalService;
import com.goaltracker.util.SecurityUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class GoalControllerMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GoalService goalService;

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
    @DisplayName("GET /api/goals/{id}/export/excel → 200 Excel export")
    void exportExcel_shouldReturnOk() throws Exception {
        given(securityUtils.getCurrentUserId()).willReturn(1L);
        given(goalService.getGoal(1L, 1L)).willReturn(sampleGoalResponse());
        given(exportService.exportGoalToExcel(1L, 1L)).willReturn(new byte[]{1, 2, 3});
        given(exportService.normalizeFilename("Test Hedef")).willReturn("test-hedef");

        mockMvc.perform(get("/api/goals/1/export/excel"))
                .andExpect(status().isOk())
                .andExpect(header().exists("Content-Disposition"))
                .andExpect(content().contentTypeCompatibleWith(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
    }

    @Test
    @WithMockUser(username = "test@test.com")
    @DisplayName("GET /api/goals/{id}/export/pdf → 200 PDF export")
    void exportPdf_shouldReturnOk() throws Exception {
        given(securityUtils.getCurrentUserId()).willReturn(1L);
        given(goalService.getGoal(1L, 1L)).willReturn(sampleGoalResponse());
        given(exportService.exportGoalToPdf(1L, 1L)).willReturn(new byte[]{1, 2, 3});
        given(exportService.normalizeFilename("Test Hedef")).willReturn("test-hedef");

        mockMvc.perform(get("/api/goals/1/export/pdf"))
                .andExpect(status().isOk())
                .andExpect(header().exists("Content-Disposition"))
                .andExpect(content().contentType("application/pdf"));
    }

    @Test
    @WithMockUser(username = "test@test.com")
    @DisplayName("GET /api/goals/{id}/export/csv → 200 CSV export")
    void exportCsv_shouldReturnOk() throws Exception {
        given(securityUtils.getCurrentUserId()).willReturn(1L);
        given(goalService.getGoal(1L, 1L)).willReturn(sampleGoalResponse());
        given(exportService.exportGoalToCsv(1L, 1L)).willReturn(new byte[]{1, 2, 3});
        given(exportService.normalizeFilename("Test Hedef")).willReturn("test-hedef");

        mockMvc.perform(get("/api/goals/1/export/csv"))
                .andExpect(status().isOk())
                .andExpect(header().exists("Content-Disposition"));
    }

    @Test
    @DisplayName("Yetkilendirilmemiş export erişimi → 302 login redirect")
    void exportExcel_shouldRedirectWhenNotAuthenticated() throws Exception {
        mockMvc.perform(get("/api/goals/1/export/excel"))
                .andExpect(status().is3xxRedirection());
    }
}
