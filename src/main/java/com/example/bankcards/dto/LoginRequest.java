package com.example.bankcards.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Запрос для аутентификации пользователя")
public class LoginRequest {

    @Schema(
            description = "Имя пользователя",
            example = "john_doe",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String username;

    @Schema(
            description = "Пароль пользователя",
            example = "password123",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String password;
}