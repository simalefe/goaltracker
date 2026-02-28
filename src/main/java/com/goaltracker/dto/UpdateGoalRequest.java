package com.goaltracker.dto;

import com.goaltracker.model.enums.GoalCategory;
import com.goaltracker.model.enums.GoalFrequency;
import com.goaltracker.model.enums.GoalType;
import com.goaltracker.validation.EndDateAfterStartDate;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@EndDateAfterStartDate
public class UpdateGoalRequest {

    @Size(max = 200, message = "Başlık en fazla 200 karakter olabilir.")
    private String title;

    @Size(max = 1000, message = "Açıklama en fazla 1000 karakter olabilir.")
    private String description;

    @Size(max = 50, message = "Birim en fazla 50 karakter olabilir.")
    private String unit;

    private GoalType goalType;

    private GoalFrequency frequency;

    @DecimalMin(value = "0.01", inclusive = true, message = "Hedef değeri sıfırdan büyük olmalıdır.")
    private BigDecimal targetValue;

    private LocalDate startDate;

    private LocalDate endDate;

    private GoalCategory category;

    @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "Geçersiz renk formatı. #RRGGBB bekleniyor.")
    private String color;

    // getters & setters
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }

    public GoalType getGoalType() { return goalType; }
    public void setGoalType(GoalType goalType) { this.goalType = goalType; }

    public GoalFrequency getFrequency() { return frequency; }
    public void setFrequency(GoalFrequency frequency) { this.frequency = frequency; }

    public BigDecimal getTargetValue() { return targetValue; }
    public void setTargetValue(BigDecimal targetValue) { this.targetValue = targetValue; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public GoalCategory getCategory() { return category; }
    public void setCategory(GoalCategory category) { this.category = category; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }
}

