package com.goaltracker.dto.request;

import com.goaltracker.validation.StrongPassword;
import jakarta.validation.constraints.NotBlank;

public record ChangePasswordRequest(
    @NotBlank(message = "Mevcut şifre boş olamaz")
    String currentPassword,

    @NotBlank(message = "Yeni şifre boş olamaz")
    @StrongPassword
    String newPassword
) {}

