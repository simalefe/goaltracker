package com.goaltracker.util;

import com.goaltracker.dto.ChartDataPointResponse;
import com.goaltracker.model.Goal;
import com.goaltracker.model.GoalEntry;
import com.goaltracker.model.User;
import com.goaltracker.model.enums.GoalFrequency;
import com.goaltracker.model.enums.GoalStatus;
import com.goaltracker.model.enums.GoalType;
import com.goaltracker.repository.GoalEntryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class GoalCalculatorTest {

    @Mock
    private GoalEntryRepository goalEntryRepository;

    @InjectMocks
    private GoalCalculator goalCalculator;

    private Goal testGoal;

    @BeforeEach
    void setUp() {
        User user = new User();
        user.setId(1L);

        testGoal = new Goal();
        testGoal.setId(10L);
        testGoal.setUser(user);
        testGoal.setGoalType(GoalType.CUMULATIVE);
        testGoal.setFrequency(GoalFrequency.DAILY);
        testGoal.setTargetValue(new BigDecimal("300.00"));
        testGoal.setStartDate(LocalDate.of(2026, 3, 1));
        testGoal.setEndDate(LocalDate.of(2026, 3, 31));
        testGoal.setUnit("sayfa");
        testGoal.setStatus(GoalStatus.ACTIVE);
    }

    @Nested
    @DisplayName("calculateTotalDays")
    class TotalDaysTests {

        @Test
        @DisplayName("31 günlük hedef → 31")
        void totalDays_normalRange() {
            long result = goalCalculator.calculateTotalDays(testGoal);
            assertThat(result).isEqualTo(31);
        }

        @Test
        @DisplayName("startDate == endDate → 1")
        void totalDays_sameDay() {
            testGoal.setStartDate(LocalDate.of(2026, 3, 1));
            testGoal.setEndDate(LocalDate.of(2026, 3, 1));
            assertThat(goalCalculator.calculateTotalDays(testGoal)).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("calculateTotalPeriods")
    class TotalPeriodsTests {

        @Test
        @DisplayName("WEEKLY, 30 gün → 5 hafta (ceiling)")
        void totalPeriods_weekly_30days() {
            testGoal.setStartDate(LocalDate.of(2026, 3, 1));
            testGoal.setEndDate(LocalDate.of(2026, 3, 30));
            testGoal.setFrequency(GoalFrequency.WEEKLY);
            long result = goalCalculator.calculateTotalPeriods(testGoal);
            // 30 days → (30 + 6) / 7 = 5 (ceiling)
            assertThat(result).isEqualTo(5);
        }

        @Test
        @DisplayName("DAILY, 31 gün → 31")
        void totalPeriods_daily() {
            assertThat(goalCalculator.calculateTotalPeriods(testGoal)).isEqualTo(31);
        }

        @Test
        @DisplayName("MONTHLY, 3 ay → 3")
        void totalPeriods_monthly() {
            testGoal.setStartDate(LocalDate.of(2026, 1, 1));
            testGoal.setEndDate(LocalDate.of(2026, 3, 31));
            testGoal.setFrequency(GoalFrequency.MONTHLY);
            assertThat(goalCalculator.calculateTotalPeriods(testGoal)).isEqualTo(3);
        }
    }

    @Nested
    @DisplayName("calculateElapsedPeriods")
    class ElapsedPeriodsTests {

        @Test
        @DisplayName("WEEKLY, 10 gün geçmiş → 2 hafta (ceiling)")
        void elapsedPeriods_weekly() {
            testGoal.setFrequency(GoalFrequency.WEEKLY);
            LocalDate asOfDate = testGoal.getStartDate().plusDays(9); // 10 days elapsed
            long result = goalCalculator.calculateElapsedPeriods(testGoal, asOfDate);
            // (10 + 6) / 7 = 2
            assertThat(result).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("calculateRequiredRate")
    class RequiredRateTests {

        @Test
        @DisplayName("daysLeft = 0 → ZERO döner")
        void requiredRate_zeroDaysLeft() {
            testGoal.setEndDate(LocalDate.now());
            BigDecimal result = goalCalculator.calculateRequiredRate(testGoal);
            assertThat(result).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("daysLeft = 5, targetValue = 100, currentProgress = 50 → 10.00")
        void requiredRate_normalCase() {
            testGoal.setTargetValue(new BigDecimal("100.00"));
            testGoal.setEndDate(LocalDate.now().plusDays(5));
            given(goalEntryRepository.sumActualValueByGoalId(10L)).willReturn(new BigDecimal("50.00"));
            BigDecimal result = goalCalculator.calculateRequiredRate(testGoal);
            assertThat(result).isEqualByComparingTo(new BigDecimal("10.00"));
        }

        @Test
        @DisplayName("currentProgress >= targetValue → ZERO (zaten tamamlanmış)")
        void requiredRate_alreadyCompleted() {
            testGoal.setEndDate(LocalDate.now().plusDays(10));
            given(goalEntryRepository.sumActualValueByGoalId(10L)).willReturn(new BigDecimal("300.00"));
            BigDecimal result = goalCalculator.calculateRequiredRate(testGoal);
            assertThat(result).isEqualByComparingTo(BigDecimal.ZERO);
        }
    }

    @Nested
    @DisplayName("calculateCompletionPct")
    class CompletionPctTests {

        @Test
        @DisplayName("currentProgress > targetValue → 100.00 (max)")
        void completionPct_maxCap() {
            given(goalEntryRepository.sumActualValueByGoalId(10L)).willReturn(new BigDecimal("500.00"));
            BigDecimal result = goalCalculator.calculateCompletionPct(testGoal);
            assertThat(result).isEqualByComparingTo(new BigDecimal("100.00"));
        }

        @Test
        @DisplayName("targetValue = 0 → 0 (sıfıra bölme koruması)")
        void completionPct_zeroTarget() {
            testGoal.setTargetValue(BigDecimal.ZERO);
            BigDecimal result = goalCalculator.calculateCompletionPct(testGoal);
            assertThat(result).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("normal hesaplama")
        void completionPct_normal() {
            given(goalEntryRepository.sumActualValueByGoalId(10L)).willReturn(new BigDecimal("150.00"));
            BigDecimal result = goalCalculator.calculateCompletionPct(testGoal);
            assertThat(result).isEqualByComparingTo(new BigDecimal("50.00"));
        }
    }

    @Nested
    @DisplayName("calculateGap")
    class GapTests {

        @Test
        @DisplayName("İleride → pozitif değer")
        void gap_ahead() {
            testGoal.setStartDate(LocalDate.now().minusDays(5));
            testGoal.setEndDate(LocalDate.now().plusDays(25));
            given(goalEntryRepository.sumActualValueByGoalId(10L)).willReturn(new BigDecimal("200.00"));
            BigDecimal gap = goalCalculator.calculateGap(testGoal);
            assertThat(gap.signum()).isPositive();
        }

        @Test
        @DisplayName("Geride → negatif değer")
        void gap_behind() {
            testGoal.setStartDate(LocalDate.now().minusDays(15));
            testGoal.setEndDate(LocalDate.now().plusDays(15));
            given(goalEntryRepository.sumActualValueByGoalId(10L)).willReturn(new BigDecimal("10.00"));
            BigDecimal gap = goalCalculator.calculateGap(testGoal);
            assertThat(gap.signum()).isNegative();
        }
    }

    @Nested
    @DisplayName("determineTrackingStatus")
    class TrackingStatusTests {

        @Test
        @DisplayName("gap > 0 → AHEAD")
        void trackingStatus_ahead() {
            testGoal.setStartDate(LocalDate.now().minusDays(5));
            testGoal.setEndDate(LocalDate.now().plusDays(25));
            given(goalEntryRepository.sumActualValueByGoalId(10L)).willReturn(new BigDecimal("200.00"));
            assertThat(goalCalculator.determineTrackingStatus(testGoal)).isEqualTo("AHEAD");
        }

        @Test
        @DisplayName("gap < 0 → BEHIND")
        void trackingStatus_behind() {
            testGoal.setStartDate(LocalDate.now().minusDays(15));
            testGoal.setEndDate(LocalDate.now().plusDays(15));
            given(goalEntryRepository.sumActualValueByGoalId(10L)).willReturn(new BigDecimal("10.00"));
            assertThat(goalCalculator.determineTrackingStatus(testGoal)).isEqualTo("BEHIND");
        }
    }

    @Nested
    @DisplayName("buildChartData")
    class ChartDataTests {

        @Test
        @DisplayName("Entry olmayan günlerde dailyActual = null")
        void chartData_nullDailyActual() {
            testGoal.setStartDate(LocalDate.now().minusDays(2));
            testGoal.setEndDate(LocalDate.now().plusDays(5));

            GoalEntry entry1 = new GoalEntry();
            entry1.setEntryDate(LocalDate.now().minusDays(2));
            entry1.setActualValue(new BigDecimal("10.00"));
            entry1.setGoal(testGoal);

            List<ChartDataPointResponse> data = goalCalculator.buildChartData(testGoal, List.of(entry1));
            assertThat(data).isNotEmpty();

            // First day has entry → dailyActual not null
            assertThat(data.get(0).dailyActual()).isNotNull();
            // Second day has no entry → dailyActual null
            assertThat(data.get(1).dailyActual()).isNull();
        }

        @Test
        @DisplayName("Kümülatif actual doğru artıyor")
        void chartData_cumulativeActual() {
            testGoal.setStartDate(LocalDate.now().minusDays(2));
            testGoal.setEndDate(LocalDate.now().plusDays(5));

            GoalEntry entry1 = new GoalEntry();
            entry1.setEntryDate(LocalDate.now().minusDays(2));
            entry1.setActualValue(new BigDecimal("10.00"));
            entry1.setGoal(testGoal);

            GoalEntry entry2 = new GoalEntry();
            entry2.setEntryDate(LocalDate.now());
            entry2.setActualValue(new BigDecimal("20.00"));
            entry2.setGoal(testGoal);

            List<ChartDataPointResponse> data = goalCalculator.buildChartData(testGoal, List.of(entry1, entry2));

            // Day 1: cumulative = 10
            assertThat(data.get(0).actual()).isEqualByComparingTo(new BigDecimal("10.00"));
            // Day 3 (today): cumulative = 30
            assertThat(data.get(2).actual()).isEqualByComparingTo(new BigDecimal("30.00"));
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCaseTests {

        @Test
        @DisplayName("startDate > today → daysSinceStart = 0, plannedProgress = 0")
        void futureGoal() {
            testGoal.setStartDate(LocalDate.now().plusDays(10));
            testGoal.setEndDate(LocalDate.now().plusDays(40));

            BigDecimal planned = goalCalculator.calculatePlannedProgress(testGoal, LocalDate.now());
            assertThat(planned).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(goalCalculator.calculateDaysSinceStart(testGoal)).isEqualTo(0);
        }

        @Test
        @DisplayName("startDate == endDate → totalDays = 1")
        void sameDayGoal() {
            testGoal.setStartDate(LocalDate.of(2026, 5, 1));
            testGoal.setEndDate(LocalDate.of(2026, 5, 1));
            assertThat(goalCalculator.calculateTotalDays(testGoal)).isEqualTo(1);
        }

        @Test
        @DisplayName("daysLeft = 0 (bugün son gün) → 0 döner")
        void daysLeft_today() {
            testGoal.setEndDate(LocalDate.now());
            assertThat(goalCalculator.calculateDaysLeft(testGoal)).isZero();
        }

        @Test
        @DisplayName("endDate null → daysLeft = 0")
        void daysLeft_nullEndDate() {
            testGoal.setEndDate(null);
            assertThat(goalCalculator.calculateDaysLeft(testGoal)).isZero();
        }

        @Test
        @DisplayName("startDate null → daysSinceStart = 0")
        void daysSinceStart_nullStartDate() {
            testGoal.setStartDate(null);
            assertThat(goalCalculator.calculateDaysSinceStart(testGoal)).isZero();
        }

        @Test
        @DisplayName("startDate null → totalDays = 0")
        void totalDays_nullStartDate() {
            testGoal.setStartDate(null);
            assertThat(goalCalculator.calculateTotalDays(testGoal)).isZero();
        }

        @Test
        @DisplayName("endDate null → totalDays = 0")
        void totalDays_nullEndDate() {
            testGoal.setEndDate(null);
            assertThat(goalCalculator.calculateTotalDays(testGoal)).isZero();
        }

        @Test
        @DisplayName("targetValue null → completionPct = 0")
        void completionPct_nullTarget() {
            testGoal.setTargetValue(null);
            assertThat(goalCalculator.calculateCompletionPct(testGoal)).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("Boş entry listesi ile chartData → boş olmayabilir ama actual null")
        void chartData_emptyEntries() {
            testGoal.setStartDate(LocalDate.now().minusDays(2));
            testGoal.setEndDate(LocalDate.now().plusDays(5));

            List<ChartDataPointResponse> data = goalCalculator.buildChartData(testGoal, List.of());
            assertThat(data).isNotEmpty();
            // All actual values should be null (no entries)
            assertThat(data.get(0).actual()).isNull();
        }

        @Test
        @DisplayName("startDate > chartEnd → boş chart data")
        void chartData_startAfterEnd() {
            testGoal.setStartDate(LocalDate.now().plusDays(10));
            testGoal.setEndDate(LocalDate.now().plusDays(20));

            List<ChartDataPointResponse> data = goalCalculator.buildChartData(testGoal, List.of());
            assertThat(data).isEmpty();
        }

        @Test
        @DisplayName("RATE tipi hedef → plannedProgress = targetValue × elapsedPeriods")
        void plannedProgress_rateType() {
            testGoal.setGoalType(GoalType.RATE);
            testGoal.setTargetValue(new BigDecimal("5.00")); // 5 per period
            testGoal.setFrequency(GoalFrequency.DAILY);
            testGoal.setStartDate(LocalDate.of(2026, 3, 1));
            testGoal.setEndDate(LocalDate.of(2026, 3, 31));

            // asOfDate = 3 March → 3 days elapsed → 5 × 3 = 15
            BigDecimal planned = goalCalculator.calculatePlannedProgress(testGoal, LocalDate.of(2026, 3, 3));
            assertThat(planned).isEqualByComparingTo(new BigDecimal("15.00"));
        }

        @Test
        @DisplayName("expectedPct → 0 döner startDate/endDate aynıyken ve totalDays <= 0")
        void expectedPct_zeroDays() {
            testGoal.setStartDate(null);
            testGoal.setEndDate(null);
            BigDecimal result = goalCalculator.calculateExpectedPct(testGoal);
            assertThat(result).isEqualByComparingTo(BigDecimal.ZERO);
        }
    }
}

