package com.goaltracker.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.goaltracker.model.enums.GoalCategory;
import com.goaltracker.model.enums.GoalStatus;

import java.math.BigDecimal;
import java.time.LocalDate;

public class GoalSummaryResponse {
    private Long id;
    private String title;
    private String unit;
    private GoalStatus status;
    private BigDecimal completionPct;
    private int currentStreak;
    private GoalCategory category;
    private String color;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;

    private int daysLeft;

    // getters & setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }

    public GoalStatus getStatus() { return status; }
    public void setStatus(GoalStatus status) { this.status = status; }

    public BigDecimal getCompletionPct() { return completionPct; }
    public void setCompletionPct(BigDecimal completionPct) { this.completionPct = completionPct; }

    public int getCurrentStreak() { return currentStreak; }
    public void setCurrentStreak(int currentStreak) { this.currentStreak = currentStreak; }

    public GoalCategory getCategory() { return category; }
    public void setCategory(GoalCategory category) { this.category = category; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public int getDaysLeft() { return daysLeft; }
    public void setDaysLeft(int daysLeft) { this.daysLeft = daysLeft; }
}

