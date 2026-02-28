package com.goaltracker.service;

import com.goaltracker.dto.*;
import com.goaltracker.model.enums.GoalCategory;
import com.goaltracker.model.enums.GoalStatus;
import com.goaltracker.model.enums.GoalType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface GoalService {

    GoalResponse createGoal(CreateGoalRequest req, Long userId);

    GoalResponse getGoal(Long id, Long userId);

    Page<GoalSummaryResponse> getGoals(Long userId, GoalStatus status, GoalCategory category,
                                        GoalType goalType, String query, Pageable pageable);

    GoalResponse updateGoal(Long id, UpdateGoalRequest req, Long userId);

    void deleteGoal(Long id, Long userId);

    GoalResponse updateStatus(Long goalId, Long userId, GoalStatus newStatus);

    long countByUserIdAndStatus(Long userId, GoalStatus status);
}

