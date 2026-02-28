package com.goaltracker.repository;

import com.goaltracker.model.Streak;
import com.goaltracker.model.enums.GoalStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface StreakRepository extends JpaRepository<Streak, Long> {

    Optional<Streak> findByGoalId(Long goalId);

    @Query("SELECT s FROM Streak s JOIN FETCH s.goal g WHERE g.user.id = :userId")
    List<Streak> findByGoal_User_Id(@Param("userId") Long userId);

    @Query("SELECT s FROM Streak s JOIN FETCH s.goal g " +
            "WHERE g.status = :status AND s.lastEntryDate < :date")
    List<Streak> findByGoal_StatusAndLastEntryDateBefore(
            @Param("status") GoalStatus status,
            @Param("date") LocalDate date);

    @Modifying
    @Query("UPDATE Streak s SET s.currentStreak = 0, s.updatedAt = CURRENT_TIMESTAMP " +
            "WHERE s.goal.status = :status AND s.lastEntryDate IS NOT NULL AND s.lastEntryDate < :date")
    int resetStaleStreaks(@Param("status") GoalStatus status, @Param("date") LocalDate date);
}

