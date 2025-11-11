package com.example.bankcards.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Schema(description = "Запрос на обновление данных карты")
@Builder
public class UpdateCardRequest {

    @Schema(
            description = "Новый баланс карты",
            example = "1500.75"
    )
    private BigDecimal balance;

    @Schema(
            description = "Новый статус карты",
            example = "BLOCKED",
            allowableValues = {"ACTIVE", "BLOCKED", "EXPIRED"}
    )
    private String status;
}