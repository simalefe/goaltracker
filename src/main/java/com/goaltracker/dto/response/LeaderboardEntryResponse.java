package com.goaltracker.dto.response;

import java.math.BigDecimal;

public class LeaderboardEntryResponse {
    private int rank;
    private Long userId;
    private String username;
    private String displayName;
    private String avatarUrl;
    private BigDecimal completionPct;
    private int currentStreak;
    private String goalTitle;
    private boolean isCurrentUser;

    public LeaderboardEntryResponse() {}

    // --- Getters & Setters ---
    public int getRank() { return rank; }
    public void setRank(int rank) { this.rank = rank; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }

    public BigDecimal getCompletionPct() { return completionPct; }
    public void setCompletionPct(BigDecimal completionPct) { this.completionPct = completionPct; }

    public int getCurrentStreak() { return currentStreak; }
    public void setCurrentStreak(int currentStreak) { this.currentStreak = currentStreak; }

    public String getGoalTitle() { return goalTitle; }
    public void setGoalTitle(String goalTitle) { this.goalTitle = goalTitle; }

    public boolean isCurrentUser() { return isCurrentUser; }
    public void setCurrentUser(boolean currentUser) { isCurrentUser = currentUser; }
}

