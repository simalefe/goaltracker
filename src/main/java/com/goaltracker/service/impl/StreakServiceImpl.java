package com.goaltracker.service.impl;

import com.goaltracker.dto.response.StreakResponse;
import com.goaltracker.model.Goal;
import com.goaltracker.model.Streak;
import com.goaltracker.model.enums.GoalStatus;
import com.goaltracker.repository.GoalRepository;
import com.goaltracker.repository.StreakRepository;
import com.goaltracker.service.StreakService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class StreakServiceImpl implements StreakService {

    private static final Logger log = LoggerFactory.getLogger(StreakServiceImpl.class);

    private final StreakRepository streakRepository;
    private final GoalRepository goalRepository;

    public StreakServiceImpl(StreakRepository streakRepository, GoalRepository goalRepository) {
        this.streakRepository = streakRepository;
        this.goalRepository = goalRepository;
    }

    @Override
    @Transactional
    public void updateStreak(Long goalId, LocalDate entryDate) {
        Streak streak = streakRepository.findByGoalId(goalId).orElseGet(() -> {
            Goal goal = goalRepository.findById(goalId)
                    .orElseThrow(() -> new RuntimeException("Hedef bulunamadı: " + goalId));
            Streak s = new Streak();
            s.setGoal(goal);
            s.setCurrentStreak(0);
            s.setLongestStreak(0);
            return s;
        });

        LocalDate lastEntry = streak.getLastEntryDate();

        if (lastEntry == null) {
            // İlk entry
            streak.setCurrentStreak(1);
        } else if (lastEntry.equals(entryDate)) {
            // Aynı gün, idempotent — değişme
            log.debug("Streak zaten güncel: goalId={}, date={}", goalId, entryDate);
            return;
        } else if (lastEntry.equals(entryDate.minusDays(1))) {
            // Dün entry girildi, bugün de giriliyor → streak++
            streak.setCurrentStreak(streak.getCurrentStreak() + 1);
        } else if (lastEntry.isBefore(entryDate.minusDays(1))) {
            // 2+ gün önce → streak koptu, yeniden başla
            streak.setCurrentStreak(1);
        } else {
            // entryDate, lastEntryDate'den önceyse (geçmişe dönük entry) → değişme
            log.debug("Geçmişe dönük entry, streak değişmedi: goalId={}, date={}", goalId, entryDate);
            return;
        }

        // longestStreak her zaman max
        if (streak.getCurrentStreak() > streak.getLongestStreak()) {
            streak.setLongestStreak(streak.getCurrentStreak());
        }

        streak.setLastEntryDate(entryDate);
        streakRepository.save(streak);

        log.info("Streak güncellendi: goalId={}, current={}, longest={}, lastEntry={}",
                goalId, streak.getCurrentStreak(), streak.getLongestStreak(), entryDate);
    }

    @Override
    @Transactional(readOnly = true)
    public StreakResponse getStreak(Long goalId) {
        Streak streak = streakRepository.findByGoalId(goalId).orElse(null);
        if (streak == null) {
            return new StreakResponse(goalId, 0, 0, null);
        }
        return toResponse(streak);
    }

    @Override
    @Transactional(readOnly = true)
    public List<StreakResponse> getUserStreaks(Long userId) {
        return streakRepository.findByGoal_User_Id(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public int getTotalStreakDays(Long userId) {
        return streakRepository.findByGoal_User_Id(userId).stream()
                .filter(s -> s.getGoal().getStatus() == GoalStatus.ACTIVE)
                .mapToInt(Streak::getCurrentStreak)
                .sum();
    }

    @Override
    @Transactional
    public int resetStaleStreaks(LocalDate date) {
        // Dün = date - 1; lastEntryDate < dün olan ACTIVE hedeflerin streak'ini sıfırla
        LocalDate yesterday = date.minusDays(1);
        int count = streakRepository.resetStaleStreaks(GoalStatus.ACTIVE, yesterday);
        log.info("Stale streak'ler sıfırlandı: count={}, date={}", count, date);
        return count;
    }

    @Override
    @Transactional(readOnly = true)
    public int getStreakForGoal(Long goalId) {
        return streakRepository.findByGoalId(goalId)
                .map(Streak::getCurrentStreak)
                .orElse(0);
    }

    @Override
    @Transactional(readOnly = true)
    public int getLongestStreakForGoal(Long goalId) {
        return streakRepository.findByGoalId(goalId)
                .map(Streak::getLongestStreak)
                .orElse(0);
    }

    private StreakResponse toResponse(Streak streak) {
        return new StreakResponse(
                streak.getGoal().getId(),
                streak.getCurrentStreak(),
                streak.getLongestStreak(),
                streak.getLastEntryDate()
        );
    }
}

