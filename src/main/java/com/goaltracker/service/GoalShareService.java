package com.goaltracker.service;

import com.goaltracker.dto.GoalResponse;
import com.goaltracker.dto.response.FriendResponse;
import com.goaltracker.exception.ErrorCode;
import com.goaltracker.exception.FriendshipException;
import com.goaltracker.exception.GoalAccessDeniedException;
import com.goaltracker.exception.GoalNotFoundException;
import com.goaltracker.mapper.GoalMapper;
import com.goaltracker.model.Goal;
import com.goaltracker.model.GoalShare;
import com.goaltracker.model.User;
import com.goaltracker.model.enums.NotificationType;
import com.goaltracker.model.enums.SharePermission;
import com.goaltracker.repository.GoalRepository;
import com.goaltracker.repository.GoalShareRepository;
import com.goaltracker.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class GoalShareService {

    private static final Logger log = LoggerFactory.getLogger(GoalShareService.class);

    private final GoalShareRepository goalShareRepository;
    private final GoalRepository goalRepository;
    private final UserRepository userRepository;
    private final FriendshipService friendshipService;
    private final NotificationService notificationService;

    public GoalShareService(GoalShareRepository goalShareRepository,
                            GoalRepository goalRepository,
                            UserRepository userRepository,
                            FriendshipService friendshipService,
                            NotificationService notificationService) {
        this.goalShareRepository = goalShareRepository;
        this.goalRepository = goalRepository;
        this.userRepository = userRepository;
        this.friendshipService = friendshipService;
        this.notificationService = notificationService;
    }

    @Transactional
    public void shareGoal(Long goalId, Long ownerUserId, Long targetUserId, SharePermission permission) {
        Goal goal = goalRepository.findById(goalId)
                .orElseThrow(() -> new GoalNotFoundException(goalId));

        // Ownership check
        if (!goal.getUser().getId().equals(ownerUserId)) {
            throw new GoalAccessDeniedException(goalId);
        }

        // Friends check
        if (!friendshipService.areFriends(ownerUserId, targetUserId)) {
            throw new FriendshipException(ErrorCode.NOT_FRIENDS,
                    ErrorCode.NOT_FRIENDS.getDefaultMessage());
        }

        // Duplicate share check
        if (goalShareRepository.existsByGoalIdAndSharedWithUserId(goalId, targetUserId)) {
            throw new FriendshipException(ErrorCode.SHARE_ALREADY_EXISTS,
                    ErrorCode.SHARE_ALREADY_EXISTS.getDefaultMessage());
        }

        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new RuntimeException("Kullanıcı bulunamadı: " + targetUserId));

        GoalShare share = new GoalShare();
        share.setGoal(goal);
        share.setSharedWithUser(targetUser);
        share.setPermission(permission);
        goalShareRepository.save(share);

        // Send notification
        User owner = goal.getUser();
        notificationService.createNotification(
                targetUserId,
                NotificationType.FRIEND_ACTIVITY,
                "Hedef Paylaşıldı",
                (owner.getDisplayName() != null ? owner.getDisplayName() : owner.getUsername())
                        + " sizinle bir hedef paylaştı: " + goal.getTitle(),
                Map.of("goalId", goalId, "ownerId", ownerUserId)
        );

        log.info("Hedef paylaşıldı: goalId={}, ownerUserId={}, targetUserId={}", goalId, ownerUserId, targetUserId);
    }

    @Transactional
    public void removeShare(Long goalId, Long ownerUserId, Long targetUserId) {
        Goal goal = goalRepository.findById(goalId)
                .orElseThrow(() -> new GoalNotFoundException(goalId));

        if (!goal.getUser().getId().equals(ownerUserId)) {
            throw new GoalAccessDeniedException(goalId);
        }

        goalShareRepository.deleteByGoalIdAndSharedWithUserId(goalId, targetUserId);
        log.info("Paylaşım kaldırıldı: goalId={}, targetUserId={}", goalId, targetUserId);
    }

    @Transactional(readOnly = true)
    public List<GoalResponse> getSharedGoals(Long userId) {
        return goalShareRepository.findBySharedWithUserId(userId)
                .stream()
                .map(gs -> GoalMapper.toResponse(gs.getGoal()))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<FriendResponse> getSharedWithUsers(Long goalId, Long ownerUserId) {
        Goal goal = goalRepository.findById(goalId)
                .orElseThrow(() -> new GoalNotFoundException(goalId));

        if (!goal.getUser().getId().equals(ownerUserId)) {
            throw new GoalAccessDeniedException(goalId);
        }

        return goalShareRepository.findByGoalId(goalId).stream()
                .map(gs -> {
                    User u = gs.getSharedWithUser();
                    return new FriendResponse(
                            u.getId(),
                            u.getUsername(),
                            u.getDisplayName(),
                            u.getAvatarUrl(),
                            gs.getCreatedAt()
                    );
                })
                .collect(Collectors.toList());
    }
}

