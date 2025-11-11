package com.example.bankcards.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Schema(description = "Информация о банковской карте")
@Builder
public class CardDto {

    @Schema(description = "Идентификатор карты", example = "1")
    private Long id;

    @Schema(description = "Номер карты", example = "1234567812345678")
    private String number;

    @Schema(description = "Маскированный номер карты", example = "**** **** **** 5678")
    private String maskedNumber;

    @Schema(description = "Срок действия карты", example = "2025-12-31")
    private LocalDate expiryDate;

    @Schema(
            description = "Статус карты",
            example = "ACTIVE",
            allowableValues = {"ACTIVE", "BLOCKED", "EXPIRED"}
    )
    private String status;

    @Schema(description = "Баланс карты", example = "1000.50")
    private BigDecimal balance;

    @Schema(description = "Идентификатор владельца карты", example = "1")
    private Long userId;

    @Schema(description = "Имя пользователя владельца карты", example = "john_doe")
    private String username;
}