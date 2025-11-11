package com.example.bankcards.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO для статистики пользователя
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserStatsDto {
    private Long userId;
    private String username;
    private Long totalCards;
    private Long activeCards;
    private Long blockedCards;

    public UserStatsDto(Long userId, String username, Long totalCards, Long activeCards) {
        this.userId = userId;
        this.username = username;
        this.totalCards = totalCards;
        this.activeCards = activeCards;
        this.blockedCards = totalCards - activeCards;
    }
}
