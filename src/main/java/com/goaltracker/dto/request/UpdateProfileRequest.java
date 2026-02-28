package com.goaltracker.dto.request;

import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
    @Size(max = 100, message = "Görünen ad en fazla 100 karakter olabilir")
    String displayName,

    @Size(max = 50)
    String timezone,

    @Size(max = 500)
    String avatarUrl
) {}

