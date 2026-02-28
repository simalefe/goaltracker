package com.goaltracker.dto.request;

import com.goaltracker.validation.StrongPassword;
import jakarta.validation.constraints.*;

public record RegisterRequest(
    @NotBlank(message = "E-posta boş olamaz")
    @Email(message = "Geçerli bir e-posta adresi giriniz")
    @Size(max = 255)
    String email,

    @NotBlank(message = "Kullanıcı adı boş olamaz")
    @Size(min = 3, max = 30, message = "Kullanıcı adı 3-30 karakter arasında olmalıdır")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "Kullanıcı adı sadece harf, rakam ve alt çizgi içerebilir")
    String username,

    @NotBlank(message = "Şifre boş olamaz")
    @StrongPassword
    String password,

    @Size(max = 100, message = "Görünen ad en fazla 100 karakter olabilir")
    String displayName
) {}

