package com.goaltracker.service;

import com.goaltracker.dto.*;

import java.util.List;

public interface GoalEntryService {

    List<GoalEntryResponse> getEntries(Long goalId, Long userId);

    GoalEntryResponse createEntry(Long goalId, Long userId, CreateEntryRequest request);

    GoalEntryResponse updateEntry(Long entryId, Long userId, UpdateEntryRequest request);

    void deleteEntry(Long entryId, Long userId);

    GoalEntryResponse getEntryById(Long entryId, Long userId);

    GoalStatsResponse getStats(Long goalId, Long userId);

    ChartDataResponse getChartData(Long goalId, Long userId);
}

