package com.goaltracker.service;

import com.goaltracker.dto.response.LeaderboardEntryResponse;
import com.goaltracker.model.Goal;
import com.goaltracker.model.User;
import com.goaltracker.model.enums.GoalCategory;
import com.goaltracker.model.enums.GoalStatus;
import com.goaltracker.repository.FriendshipRepository;
import com.goaltracker.repository.GoalRepository;
import com.goaltracker.repository.UserRepository;
import com.goaltracker.util.GoalCalculator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class LeaderboardService {

    private final FriendshipRepository friendshipRepository;
    private final GoalRepository goalRepository;
    private final UserRepository userRepository;
    private final GoalCalculator goalCalculator;
    private final StreakService streakService;

    public LeaderboardService(FriendshipRepository friendshipRepository,
                              GoalRepository goalRepository,
                              UserRepository userRepository,
                              GoalCalculator goalCalculator,
                              StreakService streakService) {
        this.friendshipRepository = friendshipRepository;
        this.goalRepository = goalRepository;
        this.userRepository = userRepository;
        this.goalCalculator = goalCalculator;
        this.streakService = streakService;
    }

    @Transactional(readOnly = true)
    public List<LeaderboardEntryResponse> getLeaderboard(Long userId, GoalCategory category) {
        // 1. Get friend IDs + self
        List<Long> friendIds = friendshipRepository.findFriendIds(userId);
        List<Long> participantIds = new ArrayList<>(friendIds);
        participantIds.add(userId);

        // 2. For each participant, find their best goal in the category
        List<LeaderboardEntryResponse> entries = new ArrayList<>();

        for (Long participantId : participantIds) {
            List<Goal> goals = goalRepository.findByUserIdAndStatus(participantId, GoalStatus.ACTIVE);

            // Filter by category if specified
            List<Goal> filtered = goals.stream()
                    .filter(g -> category == null || g.getCategory() == category)
                    .toList();

            if (filtered.isEmpty()) continue;

            // Find the goal with highest completion pct
            Goal bestGoal = null;
            BigDecimal bestPct = BigDecimal.ZERO;
            int bestStreak = 0;

            for (Goal g : filtered) {
                BigDecimal pct = goalCalculator.calculateCompletionPct(g);
                int streak = streakService.getStreakForGoal(g.getId());

                if (pct.compareTo(bestPct) > 0 || (pct.compareTo(bestPct) == 0 && streak > bestStreak)) {
                    bestGoal = g;
                    bestPct = pct;
                    bestStreak = streak;
                }
            }

            if (bestGoal != null) {
                User user = userRepository.findById(participantId).orElse(null);
                if (user == null) continue;

                LeaderboardEntryResponse entry = new LeaderboardEntryResponse();
                entry.setUserId(participantId);
                entry.setUsername(user.getUsername());
                entry.setDisplayName(user.getDisplayName());
                entry.setAvatarUrl(user.getAvatarUrl());
                entry.setCompletionPct(bestPct);
                entry.setCurrentStreak(bestStreak);
                entry.setGoalTitle(bestGoal.getTitle());
                entry.setCurrentUser(participantId.equals(userId));
                entries.add(entry);
            }
        }

        // 3. Sort by completionPct DESC, currentStreak DESC
        entries.sort(Comparator
                .comparing(LeaderboardEntryResponse::getCompletionPct).reversed()
                .thenComparing(Comparator.comparingInt(LeaderboardEntryResponse::getCurrentStreak).reversed()));

        // 4. Assign ranks
        for (int i = 0; i < entries.size(); i++) {
            entries.get(i).setRank(i + 1);
        }

        return entries;
    }
}

