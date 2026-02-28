package com.goaltracker.controller;

import com.goaltracker.dto.response.*;
import com.goaltracker.model.Badge;
import com.goaltracker.model.enums.GoalStatus;
import com.goaltracker.repository.BadgeRepository;
import com.goaltracker.repository.GoalEntryRepository;
import com.goaltracker.repository.GoalRepository;
import com.goaltracker.service.BadgeService;
import com.goaltracker.service.StreakService;
import com.goaltracker.service.UserService;
import com.goaltracker.util.SecurityUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ProfileControllerMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @MockBean
    private BadgeService badgeService;

    @MockBean
    private StreakService streakService;

    @MockBean
    private SecurityUtils securityUtils;

    @MockBean
    private GoalEntryRepository goalEntryRepository;

    @MockBean
    private GoalRepository goalRepository;

    @MockBean
    private BadgeRepository badgeRepository;

    @Test
    @WithMockUser(username = "test@test.com")
    @DisplayName("GET /profile → 200 profil sayfası")
    void shouldRenderProfilePage() throws Exception {
        given(securityUtils.getCurrentUserId()).willReturn(1L);
        given(userService.getProfile("test@test.com")).willReturn(
                new UserResponse(1L, "test@test.com", "testuser", "Test User",
                        null, "Europe/Istanbul", "USER", true, Instant.now()));
        given(goalEntryRepository.countByGoal_User_Id(1L)).willReturn(25L);
        given(goalRepository.countByUserIdAndStatus(1L, GoalStatus.COMPLETED)).willReturn(3L);
        given(streakService.getTotalStreakDays(1L)).willReturn(10);
        given(badgeService.getUserBadgeCount(1L)).willReturn(5L);
        given(badgeService.getUserBadges(1L)).willReturn(List.of());
        given(badgeRepository.findAll()).willReturn(List.of());

        mockMvc.perform(get("/profile"))
                .andExpect(status().isOk())
                .andExpect(view().name("profile/index"))
                .andExpect(model().attributeExists("user"))
                .andExpect(model().attributeExists("stats"))
                .andExpect(model().attributeExists("earnedBadges"))
                .andExpect(model().attributeExists("lockedBadges"));
    }

    @Test
    @WithMockUser(username = "test@test.com")
    @DisplayName("GET /profile → rozetler doğru ayrıştırılıyor")
    void shouldSeparateEarnedAndLockedBadges() throws Exception {
        given(securityUtils.getCurrentUserId()).willReturn(1L);
        given(userService.getProfile("test@test.com")).willReturn(
                new UserResponse(1L, "test@test.com", "testuser", "Test User",
                        null, "Europe/Istanbul", "USER", true, Instant.now()));
        given(goalEntryRepository.countByGoal_User_Id(1L)).willReturn(10L);
        given(goalRepository.countByUserIdAndStatus(1L, GoalStatus.COMPLETED)).willReturn(1L);
        given(streakService.getTotalStreakDays(1L)).willReturn(5);
        given(badgeService.getUserBadgeCount(1L)).willReturn(1L);

        BadgeResponse earnedBadge = new BadgeResponse(1L, "FIRST_STEP", "İlk Adım",
                "İlk kaydını yaptın!", "🎯", "ENTRY_COUNT", 1);
        given(badgeService.getUserBadges(1L)).willReturn(List.of(
                new UserBadgeResponse(earnedBadge, Instant.now())));

        Badge allBadge1 = new Badge();
        allBadge1.setId(1L);
        allBadge1.setCode("FIRST_STEP");
        allBadge1.setName("İlk Adım");
        allBadge1.setDescription("İlk kaydını yaptın!");
        allBadge1.setIcon("🎯");
        allBadge1.setConditionType("ENTRY_COUNT");
        allBadge1.setConditionValue(1);

        Badge allBadge2 = new Badge();
        allBadge2.setId(2L);
        allBadge2.setCode("WEEK_WARRIOR");
        allBadge2.setName("Hafta Savaşçısı");
        allBadge2.setDescription("7 gün üst üste!");
        allBadge2.setIcon("🔥");
        allBadge2.setConditionType("STREAK");
        allBadge2.setConditionValue(7);

        given(badgeRepository.findAll()).willReturn(List.of(allBadge1, allBadge2));

        mockMvc.perform(get("/profile"))
                .andExpect(status().isOk())
                .andExpect(view().name("profile/index"))
                .andExpect(model().attribute("earnedBadges", org.hamcrest.Matchers.hasSize(1)))
                .andExpect(model().attribute("lockedBadges", org.hamcrest.Matchers.hasSize(1)));
    }

    @Test
    @DisplayName("Yetkilendirilmemiş erişim → redirect")
    void shouldRedirectWhenNotAuthenticated() throws Exception {
        mockMvc.perform(get("/profile"))
                .andExpect(status().is3xxRedirection());
    }
}


