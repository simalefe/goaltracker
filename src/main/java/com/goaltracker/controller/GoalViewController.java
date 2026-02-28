package com.goaltracker.controller;

import com.goaltracker.dto.CreateGoalRequest;
import com.goaltracker.dto.GoalResponse;
import com.goaltracker.dto.GoalSummaryResponse;
import com.goaltracker.dto.GoalStatsResponse;
import com.goaltracker.dto.GoalEntryResponse;
import com.goaltracker.dto.CreateEntryRequest;
import com.goaltracker.dto.UpdateGoalRequest;
import com.goaltracker.model.enums.GoalCategory;
import com.goaltracker.model.enums.GoalFrequency;
import com.goaltracker.model.enums.GoalStatus;
import com.goaltracker.model.enums.GoalType;
import com.goaltracker.service.GoalEntryService;
import com.goaltracker.service.GoalService;
import com.goaltracker.util.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/goals")
public class GoalViewController {

    private final GoalService goalService;
    private final GoalEntryService goalEntryService;
    private final SecurityUtils securityUtils;

    public GoalViewController(GoalService goalService, GoalEntryService goalEntryService, SecurityUtils securityUtils) {
        this.goalService = goalService;
        this.goalEntryService = goalEntryService;
        this.securityUtils = securityUtils;
    }

