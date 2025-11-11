package com.example.bankcards.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO для ответа после перевода
 */
@Data
@Builder
public class TransferResponse {
    private Long transactionId;
    private String fromCardMasked;
    private String toCardMasked;
    private BigDecimal amount;
    private String description;
    private LocalDateTime timestamp;
    private String status;
}
