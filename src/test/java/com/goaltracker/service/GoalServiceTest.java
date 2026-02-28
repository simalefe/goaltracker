package com.goaltracker.service;

import com.goaltracker.dto.CreateGoalRequest;
import com.goaltracker.dto.GoalResponse;
import com.goaltracker.dto.GoalSummaryResponse;
import com.goaltracker.dto.UpdateGoalRequest;
import com.goaltracker.exception.GoalAccessDeniedException;
import com.goaltracker.exception.GoalLimitExceededException;
import com.goaltracker.exception.GoalNotFoundException;
import com.goaltracker.exception.InvalidStatusTransitionException;
import com.goaltracker.model.Goal;
import com.goaltracker.model.User;
import com.goaltracker.model.enums.*;
import com.goaltracker.repository.GoalRepository;
import com.goaltracker.service.impl.GoalServiceImpl;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

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
class GoalServiceTest {

    @Mock
    private GoalRepository goalRepository;

    @Mock
    private GoalCalculator goalCalculator;

    @Mock
    private StreakService streakService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private GoalServiceImpl goalService;

    private User testUser;
    private Goal testGoal;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@test.com");

        testGoal = new Goal();
        testGoal.setId(10L);
        testGoal.setUser(testUser);
        testGoal.setTitle("Test Hedef");
        testGoal.setDescription("Test açıklama");
        testGoal.setUnit("sayfa");
        testGoal.setGoalType(GoalType.CUMULATIVE);
        testGoal.setFrequency(GoalFrequency.DAILY);
        testGoal.setTargetValue(new BigDecimal("300.00"));
        testGoal.setStartDate(LocalDate.of(2026, 3, 1));
        testGoal.setEndDate(LocalDate.of(2026, 3, 31));
        testGoal.setCategory(GoalCategory.EDUCATION);
        testGoal.setColor("#3B82F6");
        testGoal.setStatus(GoalStatus.ACTIVE);
        testGoal.setVersion(0L);
    }

    @Nested
    @DisplayName("createGoal")
    class CreateGoalTests {

        @Test
        @DisplayName("Başarılı hedef oluşturma")
        void shouldCreateGoalSuccessfully() {
            CreateGoalRequest req = new CreateGoalRequest();
            req.setTitle("Yeni Hedef");
            req.setUnit("km");
            req.setGoalType(GoalType.DAILY);
            req.setTargetValue(new BigDecimal("10.00"));
            req.setStartDate(LocalDate.of(2026, 3, 1));
            req.setEndDate(LocalDate.of(2026, 3, 31));

            given(goalRepository.countByUserIdAndStatus(1L, GoalStatus.ACTIVE)).willReturn(0L);
            given(goalRepository.save(any(Goal.class))).willAnswer(inv -> {
                Goal g = inv.getArgument(0);
                g.setId(1L);
                return g;
            });

            GoalResponse result = goalService.createGoal(req, 1L);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getTitle()).isEqualTo("Yeni Hedef");
            verify(goalRepository).save(any(Goal.class));
        }

        @Test
        @DisplayName("Aktif hedef limiti aşıldığında hata")
        void shouldThrowWhenActiveGoalLimitExceeded() {
            CreateGoalRequest req = new CreateGoalRequest();
            req.setTitle("Fazla Hedef");
            req.setUnit("adet");
            req.setGoalType(GoalType.DAILY);
            req.setTargetValue(new BigDecimal("5.00"));
            req.setStartDate(LocalDate.of(2026, 3, 1));
            req.setEndDate(LocalDate.of(2026, 3, 31));

            given(goalRepository.countByUserIdAndStatus(1L, GoalStatus.ACTIVE)).willReturn(50L);

            assertThatThrownBy(() -> goalService.createGoal(req, 1L))
                    .isInstanceOf(GoalLimitExceededException.class);
        }
    }

    @Nested
    @DisplayName("getGoal")
    class GetGoalTests {

        @Test
        @DisplayName("Başarılı hedef getirme")
        void shouldReturnGoal() {
            given(goalRepository.findById(10L)).willReturn(Optional.of(testGoal));

            GoalResponse result = goalService.getGoal(10L, 1L);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(10L);
            assertThat(result.getTitle()).isEqualTo("Test Hedef");
        }

        @Test
        @DisplayName("Var olmayan hedef → GoalNotFoundException")
        void shouldThrowNotFoundForMissingGoal() {
            given(goalRepository.findById(999L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> goalService.getGoal(999L, 1L))
                    .isInstanceOf(GoalNotFoundException.class);
        }

        @Test
        @DisplayName("Başka kullanıcının hedefine erişim → GoalAccessDeniedException")
        void shouldThrowAccessDeniedForOtherUser() {
            given(goalRepository.findById(10L)).willReturn(Optional.of(testGoal));

            assertThatThrownBy(() -> goalService.getGoal(10L, 999L))
                    .isInstanceOf(GoalAccessDeniedException.class);
        }
    }

    @Nested
    @DisplayName("getGoals")
    class GetGoalsTests {

        @Test
        @DisplayName("Filtreli ve sayfalı liste")
        void shouldReturnFilteredPagedGoals() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Goal> page = new PageImpl<>(List.of(testGoal), pageable, 1);
            given(goalRepository.findByFilters(eq(1L), eq(GoalStatus.ACTIVE), isNull(), isNull(), isNull(), any()))
                    .willReturn(page);

            Page<GoalSummaryResponse> result = goalService.getGoals(1L, GoalStatus.ACTIVE, null, null, null, pageable);

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getTitle()).isEqualTo("Test Hedef");
        }
    }

    @Nested
    @DisplayName("updateGoal")
    class UpdateGoalTests {

        @Test
        @DisplayName("Başarılı güncelleme")
        void shouldUpdateGoalSuccessfully() {
            given(goalRepository.findById(10L)).willReturn(Optional.of(testGoal));
            given(goalRepository.save(any(Goal.class))).willReturn(testGoal);

            UpdateGoalRequest req = new UpdateGoalRequest();
            req.setTitle("Güncel Başlık");

            GoalResponse result = goalService.updateGoal(10L, req, 1L);

            assertThat(result).isNotNull();
            verify(goalRepository).save(any(Goal.class));
        }

        @Test
        @DisplayName("Başka kullanıcının hedefini güncelleme → 403")
        void shouldThrowAccessDeniedOnUpdate() {
            given(goalRepository.findById(10L)).willReturn(Optional.of(testGoal));

            UpdateGoalRequest req = new UpdateGoalRequest();
            req.setTitle("Hack");

            assertThatThrownBy(() -> goalService.updateGoal(10L, req, 999L))
                    .isInstanceOf(GoalAccessDeniedException.class);
        }

        @Test
        @DisplayName("Var olmayan hedefi güncelleme → GoalNotFoundException")
        void shouldThrowNotFoundOnUpdate() {
            given(goalRepository.findById(999L)).willReturn(Optional.empty());

            UpdateGoalRequest req = new UpdateGoalRequest();
            req.setTitle("Test");

            assertThatThrownBy(() -> goalService.updateGoal(999L, req, 1L))
                    .isInstanceOf(GoalNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("deleteGoal")
    class DeleteGoalTests {

        @Test
        @DisplayName("Başarılı silme")
        void shouldDeleteGoal() {
            given(goalRepository.findById(10L)).willReturn(Optional.of(testGoal));

            goalService.deleteGoal(10L, 1L);

            verify(goalRepository).delete(testGoal);
        }

        @Test
        @DisplayName("Var olmayan hedefi silme → 404")
        void shouldThrowNotFoundOnDelete() {
            given(goalRepository.findById(999L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> goalService.deleteGoal(999L, 1L))
                    .isInstanceOf(GoalNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("updateStatus")
    class UpdateStatusTests {

        @Test
        @DisplayName("ACTIVE → PAUSED geçişi başarılı")
        void shouldPauseActiveGoal() {
            given(goalRepository.findById(10L)).willReturn(Optional.of(testGoal));
            given(goalRepository.save(any(Goal.class))).willAnswer(inv -> inv.getArgument(0));

            GoalResponse result = goalService.updateStatus(10L, 1L, GoalStatus.PAUSED);

            assertThat(result.getStatus()).isEqualTo(GoalStatus.PAUSED);
        }

        @Test
        @DisplayName("ACTIVE → COMPLETED geçişi başarılı")
        void shouldCompleteActiveGoal() {
            given(goalRepository.findById(10L)).willReturn(Optional.of(testGoal));
            given(goalRepository.save(any(Goal.class))).willAnswer(inv -> inv.getArgument(0));

            GoalResponse result = goalService.updateStatus(10L, 1L, GoalStatus.COMPLETED);

            assertThat(result.getStatus()).isEqualTo(GoalStatus.COMPLETED);
        }

        @Test
        @DisplayName("COMPLETED → ACTIVE geçişi geçersiz → InvalidStatusTransitionException")
        void shouldThrowForInvalidTransitionCompletedToActive() {
            testGoal.setStatus(GoalStatus.COMPLETED);
            given(goalRepository.findById(10L)).willReturn(Optional.of(testGoal));

            assertThatThrownBy(() -> goalService.updateStatus(10L, 1L, GoalStatus.ACTIVE))
                    .isInstanceOf(InvalidStatusTransitionException.class)
                    .hasMessageContaining("COMPLETED → ACTIVE");
        }

        @Test
        @DisplayName("ARCHIVED → herhangi durum geçersiz → InvalidStatusTransitionException")
        void shouldThrowForArchivedTransition() {
            testGoal.setStatus(GoalStatus.ARCHIVED);
            given(goalRepository.findById(10L)).willReturn(Optional.of(testGoal));

            assertThatThrownBy(() -> goalService.updateStatus(10L, 1L, GoalStatus.ACTIVE))
                    .isInstanceOf(InvalidStatusTransitionException.class);
        }

        @Test
        @DisplayName("PAUSED → ACTIVE (resume) geçişi başarılı")
        void shouldResumeFromPaused() {
            testGoal.setStatus(GoalStatus.PAUSED);
            given(goalRepository.findById(10L)).willReturn(Optional.of(testGoal));
            given(goalRepository.save(any(Goal.class))).willAnswer(inv -> inv.getArgument(0));

            GoalResponse result = goalService.updateStatus(10L, 1L, GoalStatus.ACTIVE);

            assertThat(result.getStatus()).isEqualTo(GoalStatus.ACTIVE);
        }

        @Test
        @DisplayName("Başka kullanıcı durum değiştirme → GoalAccessDeniedException")
        void shouldThrowAccessDeniedOnStatusUpdate() {
            given(goalRepository.findById(10L)).willReturn(Optional.of(testGoal));

            assertThatThrownBy(() -> goalService.updateStatus(10L, 999L, GoalStatus.PAUSED))
                    .isInstanceOf(GoalAccessDeniedException.class);
        }

        @Test
        @DisplayName("ACTIVE → ARCHIVED geçişi başarılı")
        void shouldArchiveActiveGoal() {
            given(goalRepository.findById(10L)).willReturn(Optional.of(testGoal));
            given(goalRepository.save(any(Goal.class))).willAnswer(inv -> inv.getArgument(0));

            GoalResponse result = goalService.updateStatus(10L, 1L, GoalStatus.ARCHIVED);

            assertThat(result.getStatus()).isEqualTo(GoalStatus.ARCHIVED);
        }

        @Test
        @DisplayName("Aynı duruma geçiş → InvalidStatusTransitionException")
        void shouldThrowForSameStatusTransition() {
            given(goalRepository.findById(10L)).willReturn(Optional.of(testGoal));

            assertThatThrownBy(() -> goalService.updateStatus(10L, 1L, GoalStatus.ACTIVE))
                    .isInstanceOf(InvalidStatusTransitionException.class);
        }
    }
}

