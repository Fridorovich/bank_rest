package com.example.bankcards.controller;

import com.example.bankcards.dto.JwtDto;
import com.example.bankcards.dto.LoginRequest;
import com.example.bankcards.dto.RegisterRequest;
import com.example.bankcards.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Контроллер для аутентификации и регистрации пользователей
 * Предоставляет endpoints для входа в систему, регистрации и обновления токенов
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication Controller", description = "API для аутентификации и регистрации пользователей")
public class AuthController {
    private final AuthService authService;

    /**
     * Аутентификация пользователя в системе
     */
    @Operation(
            summary = "Аутентификация пользователя",
            description = "Проверяет учетные данные пользователя и возвращает JWT токены при успешной аутентификации"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Успешная аутентификация",
                    content = @Content(schema = @Schema(implementation = JwtDto.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Неверные учетные данные"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Некорректные данные запроса"
            )
    })
    @PostMapping("/login")
    public ResponseEntity<JwtDto> login(
            @Parameter(description = "Данные для входа в систему", required = true)
            @RequestBody LoginRequest loginRequest) {
        JwtDto jwtDto = authService.login(loginRequest);
        return ResponseEntity.ok(jwtDto);
    }

    /**
     * Обновление access token с помощью refresh token
     */
    @Operation(
            summary = "Обновление токена доступа",
            description = "Генерирует новый access token используя валидный refresh token"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Токен успешно обновлен",
                    content = @Content(schema = @Schema(implementation = JwtDto.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Некорректный заголовок Authorization или отсутствует refresh token"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Невалидный или просроченный refresh token"
            )
    })
    @PostMapping("/refresh")
    public ResponseEntity<JwtDto> refreshToken(
            @Parameter(
                    description = "Refresh token в формате 'Bearer {token}'",
                    required = true,
                    example = "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
            )
            @RequestHeader("Authorization") String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String refreshToken = authHeader.substring(7);
            JwtDto jwtDto = authService.refreshToken(refreshToken);
            return ResponseEntity.ok(jwtDto);
        }
        return ResponseEntity.badRequest().build();
    }

    /**
     * Регистрация нового пользователя
     */
    @Operation(
            summary = "Регистрация нового пользователя",
            description = "Создает новую учетную запись пользователя с ролью USER по умолчанию"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Пользователь успешно зарегистрирован",
                    content = @Content(schema = @Schema(implementation = String.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Пользователь с таким именем уже существует или пароли не совпадают"
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Внутренняя ошибка сервера при создании пользователя"
            )
    })
    @PostMapping("/register")
    public ResponseEntity<String> register(
            @Parameter(description = "Данные для регистрации нового пользователя", required = true)
            @RequestBody RegisterRequest registerRequest) {
        String result = authService.register(registerRequest);
        return ResponseEntity.ok(result);
    }
}