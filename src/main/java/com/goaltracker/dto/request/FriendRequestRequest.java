package com.goaltracker.dto.request;

import jakarta.validation.constraints.NotBlank;

public class FriendRequestRequest {

    @NotBlank(message = "Kullanıcı adı zorunludur.")
    private String receiverUsername;

    public FriendRequestRequest() {}

    public FriendRequestRequest(String receiverUsername) {
        this.receiverUsername = receiverUsername;
    }

    public String getReceiverUsername() { return receiverUsername; }
    public void setReceiverUsername(String receiverUsername) { this.receiverUsername = receiverUsername; }
}

