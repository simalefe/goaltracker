package com.goaltracker.service;

import com.goaltracker.dto.response.StreakResponse;

import java.time.LocalDate;
import java.util.List;

public interface StreakService {

    void updateStreak(Long goalId, LocalDate entryDate);

    StreakResponse getStreak(Long goalId);

    List<StreakResponse> getUserStreaks(Long userId);

    int getTotalStreakDays(Long userId);

    int resetStaleStreaks(LocalDate date);

    int getStreakForGoal(Long goalId);

    int getLongestStreakForGoal(Long goalId);
}

