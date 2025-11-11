package com.example.bankcards.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class CreateCardRequest {
    private String number;
    private LocalDate expiryDate;
    private BigDecimal initialBalance;
    private Long userId;
}