package com.goaltracker.service;

public interface ExportService {

    byte[] exportGoalToExcel(Long goalId, Long userId);

    byte[] exportGoalToPdf(Long goalId, Long userId);

    byte[] exportGoalToCsv(Long goalId, Long userId);

    byte[] generateMonthlyReport(Long userId, int year, int month);

    String normalizeFilename(String title);
}

