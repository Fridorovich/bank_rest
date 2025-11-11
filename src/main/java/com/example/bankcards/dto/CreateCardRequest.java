package com.example.bankcards.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Schema(description = "Запрос на создание новой карты")
public class CreateCardRequest {

    @Schema(
            description = "Номер карты (16-19 цифр)",
            example = "1234567812345678",
            requiredMode = Schema.RequiredMode.REQUIRED,
            minLength = 16,
            maxLength = 19
    )
    private String number;

    @Schema(
            description = "Срок действия карты",
            example = "2025-12-31",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private LocalDate expiryDate;

    @Schema(
            description = "Начальный баланс карты",
            example = "0.00"
    )
    private BigDecimal initialBalance;

    @Schema(
            description = "Идентификатор пользователя-владельца карты",
            example = "1",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private Long userId;
}