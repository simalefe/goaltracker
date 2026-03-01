package com.goaltracker.service;

import com.goaltracker.dto.response.DashboardGoalSummary;
import com.goaltracker.dto.response.DashboardResponse;
import com.goaltracker.dto.response.RecentEntryResponse;
import com.goaltracker.model.Goal;
import com.goaltracker.model.GoalEntry;
import com.goaltracker.model.User;
import com.goaltracker.model.enums.*;
import com.goaltracker.repository.GoalEntryRepository;
import com.goaltracker.repository.GoalRepository;
import com.goaltracker.service.impl.DashboardServiceImpl;
import com.goaltracker.util.GoalCalculator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private GoalRepository goalRepository;

    @Mock
    private GoalEntryRepository goalEntryRepository;

    @Mock
    private GoalCalculator goalCalculator;

    @Mock
    private StreakService streakService;

    @InjectMocks
    private DashboardServiceImpl dashboardService;

    private User testUser;
    private Goal goal1;
    private Goal goal2;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@test.com");

        goal1 = new Goal();
        goal1.setId(10L);
        goal1.setUser(testUser);
        goal1.setTitle("Kitap Okuma");
        goal1.setUnit("sayfa");
        goal1.setGoalType(GoalType.CUMULATIVE);
        goal1.setFrequency(GoalFrequency.DAILY);
        goal1.setTargetValue(new BigDecimal("300.00"));
        goal1.setStartDate(LocalDate.of(2026, 1, 1));
        goal1.setEndDate(LocalDate.of(2026, 3, 31));
        goal1.setCategory(GoalCategory.EDUCATION);
        goal1.setColor("#3B82F6");
        goal1.setStatus(GoalStatus.ACTIVE);

        goal2 = new Goal();
        goal2.setId(20L);
        goal2.setUser(testUser);
        goal2.setTitle("Koşu");
        goal2.setUnit("km");
        goal2.setGoalType(GoalType.CUMULATIVE);
        goal2.setFrequency(GoalFrequency.DAILY);
        goal2.setTargetValue(new BigDecimal("100.00"));
        goal2.setStartDate(LocalDate.of(2026, 1, 1));
        goal2.setEndDate(LocalDate.of(2026, 6, 30));
        goal2.setCategory(GoalCategory.FITNESS);
        goal2.setColor("#10B981");
        goal2.setStatus(GoalStatus.ACTIVE);
    }

    @Nested
    @DisplayName("getDashboard")
    class GetDashboardTests {

        @Test
        @DisplayName("Aktif hedef yoksa boş dashboard döner")
        void shouldReturnEmptyDashboardWhenNoActiveGoals() {
            given(goalRepository.findByUserIdAndStatus(1L, GoalStatus.ACTIVE))
                    .willReturn(List.of());

            DashboardResponse response = dashboardService.getDashboard(1L);

            assertThat(response.activeGoalCount()).isZero();
            assertThat(response.todayEntryCount()).isZero();
            assertThat(response.totalStreakDays()).isZero();
            assertThat(response.goalsOnTrack()).isZero();
            assertThat(response.goalsBehind()).isZero();
            assertThat(response.topGoals()).isEmpty();
            assertThat(response.recentEntries()).isEmpty();
        }

        @Test
        @DisplayName("Aktif hedef sayısı doğru hesaplanıyor")
        void shouldCalculateActiveGoalCountCorrectly() {
            given(goalRepository.findByUserIdAndStatus(1L, GoalStatus.ACTIVE))
                    .willReturn(List.of(goal1, goal2));
            given(goalEntryRepository.sumByGoalIds(List.of(10L, 20L)))
                    .willReturn(List.of(
                            new Object[]{10L, new BigDecimal("200.00")},
                            new Object[]{20L, new BigDecimal("30.00")}
                    ));
            given(goalEntryRepository.countDistinctGoalsByUserIdAndEntryDate(eq(1L), any(LocalDate.class)))
                    .willReturn(1);
            given(goalCalculator.calculatePlannedProgress(eq(goal1), any(LocalDate.class)))
                    .willReturn(new BigDecimal("180.00"));
            given(goalCalculator.calculatePlannedProgress(eq(goal2), any(LocalDate.class)))
                    .willReturn(new BigDecimal("50.00"));
            given(goalCalculator.calculateDaysLeft(goal1)).willReturn(31);
            given(goalCalculator.calculateDaysLeft(goal2)).willReturn(122);
            given(goalEntryRepository.findRecentByUserId(eq(1L), any(PageRequest.class)))
                    .willReturn(List.of());

            DashboardResponse response = dashboardService.getDashboard(1L);

            assertThat(response.activeGoalCount()).isEqualTo(2);
        }

        @Test
        @DisplayName("goalsOnTrack / goalsBehind ayrımı doğru (gap >= 0)")
        void shouldCalculateOnTrackAndBehindCorrectly() {
            given(goalRepository.findByUserIdAndStatus(1L, GoalStatus.ACTIVE))
                    .willReturn(List.of(goal1, goal2));
            // goal1: progress=200, planned=180 → gap=20 → onTrack
            // goal2: progress=30, planned=50 → gap=-20 → behind
            given(goalEntryRepository.sumByGoalIds(List.of(10L, 20L)))
                    .willReturn(List.of(
                            new Object[]{10L, new BigDecimal("200.00")},
                            new Object[]{20L, new BigDecimal("30.00")}
                    ));
            given(goalEntryRepository.countDistinctGoalsByUserIdAndEntryDate(eq(1L), any(LocalDate.class)))
                    .willReturn(0);
            given(goalCalculator.calculatePlannedProgress(eq(goal1), any(LocalDate.class)))
                    .willReturn(new BigDecimal("180.00"));
            given(goalCalculator.calculatePlannedProgress(eq(goal2), any(LocalDate.class)))
                    .willReturn(new BigDecimal("50.00"));
            given(goalCalculator.calculateDaysLeft(goal1)).willReturn(31);
            given(goalCalculator.calculateDaysLeft(goal2)).willReturn(122);
            given(goalEntryRepository.findRecentByUserId(eq(1L), any(PageRequest.class)))
                    .willReturn(List.of());

            DashboardResponse response = dashboardService.getDashboard(1L);

            assertThat(response.goalsOnTrack()).isEqualTo(1);
            assertThat(response.goalsBehind()).isEqualTo(1);
        }

        @Test
        @DisplayName("todayEntryCount doğru sayılıyor")
        void shouldCountTodayEntriesCorrectly() {
            given(goalRepository.findByUserIdAndStatus(1L, GoalStatus.ACTIVE))
                    .willReturn(List.of(goal1));
            given(goalEntryRepository.sumByGoalIds(List.of(10L)))
                    .willReturn(List.<Object[]>of(new Object[]{10L, new BigDecimal("100.00")}));
            given(goalEntryRepository.countDistinctGoalsByUserIdAndEntryDate(eq(1L), any(LocalDate.class)))
                    .willReturn(2);
            given(goalCalculator.calculatePlannedProgress(eq(goal1), any(LocalDate.class)))
                    .willReturn(new BigDecimal("100.00"));
            given(goalCalculator.calculateDaysLeft(goal1)).willReturn(31);
            given(goalEntryRepository.findRecentByUserId(eq(1L), any(PageRequest.class)))
                    .willReturn(List.of());

            DashboardResponse response = dashboardService.getDashboard(1L);

            assertThat(response.todayEntryCount()).isEqualTo(2);
        }

        @Test
        @DisplayName("topGoals en fazla 5 hedef döner, completionPct DESC sıralı")
        void shouldReturnTopGoalsSortedByCompletionPctDesc() {
            given(goalRepository.findByUserIdAndStatus(1L, GoalStatus.ACTIVE))
                    .willReturn(List.of(goal1, goal2));
            // goal1: 200/300 = 66.67%, goal2: 30/100 = 30%
            given(goalEntryRepository.sumByGoalIds(List.of(10L, 20L)))
                    .willReturn(List.of(
                            new Object[]{10L, new BigDecimal("200.00")},
                            new Object[]{20L, new BigDecimal("30.00")}
                    ));
            given(goalEntryRepository.countDistinctGoalsByUserIdAndEntryDate(eq(1L), any(LocalDate.class)))
                    .willReturn(0);
            given(goalCalculator.calculatePlannedProgress(eq(goal1), any(LocalDate.class)))
                    .willReturn(new BigDecimal("180.00"));
            given(goalCalculator.calculatePlannedProgress(eq(goal2), any(LocalDate.class)))
                    .willReturn(new BigDecimal("50.00"));
            given(goalCalculator.calculateDaysLeft(goal1)).willReturn(31);
            given(goalCalculator.calculateDaysLeft(goal2)).willReturn(122);
            given(goalEntryRepository.findRecentByUserId(eq(1L), any(PageRequest.class)))
                    .willReturn(List.of());

            DashboardResponse response = dashboardService.getDashboard(1L);

            assertThat(response.topGoals()).hasSize(2);
            // First should have higher completionPct
            assertThat(response.topGoals().get(0).completionPct())
                    .isGreaterThanOrEqualTo(response.topGoals().get(1).completionPct());
        }

        @Test
        @DisplayName("recentEntries son 5 entry döner")
        void shouldReturnRecentEntries() {
            GoalEntry entry = new GoalEntry();
            entry.setId(100L);
            entry.setGoal(goal1);
            entry.setEntryDate(LocalDate.of(2026, 2, 28));
            entry.setActualValue(new BigDecimal("25.00"));

            given(goalRepository.findByUserIdAndStatus(1L, GoalStatus.ACTIVE))
                    .willReturn(List.of(goal1));
            given(goalEntryRepository.sumByGoalIds(List.of(10L)))
                    .willReturn(List.<Object[]>of(new Object[]{10L, new BigDecimal("100.00")}));
            given(goalEntryRepository.countDistinctGoalsByUserIdAndEntryDate(eq(1L), any(LocalDate.class)))
                    .willReturn(1);
            given(goalCalculator.calculatePlannedProgress(eq(goal1), any(LocalDate.class)))
                    .willReturn(new BigDecimal("90.00"));
            given(goalCalculator.calculateDaysLeft(goal1)).willReturn(31);
            given(goalEntryRepository.findRecentByUserId(eq(1L), any(PageRequest.class)))
                    .willReturn(List.of(entry));

            DashboardResponse response = dashboardService.getDashboard(1L);

            assertThat(response.recentEntries()).hasSize(1);
            RecentEntryResponse recent = response.recentEntries().get(0);
            assertThat(recent.goalId()).isEqualTo(10L);
            assertThat(recent.goalTitle()).isEqualTo("Kitap Okuma");
            assertThat(recent.actualValue()).isEqualByComparingTo("25.00");
            assertThat(recent.entryDate()).isEqualTo(LocalDate.of(2026, 2, 28));
        }

        @Test
        @DisplayName("totalStreakDays Faz 5'e kadar 0 döner")
        void shouldReturnZeroForTotalStreakDays() {
            given(goalRepository.findByUserIdAndStatus(1L, GoalStatus.ACTIVE))
                    .willReturn(List.of(goal1));
            given(goalEntryRepository.sumByGoalIds(List.of(10L)))
                    .willReturn(List.<Object[]>of(new Object[]{10L, new BigDecimal("100.00")}));
            given(goalEntryRepository.countDistinctGoalsByUserIdAndEntryDate(eq(1L), any(LocalDate.class)))
                    .willReturn(0);
            given(goalCalculator.calculatePlannedProgress(eq(goal1), any(LocalDate.class)))
                    .willReturn(new BigDecimal("100.00"));
            given(goalCalculator.calculateDaysLeft(goal1)).willReturn(31);
            given(goalEntryRepository.findRecentByUserId(eq(1L), any(PageRequest.class)))
                    .willReturn(List.of());

            DashboardResponse response = dashboardService.getDashboard(1L);

            assertThat(response.totalStreakDays()).isZero();
        }

        @Test
        @DisplayName("Progress verisi olmayan hedef için varsayılan 0 kullanılır")
        void shouldHandleMissingProgressDataWithZeroDefault() {
            given(goalRepository.findByUserIdAndStatus(1L, GoalStatus.ACTIVE))
                    .willReturn(List.of(goal1));
            // No progress data returned
            given(goalEntryRepository.sumByGoalIds(List.of(10L)))
                    .willReturn(List.of());
            given(goalEntryRepository.countDistinctGoalsByUserIdAndEntryDate(eq(1L), any(LocalDate.class)))
                    .willReturn(0);
            given(goalCalculator.calculatePlannedProgress(eq(goal1), any(LocalDate.class)))
                    .willReturn(new BigDecimal("100.00"));
            given(goalCalculator.calculateDaysLeft(goal1)).willReturn(31);
            given(goalEntryRepository.findRecentByUserId(eq(1L), any(PageRequest.class)))
                    .willReturn(List.of());

            DashboardResponse response = dashboardService.getDashboard(1L);

            // Should not crash, goal is behind since progress=0 < planned=100
            assertThat(response.goalsBehind()).isEqualTo(1);
            assertThat(response.goalsOnTrack()).isZero();
        }
    }
}

