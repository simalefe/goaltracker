package com.goaltracker.controller;

import com.goaltracker.dto.response.ActivityFeedItemResponse;
import com.goaltracker.dto.response.LeaderboardEntryResponse;
import com.goaltracker.service.*;
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
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class SocialControllersApiMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private LeaderboardService leaderboardService;

    @MockBean
    private ActivityFeedService activityFeedService;

    @MockBean
    private SecurityUtils securityUtils;

    @Test
    @WithMockUser(username = "test@test.com")
    @DisplayName("GET /api/leaderboard → 200")
    void getLeaderboard_shouldReturnOk() throws Exception {
        given(securityUtils.getCurrentUserId()).willReturn(1L);

        LeaderboardEntryResponse entry = new LeaderboardEntryResponse();
        entry.setRank(1);
        entry.setUserId(1L);
        entry.setUsername("alice");
        entry.setDisplayName("Alice");
        entry.setCompletionPct(new BigDecimal("75.00"));
        entry.setCurrentStreak(5);
        entry.setGoalTitle("Kitap Okuma");
        entry.setCurrentUser(true);

        given(leaderboardService.getLeaderboard(eq(1L), any())).willReturn(List.of(entry));

        mockMvc.perform(get("/api/leaderboard")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].rank").value(1))
                .andExpect(jsonPath("$.data[0].username").value("alice"));
    }

    @Test
    @WithMockUser(username = "test@test.com")
    @DisplayName("GET /api/social/activity-feed → 200")
    void getActivityFeed_shouldReturnOk() throws Exception {
        given(securityUtils.getCurrentUserId()).willReturn(1L);

        ActivityFeedItemResponse item = new ActivityFeedItemResponse();
        item.setType("ENTRY");
        item.setUserId(2L);
        item.setUsername("bob");
        item.setGoalTitle("Spor");
        item.setValue("30");
        item.setUnit("dakika");
        item.setTimestamp(Instant.now());

        given(activityFeedService.getFeed(1L)).willReturn(List.of(item));

        mockMvc.perform(get("/api/social/activity-feed")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].type").value("ENTRY"))
                .andExpect(jsonPath("$.data[0].username").value("bob"));
    }
}

