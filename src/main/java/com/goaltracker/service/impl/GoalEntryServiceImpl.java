package com.goaltracker.service.impl;

import com.goaltracker.dto.*;
import com.goaltracker.exception.*;
import com.goaltracker.mapper.GoalEntryMapper;
import com.goaltracker.model.Goal;
import com.goaltracker.model.GoalEntry;
import com.goaltracker.model.enums.GoalStatus;
import com.goaltracker.model.event.GoalEntryCreatedEvent;
import com.goaltracker.model.event.GoalEntryDeletedEvent;
import com.goaltracker.model.event.GoalEntryUpdatedEvent;
import com.goaltracker.repository.GoalEntryRepository;
import com.goaltracker.repository.GoalRepository;
import com.goaltracker.service.GoalEntryService;
import com.goaltracker.service.StreakService;
import com.goaltracker.util.GoalCalculator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class GoalEntryServiceImpl implements GoalEntryService {

    private static final Logger log = LoggerFactory.getLogger(GoalEntryServiceImpl.class);

    private final GoalEntryRepository goalEntryRepository;
    private final GoalRepository goalRepository;
    private final GoalCalculator goalCalculator;
    private final ApplicationEventPublisher eventPublisher;
    private final StreakService streakService;

    public GoalEntryServiceImpl(GoalEntryRepository goalEntryRepository,
                                 GoalRepository goalRepository,
                                 GoalCalculator goalCalculator,
                                 ApplicationEventPublisher eventPublisher,
                                 StreakService streakService) {
        this.goalEntryRepository = goalEntryRepository;
        this.goalRepository = goalRepository;
        this.goalCalculator = goalCalculator;
        this.eventPublisher = eventPublisher;
        this.streakService = streakService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<GoalEntryResponse> getEntries(Long goalId, Long userId) {
        Goal goal = getGoalWithOwnershipCheck(goalId, userId);
        return goalEntryRepository.findByGoalIdOrderByEntryDateDesc(goal.getId())
                .stream()
                .map(GoalEntryMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    @CacheEvict(value = "dashboard", allEntries = true)
    public GoalEntryResponse createEntry(Long goalId, Long userId, CreateEntryRequest request) {
        Goal goal = getGoalWithOwnershipCheck(goalId, userId);

        // Status check — only ACTIVE goals accept entries
        if (goal.getStatus() != GoalStatus.ACTIVE) {
            throw new GoalNotActiveException(goal.getStatus().name());
        }

        // Date range check
        if (request.entryDate().isBefore(goal.getStartDate()) || request.entryDate().isAfter(goal.getEndDate())) {
            throw new EntryOutOfRangeException(goal.getStartDate(), goal.getEndDate());
        }

        // Duplicate check
        if (goalEntryRepository.existsByGoalIdAndEntryDate(goalId, request.entryDate())) {
            throw new DuplicateEntryException(request.entryDate().toString());
        }

        GoalEntry entry = GoalEntryMapper.toEntity(request, goal);
        GoalEntry saved = goalEntryRepository.save(entry);

        log.info("Entry oluşturuldu: goalId={}, entryDate={}, userId={}", goalId, request.entryDate(), userId);

        // Publish event for Phase 5 streak/badge
        eventPublisher.publishEvent(new GoalEntryCreatedEvent(
                this, goalId, userId, saved.getEntryDate(), saved.getActualValue()));

        return GoalEntryMapper.toResponse(saved);
    }

    @Override
    @Transactional
    @CacheEvict(value = "dashboard", allEntries = true)
    public GoalEntryResponse updateEntry(Long entryId, Long userId, UpdateEntryRequest request) {
        GoalEntry entry = getEntryWithOwnershipCheck(entryId, userId);

        if (request.actualValue() != null) {
            entry.setActualValue(request.actualValue());
        }
        if (request.note() != null) {
            entry.setNote(request.note());
        }

        GoalEntry saved = goalEntryRepository.save(entry);
        log.info("Entry güncellendi: entryId={}, userId={}", entryId, userId);

        eventPublisher.publishEvent(new GoalEntryUpdatedEvent(
                this, entryId, entry.getGoal().getId(), userId,
                entry.getEntryDate(), saved.getActualValue()));

        return GoalEntryMapper.toResponse(saved);
    }

    @Override
    @Transactional
    @CacheEvict(value = "dashboard", allEntries = true)
    public void deleteEntry(Long entryId, Long userId) {
        GoalEntry entry = getEntryWithOwnershipCheck(entryId, userId);
        Long goalId = entry.getGoal().getId();

        goalEntryRepository.delete(entry);
        log.info("Entry silindi: entryId={}, goalId={}, userId={}", entryId, goalId, userId);

        eventPublisher.publishEvent(new GoalEntryDeletedEvent(
                this, entryId, goalId, userId, entry.getEntryDate()));
    }

    @Override
    @Transactional(readOnly = true)
    public GoalEntryResponse getEntryById(Long entryId, Long userId) {
        GoalEntry entry = getEntryWithOwnershipCheck(entryId, userId);
        return GoalEntryMapper.toResponse(entry);
    }

    @Override
    @Transactional(readOnly = true)
    public GoalStatsResponse getStats(Long goalId, Long userId) {
        Goal goal = getGoalWithOwnershipCheck(goalId, userId);

        return new GoalStatsResponse(
                goalCalculator.calculateCurrentProgress(goalId),
                goal.getTargetValue(),
                goalCalculator.calculateCompletionPct(goal),
                goalCalculator.calculateExpectedPct(goal),
                goalCalculator.calculateGap(goal),
                goalCalculator.determineTrackingStatus(goal),
                goalCalculator.calculateRequiredRate(goal),
                goal.getUnit(),
                goalCalculator.calculateDaysLeft(goal),
                (int) goalCalculator.calculateTotalDays(goal),
                goalCalculator.calculateDaysSinceStart(goal),
                (int) goalEntryRepository.countByGoalId(goalId),
                streakService.getStreakForGoal(goalId),
                streakService.getLongestStreakForGoal(goalId)
        );
    }

    @Override
    @Transactional(readOnly = true)
    public ChartDataResponse getChartData(Long goalId, Long userId) {
        Goal goal = getGoalWithOwnershipCheck(goalId, userId);
        List<GoalEntry> entries = goalEntryRepository.findByGoalIdOrderByEntryDateDesc(goalId);

        List<ChartDataPointResponse> dataPoints = goalCalculator.buildChartData(goal, entries);

        long totalDays = goalCalculator.calculateTotalDays(goal);
        java.math.BigDecimal dailyTarget = totalDays > 0
                ? goal.getTargetValue().divide(java.math.BigDecimal.valueOf(totalDays), 2, java.math.RoundingMode.HALF_UP)
                : java.math.BigDecimal.ZERO;

        return new ChartDataResponse(dataPoints, dailyTarget);
    }

    // --- Private helpers ---

    private Goal getGoalWithOwnershipCheck(Long goalId, Long userId) {
        Goal goal = goalRepository.findById(goalId)
                .orElseThrow(() -> new GoalNotFoundException(goalId));
        if (!goal.getUser().getId().equals(userId)) {
            throw new GoalAccessDeniedException(goalId);
        }
        return goal;
    }

    private GoalEntry getEntryWithOwnershipCheck(Long entryId, Long userId) {
        GoalEntry entry = goalEntryRepository.findById(entryId)
                .orElseThrow(() -> new GoalEntryNotFoundException(entryId));
        if (!entry.getGoal().getUser().getId().equals(userId)) {
            throw new GoalAccessDeniedException(entry.getGoal().getId());
        }
        return entry;
    }
}

