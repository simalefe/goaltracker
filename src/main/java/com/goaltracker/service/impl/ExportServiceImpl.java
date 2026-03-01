package com.goaltracker.service.impl;

import com.goaltracker.dto.GoalStatsResponse;
import com.goaltracker.exception.GoalAccessDeniedException;
import com.goaltracker.exception.GoalNotFoundException;
import com.goaltracker.model.Goal;
import com.goaltracker.model.GoalEntry;
import com.goaltracker.model.User;
import com.goaltracker.model.enums.GoalStatus;
import com.goaltracker.repository.GoalEntryRepository;
import com.goaltracker.repository.GoalRepository;
import com.goaltracker.repository.UserRepository;
import com.goaltracker.service.ExportService;
import com.goaltracker.service.GoalEntryService;
import com.goaltracker.service.StreakService;
import com.goaltracker.util.GoalCalculator;
import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.events.Event;
import com.itextpdf.kernel.events.IEventHandler;
import com.itextpdf.kernel.events.PdfDocumentEvent;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfPage;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class ExportServiceImpl implements ExportService {

    private static final Logger log = LoggerFactory.getLogger(ExportServiceImpl.class);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter MONTH_FMT = DateTimeFormatter.ofPattern("yyyy-MM");

    private final GoalRepository goalRepository;
    private final GoalEntryRepository goalEntryRepository;
    private final GoalCalculator goalCalculator;
    private final StreakService streakService;
    private final GoalEntryService goalEntryService;
    private final UserRepository userRepository;

    public ExportServiceImpl(GoalRepository goalRepository,
                             GoalEntryRepository goalEntryRepository,
                             GoalCalculator goalCalculator,
                             StreakService streakService,
                             GoalEntryService goalEntryService,
                             UserRepository userRepository) {
        this.goalRepository = goalRepository;
        this.goalEntryRepository = goalEntryRepository;
        this.goalCalculator = goalCalculator;
        this.streakService = streakService;
        this.goalEntryService = goalEntryService;
        this.userRepository = userRepository;
    }

    // ─── Excel Export ──────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public byte[] exportGoalToExcel(Long goalId, Long userId) {
        Goal goal = getGoalWithOwnershipCheck(goalId, userId);
        List<GoalEntry> entries = goalEntryRepository.findByGoalIdOrderByEntryDateDesc(goalId);
        GoalStatsResponse stats = goalEntryService.getStats(goalId, userId);

        try (SXSSFWorkbook workbook = new SXSSFWorkbook(100);
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            // --- Styles ---
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle greenStyle = createColorStyle(workbook, IndexedColors.GREEN);
            CellStyle redStyle = createColorStyle(workbook, IndexedColors.RED);
            CellStyle titleStyle = createTitleStyle(workbook);

            // --- Sheet 1: Özet ---
            Sheet summarySheet = workbook.createSheet("Özet");
            buildSummarySheet(summarySheet, goal, stats, titleStyle, headerStyle, greenStyle, redStyle);

            // --- Sheet 2: İlerleme Kayıtları ---
            Sheet entriesSheet = workbook.createSheet("İlerleme Kayıtları");
            buildEntriesSheet(entriesSheet, goal, entries, headerStyle);

            // --- Sheet 3: İstatistikler ---
            Sheet statsSheet = workbook.createSheet("İstatistikler");
            buildStatsSheet(statsSheet, goal, entries, stats, headerStyle);

            workbook.write(baos);
            workbook.dispose(); // SXSSFWorkbook: clean up temp files
            return baos.toByteArray();

        } catch (IOException e) {
            log.error("Excel export hatası: goalId={}", goalId, e);
            throw new RuntimeException("Excel oluşturulurken bir hata oluştu.", e);
        }
    }

    private void buildSummarySheet(Sheet sheet, Goal goal, GoalStatsResponse stats,
                                   CellStyle titleStyle, CellStyle headerStyle,
                                   CellStyle greenStyle, CellStyle redStyle) {
        int rowIdx = 0;

        // Row 1: Title
        Row titleRow = sheet.createRow(rowIdx++);
        org.apache.poi.ss.usermodel.Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue(goal.getTitle());
        titleCell.setCellStyle(titleStyle);

        // Row 2: blank
        sheet.createRow(rowIdx++);

        // Row 3+: summary data
        String[][] data = {
                {"Birim:", goal.getUnit()},
                {"Hedef Değer:", goal.getTargetValue().toPlainString()},
                {"Başlangıç:", goal.getStartDate().format(DATE_FMT)},
                {"Bitiş:", goal.getEndDate().format(DATE_FMT)},
                {"Tamamlanma:", stats.completionPct() + "%"},
                {"Gerçekleşen:", stats.currentProgress().toPlainString() + " " + goal.getUnit()},
                {"Fark (Gap):", stats.gap().toPlainString()},
                {"Kalan Gerekli:", stats.requiredRate().toPlainString() + "/gün"},
                {"Mevcut Streak:", stats.currentStreak() + " gün"}
        };

        for (String[] pair : data) {
            Row row = sheet.createRow(rowIdx++);
            org.apache.poi.ss.usermodel.Cell labelCell = row.createCell(0);
            labelCell.setCellValue(pair[0]);
            labelCell.setCellStyle(headerStyle);

            org.apache.poi.ss.usermodel.Cell valueCell = row.createCell(1);
            valueCell.setCellValue(pair[1]);

            // Conditional coloring for gap
            if ("Fark (Gap):".equals(pair[0])) {
                if (stats.gap().compareTo(BigDecimal.ZERO) >= 0) {
                    valueCell.setCellStyle(greenStyle);
                } else {
                    valueCell.setCellStyle(redStyle);
                }
            }
        }

        // Auto-size columns
        ((SXSSFSheet) sheet).trackAllColumnsForAutoSizing();
        for (int i = 0; i < 2; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    private void buildEntriesSheet(Sheet sheet, Goal goal, List<GoalEntry> entries, CellStyle headerStyle) {
        // Header row
        Row headerRow = sheet.createRow(0);
        String[] headers = {"Tarih", "Değer", "Birim", "Not"};
        for (int i = 0; i < headers.length; i++) {
            org.apache.poi.ss.usermodel.Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // Data rows
        int rowIdx = 1;
        for (GoalEntry entry : entries) {
            Row row = sheet.createRow(rowIdx++);
            row.createCell(0).setCellValue(entry.getEntryDate().format(DATE_FMT));
            row.createCell(1).setCellValue(entry.getActualValue().doubleValue());
            row.createCell(2).setCellValue(goal.getUnit());
            row.createCell(3).setCellValue(entry.getNote() != null ? entry.getNote() : "");
        }

        // Auto-size columns
        ((SXSSFSheet) sheet).trackAllColumnsForAutoSizing();
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    private void buildStatsSheet(Sheet sheet, Goal goal, List<GoalEntry> entries,
                                 GoalStatsResponse stats, CellStyle headerStyle) {
        // Calculate additional statistics
        BigDecimal dailyAvg = BigDecimal.ZERO;
        BigDecimal weeklyAvg = BigDecimal.ZERO;
        BigDecimal maxEntry = BigDecimal.ZERO;
        long daysWithoutEntry = 0;

        if (!entries.isEmpty()) {
            BigDecimal total = entries.stream()
                    .map(GoalEntry::getActualValue)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            long daysSinceStart = Math.max(1, stats.daysSinceStart());
            dailyAvg = total.divide(BigDecimal.valueOf(daysSinceStart), 2, RoundingMode.HALF_UP);
            weeklyAvg = dailyAvg.multiply(BigDecimal.valueOf(7)).setScale(2, RoundingMode.HALF_UP);

            maxEntry = entries.stream()
                    .map(GoalEntry::getActualValue)
                    .max(BigDecimal::compareTo)
                    .orElse(BigDecimal.ZERO);

            long totalDaysCovered = ChronoUnit.DAYS.between(goal.getStartDate(),
                    goal.getEndDate().isBefore(LocalDate.now()) ? goal.getEndDate() : LocalDate.now()) + 1;
            daysWithoutEntry = Math.max(0, totalDaysCovered - entries.size());
        }

        // Header
        Row headerRow = sheet.createRow(0);
        org.apache.poi.ss.usermodel.Cell h1 = headerRow.createCell(0);
        h1.setCellValue("İstatistik");
        h1.setCellStyle(headerStyle);
        org.apache.poi.ss.usermodel.Cell h2 = headerRow.createCell(1);
        h2.setCellValue("Değer");
        h2.setCellStyle(headerStyle);

        String[][] data = {
                {"Günlük Ortalama", dailyAvg.toPlainString() + " " + goal.getUnit()},
                {"Haftalık Ortalama", weeklyAvg.toPlainString() + " " + goal.getUnit()},
                {"En Yüksek Kayıt", maxEntry.toPlainString() + " " + goal.getUnit()},
                {"Kayıtsız Gün Sayısı", String.valueOf(daysWithoutEntry)},
                {"Toplam Kayıt Sayısı", String.valueOf(stats.entryCount())},
                {"Mevcut Streak", stats.currentStreak() + " gün"},
                {"En Uzun Streak", stats.longestStreak() + " gün"},
                {"Durum", stats.trackingStatus()}
        };

        int rowIdx = 1;
        for (String[] pair : data) {
            Row row = sheet.createRow(rowIdx++);
            row.createCell(0).setCellValue(pair[0]);
            row.createCell(1).setCellValue(pair[1]);
        }

        ((SXSSFSheet) sheet).trackAllColumnsForAutoSizing();
        for (int i = 0; i < 2; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    private CellStyle createHeaderStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setBold(true);
        style.setFont(font);
        return style;
    }

    private CellStyle createTitleStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 16);
        style.setFont(font);
        return style;
    }

    private CellStyle createColorStyle(Workbook wb, IndexedColors color) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setColor(color.getIndex());
        font.setBold(true);
        style.setFont(font);
        return style;
    }

    // ─── PDF Export ────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public byte[] exportGoalToPdf(Long goalId, Long userId) {
        Goal goal = getGoalWithOwnershipCheck(goalId, userId);
        List<GoalEntry> entries = goalEntryRepository.findByGoalIdOrderByEntryDateDesc(goalId);
        GoalStatsResponse stats = goalEntryService.getStats(goalId, userId);

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdfDoc = new PdfDocument(writer);
            pdfDoc.setDefaultPageSize(PageSize.A4);

            // Add header/footer event handler
            pdfDoc.addEventHandler(PdfDocumentEvent.END_PAGE,
                    new HeaderFooterEventHandler(goal.getTitle()));

            Document document = new Document(pdfDoc);
            document.setMargins(60, 36, 50, 36); // top margin for header

            PdfFont font = PdfFontFactory.createFont(StandardFonts.HELVETICA);
            PdfFont boldFont = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);

            // Section 1: Title + Goal Info
            document.add(new Paragraph(goal.getTitle())
                    .setFont(boldFont).setFontSize(18).setMarginBottom(10));

            Table infoTable = new Table(UnitValue.createPercentArray(new float[]{1, 2}))
                    .useAllAvailableWidth();
            addInfoRow(infoTable, "Birim:", goal.getUnit(), boldFont, font);
            addInfoRow(infoTable, "Hedef Değer:", goal.getTargetValue().toPlainString(), boldFont, font);
            addInfoRow(infoTable, "Başlangıç:", goal.getStartDate().format(DATE_FMT), boldFont, font);
            addInfoRow(infoTable, "Bitiş:", goal.getEndDate().format(DATE_FMT), boldFont, font);
            addInfoRow(infoTable, "Tamamlanma:", stats.completionPct() + "%", boldFont, font);
            addInfoRow(infoTable, "Gerçekleşen:", stats.currentProgress().toPlainString() + " " + goal.getUnit(), boldFont, font);
            addInfoRow(infoTable, "Fark (Gap):", stats.gap().toPlainString(), boldFont, font);
            addInfoRow(infoTable, "Gerekli Oran:", stats.requiredRate().toPlainString() + "/gün", boldFont, font);
            addInfoRow(infoTable, "Mevcut Streak:", stats.currentStreak() + " gün", boldFont, font);
            document.add(infoTable);

            // Section 2: Statistics
            document.add(new Paragraph("İstatistikler")
                    .setFont(boldFont).setFontSize(14).setMarginTop(20).setMarginBottom(5));

            BigDecimal dailyAvg = BigDecimal.ZERO;
            BigDecimal weeklyAvg = BigDecimal.ZERO;
            BigDecimal maxEntry = BigDecimal.ZERO;

            if (!entries.isEmpty()) {
                BigDecimal total = entries.stream()
                        .map(GoalEntry::getActualValue)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                long days = Math.max(1, stats.daysSinceStart());
                dailyAvg = total.divide(BigDecimal.valueOf(days), 2, RoundingMode.HALF_UP);
                weeklyAvg = dailyAvg.multiply(BigDecimal.valueOf(7)).setScale(2, RoundingMode.HALF_UP);
                maxEntry = entries.stream()
                        .map(GoalEntry::getActualValue)
                        .max(BigDecimal::compareTo)
                        .orElse(BigDecimal.ZERO);
            }

            Table statsTable = new Table(UnitValue.createPercentArray(new float[]{1, 1}))
                    .useAllAvailableWidth();
            addInfoRow(statsTable, "Günlük Ortalama:", dailyAvg.toPlainString() + " " + goal.getUnit(), boldFont, font);
            addInfoRow(statsTable, "Haftalık Ortalama:", weeklyAvg.toPlainString() + " " + goal.getUnit(), boldFont, font);
            addInfoRow(statsTable, "En Yüksek Kayıt:", maxEntry.toPlainString() + " " + goal.getUnit(), boldFont, font);
            addInfoRow(statsTable, "Toplam Kayıt:", String.valueOf(stats.entryCount()), boldFont, font);
            addInfoRow(statsTable, "Durum:", stats.trackingStatus(), boldFont, font);
            document.add(statsTable);

            // Section 3: Entry List
            document.add(new Paragraph("İlerleme Kayıtları")
                    .setFont(boldFont).setFontSize(14).setMarginTop(20).setMarginBottom(5));

            Table entryTable = new Table(UnitValue.createPercentArray(new float[]{2, 1.5f, 1, 3}))
                    .useAllAvailableWidth();

            // Entry table headers
            DeviceRgb headerBg = new DeviceRgb(52, 58, 64);
            String[] entryHeaders = {"Tarih", "Değer", "Birim", "Not"};
            for (String h : entryHeaders) {
                entryTable.addHeaderCell(new Cell()
                        .add(new Paragraph(h).setFont(boldFont).setFontColor(ColorConstants.WHITE))
                        .setBackgroundColor(headerBg));
            }

            for (GoalEntry entry : entries) {
                entryTable.addCell(new Cell().add(new Paragraph(entry.getEntryDate().format(DATE_FMT)).setFont(font)));
                entryTable.addCell(new Cell().add(new Paragraph(entry.getActualValue().toPlainString()).setFont(font)));
                entryTable.addCell(new Cell().add(new Paragraph(goal.getUnit()).setFont(font)));
                entryTable.addCell(new Cell().add(new Paragraph(entry.getNote() != null ? entry.getNote() : "").setFont(font)));
            }

            if (entries.isEmpty()) {
                entryTable.addCell(new Cell(1, 4)
                        .add(new Paragraph("Henüz kayıt yok.").setFont(font).setFontColor(ColorConstants.GRAY))
                        .setTextAlignment(TextAlignment.CENTER));
            }

            document.add(entryTable);

            document.close();
            return baos.toByteArray();

        } catch (IOException e) {
            log.error("PDF export hatası: goalId={}", goalId, e);
            throw new RuntimeException("PDF oluşturulurken bir hata oluştu.", e);
        }
    }

    private void addInfoRow(Table table, String label, String value, PdfFont boldFont, PdfFont font) {
        table.addCell(new Cell().add(new Paragraph(label).setFont(boldFont)).setBorder(null));
        table.addCell(new Cell().add(new Paragraph(value).setFont(font)).setBorder(null));
    }

    /**
     * iText 7 event handler for page header/footer.
     */
    private static class HeaderFooterEventHandler implements IEventHandler {
        private final String goalTitle;

        HeaderFooterEventHandler(String goalTitle) {
            this.goalTitle = goalTitle;
        }

        @Override
        public void handleEvent(Event event) {
            PdfDocumentEvent docEvent = (PdfDocumentEvent) event;
            PdfDocument pdfDoc = docEvent.getDocument();
            PdfPage page = docEvent.getPage();
            Rectangle pageSize = page.getPageSize();
            PdfCanvas canvas = new PdfCanvas(page.newContentStreamBefore(), page.getResources(), pdfDoc);

            int pageNum = pdfDoc.getPageNumber(page);
            int totalPages = pdfDoc.getNumberOfPages();

            try {
                PdfFont font = PdfFontFactory.createFont(StandardFonts.HELVETICA);

                // Header
                canvas.beginText()
                        .setFontAndSize(font, 9)
                        .moveText(36, pageSize.getTop() - 30)
                        .showText(goalTitle)
                        .endText();

                // Footer
                String footerText = "Sayfa " + pageNum + " / " + totalPages;
                canvas.beginText()
                        .setFontAndSize(font, 9)
                        .moveText(pageSize.getWidth() / 2 - 30, 25)
                        .showText(footerText)
                        .endText();

            } catch (IOException e) {
                // Ignore font creation errors in handler
            }
            canvas.release();
        }
    }

    // ─── CSV Export ────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public byte[] exportGoalToCsv(Long goalId, Long userId) {
        Goal goal = getGoalWithOwnershipCheck(goalId, userId);
        List<GoalEntry> entries = goalEntryRepository.findByGoalIdOrderByEntryDateDesc(goalId);

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            // UTF-8 BOM for Excel Turkish character support
            baos.write(new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF});

            StringBuilder sb = new StringBuilder();
            // Header
            sb.append("Tarih,Değer,Birim,Not\n");

            for (GoalEntry entry : entries) {
                sb.append(entry.getEntryDate().format(DATE_FMT));
                sb.append(',');
                sb.append(entry.getActualValue().toPlainString());
                sb.append(',');
                sb.append(escapeCsv(goal.getUnit()));
                sb.append(',');
                sb.append(escapeCsv(entry.getNote() != null ? entry.getNote() : ""));
                sb.append('\n');
            }

            baos.write(sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return baos.toByteArray();

        } catch (IOException e) {
            log.error("CSV export hatası: goalId={}", goalId, e);
            throw new RuntimeException("CSV oluşturulurken bir hata oluştu.", e);
        }
    }

    /**
     * RFC 4180 CSV escape — fields with comma, quote, or newline are quoted.
     */
    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    // ─── Monthly Report ────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public byte[] generateMonthlyReport(Long userId, int year, int month) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Kullanıcı bulunamadı: " + userId));

        YearMonth ym = YearMonth.of(year, month);
        LocalDate monthStart = ym.atDay(1);
        LocalDate monthEnd = ym.atEndOfMonth();

        List<Goal> activeGoals = goalRepository.findByUserIdAndStatus(userId, GoalStatus.ACTIVE);
        // Also include completed goals
        List<Goal> completedGoals = goalRepository.findByUserIdAndStatusOrderByCreatedAtDesc(userId, GoalStatus.COMPLETED);

        // Merge, but only goals that overlap with the month
        List<Goal> allGoals = new java.util.ArrayList<>();
        allGoals.addAll(activeGoals);
        allGoals.addAll(completedGoals);
        List<Goal> monthGoals = allGoals.stream()
                .filter(g -> !g.getStartDate().isAfter(monthEnd) && !g.getEndDate().isBefore(monthStart))
                .toList();

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdfDoc = new PdfDocument(writer);
            pdfDoc.setDefaultPageSize(PageSize.A4);
            Document document = new Document(pdfDoc);

            PdfFont font = PdfFontFactory.createFont(StandardFonts.HELVETICA);
            PdfFont boldFont = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);

            // Cover Page
            document.add(new Paragraph("Aylık Rapor")
                    .setFont(boldFont).setFontSize(24)
                    .setTextAlignment(TextAlignment.CENTER).setMarginTop(150));

            String displayName = user.getDisplayName() != null ? user.getDisplayName() : user.getUsername();
            document.add(new Paragraph(displayName)
                    .setFont(font).setFontSize(16)
                    .setTextAlignment(TextAlignment.CENTER).setMarginTop(20));

            document.add(new Paragraph(ym.format(MONTH_FMT))
                    .setFont(font).setFontSize(14)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setFontColor(ColorConstants.GRAY).setMarginTop(10));

            document.add(new Paragraph(monthStart.format(DATE_FMT) + "  —  " + monthEnd.format(DATE_FMT))
                    .setFont(font).setFontSize(12)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setFontColor(ColorConstants.GRAY).setMarginTop(5));

            // Goals summary table (new page)
            document.add(new com.itextpdf.layout.element.AreaBreak());

            document.add(new Paragraph("Hedef Özeti")
                    .setFont(boldFont).setFontSize(16).setMarginBottom(10));

            if (monthGoals.isEmpty()) {
                document.add(new Paragraph("Bu ay için aktif hedef bulunamadı.")
                        .setFont(font).setFontColor(ColorConstants.GRAY));
            } else {
                Table table = new Table(UnitValue.createPercentArray(new float[]{3, 1.5f, 1.5f, 1.5f}))
                        .useAllAvailableWidth();

                DeviceRgb headerBg = new DeviceRgb(52, 58, 64);
                String[] headers = {"Hedef", "Tamamlanma %", "Toplam Kayıt", "Streak"};
                for (String h : headers) {
                    table.addHeaderCell(new Cell()
                            .add(new Paragraph(h).setFont(boldFont).setFontColor(ColorConstants.WHITE))
                            .setBackgroundColor(headerBg));
                }

                for (Goal goal : monthGoals) {
                    BigDecimal completionPct = goalCalculator.calculateCompletionPct(goal);
                    long entryCount = goalEntryRepository.countByGoalId(goal.getId());
                    int streak = streakService.getStreakForGoal(goal.getId());

                    table.addCell(new Cell().add(new Paragraph(goal.getTitle()).setFont(font)));
                    table.addCell(new Cell().add(new Paragraph(completionPct + "%").setFont(font))
                            .setTextAlignment(TextAlignment.CENTER));
                    table.addCell(new Cell().add(new Paragraph(String.valueOf(entryCount)).setFont(font))
                            .setTextAlignment(TextAlignment.CENTER));
                    table.addCell(new Cell().add(new Paragraph(streak + " gün").setFont(font))
                            .setTextAlignment(TextAlignment.CENTER));
                }

                document.add(table);
            }

            document.close();
            return baos.toByteArray();

        } catch (IOException e) {
            log.error("Aylık rapor hatası: userId={}, year={}, month={}", userId, year, month, e);
            throw new RuntimeException("Aylık rapor oluşturulurken bir hata oluştu.", e);
        }
    }

    // ─── Filename Normalization ────────────────────────────────────────

    @Override
    public String normalizeFilename(String title) {
        if (title == null || title.isBlank()) return "export";
        // Map Turkish characters that don't decompose via NFD
        String mapped = title
                .replace('ı', 'i').replace('İ', 'I')
                .replace('ş', 's').replace('Ş', 'S')
                .replace('ğ', 'g').replace('Ğ', 'G')
                .replace('ç', 'c').replace('Ç', 'C')
                .replace('ö', 'o').replace('Ö', 'O')
                .replace('ü', 'u').replace('Ü', 'U');
        String safe = Normalizer.normalize(mapped, Normalizer.Form.NFD)
                .replaceAll("[^\\p{ASCII}]", "")
                .replaceAll("[^a-zA-Z0-9\\-]", "-")
                .toLowerCase()
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "");
        return safe.isEmpty() ? "export" : safe;
    }

    // ─── Helper ────────────────────────────────────────────────────────

    private Goal getGoalWithOwnershipCheck(Long goalId, Long userId) {
        Goal goal = goalRepository.findById(goalId)
                .orElseThrow(() -> new GoalNotFoundException(goalId));
        if (!goal.getUser().getId().equals(userId)) {
            throw new GoalAccessDeniedException(goalId);
        }
        return goal;
    }
}

