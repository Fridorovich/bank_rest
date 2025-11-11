package com.example.bankcards.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class UpdateCardRequest {
    private BigDecimal balance;
    private String status;
}
