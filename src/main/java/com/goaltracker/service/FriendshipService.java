package com.goaltracker.service;

import com.goaltracker.dto.response.FriendRequestResponse;
import com.goaltracker.dto.response.FriendResponse;
import com.goaltracker.exception.ErrorCode;
import com.goaltracker.exception.FriendshipException;
import com.goaltracker.model.Friendship;
import com.goaltracker.model.User;
import com.goaltracker.model.enums.FriendshipStatus;
import com.goaltracker.model.enums.NotificationType;
import com.goaltracker.repository.FriendshipRepository;
import com.goaltracker.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class FriendshipService {

    private static final Logger log = LoggerFactory.getLogger(FriendshipService.class);

    private final FriendshipRepository friendshipRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    public FriendshipService(FriendshipRepository friendshipRepository,
                             UserRepository userRepository,
                             NotificationService notificationService) {
        this.friendshipRepository = friendshipRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    @Transactional
    public FriendRequestResponse sendRequest(Long requesterId, String receiverUsername) {
        User requester = userRepository.findById(requesterId)
                .orElseThrow(() -> new RuntimeException("Kullanıcı bulunamadı: " + requesterId));

        User receiver = userRepository.findByUsername(receiverUsername)
                .orElseThrow(() -> new FriendshipException(ErrorCode.NOT_FOUND,
                        "Kullanıcı bulunamadı: " + receiverUsername));

        // Self-request check
        if (requester.getId().equals(receiver.getId())) {
            throw new FriendshipException(ErrorCode.SELF_FRIEND_REQUEST,
                    ErrorCode.SELF_FRIEND_REQUEST.getDefaultMessage());
        }

        // Check existing friendship in both directions
        Optional<Friendship> existing = friendshipRepository.findBetweenUsers(requesterId, receiver.getId());
        if (existing.isPresent()) {
            Friendship f = existing.get();
            if (f.getStatus() == FriendshipStatus.ACCEPTED) {
                throw new FriendshipException(ErrorCode.ALREADY_FRIENDS,
                        ErrorCode.ALREADY_FRIENDS.getDefaultMessage());
            }
            if (f.getStatus() == FriendshipStatus.PENDING) {
                throw new FriendshipException(ErrorCode.REQUEST_ALREADY_SENT,
                        ErrorCode.REQUEST_ALREADY_SENT.getDefaultMessage());
            }
            if (f.getStatus() == FriendshipStatus.BLOCKED) {
                throw new FriendshipException(ErrorCode.USER_BLOCKED,
                        ErrorCode.USER_BLOCKED.getDefaultMessage());
            }
        }

        Friendship friendship = new Friendship();
        friendship.setRequester(requester);
        friendship.setReceiver(receiver);
        friendship.setStatus(FriendshipStatus.PENDING);
        friendship = friendshipRepository.save(friendship);

        // Notify receiver
        notificationService.createNotification(
                receiver.getId(),
                NotificationType.FRIEND_ACTIVITY,
                "Arkadaşlık İsteği",
                requester.getDisplayName() != null ? requester.getDisplayName() : requester.getUsername()
                        + " size arkadaşlık isteği gönderdi.",
                Map.of("requesterId", requesterId, "friendshipId", friendship.getId())
        );

        log.info("Arkadaşlık isteği gönderildi: {} → {}", requester.getUsername(), receiver.getUsername());
        return toRequestResponse(friendship);
    }

    @Transactional
    public FriendRequestResponse acceptRequest(Long friendshipId, Long currentUserId) {
        Friendship friendship = friendshipRepository.findById(friendshipId)
                .orElseThrow(() -> new FriendshipException(ErrorCode.FRIENDSHIP_NOT_FOUND,
                        ErrorCode.FRIENDSHIP_NOT_FOUND.getDefaultMessage()));

        // Only receiver can accept
        if (!friendship.getReceiver().getId().equals(currentUserId)) {
            throw new FriendshipException(ErrorCode.FORBIDDEN,
                    "Sadece alıcı arkadaşlık isteğini kabul edebilir.");
        }

        if (friendship.getStatus() != FriendshipStatus.PENDING) {
            throw new FriendshipException(ErrorCode.BAD_REQUEST,
                    "Bu istek zaten işlenmiş.");
        }

        friendship.setStatus(FriendshipStatus.ACCEPTED);
        friendship = friendshipRepository.save(friendship);

        // Notify requester
        User receiver = friendship.getReceiver();
        notificationService.createNotification(
                friendship.getRequester().getId(),
                NotificationType.FRIEND_ACTIVITY,
                "Arkadaşlık Kabul Edildi",
                (receiver.getDisplayName() != null ? receiver.getDisplayName() : receiver.getUsername())
                        + " arkadaşlık isteğinizi kabul etti.",
                Map.of("friendshipId", friendshipId)
        );

        log.info("Arkadaşlık isteği kabul edildi: friendshipId={}", friendshipId);
        return toRequestResponse(friendship);
    }

    @Transactional
    public void rejectRequest(Long friendshipId, Long currentUserId) {
        Friendship friendship = friendshipRepository.findById(friendshipId)
                .orElseThrow(() -> new FriendshipException(ErrorCode.FRIENDSHIP_NOT_FOUND,
                        ErrorCode.FRIENDSHIP_NOT_FOUND.getDefaultMessage()));

        // Only receiver can reject
        if (!friendship.getReceiver().getId().equals(currentUserId)) {
            throw new FriendshipException(ErrorCode.FORBIDDEN,
                    "Sadece alıcı arkadaşlık isteğini reddedebilir.");
        }

        friendshipRepository.delete(friendship);
        log.info("Arkadaşlık isteği reddedildi: friendshipId={}", friendshipId);
    }

    @Transactional
    public void removeFriend(Long friendshipId, Long currentUserId) {
        Friendship friendship = friendshipRepository.findById(friendshipId)
                .orElseThrow(() -> new FriendshipException(ErrorCode.FRIENDSHIP_NOT_FOUND,
                        ErrorCode.FRIENDSHIP_NOT_FOUND.getDefaultMessage()));

        // Either party can remove
        if (!friendship.getRequester().getId().equals(currentUserId)
                && !friendship.getReceiver().getId().equals(currentUserId)) {
            throw new FriendshipException(ErrorCode.FORBIDDEN,
                    "Bu arkadaşlığı kaldırma yetkiniz yok.");
        }

        friendshipRepository.delete(friendship);
        log.info("Arkadaşlık kaldırıldı: friendshipId={}", friendshipId);
    }

    @Transactional
    public void blockUser(Long friendshipId, Long currentUserId) {
        Friendship friendship = friendshipRepository.findById(friendshipId)
                .orElseThrow(() -> new FriendshipException(ErrorCode.FRIENDSHIP_NOT_FOUND,
                        ErrorCode.FRIENDSHIP_NOT_FOUND.getDefaultMessage()));

        if (!friendship.getRequester().getId().equals(currentUserId)
                && !friendship.getReceiver().getId().equals(currentUserId)) {
            throw new FriendshipException(ErrorCode.FORBIDDEN,
                    "Bu işlem için yetkiniz yok.");
        }

        friendship.setStatus(FriendshipStatus.BLOCKED);
        friendshipRepository.save(friendship);
        log.info("Kullanıcı engellendi: friendshipId={}, blockedBy={}", friendshipId, currentUserId);
    }

    @Transactional(readOnly = true)
    public List<FriendResponse> getFriends(Long userId) {
        List<Friendship> friendships = friendshipRepository.findAcceptedFriends(userId);
        return friendships.stream().map(f -> {
            User friend = f.getRequester().getId().equals(userId) ? f.getReceiver() : f.getRequester();
            return new FriendResponse(
                    friend.getId(),
                    friend.getUsername(),
                    friend.getDisplayName(),
                    friend.getAvatarUrl(),
                    f.getCreatedAt()
            );
        }).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Map<String, List<FriendRequestResponse>> getPendingRequests(Long userId) {
        List<FriendRequestResponse> incoming = friendshipRepository.findPendingIncoming(userId)
                .stream().map(this::toRequestResponse).collect(Collectors.toList());
        List<FriendRequestResponse> outgoing = friendshipRepository.findPendingOutgoing(userId)
                .stream().map(this::toRequestResponse).collect(Collectors.toList());
        return Map.of("incoming", incoming, "outgoing", outgoing);
    }

    @Transactional(readOnly = true)
    public FriendshipStatus getFriendStatus(Long userId, Long otherUserId) {
        return friendshipRepository.findBetweenUsers(userId, otherUserId)
                .map(Friendship::getStatus)
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public boolean areFriends(Long userId1, Long userId2) {
        return friendshipRepository.areFriends(userId1, userId2);
    }

    @Transactional(readOnly = true)
    public List<Long> getFriendIds(Long userId) {
        return friendshipRepository.findFriendIds(userId);
    }

    private FriendRequestResponse toRequestResponse(Friendship f) {
        FriendRequestResponse r = new FriendRequestResponse();
        r.setId(f.getId());
        r.setRequesterId(f.getRequester().getId());
        r.setRequesterUsername(f.getRequester().getUsername());
        r.setRequesterDisplayName(f.getRequester().getDisplayName());
        r.setRequesterAvatarUrl(f.getRequester().getAvatarUrl());
        r.setReceiverId(f.getReceiver().getId());
        r.setReceiverUsername(f.getReceiver().getUsername());
        r.setReceiverDisplayName(f.getReceiver().getDisplayName());
        r.setReceiverAvatarUrl(f.getReceiver().getAvatarUrl());
        r.setStatus(f.getStatus());
        r.setCreatedAt(f.getCreatedAt());
        return r;
    }
}

