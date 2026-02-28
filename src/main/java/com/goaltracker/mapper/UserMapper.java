package com.goaltracker.mapper;

import com.goaltracker.dto.response.UserResponse;
import com.goaltracker.model.User;

public final class UserMapper {

    private UserMapper() {}

    public static UserResponse toResponse(User user) {
        return new UserResponse(
            user.getId(),
            user.getEmail(),
            user.getUsername(),
            user.getDisplayName(),
            user.getAvatarUrl(),
            user.getTimezone(),
            user.getRole().name(),
            user.isEmailVerified(),
            user.getCreatedAt()
        );
    }
}

