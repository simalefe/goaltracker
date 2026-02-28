package com.goaltracker.controller;

import com.goaltracker.dto.response.BadgeResponse;
import com.goaltracker.dto.response.UserBadgeResponse;
import com.goaltracker.dto.response.UserResponse;
import com.goaltracker.dto.response.UserStatsResponse;
import com.goaltracker.model.Badge;
import com.goaltracker.model.enums.GoalStatus;
import com.goaltracker.repository.BadgeRepository;
import com.goaltracker.repository.GoalEntryRepository;
import com.goaltracker.repository.GoalRepository;
import com.goaltracker.service.BadgeService;
import com.goaltracker.service.StreakService;
import com.goaltracker.service.UserService;
import com.goaltracker.util.SecurityUtils;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/profile")
public class ProfileController {

    private final UserService userService;
    private final BadgeService badgeService;
    private final StreakService streakService;
    private final SecurityUtils securityUtils;
    private final GoalEntryRepository goalEntryRepository;
    private final GoalRepository goalRepository;
    private final BadgeRepository badgeRepository;

    public ProfileController(UserService userService,
                             BadgeService badgeService,
                             StreakService streakService,
                             SecurityUtils securityUtils,
                             GoalEntryRepository goalEntryRepository,
                             GoalRepository goalRepository,
                             BadgeRepository badgeRepository) {
        this.userService = userService;
        this.badgeService = badgeService;
        this.streakService = streakService;
        this.securityUtils = securityUtils;
        this.goalEntryRepository = goalEntryRepository;
        this.goalRepository = goalRepository;
        this.badgeRepository = badgeRepository;
    }

    @GetMapping
    public String profile(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        Long userId = securityUtils.getCurrentUserId();
        UserResponse user = userService.getProfile(userDetails.getUsername());

        // User Stats
        long totalEntries = goalEntryRepository.countByGoal_User_Id(userId);
        long completedGoals = goalRepository.countByUserIdAndStatus(userId, GoalStatus.COMPLETED);
        int totalStreakDays = streakService.getTotalStreakDays(userId);
        long earnedBadgeCount = badgeService.getUserBadgeCount(userId);
        UserStatsResponse stats = new UserStatsResponse(totalEntries, completedGoals, totalStreakDays, earnedBadgeCount);

        // User Badges (earned)
        List<UserBadgeResponse> earnedBadges = badgeService.getUserBadges(userId);

        // All badges (for locked display)
        List<Badge> allBadges = badgeRepository.findAll();
        Set<String> earnedCodes = earnedBadges.stream()
                .map(ub -> ub.badge().code())
                .collect(Collectors.toSet());

        List<BadgeResponse> lockedBadges = allBadges.stream()
                .filter(b -> !earnedCodes.contains(b.getCode()))
                .map(b -> new BadgeResponse(b.getId(), b.getCode(), b.getName(),
                        b.getDescription(), b.getIcon(), b.getConditionType(), b.getConditionValue()))
                .toList();

        model.addAttribute("user", user);
        model.addAttribute("stats", stats);
        model.addAttribute("earnedBadges", earnedBadges);
        model.addAttribute("lockedBadges", lockedBadges);
        model.addAttribute("pageTitle", "Profil");
        model.addAttribute("activePage", "profile");
        return "profile/index";
    }
}

