package com.goaltracker.repository;

import com.goaltracker.model.Goal;
import com.goaltracker.model.enums.GoalStatus;
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

    @Query(value = "SELECT * FROM goals g WHERE g.user_id = :userId " +
            "AND (CAST(:status AS text) IS NULL OR g.status = :status) " +
            "AND (CAST(:category AS text) IS NULL OR g.category = :category) " +
            "AND (CAST(:goalType AS text) IS NULL OR g.goal_type = :goalType) " +
            "AND (CAST(:query AS text) IS NULL OR LOWER(g.title) LIKE LOWER(CONCAT('%', :query, '%')))",
            countQuery = "SELECT COUNT(*) FROM goals g WHERE g.user_id = :userId " +
            "AND (CAST(:status AS text) IS NULL OR g.status = :status) " +
            "AND (CAST(:category AS text) IS NULL OR g.category = :category) " +
            "AND (CAST(:goalType AS text) IS NULL OR g.goal_type = :goalType) " +
            "AND (CAST(:query AS text) IS NULL OR LOWER(g.title) LIKE LOWER(CONCAT('%', :query, '%')))",
            nativeQuery = true)
    Page<Goal> findByFilters(@Param("userId") Long userId,
                             @Param("status") String status,
                             @Param("category") String category,
                             @Param("goalType") String goalType,
                             @Param("query") String query,
                             Pageable pageable);

    long countByUserIdAndStatus(Long userId, GoalStatus status);

    List<Goal> findByUserIdAndStatus(Long userId, GoalStatus status);

    Optional<Goal> findByIdAndUserId(Long id, Long userId);
}

