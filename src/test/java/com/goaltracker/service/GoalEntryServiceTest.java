package com.goaltracker.service;

import com.goaltracker.dto.*;
import com.goaltracker.exception.*;
import com.goaltracker.model.Goal;
import com.goaltracker.model.GoalEntry;
import com.goaltracker.model.User;
import com.goaltracker.model.enums.GoalFrequency;
import com.goaltracker.model.enums.GoalStatus;
import com.goaltracker.model.enums.GoalType;
import com.goaltracker.model.event.GoalEntryCreatedEvent;
import com.goaltracker.model.event.GoalEntryDeletedEvent;
import com.goaltracker.repository.GoalEntryRepository;
import com.goaltracker.repository.GoalRepository;
import com.goaltracker.service.impl.GoalEntryServiceImpl;
import com.goaltracker.util.GoalCalculator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GoalEntryServiceTest {

    @Mock
    private GoalEntryRepository goalEntryRepository;

    @Mock
    private GoalRepository goalRepository;

    @Mock
    private GoalCalculator goalCalculator;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private StreakService streakService;

    @InjectMocks
    private GoalEntryServiceImpl goalEntryService;

    private User testUser;
    private Goal testGoal;
    private GoalEntry testEntry;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);

        testGoal = new Goal();
        testGoal.setId(10L);
        testGoal.setUser(testUser);
        testGoal.setTitle("Test Hedef");
        testGoal.setUnit("sayfa");
        testGoal.setGoalType(GoalType.CUMULATIVE);
        testGoal.setFrequency(GoalFrequency.DAILY);
        testGoal.setTargetValue(new BigDecimal("300.00"));
        testGoal.setStartDate(LocalDate.of(2026, 3, 1));
        testGoal.setEndDate(LocalDate.of(2026, 3, 31));
        testGoal.setStatus(GoalStatus.ACTIVE);

        testEntry = new GoalEntry();
        testEntry.setId(100L);
        testEntry.setGoal(testGoal);
        testEntry.setEntryDate(LocalDate.of(2026, 3, 5));
        testEntry.setActualValue(new BigDecimal("25.00"));
        testEntry.setNote("Test note");
    }

    @Nested
    @DisplayName("createEntry")
    class CreateEntryTests {

        @Test
        @DisplayName("Başarılı entry oluşturma → 201")
        void createEntry_success() {
            CreateEntryRequest request = new CreateEntryRequest(
                    LocalDate.of(2026, 3, 5), new BigDecimal("25.00"), "Test note");

            given(goalRepository.findById(10L)).willReturn(Optional.of(testGoal));
            given(goalEntryRepository.existsByGoalIdAndEntryDate(10L, request.entryDate())).willReturn(false);
            given(goalEntryRepository.save(any(GoalEntry.class))).willReturn(testEntry);

            GoalEntryResponse result = goalEntryService.createEntry(10L, 1L, request);

            assertThat(result).isNotNull();
            assertThat(result.goalId()).isEqualTo(10L);
            assertThat(result.actualValue()).isEqualByComparingTo(new BigDecimal("25.00"));
            verify(eventPublisher).publishEvent(any(GoalEntryCreatedEvent.class));
        }

        @Test
        @DisplayName("Aynı gün aynı hedefe 2. entry → DuplicateEntryException (409)")
        void createEntry_duplicate() {
            CreateEntryRequest request = new CreateEntryRequest(
                    LocalDate.of(2026, 3, 5), new BigDecimal("25.00"), null);

            given(goalRepository.findById(10L)).willReturn(Optional.of(testGoal));
            given(goalEntryRepository.existsByGoalIdAndEntryDate(10L, request.entryDate())).willReturn(true);

            assertThatThrownBy(() -> goalEntryService.createEntry(10L, 1L, request))
                    .isInstanceOf(DuplicateEntryException.class);
        }

        @Test
        @DisplayName("Tarih hedef aralığı dışında → EntryOutOfRangeException (400)")
        void createEntry_outOfRange() {
            CreateEntryRequest request = new CreateEntryRequest(
                    LocalDate.of(2026, 4, 5), new BigDecimal("25.00"), null);

            given(goalRepository.findById(10L)).willReturn(Optional.of(testGoal));

            assertThatThrownBy(() -> goalEntryService.createEntry(10L, 1L, request))
                    .isInstanceOf(EntryOutOfRangeException.class);
        }

        @Test
        @DisplayName("ARCHIVED hedefe entry → GoalNotActiveException (400)")
        void createEntry_archivedGoal() {
            testGoal.setStatus(GoalStatus.ARCHIVED);
            CreateEntryRequest request = new CreateEntryRequest(
                    LocalDate.of(2026, 3, 5), new BigDecimal("25.00"), null);

            given(goalRepository.findById(10L)).willReturn(Optional.of(testGoal));

            assertThatThrownBy(() -> goalEntryService.createEntry(10L, 1L, request))
                    .isInstanceOf(GoalNotActiveException.class)
                    .hasMessageContaining("ARCHIVED");
        }

        @Test
        @DisplayName("COMPLETED hedefe entry → GoalNotActiveException (400)")
        void createEntry_completedGoal() {
            testGoal.setStatus(GoalStatus.COMPLETED);
            CreateEntryRequest request = new CreateEntryRequest(
                    LocalDate.of(2026, 3, 5), new BigDecimal("25.00"), null);

            given(goalRepository.findById(10L)).willReturn(Optional.of(testGoal));

            assertThatThrownBy(() -> goalEntryService.createEntry(10L, 1L, request))
                    .isInstanceOf(GoalNotActiveException.class)
                    .hasMessageContaining("COMPLETED");
        }

        @Test
        @DisplayName("PAUSED hedefe entry → GoalNotActiveException (400)")
        void createEntry_pausedGoal() {
            testGoal.setStatus(GoalStatus.PAUSED);
            CreateEntryRequest request = new CreateEntryRequest(
                    LocalDate.of(2026, 3, 5), new BigDecimal("25.00"), null);

            given(goalRepository.findById(10L)).willReturn(Optional.of(testGoal));

            assertThatThrownBy(() -> goalEntryService.createEntry(10L, 1L, request))
                    .isInstanceOf(GoalNotActiveException.class)
                    .hasMessageContaining("PAUSED");
        }

        @Test
        @DisplayName("Başka kullanıcının hedefine entry → GoalAccessDeniedException (403)")
        void createEntry_accessDenied() {
            CreateEntryRequest request = new CreateEntryRequest(
                    LocalDate.of(2026, 3, 5), new BigDecimal("25.00"), null);

            given(goalRepository.findById(10L)).willReturn(Optional.of(testGoal));

            assertThatThrownBy(() -> goalEntryService.createEntry(10L, 999L, request))
                    .isInstanceOf(GoalAccessDeniedException.class);
        }

        @Test
        @DisplayName("Entry oluşturma sonrası GoalEntryCreatedEvent publish edilir")
        void createEntry_publishesEvent() {
            CreateEntryRequest request = new CreateEntryRequest(
                    LocalDate.of(2026, 3, 5), new BigDecimal("25.00"), "Test");

            given(goalRepository.findById(10L)).willReturn(Optional.of(testGoal));
            given(goalEntryRepository.existsByGoalIdAndEntryDate(10L, request.entryDate())).willReturn(false);
            given(goalEntryRepository.save(any(GoalEntry.class))).willReturn(testEntry);

            goalEntryService.createEntry(10L, 1L, request);

            ArgumentCaptor<GoalEntryCreatedEvent> captor = ArgumentCaptor.forClass(GoalEntryCreatedEvent.class);
            verify(eventPublisher).publishEvent(captor.capture());
            assertThat(captor.getValue().getGoalId()).isEqualTo(10L);
        }
    }

    @Nested
    @DisplayName("deleteEntry")
    class DeleteEntryTests {

        @Test
        @DisplayName("Entry silme sonrası GoalEntryDeletedEvent publish edilir")
        void deleteEntry_publishesEvent() {
            given(goalEntryRepository.findById(100L)).willReturn(Optional.of(testEntry));

            goalEntryService.deleteEntry(100L, 1L);

            verify(goalEntryRepository).delete(testEntry);
            ArgumentCaptor<GoalEntryDeletedEvent> captor = ArgumentCaptor.forClass(GoalEntryDeletedEvent.class);
            verify(eventPublisher).publishEvent(captor.capture());
            assertThat(captor.getValue().getGoalId()).isEqualTo(10L);
        }

        @Test
        @DisplayName("Başka kullanıcının entry'si → GoalAccessDeniedException")
        void deleteEntry_accessDenied() {
            given(goalEntryRepository.findById(100L)).willReturn(Optional.of(testEntry));

            assertThatThrownBy(() -> goalEntryService.deleteEntry(100L, 999L))
                    .isInstanceOf(GoalAccessDeniedException.class);
        }

        @Test
        @DisplayName("Entry bulunamadı → GoalEntryNotFoundException")
        void deleteEntry_notFound() {
            given(goalEntryRepository.findById(999L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> goalEntryService.deleteEntry(999L, 1L))
                    .isInstanceOf(GoalEntryNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("updateEntry")
    class UpdateEntryTests {

        @Test
        @DisplayName("Entry güncelleme çalışıyor")
        void updateEntry_success() {
            UpdateEntryRequest request = new UpdateEntryRequest(new BigDecimal("30.00"), "Güncellendi");

            given(goalEntryRepository.findById(100L)).willReturn(Optional.of(testEntry));
            given(goalEntryRepository.save(any(GoalEntry.class))).willReturn(testEntry);

            GoalEntryResponse result = goalEntryService.updateEntry(100L, 1L, request);

            assertThat(result).isNotNull();
            verify(goalEntryRepository).save(testEntry);
        }
    }

    @Nested
    @DisplayName("getEntries")
    class GetEntriesTests {

        @Test
        @DisplayName("Entry listesi döner")
        void getEntries_success() {
            given(goalRepository.findById(10L)).willReturn(Optional.of(testGoal));
            given(goalEntryRepository.findByGoalIdOrderByEntryDateDesc(10L)).willReturn(List.of(testEntry));

            List<GoalEntryResponse> result = goalEntryService.getEntries(10L, 1L);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).goalId()).isEqualTo(10L);
        }
    }
}

