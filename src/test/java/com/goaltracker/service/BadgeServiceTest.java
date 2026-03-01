package com.goaltracker.service;

import com.goaltracker.dto.response.UserBadgeResponse;
import com.goaltracker.model.Badge;
import com.goaltracker.model.Goal;
import com.goaltracker.model.User;
import com.goaltracker.model.UserBadge;
import com.goaltracker.model.enums.GoalStatus;
import com.goaltracker.model.enums.GoalType;
import com.goaltracker.repository.*;
import com.goaltracker.service.impl.BadgeServiceImpl;
import com.goaltracker.util.GoalCalculator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BadgeServiceTest {

    @Mock
    private BadgeRepository badgeRepository;

    @Mock
    private UserBadgeRepository userBadgeRepository;

    @Mock
    private GoalEntryRepository goalEntryRepository;

    @Mock
    private GoalRepository goalRepository;

    @Mock
    private StreakService streakService;

    @Mock
    private GoalCalculator goalCalculator;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private BadgeServiceImpl badgeService;

    private Badge firstStepBadge;
    private Badge weekWarriorBadge;
    private Badge speedDemonBadge;
    private Badge multiTaskerBadge;

    @BeforeEach
    void setUp() {
        firstStepBadge = new Badge();
        firstStepBadge.setId(1L);
        firstStepBadge.setCode("FIRST_STEP");
        firstStepBadge.setName("İlk Adım");
        firstStepBadge.setDescription("İlk ilerleme kaydını yaptın!");
        firstStepBadge.setIcon("🎯");
        firstStepBadge.setConditionType("ENTRY_COUNT");
        firstStepBadge.setConditionValue(1);

        weekWarriorBadge = new Badge();
        weekWarriorBadge.setId(2L);
        weekWarriorBadge.setCode("WEEK_WARRIOR");
        weekWarriorBadge.setName("Hafta Savaşçısı");
        weekWarriorBadge.setDescription("7 gün üst üste hedefe ulaştın!");
        weekWarriorBadge.setIcon("🔥");
        weekWarriorBadge.setConditionType("STREAK");
        weekWarriorBadge.setConditionValue(7);

        speedDemonBadge = new Badge();
        speedDemonBadge.setId(4L);
        speedDemonBadge.setCode("SPEED_DEMON");
        speedDemonBadge.setName("Süper Hızlı");
        speedDemonBadge.setDescription("Planının %150'sini tutturdun!");
        speedDemonBadge.setIcon("⚡");
        speedDemonBadge.setConditionType("PACE_PCT");
        speedDemonBadge.setConditionValue(150);

        multiTaskerBadge = new Badge();
        multiTaskerBadge.setId(6L);
        multiTaskerBadge.setCode("MULTI_TASKER");
        multiTaskerBadge.setName("Çok Yönlü");
        multiTaskerBadge.setDescription("Aynı anda 5 aktif hedefin var!");
        multiTaskerBadge.setIcon("🌟");
        multiTaskerBadge.setConditionType("ACTIVE_GOALS");
        multiTaskerBadge.setConditionValue(5);
    }

    @Nested
    @DisplayName("checkAndAwardBadges")
    class CheckAndAwardBadgesTests {

        @Test
        @DisplayName("FIRST_STEP: 1. entry sonrası kazanılıyor")
        void shouldAwardFirstStepBadge() {
            Long userId = 1L;
            Long goalId = 10L;

            given(streakService.getStreakForGoal(goalId)).willReturn(1);
            given(badgeRepository.findByConditionType("STREAK")).willReturn(List.of(weekWarriorBadge));
            given(badgeRepository.findByConditionType("ENTRY_COUNT")).willReturn(List.of(firstStepBadge));
            given(badgeRepository.findByConditionType("COMPLETIONS")).willReturn(List.of());
            given(badgeRepository.findByConditionType("ACTIVE_GOALS")).willReturn(List.of(multiTaskerBadge));
            given(badgeRepository.findByConditionType("PACE_PCT")).willReturn(List.of(speedDemonBadge));
            given(goalEntryRepository.countByGoal_User_Id(userId)).willReturn(1L);
            given(goalRepository.countByUserIdAndStatus(userId, GoalStatus.ACTIVE)).willReturn(1L);
            given(goalRepository.findById(goalId)).willReturn(Optional.empty());

            given(userBadgeRepository.existsByUserIdAndBadgeId(userId, firstStepBadge.getId())).willReturn(false);
            given(userBadgeRepository.save(any(UserBadge.class))).willAnswer(inv -> inv.getArgument(0));

            badgeService.checkAndAwardBadges(userId, goalId);

            verify(userBadgeRepository).save(argThat(ub ->
                    ub.getBadge().getCode().equals("FIRST_STEP")));
        }

        @Test
        @DisplayName("WEEK_WARRIOR: streak 7'ye ulaşınca kazanılıyor")
        void shouldAwardWeekWarriorBadge() {
            Long userId = 1L;
            Long goalId = 10L;

            given(streakService.getStreakForGoal(goalId)).willReturn(7);
            given(badgeRepository.findByConditionType("STREAK")).willReturn(List.of(weekWarriorBadge));
            given(badgeRepository.findByConditionType("ENTRY_COUNT")).willReturn(List.of(firstStepBadge));
            given(badgeRepository.findByConditionType("COMPLETIONS")).willReturn(List.of());
            given(badgeRepository.findByConditionType("ACTIVE_GOALS")).willReturn(List.of());
            given(badgeRepository.findByConditionType("PACE_PCT")).willReturn(List.of());
            given(goalEntryRepository.countByGoal_User_Id(userId)).willReturn(7L);

            given(userBadgeRepository.existsByUserIdAndBadgeId(userId, weekWarriorBadge.getId())).willReturn(false);
            given(userBadgeRepository.existsByUserIdAndBadgeId(userId, firstStepBadge.getId())).willReturn(true); // already earned
            given(userBadgeRepository.save(any(UserBadge.class))).willAnswer(inv -> inv.getArgument(0));

            badgeService.checkAndAwardBadges(userId, goalId);

            verify(userBadgeRepository).save(argThat(ub ->
                    ub.getBadge().getCode().equals("WEEK_WARRIOR")));
        }

        @Test
        @DisplayName("Zaten kazanılmış badge tekrar verilmez (idempotent)")
        void shouldNotAwardAlreadyEarnedBadge() {
            Long userId = 1L;
            Long goalId = 10L;

            given(streakService.getStreakForGoal(goalId)).willReturn(1);
            given(badgeRepository.findByConditionType("STREAK")).willReturn(List.of());
            given(badgeRepository.findByConditionType("ENTRY_COUNT")).willReturn(List.of(firstStepBadge));
            given(badgeRepository.findByConditionType("COMPLETIONS")).willReturn(List.of());
            given(badgeRepository.findByConditionType("ACTIVE_GOALS")).willReturn(List.of());
            given(badgeRepository.findByConditionType("PACE_PCT")).willReturn(List.of());
            given(goalEntryRepository.countByGoal_User_Id(userId)).willReturn(5L);

            // Already earned
            given(userBadgeRepository.existsByUserIdAndBadgeId(userId, firstStepBadge.getId())).willReturn(true);

            badgeService.checkAndAwardBadges(userId, goalId);

            verify(userBadgeRepository, never()).save(any());
        }

        @Test
        @DisplayName("SPEED_DEMON: pace >= 150 kontrolü doğru çalışıyor")
        void shouldAwardSpeedDemonWhenPaceIsHigh() {
            Long userId = 1L;
            Long goalId = 10L;

            Goal goal = new Goal();
            goal.setId(goalId);
            goal.setGoalType(GoalType.CUMULATIVE);
            goal.setTargetValue(new BigDecimal("100"));
            goal.setStartDate(LocalDate.of(2026, 1, 1));
            goal.setEndDate(LocalDate.of(2026, 3, 31));

            given(streakService.getStreakForGoal(goalId)).willReturn(1);
            given(badgeRepository.findByConditionType("STREAK")).willReturn(List.of());
            given(badgeRepository.findByConditionType("ENTRY_COUNT")).willReturn(List.of());
            given(badgeRepository.findByConditionType("COMPLETIONS")).willReturn(List.of());
            given(badgeRepository.findByConditionType("ACTIVE_GOALS")).willReturn(List.of());
            given(badgeRepository.findByConditionType("PACE_PCT")).willReturn(List.of(speedDemonBadge));
            given(goalRepository.findById(goalId)).willReturn(Optional.of(goal));
            given(goalCalculator.calculateCurrentProgress(goalId)).willReturn(new BigDecimal("75"));
            given(goalCalculator.calculatePlannedProgress(eq(goal), any(LocalDate.class)))
                    .willReturn(new BigDecimal("50")); // pace = 150%

            given(userBadgeRepository.existsByUserIdAndBadgeId(userId, speedDemonBadge.getId())).willReturn(false);
            given(userBadgeRepository.save(any(UserBadge.class))).willAnswer(inv -> inv.getArgument(0));

            badgeService.checkAndAwardBadges(userId, goalId);

            verify(userBadgeRepository).save(argThat(ub ->
                    ub.getBadge().getCode().equals("SPEED_DEMON")));
        }

        @Test
        @DisplayName("MULTI_TASKER: 5 aktif hedef olunca kazanılıyor")
        void shouldAwardMultiTaskerBadge() {
            Long userId = 1L;
            Long goalId = 10L;

            given(streakService.getStreakForGoal(goalId)).willReturn(1);
            given(badgeRepository.findByConditionType("STREAK")).willReturn(List.of());
            given(badgeRepository.findByConditionType("ENTRY_COUNT")).willReturn(List.of());
            given(badgeRepository.findByConditionType("COMPLETIONS")).willReturn(List.of());
            given(badgeRepository.findByConditionType("ACTIVE_GOALS")).willReturn(List.of(multiTaskerBadge));
            given(badgeRepository.findByConditionType("PACE_PCT")).willReturn(List.of());
            given(goalRepository.countByUserIdAndStatus(userId, GoalStatus.ACTIVE)).willReturn(5L);

            given(userBadgeRepository.existsByUserIdAndBadgeId(userId, multiTaskerBadge.getId())).willReturn(false);
            given(userBadgeRepository.save(any(UserBadge.class))).willAnswer(inv -> inv.getArgument(0));

            badgeService.checkAndAwardBadges(userId, goalId);

            verify(userBadgeRepository).save(argThat(ub ->
                    ub.getBadge().getCode().equals("MULTI_TASKER")));
        }
    }

    @Nested
    @DisplayName("getUserBadges")
    class GetUserBadgesTests {

        @Test
        @DisplayName("Kazanılan rozetleri döner")
        void shouldReturnEarnedBadges() {
            UserBadge ub = new UserBadge();
            ub.setId(1L);
            User user = new User();
            user.setId(1L);
            ub.setUser(user);
            ub.setBadge(firstStepBadge);
            ub.setEarnedAt(Instant.now());

            given(userBadgeRepository.findByUserId(1L)).willReturn(List.of(ub));

            List<UserBadgeResponse> badges = badgeService.getUserBadges(1L);

            assertThat(badges).hasSize(1);
            assertThat(badges.get(0).badge().code()).isEqualTo("FIRST_STEP");
            assertThat(badges.get(0).badge().name()).isEqualTo("İlk Adım");
        }
    }
}

