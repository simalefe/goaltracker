package com.goaltracker.service;

import com.goaltracker.dto.response.ActivityFeedItemResponse;
import com.goaltracker.model.GoalEntry;
import com.goaltracker.model.User;
import com.goaltracker.model.UserBadge;
import com.goaltracker.model.Goal;
import com.goaltracker.model.enums.GoalStatus;
import com.goaltracker.repository.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class ActivityFeedService {

    private static final int FEED_LIMIT = 20;
    private static final int DAYS_BACK = 7;

    private final FriendshipRepository friendshipRepository;
    private final GoalEntryRepository goalEntryRepository;
    private final GoalRepository goalRepository;
    private final UserBadgeRepository userBadgeRepository;
    private final UserRepository userRepository;

    public ActivityFeedService(FriendshipRepository friendshipRepository,
                               GoalEntryRepository goalEntryRepository,
                               GoalRepository goalRepository,
                               UserBadgeRepository userBadgeRepository,
                               UserRepository userRepository) {
        this.friendshipRepository = friendshipRepository;
        this.goalEntryRepository = goalEntryRepository;
        this.goalRepository = goalRepository;
        this.userBadgeRepository = userBadgeRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<ActivityFeedItemResponse> getFeed(Long userId) {
        List<Long> friendIds = friendshipRepository.findFriendIds(userId);
        if (friendIds.isEmpty()) {
            return List.of();
        }

        Instant since = Instant.now().minus(DAYS_BACK, ChronoUnit.DAYS);
        List<ActivityFeedItemResponse> feed = new ArrayList<>();

        for (Long friendId : friendIds) {
            User friend = userRepository.findById(friendId).orElse(null);
            if (friend == null) continue;

            // Entry activities (last 7 days)
            List<GoalEntry> entries = goalEntryRepository.findRecentByUserId(friendId,
                    PageRequest.of(0, FEED_LIMIT));
            for (GoalEntry entry : entries) {
                if (entry.getCreatedAt().isBefore(since)) continue;
                ActivityFeedItemResponse item = new ActivityFeedItemResponse();
                item.setType("ENTRY");
                item.setUserId(friendId);
                item.setUsername(friend.getUsername());
                item.setDisplayName(friend.getDisplayName());
                item.setAvatarUrl(friend.getAvatarUrl());
                item.setGoalTitle(entry.getGoal().getTitle());
                item.setValue(entry.getActualValue().toPlainString());
                item.setUnit(entry.getGoal().getUnit());
                item.setTimestamp(entry.getCreatedAt());
                feed.add(item);
            }

            // Goal completions (last 7 days)
            List<Goal> completedGoals = goalRepository.findByUserIdAndStatusOrderByCreatedAtDesc(
                    friendId, GoalStatus.COMPLETED);
            for (Goal goal : completedGoals) {
                if (goal.getUpdatedAt().isBefore(since)) continue;
                ActivityFeedItemResponse item = new ActivityFeedItemResponse();
                item.setType("GOAL_COMPLETED");
                item.setUserId(friendId);
                item.setUsername(friend.getUsername());
                item.setDisplayName(friend.getDisplayName());
                item.setAvatarUrl(friend.getAvatarUrl());
                item.setGoalTitle(goal.getTitle());
                item.setTimestamp(goal.getUpdatedAt());
                feed.add(item);
            }

            // Badge earned (last 7 days)
            List<UserBadge> badges = userBadgeRepository.findByUserId(friendId);
            for (UserBadge ub : badges) {
                if (ub.getEarnedAt().isBefore(since)) continue;
                ActivityFeedItemResponse item = new ActivityFeedItemResponse();
                item.setType("BADGE_EARNED");
                item.setUserId(friendId);
                item.setUsername(friend.getUsername());
                item.setDisplayName(friend.getDisplayName());
                item.setAvatarUrl(friend.getAvatarUrl());
                item.setValue(ub.getBadge().getName());
                item.setTimestamp(ub.getEarnedAt());
                feed.add(item);
            }
        }

        // Sort by timestamp DESC and limit
        feed.sort(Comparator.comparing(ActivityFeedItemResponse::getTimestamp).reversed());
        return feed.size() > FEED_LIMIT ? feed.subList(0, FEED_LIMIT) : feed;
    }
}


