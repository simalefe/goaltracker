package com.goaltracker.repository;

import com.goaltracker.model.Goal;
import com.goaltracker.model.enums.GoalCategory;
import com.goaltracker.model.enums.GoalStatus;
import com.goaltracker.model.enums.GoalType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface GoalRepository extends JpaRepository<Goal, Long> {

    @Query("SELECT g FROM Goal g WHERE g.user.id = :userId AND g.status = :status")
    Page<Goal> findByUserIdAndStatus(@Param("userId") Long userId, @Param("status") GoalStatus status, Pageable pageable);

    @Query("SELECT g FROM Goal g WHERE g.user.id = :userId")
    Page<Goal> findByUserId(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT g FROM Goal g WHERE g.user.id = :userId AND g.status = :status ORDER BY g.createdAt DESC")
    List<Goal> findByUserIdAndStatusOrderByCreatedAtDesc(@Param("userId") Long userId, @Param("status") GoalStatus status);

    @Query("SELECT g FROM Goal g WHERE g.user.id = :userId " +
            "AND (:status IS NULL OR g.status = :status) " +
            "AND (:category IS NULL OR g.category = :category) " +
            "AND (:goalType IS NULL OR g.goalType = :goalType) " +
            "AND (:query IS NULL OR LOWER(g.title) LIKE LOWER(CONCAT('%', :query, '%')))")
    Page<Goal> findByFilters(@Param("userId") Long userId,
                             @Param("status") GoalStatus status,
                             @Param("category") GoalCategory category,
                             @Param("goalType") GoalType goalType,
                             @Param("query") String query,
                             Pageable pageable);

    long countByUserIdAndStatus(Long userId, GoalStatus status);

    List<Goal> findByUserIdAndStatus(Long userId, GoalStatus status);

    Optional<Goal> findByIdAndUserId(Long id, Long userId);
}

