package com.goaltracker.repository;

import com.goaltracker.model.Notification;
import com.goaltracker.model.enums.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Page<Notification> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    long countByUserIdAndIsReadFalse(Long userId);

    List<Notification> findByUserIdAndIsReadFalse(Long userId);

    @Query("SELECT n FROM Notification n JOIN FETCH n.user")
    List<Notification> findAllWithUsers();

    @Query("SELECT n FROM Notification n WHERE n.scheduledAt <= :now AND n.sentAt IS NULL")
    List<Notification> findPendingNotifications(@Param("now") Instant now);

    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.user.id = :userId AND n.isRead = false")
    int markAllAsReadByUserId(@Param("userId") Long userId);

    @Query("SELECT COUNT(n) > 0 FROM Notification n WHERE n.user.id = :userId AND n.type = :type " +
            "AND n.createdAt >= :since")
    boolean existsByUserIdAndTypeAndCreatedAtAfter(
            @Param("userId") Long userId,
            @Param("type") NotificationType type,
            @Param("since") Instant since);

    Page<Notification> findByUserIdAndTypeOrderByCreatedAtDesc(
            Long userId, NotificationType type, Pageable pageable);
}

