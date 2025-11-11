package com.example.bankcards.dto;

import lombok.Builder;
import lombok.Data;

/**
 * DTO для запроса блокировки карты
 */
@Data
@Builder
public class BlockCardRequest {
    private String reason;
}
