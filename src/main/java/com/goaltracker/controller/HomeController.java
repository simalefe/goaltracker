package com.goaltracker.controller;

import com.goaltracker.dto.response.DashboardResponse;
import com.goaltracker.dto.response.StreakResponse;
import com.goaltracker.service.DashboardService;
import com.goaltracker.service.StreakService;
import com.goaltracker.util.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Comparator;
import java.util.List;

@Controller
public class HomeController {

    private static final Logger log = LoggerFactory.getLogger(HomeController.class);

    private final DashboardService dashboardService;
    private final StreakService streakService;
    private final SecurityUtils securityUtils;

    public HomeController(DashboardService dashboardService, StreakService streakService, SecurityUtils securityUtils) {
        this.dashboardService = dashboardService;
        this.streakService = streakService;
        this.securityUtils = securityUtils;
    }

    @GetMapping("/")
    public String index() {
        return "redirect:/dashboard";
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        Long userId = securityUtils.getCurrentUserId();

        try {
            DashboardResponse dashboard = dashboardService.getDashboard(userId);

            model.addAttribute("activeGoalCount", dashboard.activeGoalCount());
            model.addAttribute("todayEntryCount", dashboard.todayEntryCount());
            model.addAttribute("totalStreakDays", dashboard.totalStreakDays());
            model.addAttribute("goalsOnTrack", dashboard.goalsOnTrack());
            model.addAttribute("goalsBehind", dashboard.goalsBehind());
            model.addAttribute("topGoals", dashboard.topGoals());
            model.addAttribute("recentEntries", dashboard.recentEntries());

            // Streak list for dashboard — sorted by currentStreak desc
            List<StreakResponse> streaks = streakService.getUserStreaks(userId).stream()
                    .filter(s -> s.currentStreak() > 0)
                    .sorted(Comparator.comparingInt(StreakResponse::currentStreak).reversed())
                    .limit(5)
                    .toList();
            model.addAttribute("streaks", streaks);

            log.debug("Dashboard verisi yüklendi: userId={}, activeGoals={}", userId, dashboard.activeGoalCount());
        } catch (Exception e) {
            log.error("Dashboard verisi yüklenirken hata oluştu: userId={}", userId, e);
            model.addAttribute("activeGoalCount", 0);
            model.addAttribute("todayEntryCount", 0);
            model.addAttribute("totalStreakDays", 0);
            model.addAttribute("goalsOnTrack", 0);
            model.addAttribute("goalsBehind", 0);
            model.addAttribute("topGoals", List.of());
            model.addAttribute("recentEntries", List.of());
            model.addAttribute("streaks", List.of());
            model.addAttribute("errorMessage", "Dashboard verileri yüklenirken bir hata oluştu.");
        }

        model.addAttribute("pageTitle", "Dashboard");
        model.addAttribute("activePage", "dashboard");
        return "dashboard/index";
    }
}

