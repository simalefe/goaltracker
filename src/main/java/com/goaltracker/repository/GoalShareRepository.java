package com.goaltracker.repository;

import com.goaltracker.model.GoalShare;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface GoalShareRepository extends JpaRepository<GoalShare, Long> {

    @Query("SELECT gs FROM GoalShare gs " +
            "JOIN FETCH gs.goal g " +
            "JOIN FETCH g.user " +
            "WHERE gs.sharedWithUser.id = :userId")
    List<GoalShare> findBySharedWithUserId(@Param("userId") Long userId);

    Optional<GoalShare> findByGoalIdAndSharedWithUserId(Long goalId, Long userId);

    boolean existsByGoalIdAndSharedWithUserId(Long goalId, Long userId);

    @Query("SELECT gs FROM GoalShare gs " +
            "JOIN FETCH gs.sharedWithUser " +
            "WHERE gs.goal.id = :goalId")
    List<GoalShare> findByGoalId(@Param("goalId") Long goalId);

    void deleteByGoalIdAndSharedWithUserId(Long goalId, Long userId);
}

