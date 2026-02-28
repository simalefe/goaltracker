package com.goaltracker.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.goaltracker.model.enums.FriendshipStatus;
import java.time.Instant;

public class FriendRequestResponse {
    private Long id;
    private Long requesterId;
    private String requesterUsername;
    private String requesterDisplayName;
    private String requesterAvatarUrl;
    private Long receiverId;
    private String receiverUsername;
    private String receiverDisplayName;
    private String receiverAvatarUrl;
    private FriendshipStatus status;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
    private Instant createdAt;

    public FriendRequestResponse() {}

    // --- Getters & Setters ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getRequesterId() { return requesterId; }
    public void setRequesterId(Long requesterId) { this.requesterId = requesterId; }

    public String getRequesterUsername() { return requesterUsername; }
    public void setRequesterUsername(String requesterUsername) { this.requesterUsername = requesterUsername; }

    public String getRequesterDisplayName() { return requesterDisplayName; }
    public void setRequesterDisplayName(String requesterDisplayName) { this.requesterDisplayName = requesterDisplayName; }

    public String getRequesterAvatarUrl() { return requesterAvatarUrl; }
    public void setRequesterAvatarUrl(String requesterAvatarUrl) { this.requesterAvatarUrl = requesterAvatarUrl; }

    public Long getReceiverId() { return receiverId; }
    public void setReceiverId(Long receiverId) { this.receiverId = receiverId; }

    public String getReceiverUsername() { return receiverUsername; }
    public void setReceiverUsername(String receiverUsername) { this.receiverUsername = receiverUsername; }

    public String getReceiverDisplayName() { return receiverDisplayName; }
    public void setReceiverDisplayName(String receiverDisplayName) { this.receiverDisplayName = receiverDisplayName; }

    public String getReceiverAvatarUrl() { return receiverAvatarUrl; }
    public void setReceiverAvatarUrl(String receiverAvatarUrl) { this.receiverAvatarUrl = receiverAvatarUrl; }

    public FriendshipStatus getStatus() { return status; }
    public void setStatus(FriendshipStatus status) { this.status = status; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}

