package com.goaltracker.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record UpdateEntryRequest(
        @DecimalMin(value = "0.0", message = "Değer 0 veya daha büyük olmalıdır")
        BigDecimal actualValue,

        @Size(max = 500, message = "Not en fazla 500 karakter olabilir")
        String note
) {}

