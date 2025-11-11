package com.example.bankcards.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
public class CardDto {
    private Long id;
    private String number;
    private LocalDate expiryDate;
    private String status;
    private BigDecimal balance;
    private String maskedNumber;
    private Long userId;
    private String username;
}