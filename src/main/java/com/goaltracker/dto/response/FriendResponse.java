package com.goaltracker.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.Instant;

public class FriendResponse {
    private Long userId;
    private String username;
    private String displayName;
    private String avatarUrl;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
    private Instant friendsSince;

    public FriendResponse() {}

    public FriendResponse(Long userId, String username, String displayName, String avatarUrl, Instant friendsSince) {
        this.userId = userId;
        this.username = username;
        this.displayName = displayName;
        this.avatarUrl = avatarUrl;
        this.friendsSince = friendsSince;
    }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }

    public Instant getFriendsSince() { return friendsSince; }
    public void setFriendsSince(Instant friendsSince) { this.friendsSince = friendsSince; }
}

