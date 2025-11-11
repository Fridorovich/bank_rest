package com.example.bankcards.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * DTO для запроса перевода между картами
 */
@Data
@Builder
public class TransferRequest {

    @NotNull(message = "Card from ID is required")
    private Long fromCardId;

    @NotNull(message = "Card to ID is required")
    private Long toCardId;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    private BigDecimal amount;

    private String description;
}
