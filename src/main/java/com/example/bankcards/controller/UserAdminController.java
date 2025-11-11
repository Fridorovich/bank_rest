package com.example.bankcards.controller;

import com.example.bankcards.dto.*;
import com.example.bankcards.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * Контроллер для управления пользователями (только для ADMIN)
 * Предоставляет полный CRUD функционал для работы с пользователями системы
 */
@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Validated
@Tag(name = "User Admin Controller", description = "API для управления пользователями системы администратором")
public class UserAdminController {
    private final UserService userService;

    /**
     * Получение пользователей с фильтрацией и пагинацией
     */
    @Operation(
            summary = "Фильтрация пользователей",
            description = "Возвращает список пользователей с поддержкой фильтрации по имени и роли, а также пагинации. Требуются права администратора."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Список пользователей успешно получен"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Некорректные параметры фильтрации"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Отсутствуют права администратора"
            )
    })
    @PostMapping("/filter")
    public ResponseEntity<PageResponse<UserDto>> getUsersFiltered(
            @Parameter(description = "Параметры фильтрации и пагинации", required = true)
            @RequestBody UserFilterRequest filter) {
        PageResponse<UserDto> users = userService.getAllUsersWithFilter(filter);
        return ResponseEntity.ok(users);
    }

    /**
     * Получение пользователя по идентификатору
     */
    @Operation(
            summary = "Получение пользователя по ID",
            description = "Возвращает детальную информацию о пользователе по его идентификатору. Требуются права администратора."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Пользователь успешно найден",
                    content = @Content(schema = @Schema(implementation = UserDto.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Пользователь с указанным ID не найден"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Отсутствуют права администратора"
            )
    })
    @GetMapping("/{userId}")
    public ResponseEntity<UserDto> getUser(
            @Parameter(description = "Идентификатор пользователя", required = true, example = "1")
            @PathVariable Long userId) {
        UserDto user = userService.getUserById(userId);
        return ResponseEntity.ok(user);
    }

    /**
     * Создание нового пользователя
     */
    @Operation(
            summary = "Создание пользователя",
            description = "Создает нового пользователя в системе с указанными ролями. Если роли не указаны, назначается роль USER по умолчанию. Требуются права администратора."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Пользователь успешно создан",
                    content = @Content(schema = @Schema(implementation = UserDto.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Пользователь с таким именем уже существует или некорректные данные"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Указанные роли не найдены в системе"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Отсутствуют права администратора"
            )
    })
    @PostMapping
    public ResponseEntity<UserDto> createUser(
            @Parameter(description = "Данные для создания пользователя", required = true)
            @Valid @RequestBody CreateUserRequest request) {
        UserDto user = userService.createUser(request);
        return ResponseEntity.ok(user);
    }

    /**
     * Обновление данных пользователя
     */
    @Operation(
            summary = "Обновление пользователя",
            description = "Обновляет данные пользователя: пароль и/или роли. Требуются права администратора."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Пользователь успешно обновлен",
                    content = @Content(schema = @Schema(implementation = UserDto.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Некорректные данные для обновления"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Пользователь с указанным ID не найден или указанные роли не существуют"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Отсутствуют права администратора"
            )
    })
    @PutMapping("/{userId}")
    public ResponseEntity<UserDto> updateUser(
            @Parameter(description = "Идентификатор пользователя", required = true, example = "1")
            @PathVariable Long userId,
            @Parameter(description = "Данные для обновления пользователя", required = true)
            @Valid @RequestBody UpdateUserRequest request) {
        UserDto user = userService.updateUser(userId, request);
        return ResponseEntity.ok(user);
    }

    /**
     * Удаление пользователя
     */
    @Operation(
            summary = "Удаление пользователя",
            description = "Удаляет пользователя из системы. Пользователь не может быть удален если у него есть активные карты. Требуются права администратора."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "204",
                    description = "Пользователь успешно удален"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Невозможно удалить пользователя с активными картами"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Пользователь с указанным ID не найден"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Отсутствуют права администратора"
            )
    })
    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> deleteUser(
            @Parameter(description = "Идентификатор пользователя", required = true, example = "1")
            @PathVariable Long userId) {
        userService.deleteUser(userId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Получение статистики пользователя
     */
    @Operation(
            summary = "Статистика пользователя",
            description = "Возвращает статистическую информацию о пользователе: количество карт, активных карт и т.д. Требуются права администратора."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Статистика успешно получена",
                    content = @Content(schema = @Schema(implementation = UserStatsDto.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Пользователь с указанным ID не найден"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Отсутствуют права администратора"
            )
    })
    @GetMapping("/{userId}/stats")
    public ResponseEntity<UserStatsDto> getUserStats(
            @Parameter(description = "Идентификатор пользователя", required = true, example = "1")
            @PathVariable Long userId) {
        UserStatsDto stats = userService.getUserStats(userId);
        return ResponseEntity.ok(stats);
    }

    /**
     * Получение всех пользователей
     */
    @Operation(
            summary = "Получение всех пользователей",
            description = "Возвращает список всех пользователей системы с поддержкой пагинации. Требуются права администратора."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Список пользователей успешно получен"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Отсутствуют права администратора"
            )
    })
    @GetMapping
    public ResponseEntity<PageResponse<UserDto>> getAllUsers(
            @Parameter(description = "Номер страницы", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Размер страницы", example = "10")
            @RequestParam(defaultValue = "10") int size) {
        UserFilterRequest filter = UserFilterRequest.builder()
                .page(page)
                .size(size)
                .build();
        return getUsersFiltered(filter);
    }
}