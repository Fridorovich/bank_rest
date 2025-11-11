package com.example.bankcards.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.Set;

/**
 * DTO для отображения информации о пользователе
 */
@Data
@Builder
public class UserDto {
    private Long id;
    private String username;
    private Set<String> roles;
    private LocalDateTime createdAt;
    private int cardsCount;
}
