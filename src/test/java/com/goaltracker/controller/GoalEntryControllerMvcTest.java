package com.goaltracker.controller;

import com.goaltracker.dto.ChartDataResponse;
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
import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class GoalEntryControllerMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GoalEntryService goalEntryService;

    @MockBean
    private SecurityUtils securityUtils;

    @Test
    @WithMockUser(username = "test@test.com")
    @DisplayName("GET /api/goals/{id}/chart-data → 200 grafik verisi")
    void getChartData_shouldReturnOk() throws Exception {
        given(securityUtils.getCurrentUserId()).willReturn(1L);
        ChartDataResponse chartData = new ChartDataResponse(
                List.of(), new BigDecimal("10.00")
        );
        given(goalEntryService.getChartData(10L, 1L)).willReturn(chartData);

        mockMvc.perform(get("/api/goals/10/chart-data")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isNotEmpty());
    }

    @Test
    @DisplayName("Yetkilendirilmemiş chart-data erişimi → 302 login redirect")
    void getChartData_shouldRedirectWhenNotAuthenticated() throws Exception {
        mockMvc.perform(get("/api/goals/10/chart-data")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().is3xxRedirection());
    }
}
