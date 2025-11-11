package com.example.bankcards.dto;

import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "Статистика пользователя")
public class UserStatsDto {
    @Schema(description = "Идентификатор пользователя", example = "1")
    private Long userId;

    @Schema(description = "Имя пользователя", example = "john_doe")
    private String username;

    @Schema(description = "Общее количество карт", example = "5")
    private Long totalCards;

    @Schema(description = "Количество активных карт", example = "3")
    private Long activeCards;

    @Schema(description = "Количество заблокированных карт", example = "1")
    private Long blockedCards;

    @Schema(description = "Общий баланс всех карт", example = "12500.75")
    private BigDecimal totalBalance;

    public UserStatsDto(Long userId, String username, Long totalCards, Long activeCards) {
        this.userId = userId;
        this.username = username;
        this.totalCards = totalCards;
        this.activeCards = activeCards;
        this.blockedCards = totalCards - activeCards;
    }
}