    @GetMapping
    public String listGoals(
            @RequestParam(value = "status", required = false) GoalStatus status,
            @RequestParam(value = "category", required = false) GoalCategory category,
            @RequestParam(value = "goalType", required = false) GoalType goalType,
            @RequestParam(value = "query", required = false) String query,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "12") int size,
            @RequestParam(value = "sort", defaultValue = "createdAt,desc") String sort,
            Model model) {

        Long userId = securityUtils.getCurrentUserId();

        // Parse sort parameter
        String[] sortParts = sort.split(",");
        String sortField = sortParts[0];
        Sort.Direction direction = sortParts.length > 1 && sortParts[1].equalsIgnoreCase("asc")
                ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, Math.min(size, 50), Sort.by(direction, sortField));

        Page<GoalSummaryResponse> goals = goalService.getGoals(userId, status, category, goalType, query, pageable);

        model.addAttribute("goals", goals);
        model.addAttribute("currentStatus", status);
        model.addAttribute("currentCategory", category);
        model.addAttribute("currentGoalType", goalType);
        model.addAttribute("currentQuery", query);
        model.addAttribute("currentSort", sort);
        model.addAttribute("statuses", GoalStatus.values());
        model.addAttribute("categories", GoalCategory.values());
        model.addAttribute("goalTypes", GoalType.values());
        model.addAttribute("pageTitle", "Hedefler");
        model.addAttribute("activePage", "goals");
        return "goals/list";
    }

    @GetMapping("/new")
    public String createGoalForm(Model model) {
        model.addAttribute("goalForm", new CreateGoalRequest());
        model.addAttribute("categories", GoalCategory.values());
        model.addAttribute("goalTypes", GoalType.values());
        model.addAttribute("frequencies", GoalFrequency.values());
        model.addAttribute("pageTitle", "Yeni Hedef");
        model.addAttribute("activePage", "goals");
        return "goals/create";
    }

    @PostMapping
    public String createGoal(@Valid @ModelAttribute("goalForm") CreateGoalRequest goalForm,
                             BindingResult bindingResult,
                             Model model,
                             RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("categories", GoalCategory.values());
            model.addAttribute("goalTypes", GoalType.values());
            model.addAttribute("frequencies", GoalFrequency.values());
            model.addAttribute("pageTitle", "Yeni Hedef");
            model.addAttribute("activePage", "goals");
            return "goals/create";
        }

        Long userId = securityUtils.getCurrentUserId();
        GoalResponse created = goalService.createGoal(goalForm, userId);
        redirectAttributes.addFlashAttribute("successMessage", "Hedef başarıyla oluşturuldu.");
        return "redirect:/goals/" + created.getId();
    }

    @GetMapping("/{id}")
    public String goalDetail(@PathVariable("id") Long id, Model model) {
        Long userId = securityUtils.getCurrentUserId();
        GoalResponse goal = goalService.getGoal(id, userId);
        GoalStatsResponse stats = goalEntryService.getStats(id, userId);
        java.util.List<GoalEntryResponse> entries = goalEntryService.getEntries(id, userId);

        model.addAttribute("goal", goal);
        model.addAttribute("stats", stats);
        model.addAttribute("entries", entries);
        model.addAttribute("entryForm", new CreateEntryRequest(null, null, null));
        model.addAttribute("statuses", GoalStatus.values());
        model.addAttribute("pageTitle", goal.getTitle());
        model.addAttribute("activePage", "goals");
        return "goals/detail";
    }

    @GetMapping("/{id}/edit")
    public String editGoalForm(@PathVariable("id") Long id, Model model) {
        Long userId = securityUtils.getCurrentUserId();
        GoalResponse goal = goalService.getGoal(id, userId);

        // Pre-fill the update form
        UpdateGoalRequest form = new UpdateGoalRequest();
        form.setTitle(goal.getTitle());
        form.setDescription(goal.getDescription());
        form.setUnit(goal.getUnit());
        form.setGoalType(goal.getGoalType());
        form.setFrequency(goal.getFrequency());
        form.setTargetValue(goal.getTargetValue());
        form.setStartDate(goal.getStartDate());
        form.setEndDate(goal.getEndDate());
        form.setCategory(goal.getCategory());
        form.setColor(goal.getColor());

        model.addAttribute("goalForm", form);
        model.addAttribute("goalId", id);
        model.addAttribute("categories", GoalCategory.values());
        model.addAttribute("goalTypes", GoalType.values());
        model.addAttribute("frequencies", GoalFrequency.values());
        model.addAttribute("pageTitle", "Hedef Düzenle");
        model.addAttribute("activePage", "goals");
        return "goals/edit";
    }

    @PostMapping("/{id}/edit")
    public String updateGoal(@PathVariable("id") Long id,
                             @Valid @ModelAttribute("goalForm") UpdateGoalRequest goalForm,
                             BindingResult bindingResult,
                             Model model,
                             RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("goalId", id);
            model.addAttribute("categories", GoalCategory.values());
            model.addAttribute("goalTypes", GoalType.values());
            model.addAttribute("frequencies", GoalFrequency.values());
            model.addAttribute("pageTitle", "Hedef Düzenle");
            model.addAttribute("activePage", "goals");
            return "goals/edit";
        }

        Long userId = securityUtils.getCurrentUserId();
        goalService.updateGoal(id, goalForm, userId);
        redirectAttributes.addFlashAttribute("successMessage", "Hedef başarıyla güncellendi.");
        return "redirect:/goals/" + id;
    }

    @PostMapping("/{id}/delete")
    public String deleteGoal(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        Long userId = securityUtils.getCurrentUserId();
        goalService.deleteGoal(id, userId);
        redirectAttributes.addFlashAttribute("successMessage", "Hedef başarıyla silindi.");
        return "redirect:/goals";
    }

    @PostMapping("/{id}/status")
    public String updateStatus(@PathVariable("id") Long id,
                               @RequestParam("newStatus") GoalStatus newStatus,
                               RedirectAttributes redirectAttributes) {
        Long userId = securityUtils.getCurrentUserId();
        try {
            goalService.updateStatus(id, userId, newStatus);
            redirectAttributes.addFlashAttribute("successMessage", "Hedef durumu güncellendi.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/goals/" + id;
    }

    // ---- Entry MVC Endpoints ----

    @PostMapping("/{goalId}/entries")
    public String createEntry(@PathVariable("goalId") Long goalId,
                              @RequestParam("entryDate") java.time.LocalDate entryDate,
                              @RequestParam("actualValue") java.math.BigDecimal actualValue,
                              @RequestParam(value = "note", required = false) String note,
                              RedirectAttributes redirectAttributes) {
        Long userId = securityUtils.getCurrentUserId();
        try {
            CreateEntryRequest request = new CreateEntryRequest(entryDate, actualValue, note);
            goalEntryService.createEntry(goalId, userId, request);
            redirectAttributes.addFlashAttribute("successMessage", "İlerleme kaydedildi.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/goals/" + goalId;
    }

    @PostMapping("/{goalId}/entries/{entryId}/edit")
    public String updateEntry(@PathVariable("goalId") Long goalId,
                              @PathVariable("entryId") Long entryId,
                              @RequestParam(value = "actualValue", required = false) java.math.BigDecimal actualValue,
                              @RequestParam(value = "note", required = false) String note,
                              RedirectAttributes redirectAttributes) {
        Long userId = securityUtils.getCurrentUserId();
        try {
            com.goaltracker.dto.UpdateEntryRequest request = new com.goaltracker.dto.UpdateEntryRequest(actualValue, note);
            goalEntryService.updateEntry(entryId, userId, request);
            redirectAttributes.addFlashAttribute("successMessage", "Kayıt güncellendi.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/goals/" + goalId;
    }

    @PostMapping("/{goalId}/entries/{entryId}/delete")
    public String deleteEntry(@PathVariable("goalId") Long goalId,
                              @PathVariable("entryId") Long entryId,
                              RedirectAttributes redirectAttributes) {
        Long userId = securityUtils.getCurrentUserId();
        try {
            goalEntryService.deleteEntry(entryId, userId);
            redirectAttributes.addFlashAttribute("successMessage", "Kayıt silindi.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/goals/" + goalId;
    }
}

