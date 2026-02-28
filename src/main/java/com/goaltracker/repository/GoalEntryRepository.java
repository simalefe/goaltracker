package com.goaltracker.repository;

import com.goaltracker.model.GoalEntry;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface GoalEntryRepository extends JpaRepository<GoalEntry, Long> {

    List<GoalEntry> findByGoalIdOrderByEntryDateDesc(Long goalId);

    Optional<GoalEntry> findByGoalIdAndEntryDate(Long goalId, LocalDate date);

    boolean existsByGoalIdAndEntryDate(Long goalId, LocalDate date);

    @Query("SELECT COALESCE(SUM(e.actualValue), 0) FROM GoalEntry e WHERE e.goal.id = :goalId")
    BigDecimal sumActualValueByGoalId(@Param("goalId") Long goalId);

    List<GoalEntry> findByGoalIdAndEntryDateBetween(Long goalId, LocalDate from, LocalDate to);

    long countByGoalIdAndEntryDateBetween(Long goalId, LocalDate from, LocalDate to);

    long countByGoalId(Long goalId);

    // --- Dashboard queries (Phase 4) ---

    /**
     * Batch aggregate: SUM(actual_value) grouped by goal_id for given goal IDs.
     * Returns Object[] where [0] = goalId (Long), [1] = total (BigDecimal).
     */
    @Query("SELECT e.goal.id, COALESCE(SUM(e.actualValue), 0) " +
            "FROM GoalEntry e WHERE e.goal.id IN :goalIds GROUP BY e.goal.id")
    List<Object[]> sumByGoalIds(@Param("goalIds") List<Long> goalIds);

    /**
     * Count of distinct goals that have an entry on the given date for the user.
     */
    @Query("SELECT COUNT(DISTINCT e.goal.id) FROM GoalEntry e " +
            "WHERE e.goal.user.id = :userId AND e.entryDate = :date")
    int countDistinctGoalsByUserIdAndEntryDate(@Param("userId") Long userId, @Param("date") LocalDate date);

    /**
     * Recent entries for a user's goals, ordered by entryDate DESC, createdAt DESC.
     */
    @Query("SELECT e FROM GoalEntry e JOIN FETCH e.goal g " +
            "WHERE g.user.id = :userId ORDER BY e.entryDate DESC, e.createdAt DESC")
    List<GoalEntry> findRecentByUserId(@Param("userId") Long userId, Pageable pageable);

    /**
     * Total entry count for a user across all goals.
     */
    @Query("SELECT COUNT(e) FROM GoalEntry e WHERE e.goal.user.id = :userId")
    long countByGoal_User_Id(@Param("userId") Long userId);
}

