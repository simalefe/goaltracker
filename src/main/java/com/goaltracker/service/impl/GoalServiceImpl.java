package com.goaltracker.service.impl;

import com.goaltracker.dto.*;
import com.goaltracker.exception.GoalAccessDeniedException;
import com.goaltracker.exception.GoalLimitExceededException;
import com.goaltracker.exception.GoalNotFoundException;
import com.goaltracker.exception.InvalidStatusTransitionException;
import com.goaltracker.mapper.GoalMapper;
import com.goaltracker.model.Goal;
import com.goaltracker.model.User;
import com.goaltracker.model.enums.GoalCategory;
import com.goaltracker.model.enums.GoalStatus;
import com.goaltracker.model.enums.GoalType;
import com.goaltracker.model.event.GoalCompletedEvent;
import com.goaltracker.repository.GoalRepository;
import com.goaltracker.service.GoalService;
import com.goaltracker.service.StreakService;
import com.goaltracker.util.GoalCalculator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GoalServiceImpl implements GoalService {

    private static final Logger log = LoggerFactory.getLogger(GoalServiceImpl.class);
    private static final int MAX_ACTIVE_GOALS = 50;
    private static final int MAX_PAGE_SIZE = 50;

    private final GoalRepository goalRepository;
    private final GoalCalculator goalCalculator;
    private final StreakService streakService;
    private final ApplicationEventPublisher eventPublisher;

    public GoalServiceImpl(GoalRepository goalRepository, GoalCalculator goalCalculator,
                           StreakService streakService, ApplicationEventPublisher eventPublisher) {
        this.goalRepository = goalRepository;
        this.goalCalculator = goalCalculator;
        this.streakService = streakService;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    public GoalResponse createGoal(CreateGoalRequest req, Long userId) {
        // Aktif hedef limit kontrolü
        long activeCount = goalRepository.countByUserIdAndStatus(userId, GoalStatus.ACTIVE);
        if (activeCount >= MAX_ACTIVE_GOALS) {
            throw new GoalLimitExceededException(
                    "Aktif hedef sınırına ulaştınız. En fazla " + MAX_ACTIVE_GOALS + " aktif hedef olabilir.");
        }

        Goal g = GoalMapper.toEntity(req);
        User u = new User();
        u.setId(userId);
        g.setUser(u);
        g.setStatus(GoalStatus.ACTIVE);

        Goal saved = goalRepository.save(g);
        log.info("Hedef oluşturuldu: id={}, userId={}", saved.getId(), userId);
        return enrichResponse(GoalMapper.toResponse(saved), saved);
    }

    @Override
    @Transactional(readOnly = true)
    public GoalResponse getGoal(Long id, Long userId) {
        Goal g = getGoalWithOwnershipCheck(id, userId);
        return enrichResponse(GoalMapper.toResponse(g), g);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<GoalSummaryResponse> getGoals(Long userId, GoalStatus status, GoalCategory category,
                                               GoalType goalType, String query, Pageable pageable) {
        // Max page size limiti
        int size = Math.min(pageable.getPageSize(), MAX_PAGE_SIZE);
        Sort sort = pageable.getSort().isSorted()
                ? normalizeSort(pageable.getSort())
                : Sort.by(Sort.Direction.DESC, "created_at");
        Pageable limitedPageable = PageRequest.of(pageable.getPageNumber(), size, sort);

        Page<Goal> page = goalRepository.findByFilters(
                userId,
                status != null ? status.name() : null,
                category != null ? category.name() : null,
                goalType != null ? goalType.name() : null,
                query,
                limitedPageable);
        return page.map(g -> enrichSummaryResponse(GoalMapper.toSummaryResponse(g), g));
    }

    @Override
    @Transactional
    public GoalResponse updateGoal(Long id, UpdateGoalRequest req, Long userId) {
        Goal g = getGoalWithOwnershipCheck(id, userId);
        GoalMapper.updateEntityFromDto(req, g);
        Goal saved = goalRepository.save(g);
        log.info("Hedef güncellendi: id={}, userId={}", id, userId);
        return enrichResponse(GoalMapper.toResponse(saved), saved);
    }

    @Override
    @Transactional
    public void deleteGoal(Long id, Long userId) {
        Goal g = getGoalWithOwnershipCheck(id, userId);
        goalRepository.delete(g);
        log.info("Hedef silindi: id={}, userId={}", id, userId);
    }

    @Override
    @Transactional
    public GoalResponse updateStatus(Long goalId, Long userId, GoalStatus newStatus) {
        Goal g = getGoalWithOwnershipCheck(goalId, userId);

        if (!GoalStatus.isValidTransition(g.getStatus(), newStatus)) {
            throw new InvalidStatusTransitionException(
                    "Geçersiz durum geçişi: " + g.getStatus() + " → " + newStatus);
        }

        g.setStatus(newStatus);
        Goal saved = goalRepository.save(g);
        log.info("Hedef durumu güncellendi: id={}, {} → {}", goalId, g.getStatus(), newStatus);

        // Publish GoalCompletedEvent when status changes to COMPLETED
        if (newStatus == GoalStatus.COMPLETED) {
            eventPublisher.publishEvent(
                    new GoalCompletedEvent(this, goalId, userId, saved.getTitle()));
        }

        return enrichResponse(GoalMapper.toResponse(saved), saved);
    }

    @Override
    @Transactional(readOnly = true)
    public long countByUserIdAndStatus(Long userId, GoalStatus status) {
        return goalRepository.countByUserIdAndStatus(userId, status);
    }

    /**
     * Enriches GoalResponse with computed progress fields from GoalCalculator.
     */
    private GoalResponse enrichResponse(GoalResponse r, Goal g) {
        if (g.getStatus() != GoalStatus.COMPLETED) {
            r.setCompletionPct(goalCalculator.calculateCompletionPct(g));
            r.setCurrentProgress(goalCalculator.calculateCurrentProgress(g.getId()));
        }
        r.setCurrentStreak(streakService.getStreakForGoal(g.getId()));
        return r;
    }

    /**
     * Enriches GoalSummaryResponse with computed progress fields from GoalCalculator.
     */
    private GoalSummaryResponse enrichSummaryResponse(GoalSummaryResponse r, Goal g) {
        if (g.getStatus() != GoalStatus.COMPLETED) {
            r.setCompletionPct(goalCalculator.calculateCompletionPct(g));
        }
        r.setCurrentStreak(streakService.getStreakForGoal(g.getId()));
        return r;
    }

    /**
     * Ownership kontrolü — Goal'u bulur ve userId ile eşleştirir.
     */
    private Goal getGoalWithOwnershipCheck(Long goalId, Long userId) {
        Goal goal = goalRepository.findById(goalId)
                .orElseThrow(() -> new GoalNotFoundException(goalId));
        if (!goal.getUser().getId().equals(userId)) {
            throw new GoalAccessDeniedException(goalId);
        }
        return goal;
    }

    /**
     * Native SQL sorgularında kullanılmak üzere Sort içindeki Java camelCase alan adlarını
     * PostgreSQL kolon adlarına (snake_case) dönüştürür.
     */
    private Sort normalizeSort(Sort sort) {
        return Sort.by(
                sort.stream()
                        .map(order -> order.withProperty(toDbColumnName(order.getProperty())))
                        .toList()
        );
    }

    private String toDbColumnName(String javaFieldName) {
        return switch (javaFieldName) {
            case "createdAt"  -> "created_at";
            case "updatedAt"  -> "updated_at";
            case "targetDate" -> "target_date";
            case "goalType"   -> "goal_type";
            default           -> javaFieldName;
        };
    }
}

