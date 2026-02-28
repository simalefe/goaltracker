package com.goaltracker.dto.request;

import com.goaltracker.validation.StrongPassword;
import jakarta.validation.constraints.NotBlank;

public record ResetPasswordRequest(
    @NotBlank(message = "Token boş olamaz")
    String token,

    @NotBlank(message = "Yeni şifre boş olamaz")
    @StrongPassword
    String newPassword
) {}

