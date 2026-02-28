package com.goaltracker.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateEntryRequest(
        @NotNull(message = "Tarih boş olamaz")
        LocalDate entryDate,

        @NotNull(message = "Değer boş olamaz")
        @DecimalMin(value = "0.0", message = "Değer 0 veya daha büyük olmalıdır")
        BigDecimal actualValue,

        @Size(max = 500, message = "Not en fazla 500 karakter olabilir")
        String note
) {}

