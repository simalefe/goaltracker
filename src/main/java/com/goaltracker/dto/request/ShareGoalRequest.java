package com.goaltracker.dto.request;

import com.goaltracker.model.enums.SharePermission;
import jakarta.validation.constraints.NotNull;

public class ShareGoalRequest {

    @NotNull(message = "Kullanıcı ID zorunludur.")
    private Long userId;

    private SharePermission permission = SharePermission.READ;

    public ShareGoalRequest() {}

    public ShareGoalRequest(Long userId, SharePermission permission) {
        this.userId = userId;
        this.permission = permission;
    }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public SharePermission getPermission() { return permission; }
    public void setPermission(SharePermission permission) { this.permission = permission; }
}
