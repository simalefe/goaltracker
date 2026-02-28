package com.goaltracker.service;

import com.goaltracker.dto.GoalStatsResponse;
import com.goaltracker.exception.GoalAccessDeniedException;
import com.goaltracker.exception.GoalNotFoundException;
import com.goaltracker.model.Goal;
import com.goaltracker.model.GoalEntry;
import com.goaltracker.model.User;
import com.goaltracker.model.enums.*;
import com.goaltracker.repository.GoalEntryRepository;
import com.goaltracker.repository.GoalRepository;
import com.goaltracker.repository.UserRepository;
import com.goaltracker.service.impl.ExportServiceImpl;
import com.goaltracker.util.GoalCalculator;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class ExportServiceTest {

    @Mock
    private GoalRepository goalRepository;

    @Mock
    private GoalEntryRepository goalEntryRepository;

    @Mock
    private GoalCalculator goalCalculator;

    @Mock
    private StreakService streakService;

    @Mock
    private GoalEntryService goalEntryService;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ExportServiceImpl exportService;

    private User testUser;
    private Goal testGoal;
    private List<GoalEntry> testEntries;
    private GoalStatsResponse testStats;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@test.com");
        testUser.setUsername("testuser");
        testUser.setDisplayName("Test User");

        testGoal = new Goal();
        testGoal.setId(10L);
        testGoal.setUser(testUser);
        testGoal.setTitle("Kitap Okuma");
        testGoal.setUnit("sayfa");
        testGoal.setGoalType(GoalType.CUMULATIVE);
        testGoal.setFrequency(GoalFrequency.DAILY);
        testGoal.setTargetValue(new BigDecimal("300.00"));
        testGoal.setStartDate(LocalDate.of(2026, 2, 1));
        testGoal.setEndDate(LocalDate.of(2026, 2, 28));
        testGoal.setCategory(GoalCategory.EDUCATION);
        testGoal.setStatus(GoalStatus.ACTIVE);

        // Create test entries
        testEntries = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            GoalEntry entry = new GoalEntry();
            entry.setId((long) i);
            entry.setGoal(testGoal);
            entry.setEntryDate(LocalDate.of(2026, 2, i));
            entry.setActualValue(new BigDecimal("20.00"));
            entry.setNote(i == 3 ? "Virgül, içeren not" : "Not " + i);
            testEntries.add(entry);
        }

        testStats = new GoalStatsResponse(
                new BigDecimal("100.00"),  // currentProgress
                new BigDecimal("300.00"),  // targetValue
                new BigDecimal("33.33"),   // completionPct
                new BigDecimal("17.86"),   // expectedPct
                new BigDecimal("46.43"),   // gap
                "AHEAD",                    // trackingStatus
                new BigDecimal("9.52"),    // requiredRate
                "sayfa",                    // unit
                21,                         // daysLeft
                28,                         // totalDays
                7,                          // daysSinceStart
                5,                          // entryCount
                5,                          // currentStreak
                5                           // longestStreak
        );
    }

    // ─── Excel Tests ──────────────────────────────────────────────

    @Nested
    @DisplayName("Excel Export")
    class ExcelExportTests {

        @Test
        @DisplayName("Excel oluşturuluyor, 3 sayfa mevcut")
        void shouldExportExcelWithThreeSheets() {
            given(goalRepository.findById(10L)).willReturn(Optional.of(testGoal));
            given(goalEntryRepository.findByGoalIdOrderByEntryDateDesc(10L)).willReturn(testEntries);
            given(goalEntryService.getStats(10L, 1L)).willReturn(testStats);

            byte[] data = exportService.exportGoalToExcel(10L, 1L);

            assertThat(data).isNotNull();
            assertThat(data.length).isGreaterThan(0);
            // XLSX magic bytes: PK (ZIP)
            assertThat(data[0]).isEqualTo((byte) 0x50);
            assertThat(data[1]).isEqualTo((byte) 0x4B);
        }

        @Test
        @DisplayName("Entry'siz hedef export → crash yok")
        void shouldExportExcelWithNoEntries() {
            given(goalRepository.findById(10L)).willReturn(Optional.of(testGoal));
            given(goalEntryRepository.findByGoalIdOrderByEntryDateDesc(10L)).willReturn(List.of());
            GoalStatsResponse emptyStats = new GoalStatsResponse(
                    BigDecimal.ZERO, new BigDecimal("300.00"), BigDecimal.ZERO,
                    BigDecimal.ZERO, BigDecimal.ZERO, "ON_TRACK",
                    BigDecimal.ZERO, "sayfa", 21, 28, 7, 0, 0, 0);
            given(goalEntryService.getStats(10L, 1L)).willReturn(emptyStats);

            byte[] data = exportService.exportGoalToExcel(10L, 1L);

            assertThat(data).isNotNull();
            assertThat(data.length).isGreaterThan(0);
        }

        @Test
        @DisplayName("Başka kullanıcının hedefini export → hata")
        void shouldThrowWhenAccessDenied() {
            given(goalRepository.findById(10L)).willReturn(Optional.of(testGoal));

            assertThatThrownBy(() -> exportService.exportGoalToExcel(10L, 999L))
                    .isInstanceOf(GoalAccessDeniedException.class);
        }

        @Test
        @DisplayName("Var olmayan hedef → hata")
        void shouldThrowWhenGoalNotFound() {
            given(goalRepository.findById(999L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> exportService.exportGoalToExcel(999L, 1L))
                    .isInstanceOf(GoalNotFoundException.class);
        }
    }

    // ─── PDF Tests ────────────────────────────────────────────────

    @Nested
    @DisplayName("PDF Export")
    class PdfExportTests {

        @Test
        @DisplayName("PDF oluşturuluyor")
        void shouldExportPdf() {
            given(goalRepository.findById(10L)).willReturn(Optional.of(testGoal));
            given(goalEntryRepository.findByGoalIdOrderByEntryDateDesc(10L)).willReturn(testEntries);
            given(goalEntryService.getStats(10L, 1L)).willReturn(testStats);

            byte[] data = exportService.exportGoalToPdf(10L, 1L);

            assertThat(data).isNotNull();
            assertThat(data.length).isGreaterThan(0);
            // PDF magic bytes
            assertThat(new String(data, 0, 5)).isEqualTo("%PDF-");
        }

        @Test
        @DisplayName("Entry'siz hedef PDF export → crash yok")
        void shouldExportPdfWithNoEntries() {
            given(goalRepository.findById(10L)).willReturn(Optional.of(testGoal));
            given(goalEntryRepository.findByGoalIdOrderByEntryDateDesc(10L)).willReturn(List.of());
            GoalStatsResponse emptyStats = new GoalStatsResponse(
                    BigDecimal.ZERO, new BigDecimal("300.00"), BigDecimal.ZERO,
                    BigDecimal.ZERO, BigDecimal.ZERO, "ON_TRACK",
                    BigDecimal.ZERO, "sayfa", 21, 28, 7, 0, 0, 0);
            given(goalEntryService.getStats(10L, 1L)).willReturn(emptyStats);

            byte[] data = exportService.exportGoalToPdf(10L, 1L);

            assertThat(data).isNotNull();
            assertThat(new String(data, 0, 5)).isEqualTo("%PDF-");
        }

        @Test
        @DisplayName("Başka kullanıcının hedefini PDF export → 403")
        void shouldThrowWhenPdfAccessDenied() {
            given(goalRepository.findById(10L)).willReturn(Optional.of(testGoal));

            assertThatThrownBy(() -> exportService.exportGoalToPdf(10L, 999L))
                    .isInstanceOf(GoalAccessDeniedException.class);
        }
    }

    // ─── CSV Tests ────────────────────────────────────────────────

    @Nested
    @DisplayName("CSV Export")
    class CsvExportTests {

        @Test
        @DisplayName("CSV oluşturuluyor, UTF-8 BOM mevcut")
        void shouldExportCsvWithBom() {
            given(goalRepository.findById(10L)).willReturn(Optional.of(testGoal));
            given(goalEntryRepository.findByGoalIdOrderByEntryDateDesc(10L)).willReturn(testEntries);

            byte[] data = exportService.exportGoalToCsv(10L, 1L);

            assertThat(data).isNotNull();
            assertThat(data.length).isGreaterThan(0);
            // UTF-8 BOM check
            assertThat(data[0]).isEqualTo((byte) 0xEF);
            assertThat(data[1]).isEqualTo((byte) 0xBB);
            assertThat(data[2]).isEqualTo((byte) 0xBF);

            // Check header
            String csv = new String(data, 3, data.length - 3, java.nio.charset.StandardCharsets.UTF_8);
            assertThat(csv).startsWith("Tarih,");
            assertThat(csv).contains("Değer");
            assertThat(csv).contains("Birim");
            assertThat(csv).contains("Not");
        }

        @Test
        @DisplayName("Virgül içeren not alanları tırnak içinde")
        void shouldEscapeCommasInCsv() {
            given(goalRepository.findById(10L)).willReturn(Optional.of(testGoal));
            given(goalEntryRepository.findByGoalIdOrderByEntryDateDesc(10L)).willReturn(testEntries);

            byte[] data = exportService.exportGoalToCsv(10L, 1L);

            String csv = new String(data, 3, data.length - 3, java.nio.charset.StandardCharsets.UTF_8);
            // Entry with index 2 (3rd) has comma in note
            assertThat(csv).contains("\"Virgül, içeren not\"");
        }

        @Test
        @DisplayName("Entry'siz hedef CSV export → yalnızca header")
        void shouldExportCsvWithNoEntries() {
            given(goalRepository.findById(10L)).willReturn(Optional.of(testGoal));
            given(goalEntryRepository.findByGoalIdOrderByEntryDateDesc(10L)).willReturn(List.of());

            byte[] data = exportService.exportGoalToCsv(10L, 1L);

            String csv = new String(data, 3, data.length - 3, java.nio.charset.StandardCharsets.UTF_8);
            String[] lines = csv.split("\n");
            assertThat(lines).hasSize(1); // only header
        }

        @Test
        @DisplayName("Başka kullanıcının hedefini CSV export → 403")
        void shouldThrowWhenCsvAccessDenied() {
            given(goalRepository.findById(10L)).willReturn(Optional.of(testGoal));

            assertThatThrownBy(() -> exportService.exportGoalToCsv(10L, 999L))
                    .isInstanceOf(GoalAccessDeniedException.class);
        }
    }

    // ─── Monthly Report Tests ─────────────────────────────────────

    @Nested
    @DisplayName("Monthly Report")
    class MonthlyReportTests {

        @Test
        @DisplayName("Aylık rapor PDF oluşturuluyor")
        void shouldGenerateMonthlyReport() {
            given(userRepository.findById(1L)).willReturn(Optional.of(testUser));
            given(goalRepository.findByUserIdAndStatus(1L, GoalStatus.ACTIVE))
                    .willReturn(List.of(testGoal));
            given(goalRepository.findByUserIdAndStatusOrderByCreatedAtDesc(1L, GoalStatus.COMPLETED))
                    .willReturn(List.of());
            given(goalCalculator.calculateCompletionPct(testGoal)).willReturn(new BigDecimal("33.33"));
            given(goalEntryRepository.countByGoalId(10L)).willReturn(5L);
            given(streakService.getStreakForGoal(10L)).willReturn(5);

            byte[] data = exportService.generateMonthlyReport(1L, 2026, 2);

            assertThat(data).isNotNull();
            assertThat(new String(data, 0, 5)).isEqualTo("%PDF-");
        }

        @Test
        @DisplayName("Hedefsiz ay → boş rapor, crash yok")
        void shouldGenerateEmptyMonthlyReport() {
            given(userRepository.findById(1L)).willReturn(Optional.of(testUser));
            given(goalRepository.findByUserIdAndStatus(1L, GoalStatus.ACTIVE))
                    .willReturn(List.of());
            given(goalRepository.findByUserIdAndStatusOrderByCreatedAtDesc(1L, GoalStatus.COMPLETED))
                    .willReturn(List.of());

            byte[] data = exportService.generateMonthlyReport(1L, 2026, 2);

            assertThat(data).isNotNull();
            assertThat(new String(data, 0, 5)).isEqualTo("%PDF-");
        }
    }

    // ─── Filename Normalization Tests ─────────────────────────────

    @Nested
    @DisplayName("Filename Normalization")
    class FilenameTests {

        @Test
        @DisplayName("Türkçe karakterler normalize ediliyor")
        void shouldNormalizeTurkishCharacters() {
            assertThat(exportService.normalizeFilename("Kitap Okuma!")).isEqualTo("kitap-okuma");
        }

        @Test
        @DisplayName("Boşluklar tire ile değiştiriliyor")
        void shouldReplaceSpacesWithDashes() {
            assertThat(exportService.normalizeFilename("Hedef Adı")).isEqualTo("hedef-adi");
        }

        @Test
        @DisplayName("Null veya boş → 'export'")
        void shouldReturnExportForNullOrBlank() {
            assertThat(exportService.normalizeFilename(null)).isEqualTo("export");
            assertThat(exportService.normalizeFilename("")).isEqualTo("export");
            assertThat(exportService.normalizeFilename("   ")).isEqualTo("export");
        }

        @Test
        @DisplayName("Çoklu tire tek tireye dönüyor")
        void shouldCollapseDashes() {
            assertThat(exportService.normalizeFilename("A -- B")).isEqualTo("a-b");
        }
    }

    // ─── Large Entry Set ──────────────────────────────────────────

    @Test
    @DisplayName("500+ entry → bellek hatası yok (streaming)")
    void shouldHandleLargeEntrySetWithoutMemoryIssue() {
        List<GoalEntry> largeEntries = new ArrayList<>();
        for (int i = 1; i <= 600; i++) {
            GoalEntry entry = new GoalEntry();
            entry.setId((long) i);
            entry.setGoal(testGoal);
            entry.setEntryDate(LocalDate.of(2026, 1, 1).plusDays(i % 28));
            entry.setActualValue(new BigDecimal("10.00"));
            entry.setNote("Entry " + i);
            largeEntries.add(entry);
        }

        given(goalRepository.findById(10L)).willReturn(Optional.of(testGoal));
        given(goalEntryRepository.findByGoalIdOrderByEntryDateDesc(10L)).willReturn(largeEntries);
        given(goalEntryService.getStats(10L, 1L)).willReturn(testStats);

        byte[] excelData = exportService.exportGoalToExcel(10L, 1L);
        assertThat(excelData).isNotNull();
        assertThat(excelData.length).isGreaterThan(0);

        byte[] pdfData = exportService.exportGoalToPdf(10L, 1L);
        assertThat(pdfData).isNotNull();
        assertThat(pdfData.length).isGreaterThan(0);

        byte[] csvData = exportService.exportGoalToCsv(10L, 1L);
        assertThat(csvData).isNotNull();
        assertThat(csvData.length).isGreaterThan(0);
    }
}

