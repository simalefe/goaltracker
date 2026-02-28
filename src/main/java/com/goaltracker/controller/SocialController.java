package com.goaltracker.controller;

import com.goaltracker.dto.response.ActivityFeedItemResponse;
import com.goaltracker.dto.response.FriendRequestResponse;
import com.goaltracker.dto.response.FriendResponse;
import com.goaltracker.dto.response.LeaderboardEntryResponse;
import com.goaltracker.model.enums.GoalCategory;
import com.goaltracker.service.ActivityFeedService;
import com.goaltracker.service.FriendshipService;
import com.goaltracker.service.LeaderboardService;
import com.goaltracker.service.UserService;
import com.goaltracker.util.SecurityUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/social")
public class SocialController {

    private final FriendshipService friendshipService;
    private final LeaderboardService leaderboardService;
    private final ActivityFeedService activityFeedService;
    private final UserService userService;
    private final SecurityUtils securityUtils;

    public SocialController(FriendshipService friendshipService,
                            LeaderboardService leaderboardService,
                            ActivityFeedService activityFeedService,
                            UserService userService,
                            SecurityUtils securityUtils) {
        this.friendshipService = friendshipService;
        this.leaderboardService = leaderboardService;
        this.activityFeedService = activityFeedService;
        this.userService = userService;
        this.securityUtils = securityUtils;
    }

    @GetMapping
    public String socialIndex(
            @RequestParam(value = "tab", required = false, defaultValue = "friends") String tab,
            @RequestParam(value = "category", required = false) GoalCategory category,
            Model model) {
        Long userId = securityUtils.getCurrentUserId();

        // Friends tab
        List<FriendResponse> friends = friendshipService.getFriends(userId);
        Map<String, List<FriendRequestResponse>> pending = friendshipService.getPendingRequests(userId);
        model.addAttribute("friends", friends);
        model.addAttribute("pendingIncoming", pending.get("incoming"));
        model.addAttribute("pendingOutgoing", pending.get("outgoing"));

        // Leaderboard tab
        List<LeaderboardEntryResponse> leaderboard = leaderboardService.getLeaderboard(userId, category);
        model.addAttribute("leaderboard", leaderboard);
        model.addAttribute("categories", GoalCategory.values());
        model.addAttribute("selectedCategory", category);

        // Activity Feed tab
        List<ActivityFeedItemResponse> feedItems = activityFeedService.getFeed(userId);
        model.addAttribute("feedItems", feedItems);

        model.addAttribute("activeTab", tab);
        model.addAttribute("activePage", "social");
        model.addAttribute("pageTitle", "Sosyal");
        return "social/index";
    }

    @PostMapping("/friends/request")
    public String sendFriendRequest(@RequestParam("receiverUsername") String receiverUsername,
                                    RedirectAttributes redirectAttributes) {
        Long userId = securityUtils.getCurrentUserId();
        try {
            friendshipService.sendRequest(userId, receiverUsername);
            redirectAttributes.addFlashAttribute("successMessage", "Arkadaşlık isteği gönderildi.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/social?tab=friends";
    }

    @PostMapping("/friends/{id}/accept")
    public String acceptFriendRequest(@PathVariable("id") Long id,
                                      RedirectAttributes redirectAttributes) {
        Long userId = securityUtils.getCurrentUserId();
        try {
            friendshipService.acceptRequest(id, userId);
            redirectAttributes.addFlashAttribute("successMessage", "Arkadaşlık isteği kabul edildi.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/social?tab=friends";
    }

    @PostMapping("/friends/{id}/reject")
    public String rejectFriendRequest(@PathVariable("id") Long id,
                                      RedirectAttributes redirectAttributes) {
        Long userId = securityUtils.getCurrentUserId();
        try {
            friendshipService.rejectRequest(id, userId);
            redirectAttributes.addFlashAttribute("successMessage", "Arkadaşlık isteği reddedildi.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/social?tab=friends";
    }

    @PostMapping("/friends/{id}/remove")
    public String removeFriend(@PathVariable("id") Long id,
                               RedirectAttributes redirectAttributes) {
        Long userId = securityUtils.getCurrentUserId();
        try {
            friendshipService.removeFriend(id, userId);
            redirectAttributes.addFlashAttribute("successMessage", "Arkadaş kaldırıldı.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/social?tab=friends";
    }

    @GetMapping("/search")
    public String searchUsers(@RequestParam("query") String query, Model model) {
        Long userId = securityUtils.getCurrentUserId();
        model.addAttribute("searchResults", userService.searchUsers(query));
        model.addAttribute("activePage", "social");
        model.addAttribute("pageTitle", "Kullanıcı Ara");
        return "social/search";
    }
}

