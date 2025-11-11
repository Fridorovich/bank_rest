package com.example.bankcards.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Запрос для регистрации нового пользователя")
public class RegisterRequest {

    @Schema(
            description = "Имя пользователя (должно быть уникальным)",
            example = "new_user",
            requiredMode = Schema.RequiredMode.REQUIRED,
            minLength = 3,
            maxLength = 50
    )
    private String username;

    @Schema(
            description = "Пароль пользователя",
            example = "securePassword123",
            requiredMode = Schema.RequiredMode.REQUIRED,
            minLength = 6
    )
    private String password;

    @Schema(
            description = "Подтверждение пароля",
            example = "securePassword123",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String confirmPassword;
}