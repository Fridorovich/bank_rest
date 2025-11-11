package com.example.bankcards.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;
import java.util.Set;

@Data
@Builder
@Schema(description = "Информация о пользователе")
public class UserDto {

    @Schema(description = "Идентификатор пользователя", example = "1")
    private Long id;

    @Schema(description = "Имя пользователя", example = "john_doe")
    private String username;

    @Schema(
            description = "Роли пользователя",
            example = "[\"USER\", \"ADMIN\"]",
            allowableValues = {"USER", "ADMIN"}
    )
    private Set<String> roles;

    @Schema(description = "Количество карт пользователя", example = "3")
    private int cardsCount;
}