package com.goaltracker.service;

import com.goaltracker.dto.response.StreakResponse;
import com.goaltracker.model.Goal;
import com.goaltracker.model.Streak;
import com.goaltracker.model.User;
import com.goaltracker.model.enums.GoalStatus;
import com.goaltracker.repository.GoalRepository;
import com.goaltracker.repository.StreakRepository;
import com.goaltracker.service.impl.StreakServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StreakServiceTest {

    @Mock
    private StreakRepository streakRepository;

    @Mock
    private GoalRepository goalRepository;

    @InjectMocks
    private StreakServiceImpl streakService;

    private Goal testGoal;
    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);

        testGoal = new Goal();
        testGoal.setId(10L);
        testGoal.setUser(testUser);
        testGoal.setStatus(GoalStatus.ACTIVE);
    }

    @Nested
    @DisplayName("updateStreak")
    class UpdateStreakTests {

        @Test
        @DisplayName("İlk entry → currentStreak = 1")
        void firstEntry_shouldSetStreakToOne() {
            given(streakRepository.findByGoalId(10L)).willReturn(Optional.empty());
            given(goalRepository.findById(10L)).willReturn(Optional.of(testGoal));
            given(streakRepository.save(any(Streak.class))).willAnswer(inv -> inv.getArgument(0));

            streakService.updateStreak(10L, LocalDate.of(2026, 3, 1));

            ArgumentCaptor<Streak> captor = ArgumentCaptor.forClass(Streak.class);
            verify(streakRepository).save(captor.capture());
            Streak saved = captor.getValue();
            assertThat(saved.getCurrentStreak()).isEqualTo(1);
            assertThat(saved.getLongestStreak()).isEqualTo(1);
            assertThat(saved.getLastEntryDate()).isEqualTo(LocalDate.of(2026, 3, 1));
        }

        @Test
        @DisplayName("Dün entry girildi, bugün de → currentStreak++")
        void consecutiveDay_shouldIncrementStreak() {
            Streak existing = new Streak();
            existing.setGoal(testGoal);
            existing.setCurrentStreak(3);
            existing.setLongestStreak(5);
            existing.setLastEntryDate(LocalDate.of(2026, 3, 4));

            given(streakRepository.findByGoalId(10L)).willReturn(Optional.of(existing));
            given(streakRepository.save(any(Streak.class))).willAnswer(inv -> inv.getArgument(0));

            streakService.updateStreak(10L, LocalDate.of(2026, 3, 5));

            ArgumentCaptor<Streak> captor = ArgumentCaptor.forClass(Streak.class);
            verify(streakRepository).save(captor.capture());
            Streak saved = captor.getValue();
            assertThat(saved.getCurrentStreak()).isEqualTo(4);
            assertThat(saved.getLongestStreak()).isEqualTo(5); // max korunur
        }

        @Test
        @DisplayName("2 gün önce entry girildi, bugün → streak = 1 (sıfırlandı)")
        void gapInDays_shouldResetStreakToOne() {
            Streak existing = new Streak();
            existing.setGoal(testGoal);
            existing.setCurrentStreak(7);
            existing.setLongestStreak(7);
            existing.setLastEntryDate(LocalDate.of(2026, 3, 2));

            given(streakRepository.findByGoalId(10L)).willReturn(Optional.of(existing));
            given(streakRepository.save(any(Streak.class))).willAnswer(inv -> inv.getArgument(0));

            streakService.updateStreak(10L, LocalDate.of(2026, 3, 5));

            ArgumentCaptor<Streak> captor = ArgumentCaptor.forClass(Streak.class);
            verify(streakRepository).save(captor.capture());
            Streak saved = captor.getValue();
            assertThat(saved.getCurrentStreak()).isEqualTo(1);
            assertThat(saved.getLongestStreak()).isEqualTo(7); // max korunur
        }

        @Test
        @DisplayName("Aynı gün tekrar event → streak değişmez (idempotent)")
        void sameDayEntry_shouldNotChangeStreak() {
            Streak existing = new Streak();
            existing.setGoal(testGoal);
            existing.setCurrentStreak(3);
            existing.setLongestStreak(5);
            existing.setLastEntryDate(LocalDate.of(2026, 3, 5));

            given(streakRepository.findByGoalId(10L)).willReturn(Optional.of(existing));

            streakService.updateStreak(10L, LocalDate.of(2026, 3, 5));

            verify(streakRepository, never()).save(any());
        }

        @Test
        @DisplayName("longestStreak doğru güncelleniyor")
        void shouldUpdateLongestStreak() {
            Streak existing = new Streak();
            existing.setGoal(testGoal);
            existing.setCurrentStreak(4);
            existing.setLongestStreak(4);
            existing.setLastEntryDate(LocalDate.of(2026, 3, 4));

            given(streakRepository.findByGoalId(10L)).willReturn(Optional.of(existing));
            given(streakRepository.save(any(Streak.class))).willAnswer(inv -> inv.getArgument(0));

            streakService.updateStreak(10L, LocalDate.of(2026, 3, 5));

            ArgumentCaptor<Streak> captor = ArgumentCaptor.forClass(Streak.class);
            verify(streakRepository).save(captor.capture());
            Streak saved = captor.getValue();
            assertThat(saved.getCurrentStreak()).isEqualTo(5);
            assertThat(saved.getLongestStreak()).isEqualTo(5);
        }
    }

    @Nested
    @DisplayName("resetStaleStreaks")
    class ResetStaleStreaksTests {

        @Test
        @DisplayName("ACTIVE hedef dünden eski entry → streak = 0")
        void shouldResetStaleActiveStreaks() {
            LocalDate today = LocalDate.of(2026, 3, 5);
            given(streakRepository.resetStaleStreaks(GoalStatus.ACTIVE, today.minusDays(1)))
                    .willReturn(3);

            int count = streakService.resetStaleStreaks(today);

            assertThat(count).isEqualTo(3);
            verify(streakRepository).resetStaleStreaks(GoalStatus.ACTIVE, today.minusDays(1));
        }
    }

    @Nested
    @DisplayName("getStreak")
    class GetStreakTests {

        @Test
        @DisplayName("Streak varsa response döner")
        void shouldReturnStreakResponse() {
            Streak streak = new Streak();
            streak.setGoal(testGoal);
            streak.setCurrentStreak(5);
            streak.setLongestStreak(10);
            streak.setLastEntryDate(LocalDate.of(2026, 3, 5));

            given(streakRepository.findByGoalId(10L)).willReturn(Optional.of(streak));

            StreakResponse response = streakService.getStreak(10L);

            assertThat(response.goalId()).isEqualTo(10L);
            assertThat(response.currentStreak()).isEqualTo(5);
            assertThat(response.longestStreak()).isEqualTo(10);
        }

        @Test
        @DisplayName("Streak yoksa sıfır değerli response döner")
        void shouldReturnZeroWhenNoStreak() {
            given(streakRepository.findByGoalId(99L)).willReturn(Optional.empty());

            StreakResponse response = streakService.getStreak(99L);

            assertThat(response.goalId()).isEqualTo(99L);
            assertThat(response.currentStreak()).isZero();
            assertThat(response.longestStreak()).isZero();
            assertThat(response.lastEntryDate()).isNull();
        }
    }

    @Nested
    @DisplayName("getTotalStreakDays")
    class GetTotalStreakDaysTests {

        @Test
        @DisplayName("Aktif hedeflerin streak toplamı döner")
        void shouldReturnSumOfActiveStreaks() {
            Streak s1 = new Streak();
            s1.setGoal(testGoal);
            s1.setCurrentStreak(5);

            Goal goal2 = new Goal();
            goal2.setId(20L);
            goal2.setUser(testUser);
            goal2.setStatus(GoalStatus.ACTIVE);
            Streak s2 = new Streak();
            s2.setGoal(goal2);
            s2.setCurrentStreak(3);

            given(streakRepository.findByGoal_User_Id(1L)).willReturn(List.of(s1, s2));

            int total = streakService.getTotalStreakDays(1L);

            assertThat(total).isEqualTo(8);
        }
    }
}

