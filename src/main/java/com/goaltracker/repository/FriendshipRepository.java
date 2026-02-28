package com.goaltracker.repository;

import com.goaltracker.model.Friendship;
import com.goaltracker.model.enums.FriendshipStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FriendshipRepository extends JpaRepository<Friendship, Long> {

    Optional<Friendship> findByRequesterIdAndReceiverId(Long requesterId, Long receiverId);

    List<Friendship> findByReceiverIdAndStatus(Long receiverId, FriendshipStatus status);

    List<Friendship> findByRequesterIdAndStatus(Long requesterId, FriendshipStatus status);

    /**
     * Find all ACCEPTED friendships where the user is either requester or receiver.
     */
    @Query("SELECT f FROM Friendship f " +
            "JOIN FETCH f.requester " +
            "JOIN FETCH f.receiver " +
            "WHERE (f.requester.id = :userId OR f.receiver.id = :userId) " +
            "AND f.status = 'ACCEPTED'")
    List<Friendship> findAcceptedFriends(@Param("userId") Long userId);

    /**
     * Check if two users are friends (ACCEPTED status, either direction).
     */
    @Query("SELECT CASE WHEN COUNT(f) > 0 THEN true ELSE false END FROM Friendship f " +
            "WHERE ((f.requester.id = :userId1 AND f.receiver.id = :userId2) " +
            "   OR  (f.requester.id = :userId2 AND f.receiver.id = :userId1)) " +
            "AND f.status = 'ACCEPTED'")
    boolean areFriends(@Param("userId1") Long userId1, @Param("userId2") Long userId2);

    /**
     * Find friendship between two users in any direction with any status.
     */
    @Query("SELECT f FROM Friendship f " +
            "WHERE (f.requester.id = :userId1 AND f.receiver.id = :userId2) " +
            "   OR (f.requester.id = :userId2 AND f.receiver.id = :userId1)")
    Optional<Friendship> findBetweenUsers(@Param("userId1") Long userId1, @Param("userId2") Long userId2);

    /**
     * Find all pending requests received by userId.
     */
    @Query("SELECT f FROM Friendship f " +
            "JOIN FETCH f.requester " +
            "JOIN FETCH f.receiver " +
            "WHERE f.receiver.id = :userId AND f.status = 'PENDING'")
    List<Friendship> findPendingIncoming(@Param("userId") Long userId);

    /**
     * Find all pending requests sent by userId.
     */
    @Query("SELECT f FROM Friendship f " +
            "JOIN FETCH f.requester " +
            "JOIN FETCH f.receiver " +
            "WHERE f.requester.id = :userId AND f.status = 'PENDING'")
    List<Friendship> findPendingOutgoing(@Param("userId") Long userId);

    /**
     * Get friend IDs for a user (both directions).
     */
    @Query("SELECT CASE WHEN f.requester.id = :userId THEN f.receiver.id ELSE f.requester.id END " +
            "FROM Friendship f " +
            "WHERE (f.requester.id = :userId OR f.receiver.id = :userId) " +
            "AND f.status = 'ACCEPTED'")
    List<Long> findFriendIds(@Param("userId") Long userId);
}

